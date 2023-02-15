/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

// import org.rust.cargo.runconfig.CargoAwareConfiguration
// import org.rust.cargo.runconfig.ParsedCommand
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

// TODO: rename: no need for "Command" part anymore?
class CustomBuildCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoCommandConfiguration(project, name, factory) {
    override var command: String = "run" // TODO: it's a hack. not sure if a goood idea

    // var outDir: String = project.basePath + "/target/pseudoOutDir" // TODO: un-hack
    var outDir: String? = null // TODO: let's try keeping it empty by default and only assign an actual value when used

    // TODO: just a test
    override fun setBeforeRunTasks(value: MutableList<BeforeRunTask<*>>) {
        super.setBeforeRunTasks(value)
    }

//    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
////        val wasmPack = environment.project.toolchain?.wasmPack() ?: return null
////        val workingDirectory = workingDirectory?.toFile() ?: return null
////        return WasmPackCommandRunState(environment, this, wasmPack, workingDirectory)
//    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        CustomBuildConfigurationEditor(project)

    // TODO: this was copy-pasted here from CargoCommandConfiguration. needs to be de-duplicated later.
    // TODO: We need to store package or build.rs file so it'd be possible to identify our thing.
    // fun canBeFrom(cmd: CargoCommandLine): Boolean =
    fun canBeFrom(/*TODO: add some arg!*/): Boolean =
        // TODO: lets try always returning true for now.
        //       needs to be fixed later, something should to be checked (package? target?)

        // command == ParametersListUtil.join(cmd.command, *cmd.additionalArguments.toTypedArray())

        true

}
