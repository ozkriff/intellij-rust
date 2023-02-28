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
import org.rust.cargo.runconfig.CargoRunStateBase
import org.rust.cargo.runconfig.RsExecutableRunner
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildToolWindowAvailable
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.toolchain.impl.CargoMetadata.TargetKind.CUSTOM_BUILD
import org.rust.cargo.util.CargoArgsParser
import org.rust.openapiext.pathAsPath
import org.rust.stdext.toPath

// TODO: clean up imports after refactoring `doExecute`

private const val ERROR_MESSAGE_TITLE: String = "Unable to run Custom Build Script"

class CustomBuildRunner : RsExecutableRunner(DefaultRunExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {
    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != DefaultRunExecutor.EXECUTOR_ID ||
            profile !is CustomBuildConfiguration ||
            profile.clean() !is CargoCommandConfiguration.CleanConfiguration.Ok) return false
        return profile.isBuildToolWindowAvailable
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CargoRunStateBase) return null
        if (state.runConfiguration !is CustomBuildConfiguration) return null

        val crateRootUrl = state.runConfiguration.crateRootUrl

        // TODO: move this logic out of here, artifacts need to be filtered before doExecute.
        val artifacts = environment.artifacts.orEmpty()
        val artifactMessages = artifacts.find { message ->
            val isCustomBuild = message.target.cleanKind == CUSTOM_BUILD
            val isCorrectCrate = "file://" + message.target.src_path == crateRootUrl
            isCustomBuild && isCorrectCrate
        }

        if (artifactMessages == null || artifactMessages.filenames.size != 1) {
            // TODO: better msg + translation
            environment.project.showErrorDialog("Error: no corresponding build script artifact found")
            return null
        }
        val exePath = artifactMessages.filenames[0]

        // --------------------------

        // return super.doExecute(state, environment)

        // ------------------------------

        // val artifacts = environment.artifacts.orEmpty()
        val artifact = artifacts.firstOrNull()
        // val binaries = artifact?.executables.orEmpty()

        val pkg = findPackage(artifact, environment)

        val outDir = state.runConfiguration.outDir
            ?: pkg?.outDir?.pathAsPath?.toString()
            ?: (environment.project.basePath + "/target/pseudoOutDir") // TODO: handle properly

        // val runCargoCommand = state.prepareCommandLine().copy(emulateTerminal = false)
        val runCargoCommand = state.prepareCommandLine().copy(emulateTerminal = true) // TODO: ugh

        val workingDirectory = pkg?.rootDirectory
            ?.takeIf { runCargoCommand.command == "test" }
            ?: runCargoCommand.workingDirectory
        val environmentVariables = runCargoCommand.environmentVariables.run { with(envs + pkg?.env.orEmpty()) }
            .with(mapOf("OUT_DIR" to outDir)) // TODO: ugh
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
        )

        return showRunContent(state, environment, runExecutable)
    }

    companion object {
        const val RUNNER_ID = "CustomBuildRunner"
    }
}
