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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import org.rust.cargo.runconfig.RsExecutableRunner.Companion.artifacts
import org.rust.cargo.runconfig.RsProcessHandler
import org.rust.cargo.toolchain.impl.CargoMetadata
import org.rust.cargo.toolchain.impl.CompilerArtifactMessage

// TODO: re-read https://plugins.jetbrains.com/docs/intellij/run-configuration-execution.html#running-a-process

// TODO: why is it named *Command*?
class CustomBuildCommandState(
    environment: ExecutionEnvironment,
    val runConfiguration: CustomBuildCommandConfiguration
): CommandLineState(environment) {
    companion object {
        // note: the hack required a normal build to be manually called first
        var ARTIFACTS_HACK: List<CompilerArtifactMessage> = listOf() // TODO: remove this hack later
    }

    override fun startProcess(): ProcessHandler {
        val e = environment
        // e.artifacts

        // val artifacts = environment.artifacts // TODO

        val artifacts2 = environment.artifacts // TODO

        val artifacts = ARTIFACTS_HACK
        assert(artifacts.isNotEmpty())
        val artifact = artifacts.find { message -> message.target.cleanKind == CargoMetadata.TargetKind.CUSTOM_BUILD }!!
        val binPath = artifact.executables[0] // TODO: un-hardcode

        // val basePath = "/home/ozkriff/CLionProjects/build-rs-run-debug/target/debug/build/"
        // val executable = basePath + "build-rs-run-debug-a27d5b8d467c13c3/" + "build-script-build"
        // val outDir = basePath + "build-rs-run-debug-4aee5763f3ce407a/" + "out" // TODO: get it from the settings!

        val outDir = runConfiguration.outDir

        runWriteAction {
            VfsUtil.createDirectoryIfMissing(outDir) // TODO: is this a good idea?
        }

        val commandLine = GeneralCommandLine(binPath)
            .withWorkDirectory(outDir) // TODO: set the correct working directory
            .withEnvironment("OUT_DIR", outDir)
            .withCharset(Charsets.UTF_8)
            // .also { toolchain.patchCommandLine(it) } // TODO: ?

        val handler = RsProcessHandler(commandLine)
        ProcessTerminatedListener.attach(handler) // shows exit code upon termination
        return handler

        // val params = ParametersListUtil.parse(runConfiguration.command)
        // val commandLine = wasmPack.createCommandLine(
        //     workingDirectory,
        //     params.firstOrNull().orEmpty(),
        //     params.drop(1)
        // )
    }
}
