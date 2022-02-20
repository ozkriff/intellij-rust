/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.util.execution.ParametersListUtil
import org.rust.cargo.runconfig.buildtool.RsBuildTaskProvider
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType
import org.rust.cargo.toolchain.tools.Rustup
import org.rust.openapiext.project

// создать класс аналог CargoBuildTaskProvider и там все добавлять свое
// учесть, что где-то в кишках может быть явная проверка,
// что конфигурация это CargoBuildConfiguration

class CustomBuildBuildTaskProvider : RsBuildTaskProvider<CustomBuildBuildTaskProvider.BuildTask>() {
    override fun getId(): Key<BuildTask> = ID

    override fun createTask(runConfiguration: RunConfiguration): BuildTask? =
        if (runConfiguration is CustomBuildCommandConfiguration) BuildTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: BuildTask
    ): Boolean {
        if (configuration !is CustomBuildCommandConfiguration) return false
        // val buildConfiguration = getBuildConfiguration(configuration) ?: return true

        // val project = context.project ?: return false

        // val buildConfiguration = CargoCommandConfiguration(
        //     project, configuration.name, CargoCommandConfigurationType.getInstance().factory
        // ).apply {
        //     command = "build" // TODO:?
        //     workingDirectory = configuration.workingDirectory
        // }

        // val buildConfiguration = CargoCommandConfiguration(
        //     project, configuration.name, CargoCommandConfigurationType.getInstance().factory
        // ).apply {
        //     command = "build" // TODO:?
        //     workingDirectory = configuration.workingDirectory
        // }

        // // TODO: do i actually need all this here?
        val projectDirectory = configuration.workingDirectory ?: return false
        val configArgs = ParametersListUtil.parse(configuration.command)
        val targetFlagIdx = configArgs.indexOf("--target")
        val targetTriple = if (targetFlagIdx != -1) configArgs.getOrNull(targetFlagIdx + 1).orEmpty() else ""

        if (Rustup.checkNeedInstallTarget(configuration.project, projectDirectory, targetTriple)) return false

        // TODO: just pass my own configuration here. the build isn't required at ll
        return doExecuteTask(configuration, environment)
    }

    class BuildTask : RsBuildTaskProvider.BuildTask<BuildTask>(ID)

    companion object {
        @JvmField
        val ID: Key<BuildTask> = Key.create("CUSTOM_BUILD.BUILD_TASK_PROVIDER")
    }
}
