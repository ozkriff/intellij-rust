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
import org.rust.cargo.project.settings.toolchain
import org.rust.cargo.runconfig.CargoAwareConfiguration
import org.rust.cargo.runconfig.ParsedCommand
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.RustChannel
import org.rust.cargo.toolchain.tools.isRustupAvailable
import java.io.File

// TODO: проверить в нормальных раннерах, что конфиги точно один объект
//       при изменениях в UI и самом раннере. а то вдруг там они тоже разные

// TODO: rename: no need for "Command" part anymore?
// TODO: move it to some other place!
// TODO: это выглядит подходящим местом что бы разрешить пользователю менять (псевдо~)OUT_DIR
class CustomBuildCommandConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoAwareConfiguration(project, name, factory) {
    override var command: String = "build" // TODO: shouldn't assume build at all, right? or?

    // var outDir: String = project.basePath + "/target/pseudoOutDir" // TODO: un-hack
    var outDir: String? = null // TODO: let's try keeping it empty by default and only assign an actual value when used

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {

        // val workingDirectory = workingDirectory?.toFile() ?: return null
        return CustomBuildCommandState(environment, this)
    }

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
    // fun canBeFrom(cmd: CargoCommandLine): Boolean =
    fun canBeFrom(/*TODO: add some arg!*/): Boolean =
        // TODO: lets try always returning true for now.
        //       needs to be fixed later, something should to be checked (package? target?)
        true
        // command == ParametersListUtil.join(cmd.command, *cmd.additionalArguments.toTypedArray())

    // TODO: implement writeExternal, readExternal later!
    //       https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html#persistence

    // TODO: uh? code duplication? or how should this be done without a fake CargoCommandLine?
    override fun clean(): CleanConfiguration {
        val workingDirectory = workingDirectory
            ?: return CleanConfiguration.error("No working directory specified")

        // val cmd = null // TODO: tmp thing

        // TODO: is it a good idea? seems to kinda work
        val cmd = run {
            // val command = "build" // TODO: just an experiment. what if i make a command out of this?
            val parsed = ParsedCommand.parse(command)
                ?: return CleanConfiguration.error("No command specified")

            val redirectInputFile: File? = null // TODO: hack

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
