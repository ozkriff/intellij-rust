/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Key
import com.intellij.task.ProjectTaskManager
import org.rust.cargo.runconfig.CargoAwareConfiguration
import org.rust.cargo.runconfig.buildtool.CargoBuildManager.createBuildEnvironment
import java.util.concurrent.CompletableFuture
import javax.swing.Icon

abstract class RsBuildTaskProvider<T : RsBuildTaskProvider.BuildTask<T>> : BeforeRunTaskProvider<T>() {
    override fun getName(): String = "Build"
    override fun getIcon(): Icon = AllIcons.Actions.Compile
    override fun isSingleton(): Boolean = true

    // TODO: CargoCommandConfiguration? or more abstract thing?
    protected fun doExecuteTask(buildConfiguration: CargoAwareConfiguration, environment: ExecutionEnvironment): Boolean {
        val buildEnvironment = createBuildEnvironment(buildConfiguration, environment) ?: return false
        val buildableElement = CargoBuildConfiguration(buildConfiguration, buildEnvironment)

        val result = CompletableFuture<Boolean>()
        ProjectTaskManager.getInstance(environment.project).build(buildableElement).onProcessed {
            result.complete(!it.hasErrors() && !it.isAborted)
        }
        return result.get()
    }

    // // TODO: merge with doExecuteTask later
    // protected fun doExecuteTask2(buildConfiguration: RsCommandConfiguration, environment: ExecutionEnvironment): Boolean {
    //     val buildEnvironment = createBuildEnvironment(buildConfiguration, environment) ?: return false
    //     val buildableElement = CargoBuildConfiguration(buildConfiguration, buildEnvironment)
    //
    //     val result = CompletableFuture<Boolean>()
    //     ProjectTaskManager.getInstance(environment.project).build(buildableElement).onProcessed {
    //         result.complete(!it.hasErrors() && !it.isAborted)
    //     }
    //     return result.get()
    // }

    abstract class BuildTask<T : BuildTask<T>>(providerId: Key<T>) : BeforeRunTask<T>(providerId) {
        init {
            isEnabled = true
        }
    }
}
