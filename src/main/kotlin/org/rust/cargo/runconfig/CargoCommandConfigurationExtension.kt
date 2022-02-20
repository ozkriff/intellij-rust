/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.UserDataHolderBase

class ConfigurationExtensionContext : UserDataHolderBase()

// TODO: maybe even rename?.. it's not ;inked to CargoCommand anymore?
abstract class CargoCommandConfigurationExtension : RunConfigurationExtensionBase<CargoAwareConfiguration>() {

    abstract fun attachToProcess(
        configuration: CargoAwareConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    )

    abstract fun patchCommandLine(
        configuration: CargoAwareConfiguration,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    )

    open fun patchCommandLineState(
        configuration: CargoAwareConfiguration,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
    }

    override fun patchCommandLine(
        configuration: CargoAwareConfiguration,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String
    ) {
        LOG.error("use the other overload of 'patchCommandLine' method")
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<CargoCommandConfigurationExtension>("org.rust.runConfigurationExtension")

        private val LOG: Logger = logger<CargoCommandConfigurationExtension>()
    }
}
