/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import org.rust.cargo.runconfig.CargoAwareConfiguration
import org.rust.cargo.toolchain.CargoCommandLine

// TODO: rename: no need for "Command" part anymore?
// TODO: move it to some other place!
// TODO: это выглядит подходящим местом что бы разрешить пользователю менять (псевдо~)OUT_DIR
class CustomBuildCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoAwareConfiguration(project, name, factory) {
    override var command: String = "build" // TODO: sholdn't assume build at all.

    var outDir: String = project.basePath + "/target/pseudoOutDir" // TODO: un-hack

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {

        // val workingDirectory = workingDirectory?.toFile() ?: return null
        return CustomBuildCommandState(environment, this)
    }

    // TODO: just a test. remove later.
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
    fun canBeFrom(cmd: CargoCommandLine): Boolean =
        command == ParametersListUtil.join(cmd.command, *cmd.additionalArguments.toTypedArray())

    // TODO: implement writeExternal, readExternal later!
    //       https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html#persistence

    // TODO: uh?
    override fun clean(): CargoAwareConfiguration.CleanConfiguration {
        val workingDirectory = workingDirectory
            ?: return CargoAwareConfiguration.CleanConfiguration.error("No working directory specified")

        return CargoAwareConfiguration.CleanConfiguration.error("I have no idea what's going on for now. just testing") // TODO: just a tmp test by ozkriff


        //
        // val cmd = run {
        //     val parsed = ParsedCommand.parse(command)
        //         ?: return CargoCommandConfiguration.CleanConfiguration.error("No command specified")
        //
        //     CargoCommandLine(
        //         parsed.command,
        //         workingDirectory,
        //         parsed.additionalArguments,
        //         redirectInputFile,
        //         backtrace,
        //         parsed.toolchain,
        //         channel,
        //         env,
        //         requiredFeatures,
        //         allFeatures,
        //         emulateTerminal,
        //         withSudo
        //     )
        // }
        //
        // val toolchain = project.toolchain
        //     ?: return CargoCommandConfiguration.CleanConfiguration.error("No Rust toolchain specified")
        //
        // if (!toolchain.looksLikeValidToolchain()) {
        //     return CargoCommandConfiguration.CleanConfiguration.error("Invalid toolchain: ${toolchain.presentableLocation}")
        // }
        //
        // if (!toolchain.isRustupAvailable && channel != RustChannel.DEFAULT) {
        //     return CargoCommandConfiguration.CleanConfiguration.error("Channel '$channel' is set explicitly with no rustup available")
        // }

        // return CargoCommandConfiguration.CleanConfiguration.Ok(cmd, toolchain)
    }


//    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
//        val wasmPack = environment.project.toolchain?.wasmPack() ?: return null
//        val workingDirectory = workingDirectory?.toFile() ?: return null
//        return WasmPackCommandRunState(environment, this, wasmPack, workingDirectory)
//    }
//
//    override fun writeExternal(element: Element) {
//        super.writeExternal(element)
//        element.writeString("command", command)
//        element.writePath("workingDirectory", workingDirectory)
//    }
//
//    override fun readExternal(element: Element) {
//        super.readExternal(element)
//        element.readString("command")?.let { command = it }
//        element.readPath("workingDirectory")?.let { workingDirectory = it }
//    }
//
//    override fun suggestedName(): String = command.substringBefore(' ').capitalize()
//
//    fun setFromCmd(cmd: WasmPackCommandLine) {
//        command = ParametersListUtil.join(cmd.command, *cmd.additionalArguments.toTypedArray())
//        workingDirectory = cmd.workingDirectory
//    }
}
