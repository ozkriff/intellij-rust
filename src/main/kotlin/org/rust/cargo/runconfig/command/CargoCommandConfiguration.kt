/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.execution.ParametersListUtil
import org.rust.RsBundle
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.toolchain
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.cargo.runconfig.*
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties
import org.rust.cargo.runconfig.ui.CargoCommandConfigurationEditor
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.RustChannel
import org.rust.cargo.toolchain.tools.Cargo
import org.rust.cargo.toolchain.tools.isRustupAvailable
import org.rust.ide.experiments.RsExperiments
import org.rust.openapiext.isFeatureEnabled
import org.rust.openapiext.isUnitTestMode
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

// TODO: uh? remove this? ooor?
val CargoCommandConfiguration.hasRemoteTarget: Boolean
    get() = (this as CargoAwareConfiguration).hasRemoteTarget

//val CargoCommandConfiguration.hasRemoteTarget: Boolean
//    get() = defaultTargetName != null

// TODO: update the description late
/**
 * This class describes a Run Configuration.
 * It is basically a bunch of values which are persisted to .xml files inside .idea,
 * or displayed in the GUI form. It has to be mutable to satisfy various IDE's APIs.
 *
 * Class is open not to break [EduTools](https://plugins.jetbrains.com/plugin/10081-edutools) plugin
 */
open class CargoCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoAwareConfiguration(project, name, factory) {
    override var command: String = "run"

    private val redirectInputFile: File?
        get() {
            if (!isRedirectInput) return null
            if (redirectInputPath?.isNotEmpty() != true) return null
            val redirectInputPath = FileUtil.toSystemDependentName(ProgramParametersUtil.expandPathAndMacros(redirectInputPath, null, project))
            var file = File(redirectInputPath)
            if (!file.isAbsolute && workingDirectory != null) {
                file = File(File(workingDirectory.toString()), redirectInputPath)
            }
            return file
        }

    fun setFromCmd(cmd: CargoCommandLine) {
        channel = cmd.channel
        command = cmd.toRawCommand()
        requiredFeatures = cmd.requiredFeatures
        allFeatures = cmd.allFeatures
        emulateTerminal = cmd.emulateTerminal
        withSudo = cmd.withSudo
        backtrace = cmd.backtraceMode
        workingDirectory = cmd.workingDirectory
        env = cmd.environmentVariables
        isRedirectInput = cmd.redirectInputFrom != null
        redirectInputPath = cmd.redirectInputFrom?.path
    }

    // NOTE: code duplication? extract it somewhere?
    fun canBeFrom(cmd: CargoCommandLine): Boolean =
        command == cmd.toRawCommand()

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        if (isRedirectInput) {
            val file = redirectInputFile
            when {
                file?.exists() != true -> throw RuntimeConfigurationWarning("Input file doesn't exist")
                !file.isFile -> throw RuntimeConfigurationWarning("Input file is not valid")
            }
        }

        val config = clean()
        if (config is CleanConfiguration.Err) throw config.error
        config as CleanConfiguration.Ok

        // TODO: remove when `com.intellij.execution.process.ElevationService` supports error stream redirection
        // https://github.com/intellij-rust/intellij-rust/issues/7320
        if (withSudo && showTestToolWindow(config.getMeCmd())) {
            val message = if (SystemInfo.isWindows) {
                RsBundle.message("notification.run.tests.as.root.windows")
            } else {
                RsBundle.message("notification.run.tests.as.root.unix")
            }
            throw RuntimeConfigurationWarning(message)
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        CargoCommandConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val config = clean().ok ?: return null
        return if (showTestToolWindow(config.getMeCmd())) {
            CargoTestRunState(environment, this, config)
        } else {
            CargoRunState(environment, this, config)
        }
    }

    private fun showTestToolWindow(commandLine: CargoCommandLine): Boolean = when {
        !isFeatureEnabled(RsExperiments.TEST_TOOL_WINDOW) -> false
        commandLine.command != "test" -> false
        "--nocapture" in commandLine.additionalArguments -> false
        Cargo.TEST_NOCAPTURE_ENABLED_KEY.asBoolean() -> false
        else -> !hasRemoteTarget
    }

    override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties? {
        val config = clean().ok ?: return null
        return if (showTestToolWindow(config.getMeCmd())) {
            CargoTestConsoleProperties(this, executor)
        } else {
            null
        }
    }

