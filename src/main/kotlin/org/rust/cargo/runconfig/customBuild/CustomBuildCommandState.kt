/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import org.rust.cargo.runconfig.RsProcessHandler
import org.rust.cargo.toolchain.impl.CargoMetadata
import org.rust.cargo.toolchain.impl.CompilerArtifactMessage

// TODO: re-read https://plugins.jetbrains.com/docs/intellij/run-configuration-execution.html#running-a-process

// TODO: why is it still named *Command*?
class CustomBuildCommandState(
    environment: ExecutionEnvironment,
    val runConfiguration: CustomBuildCommandConfiguration
): CommandLineState(environment) {
    companion object {
        // note: the hack requires a normal build to be called first!
        var ARTIFACTS_MESSAGES_HACK: List<CompilerArtifactMessage> = listOf() // TODO: remove this hack later
    }

    override fun startProcess(): ProcessHandler {
        val outDir = runConfiguration.outDir
        runWriteAction {
            VfsUtil.createDirectoryIfMissing(outDir) // TODO: is this a good idea?
        }

        val binPath = customBuildBinPath()
        val commandLine = GeneralCommandLine(binPath)
            .withWorkDirectory(outDir) // TODO: set the correct working directory
            .withEnvironment("OUT_DIR", outDir)
            .withCharset(Charsets.UTF_8)
            // .also { toolchain.patchCommandLine(it) } // TODO: ?

        val handler = RsProcessHandler(commandLine)
        ProcessTerminatedListener.attach(handler) // shows exit code upon termination
        return handler
    }

    // TODO: should this be moved to CustomBuildCommandConfiguration maybe?
    //   Like, we should already know what should be run when creating the configuration?
    //   oooor nope cause the build wasn't run at that point yet?
    private fun customBuildBinPath(): String {
        val artifacts = ARTIFACTS_MESSAGES_HACK
        assert(artifacts.isNotEmpty()) // TODO: we're assuming that the build was finished at this point. is it a good idea?

        // TODO: what if there are none? needs proper handling
        val artifactMessages = artifacts.find { message -> message.target.cleanKind == CargoMetadata.TargetKind.CUSTOM_BUILD }!!

        val binPath = artifactMessages.executables[0] // TODO: un-hardcode
        assert(artifactMessages.executables.count() == 1) // TODO: nooot sure if this is always true
        return binPath
    }
}
