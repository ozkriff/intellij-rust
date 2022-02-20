/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.Location
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.psi.ext.cargoWorkspace
import org.rust.openapiext.toPsiFile

// TODO: re-read https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html#creating-configurations-from-context
class CustomBuildRunConfigurationProducer : LazyRunConfigurationProducer<CustomBuildCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CustomBuildConfigurationType.getInstance().factory
    }

    // TODO: DO I need to re-implement this? or fixed isConfigurationFromContext should be enough?
    // override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
    //     return super.findExistingConfiguration(context)
    // }

    // TODO: findExistingConfiguration ???

    override fun isConfigurationFromContext(configuration: CustomBuildCommandConfiguration, context: ConfigurationContext): Boolean {
        // TODO("Not yet implemented")
        val location = context.location ?: return false

        // TODO: тут тоже стоит посмотреть на получаемую строку запуска??
        // context.dataContext.
        // configuration.
        val target = findCustomBuildTarget(location) ?: return false

        return configuration.canBeFrom(target.cargoCommandLine)
    }

    override fun setupConfigurationFromContext(
        configuration: CustomBuildCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        // TODO("Not yet implemented")

        val location = context.location ?: return false
        val target = findCustomBuildTarget(location) ?: return false
        val fn = location.psiElement.ancestorStrict<RsFunction>()
        val source = if (fn != null && isBuildScriptMainFunction(fn)) fn else context.psiLocation?.containingFile
        sourceElement.set(source)

        // NOTE
        configuration.name = target.configurationName
        // val cmd = target.cargoCommandLine.mergeWithDefault(configuration)
        // configuration.setFromCmd(cmd) // TODO: ?
        return true
    }

    // TODO: тут немного напрашивается???
    private class ExecutableTarget(target: CargoWorkspace.Target) {
        val configurationName: String = "Run ${target.name}"

        // TODO: это надо переделать
        // TODO: а тут что в итоге генерится? мы же не можем прям каргу тут запустить, да? у нас кастомный бинарь
        val cargoCommandLine = CargoCommandLine.forTarget(target, "run") // TODO: нафиг cargo!
    }

    companion object {
        fun isBuildScriptMainFunction(fn: RsFunction): Boolean {
            val ws = fn.cargoWorkspace ?: return false
            return fn.parent is RsFile && fn.name == "main" && findCustomBuildTarget(ws, fn.containingFile.virtualFile) != null
        }

        // TODO: I don't need this anymore, right?
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

/*
// TODO: надо отнаследовать эту штуку не от CargoRunConfigurationProducer, а от чего-то безкаргового ниже уровнем
class CargoBuildScriptRunConfigurationProducer : CargoRunConfigurationProducer() {
    override fun isConfigurationFromContext(
        configuration: CargoCommandConfiguration, // TODO: replace with BuildScriptCommandConfiguration !!!
        context: ConfigurationContext
    ): Boolean {
        val location = context.location ?: return false

        // TODO: тут тоже стоит посмотреть на получаемую строку запуска

        // context.dataContext.
        // configuration.

        val target = findCustomBuildTarget(location) ?: return false

        return configuration.canBeFrom(target.cargoCommandLine)
    }

    override fun setupConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val location = context.location ?: return false
        val target = findCustomBuildTarget(location) ?: return false
        val fn = location.psiElement.ancestorStrict<RsFunction>()
        val source = if (fn != null && isBuildScriptMainFunction(fn)) fn else context.psiLocation?.containingFile
        sourceElement.set(source)

        // NOTE

        configuration.name = target.configurationName
        val cmd = target.cargoCommandLine.mergeWithDefault(configuration)
        configuration.setFromCmd(cmd)
        return true
    }

}
*/
