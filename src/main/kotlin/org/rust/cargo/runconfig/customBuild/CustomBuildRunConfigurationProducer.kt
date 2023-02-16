/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.CargoRunConfigurationProducer
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.psi.ext.cargoWorkspace
import org.rust.openapiext.toPsiFile

class CustomBuildRunConfigurationProducer: CargoRunConfigurationProducer() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CustomBuildConfigurationType.getInstance().factory
    }

    override fun isConfigurationFromContext(configuration: CargoCommandConfiguration, context: ConfigurationContext): Boolean {
        if (configuration !is CustomBuildConfiguration) return false

        val location = context.location ?: return false
        val target = findCustomBuildTarget(location) ?: return false

        // return configuration.canBeFrom(target.cargoCommandLine) // TODO: what should be passed here?
        return configuration.canBeFrom(target.crateRoot)
    }

    // TODO: what exactly should be done here?
    override fun setupConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (configuration !is CustomBuildConfiguration) return false // TODO: do additional settings here?

        val location = context.location ?: return false
        val target = findCustomBuildTarget(location) ?: return false
        val fn = location.psiElement.ancestorStrict<RsFunction>()
        val source = if (fn != null && isBuildScriptMainFunction(fn)) fn else context.psiLocation?.containingFile
        sourceElement.set(source)

        // NOTE
        // configuration.name = target.configurationName

        // val cmd = target.cargoCommandLine.mergeWithDefault(configuration)
        // configuration.setFromCmd(cmd) // TODO: ?
        return true
    }

    // TODO: Do I need to re-implement this? or fixed isConfigurationFromContext should be enough?
    // override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
    //     return super.findExistingConfiguration(context)
    // }

    private class ExecutableTarget(target: CargoWorkspace.Target) {
        val configurationName: String = "Run ${target.name}"

        // TODO: take some path and save it - and use later in comparison in canBeFrom

        val crateRoot: VirtualFile = target.crateRoot!! // TODO: handle error properly

        val pkg: CargoWorkspace.Package = target.pkg
    }

    companion object {
        fun isBuildScriptMainFunction(fn: RsFunction): Boolean {
            val ws = fn.cargoWorkspace ?: return false
            return fn.parent is RsFile && fn.name == "main" && findCustomBuildTarget(ws, fn.containingFile.virtualFile) != null
        }

        private fun findCustomBuildTarget(location: Location<*>): ExecutableTarget? {
            val file = location.virtualFile ?: return null
            val rsFile = file.toPsiFile(location.project) as? RsFile ?: return null
            val ws = rsFile.cargoWorkspace ?: return null
            return findCustomBuildTarget(ws, file)
        }

        private fun findCustomBuildTarget(ws: CargoWorkspace, file: VirtualFile): ExecutableTarget? {
            val target = ws.findTargetByCrateRoot(file) ?: return null
            if (!target.kind.isCustomBuild) return null
            return ExecutableTarget(target)
        }
    }
}
