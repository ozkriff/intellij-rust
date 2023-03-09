/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import org.rust.RsBundle
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.CargoRunStateBase
import org.rust.cargo.runconfig.RsExecutableRunner
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildToolWindowAvailable
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.impl.CargoMetadata
import org.rust.cargo.toolchain.impl.CompilerArtifactMessage
import org.rust.openapiext.pathAsPath

private val ERROR_MESSAGE_TITLE = RsBundle.message("run.config.rust.custom.build.runner.error.title")

open class CustomBuildRunner(
    executorId: String = DefaultRunExecutor.EXECUTOR_ID,
    errorMessageTitle: String = ERROR_MESSAGE_TITLE
) : RsExecutableRunner(executorId, errorMessageTitle) {
    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != this.executorId ||
            profile !is CustomBuildConfiguration ||
            profile.clean() !is CargoCommandConfiguration.CleanConfiguration.Ok) return false
        return profile.isBuildToolWindowAvailable
    }

    override fun getAdditionalEnvVars(
        state: CargoRunStateBase,
        pkg: CargoWorkspace.Package?
    ): Map<String, String> {
        val outDir = getOutDir(state, pkg) ?: return mapOf()
        assert(outDir.isNotEmpty())
        return mapOf("OUT_DIR" to outDir)
    }

    private fun getOutDir(state: CargoRunStateBase, pkg: CargoWorkspace.Package?): String? {
        if (state.runConfiguration !is CustomBuildConfiguration) return null
        return if (state.runConfiguration.isCustomOutDir) state.runConfiguration.customOutDir.toString()
            else pkg?.outDir?.pathAsPath?.toString()
            ?: (state.environment.project.basePath + "/target/pseudoOutDir") // TODO: ugh?
    }

    override fun getArtifacts(state: CargoRunStateBase): List<CompilerArtifactMessage> {
        if (state.runConfiguration !is CustomBuildConfiguration) {
            return listOf()
        }
        val crateRootUrl = state.runConfiguration.crateRootUrl
        return state.environment.artifacts.orEmpty().filter { message ->
            message.target.cleanKind == CargoMetadata.TargetKind.CUSTOM_BUILD
                && "file://" + message.target.src_path == crateRootUrl // TODO: better way to get URL?
        }
    }

    // TODO: shouldn't be needed after https://github.com/intellij-rust/intellij-rust/issues/9778 is fixed
    override fun modifyFinalCommand(state: CargoRunStateBase): CargoCommandLine {
        return super.modifyFinalCommand(state).copy(emulateTerminal = true)
    }

    companion object {
        const val RUNNER_ID = "CustomBuildRunner"
    }
}