    // TODO: extract this?
    override fun clean(): CleanConfiguration {
        val workingDirectory = workingDirectory
            ?: return CleanConfiguration.error("No working directory specified")

        val cmd = run {
            val parsed = ParsedCommand.parse(command)
                ?: return CleanConfiguration.error("No command specified")

            CargoCommandLine(
                parsed.command,
                workingDirectory,
                parsed.additionalArguments,
                redirectInputFile,
                backtrace,
                parsed.toolchain,
                channel,
                env,
                requiredFeatures,
                allFeatures,
                emulateTerminal,
                withSudo
            )
        }

        val toolchain = project.toolchain
            ?: return CleanConfiguration.error("No Rust toolchain specified")

        if (!toolchain.looksLikeValidToolchain()) {
            return CleanConfiguration.error("Invalid toolchain: ${toolchain.presentableLocation}")
        }

        if (!toolchain.isRustupAvailable && channel != RustChannel.DEFAULT) {
            return CleanConfiguration.error("Channel '$channel' is set explicitly with no rustup available")
        }

        return CleanConfiguration.Ok(cmd, toolchain)
    }

    private fun CargoCommandLine.toRawCommand(): String {
        val toolchainOverride = toolchain?.let { "+$it" }
        return ParametersListUtil.join(listOfNotNull(toolchainOverride, command, *additionalArguments.toTypedArray()))
    }

    companion object {
        fun findCargoProject(project: Project, additionalArgs: List<String>, workingDirectory: Path?): CargoProject? {
            val cargoProjects = project.cargoProjects
            cargoProjects.allProjects.singleOrNull()?.let { return it }

            val manifestPath = run {
                val idx = additionalArgs.indexOf("--manifest-path")
                if (idx == -1) return@run null
                additionalArgs.getOrNull(idx + 1)?.let { Paths.get(it) }
            }

            for (dir in listOfNotNull(manifestPath?.parent, workingDirectory)) {
                LocalFileSystem.getInstance().findFileByIoFile(dir.toFile())
                    ?.let { cargoProjects.findProjectForFile(it) }
                    ?.let { return it }
            }
            return null
        }

        fun findCargoProject(project: Project, cmd: String, workingDirectory: Path?): CargoProject? = findCargoProject(
            project, ParametersListUtil.parse(cmd), workingDirectory
        )

        fun findCargoPackage(
            cargoProject: CargoProject,
            additionalArgs: List<String>,
            workingDirectory: Path?
        ): CargoWorkspace.Package? {
            val packages = cargoProject.workspace?.packages
                ?.filter { it.origin == PackageOrigin.WORKSPACE }
                .orEmpty()

            packages.singleOrNull()?.let { return it }

            val packageName = run {
                val idx = additionalArgs.indexOf("--package")
                if (idx == -1) return@run null
                additionalArgs.getOrNull(idx + 1)
            }

            if (packageName != null) {
                return packages.find { it.name == packageName }
            }

            return packages.find { it.rootDirectory == workingDirectory }
        }

        fun findCargoTargets(
            cargoPackage: CargoWorkspace.Package,
            additionalArgs: List<String>
        ): List<CargoWorkspace.Target> {

            fun hasTarget(option: String, name: String): Boolean {
                if ("$option=$name" in additionalArgs) return true
                return additionalArgs.windowed(2).any { pair ->
                    pair.first() == option && pair.last() == name
                }
            }

            return cargoPackage.targets.filter { target ->
                when (target.kind) {
                    CargoWorkspace.TargetKind.Bin -> hasTarget("--bin", target.name)
                    CargoWorkspace.TargetKind.Test -> hasTarget("--test", target.name)
                    CargoWorkspace.TargetKind.ExampleBin,
                    is CargoWorkspace.TargetKind.ExampleLib -> hasTarget("--example", target.name)
                    CargoWorkspace.TargetKind.Bench -> hasTarget("--bench", target.name)
                    is CargoWorkspace.TargetKind.Lib -> "--lib" in additionalArgs
                    else -> false
                }
            }
        }

        val emulateTerminalDefault: Boolean
            get() = isFeatureEnabled(RsExperiments.EMULATE_TERMINAL) && !isUnitTestMode
    }
}

// TODO: move to [CargoAwareConfiguration]?
val CargoProject.workingDirectory: Path get() = manifest.parent
