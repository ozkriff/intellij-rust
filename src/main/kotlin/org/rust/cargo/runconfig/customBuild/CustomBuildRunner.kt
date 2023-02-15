/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.cargo.runconfig.CargoRunStateBase
import org.rust.cargo.runconfig.RsExecutableRunner
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildToolWindowAvailable
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.toolchain.impl.CargoMetadata
import org.rust.cargo.util.CargoArgsParser
import org.rust.stdext.toPath

// TODO: clean up imports after refactoring `doExecute`

private const val ERROR_MESSAGE_TITLE: String = "Unable to run Custom Build Script"

class CustomBuildRunner : RsExecutableRunner(DefaultRunExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {
    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != DefaultRunExecutor.EXECUTOR_ID ||
            profile !is CustomBuildCommandConfiguration ||
            profile.clean() !is CargoCommandConfiguration.CleanConfiguration.Ok) return false
        return profile.isBuildToolWindowAvailable
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {

        // TODO: handle err better than just `!!`
        val artifacts = environment.artifacts.orEmpty()
        val artifactMessages = artifacts.find { message -> message.target.cleanKind == CargoMetadata.TargetKind.CUSTOM_BUILD }!! // TODO: find actual package I need
        val exePath = artifactMessages.filenames[0]

        val outDir = environment.project.basePath + "/target/pseudoOutDir"

        // --------------------------

        // return super.doExecute(state, environment)

        // ------------------------------

        if (state !is CargoRunStateBase) return null

        // val artifacts = environment.artifacts.orEmpty()
        val artifact = artifacts.firstOrNull()
        // val binaries = artifact?.executables.orEmpty()

        // @Suppress("UnstableApiUsage")
        // @NlsContexts.DialogMessage
        // fun checkErrors(items: List<Any>, itemName: String): String? = when {
        //     items.isEmpty() -> "Can't find a $itemName."
        //     items.size > 1 -> "More than one $itemName was produced. " +
        //         "Please specify `--bin`, `--lib`, `--test` or `--example` flag explicitly."
        //     else -> null
        // }

        // val errorMessage = checkErrors(artifacts, "artifact") ?: checkErrors(binaries, "binary")
        // if (errorMessage != null) {
        //     environment.project.showErrorDialog(errorMessage)
        //     return null
        // }

        val pkg = artifact?.package_id?.let { id ->
            environment.project.cargoProjects.allProjects
                .mapNotNull { it.workspace?.findPackageById(id) }
                .firstOrNull { it.origin == PackageOrigin.WORKSPACE }
        }

        val runCargoCommand = state.prepareCommandLine().copy(emulateTerminal = false)
        val workingDirectory = pkg?.rootDirectory
            ?.takeIf { runCargoCommand.command == "test" }
            ?: runCargoCommand.workingDirectory
        val environmentVariables = runCargoCommand.environmentVariables.run { with(envs + pkg?.env.orEmpty()) }
        val (_, executableArguments) = CargoArgsParser.parseArgs(runCargoCommand.command, runCargoCommand.additionalArguments)
        val runExecutable = state.toolchain.createGeneralCommandLine(

            // binaries.single().toPath(),
            exePath.toPath(), // TODO: ugh

            workingDirectory,
            runCargoCommand.redirectInputFrom,
            runCargoCommand.backtraceMode,
            environmentVariables,
            executableArguments,
            runCargoCommand.emulateTerminal,
            runCargoCommand.withSudo,
            patchToRemote = false // patching is performed for debugger/profiler/valgrind on CLion side if needed
        ).withEnvironment("OUT_DIR", outDir) // TODO: ugh, just slapping this on here for now

        return showRunContent(state, environment, runExecutable)
    }

    companion object {
        const val RUNNER_ID = "CustomBuildRunner"
    }
}
