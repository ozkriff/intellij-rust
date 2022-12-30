/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig

import com.intellij.execution.InputRedirectAware
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.testframework.actions.ConsolePropertiesProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.target.BuildTarget
import org.rust.cargo.runconfig.target.RsLanguageRuntimeConfiguration
import org.rust.cargo.runconfig.target.RsLanguageRuntimeType
import org.rust.cargo.toolchain.BacktraceMode
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RustChannel

val CargoAwareConfiguration.hasRemoteTarget: Boolean
    get() = defaultTargetName != null

// TODO: Move to some other module probably
abstract class CargoAwareConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : RsCommandConfiguration(project, name, factory),
    InputRedirectAware.InputRedirectOptions,
    ConsolePropertiesProvider,
    TargetEnvironmentAwareRunProfile
{
    var channel: RustChannel = RustChannel.DEFAULT
    var requiredFeatures: Boolean = true
    var allFeatures: Boolean = false
    var emulateTerminal: Boolean = CargoCommandConfiguration.emulateTerminalDefault // TODO: !!!
    var withSudo: Boolean = false
    var buildTarget: BuildTarget = BuildTarget.REMOTE
    var backtrace: BacktraceMode = BacktraceMode.SHORT
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT


    // TODO: protected or private?
    private var isRedirectInput: Boolean = false
    private var redirectInputPath: String? = null

    // TODO: move


    override fun isRedirectInput(): Boolean = isRedirectInput

    override fun setRedirectInput(value: Boolean) {
        isRedirectInput = value
    }

    override fun getRedirectInputPath(): String? = redirectInputPath

    override fun setRedirectInputPath(value: String?) {
        redirectInputPath = value
    }

    override fun canRunOn(target: TargetEnvironmentConfiguration): Boolean {
        return target.runtimes.findByType(RsLanguageRuntimeConfiguration::class.java) != null
    }

    override fun getDefaultLanguageRuntimeType(): LanguageRuntimeType<*>? {
        return LanguageRuntimeType.EXTENSION_NAME.findExtension(RsLanguageRuntimeType::class.java)
    }

    override fun getDefaultTargetName(): String? {
        return options.remoteTarget
    }

    override fun setDefaultTargetName(targetName: String?) {
        options.remoteTarget = targetName
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeEnum("channel", channel)
        element.writeBool("requiredFeatures", requiredFeatures)
        element.writeBool("allFeatures", allFeatures)
        element.writeBool("emulateTerminal", emulateTerminal)
        element.writeBool("withSudo", withSudo)
        element.writeEnum("buildTarget", buildTarget)
        element.writeEnum("backtrace", backtrace)
        env.writeExternal(element)
        element.writeBool("isRedirectInput", isRedirectInput)
        element.writeString("redirectInputPath", redirectInputPath ?: "")
    }

    /**
     * If you change serialization, make sure that the old variant is still
     * readable for several releases.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readEnum<RustChannel>("channel")?.let { channel = it }
        element.readBool("requiredFeatures")?.let { requiredFeatures = it }
        element.readBool("allFeatures")?.let { allFeatures = it }
        element.readBool("emulateTerminal")?.let { emulateTerminal = it }
        element.readBool("withSudo")?.let { withSudo = it }
        element.readEnum<BuildTarget>("buildTarget")?.let { buildTarget = it }
        element.readEnum<BacktraceMode>("backtrace")?.let { backtrace = it }
        env = EnvironmentVariablesData.readExternal(element)
        element.readBool("isRedirectInput")?.let { isRedirectInput = it }
        element.readString("redirectInputPath")?.let { redirectInputPath = it }
    }





    sealed class CleanConfiguration {
        class Ok(
            // TODO: this is kinda weird. !!!
            //       CargoAwareConfiguration shouldn't know anything ab. CargoCommandLine!
            // TODO: as a first step, let's try making it optional maybe?
            val cmd: CargoCommandLine?,
            val toolchain: RsToolchainBase
        ) : CleanConfiguration() {

            // TODO: a tmp hack, replace with a proper thing later
            fun getMeCmd(): CargoCommandLine {
                assert(cmd != null)
                return cmd!!
            }
        }

        class Err(val error: RuntimeConfigurationError) : CleanConfiguration()

        val ok: Ok? get() = this as? Ok

        companion object {
            fun error(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) = Err(RuntimeConfigurationError(message))
        }
    }

    abstract fun clean(): CleanConfiguration
}

data class ParsedCommand(val command: String, val toolchain: String?, val additionalArguments: List<String>) {
    companion object {
        fun parse(rawCommand: String): ParsedCommand? {
            val args = ParametersListUtil.parse(rawCommand)
            val command = args.firstOrNull { !it.startsWith("+") } ?: return null
            val toolchain = args.firstOrNull()?.takeIf { it.startsWith("+") }?.removePrefix("+")
            val additionalArguments = args.drop(args.indexOf(command) + 1)
            return ParsedCommand(command, toolchain, additionalArguments)
        }
    }
}
