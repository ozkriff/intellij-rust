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
import org.rust.cargo.runconfig.RsExecutableRunner
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.getBuildConfiguration
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildConfiguration
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.isBuildToolWindowAvailable

private const val ERROR_MESSAGE_TITLE: String = "Unable to run Custom Build Script"

class CustomBuildRunner : RsExecutableRunner(DefaultRunExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {
    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        // TODO: uh, do I need these kind of checks here?
        // if (executorId != this.runnerId || profile !is BuildScriptCommandConfiguration ||
        //     profile.clean() !is CargoCommandConfiguration.CleanConfiguration.Ok) return false

        if (executorId != DefaultRunExecutor.EXECUTOR_ID || profile !is CustomBuildCommandConfiguration) return false

        // TODO: only allow Run conf for now

        return profile.isBuildToolWindowAvailable &&
            !isBuildConfiguration(profile) &&
            getBuildConfiguration(profile) != null
    }

    // TODO: Do I need to override execute or doExecute?

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        // TODO: hack what we're actually executing here according to my needs
        return super.doExecute(state, environment)
    }

    companion object {
        const val RUNNER_ID = "CustomBuildRunner"
    }
}
