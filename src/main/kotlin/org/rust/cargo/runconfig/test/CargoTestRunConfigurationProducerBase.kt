/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.model.impl.allTargets
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.CargoRunConfigurationProducer
import org.rust.cargo.runconfig.mergeWithDefault
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.ide.refactoring.RsNamesValidator
import org.rust.lang.core.crate.asNotFake
import org.rust.lang.core.psi.RS_RAW_PREFIX
import org.rust.lang.core.psi.RsFile
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.RsModDeclItem
import org.rust.lang.core.psi.ext.*
import org.rust.openapiext.pathAsPath
import org.rust.openapiext.toPsiFile
import org.rust.stdext.capitalized

private typealias TestConfigProvider = (List<PsiElement>, Boolean) -> TestConfig?

abstract class CargoTestRunConfigurationProducerBase : CargoRunConfigurationProducer() {
    protected abstract val commandName: String
    private val testConfigProviders: MutableList<TestConfigProvider> = mutableListOf()

    override fun isConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val testConfig = findTestConfig(context) ?: return false
        return configuration.canBeFrom(testConfig.cargoCommandLine())
    }

    override fun setupConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val testConfig = findTestConfig(context) ?: return false
        sourceElement.set(testConfig.originalElement)
        configuration.name = testConfig.configurationName
        val cmd = testConfig.cargoCommandLine().mergeWithDefault(configuration)
        configuration.setFromCmd(cmd)
        return true
    }

    // TODO: investigate how this is used
    protected fun registerConfigProvider(provider: TestConfigProvider) {
        testConfigProviders.add(provider)
    }

    protected fun registerDirectoryConfigProvider(provider: (PsiDirectory) -> TestConfig?) {
        testConfigProviders.add { elements, _ ->
            val dir = elements.singleOrNull() as? PsiDirectory ?: return@add null
            provider(dir)
        }
    }

    fun findTestConfig(elements: List<PsiElement>, climbUp: Boolean = true): TestConfig? {
        for (provider in testConfigProviders) {
            val config = provider(elements, climbUp)
            if (config != null) return config
        }
        return null
    }

    private fun findTestConfig(context: ConfigurationContext): TestConfig? {
        val elements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(context.dataContext)?.toList()
            ?: context.location?.psiElement?.let { listOf(it) }
        return elements?.let { findTestConfig(it) }
    }

    protected fun createConfigForDirectory(dir: PsiDirectory): TestConfig? {
        val filesConfig = createConfigForMultipleFiles(dir.targets, climbUp = false) ?: return null

        val sourceRoot = dir.sourceRoot ?: return null
        val suitableSourceRootName = StringUtil.pluralize(commandName)
        if (sourceRoot.name != suitableSourceRootName) return null

        return DirectoryTestConfig(commandName, filesConfig.targets, dir)
    }

    protected fun createConfigForMultipleFiles(elements: List<PsiElement>, climbUp: Boolean): TestConfig? {
        val modConfigs = elements.mapNotNull { createConfigFor<RsMod>(listOf(it), climbUp) }

        if (modConfigs.size == 1) {
            return modConfigs.first()
        }

        val targets = modConfigs.flatMap { it.targets }
        if (targets.size <= 1) return null

        // If the selection spans more than one package, bail out.
        val packages = targets.map { it.pkg }
        if (packages.distinct().size > 1) return null

        return MultipleFileTestConfig(commandName, targets, modConfigs.first().sourceElement)
    }

    protected inline fun <reified T : RsQualifiedNamedElement> createConfigFor(
        elements: List<PsiElement>,
        climbUp: Boolean
    ): TestConfig? {
        val (originalElement, sourceElement) = elements
            .mapNotNull {
                val originalElement = findElement<T>(it, climbUp) ?: return@mapNotNull null
                val sourceElement = getSourceElement(originalElement) ?: return@mapNotNull null
                originalElement to sourceElement
            }
            .singleOrNull { (_, sourceElement) ->
                isSuitable(sourceElement) && sourceElement.containingCargoTarget != null
            }
            ?: return null
        val target = sourceElement.containingCargoTarget ?: return null
        val configPath = sourceElement.crateRelativePath.configPath() ?: return null
        return SingleItemTestConfig(
            commandName, configPath, target, sourceElement, originalElement,
            isIgnoredTest(sourceElement)
        )
    }

    protected inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
        if (base is T) return base
        if (!climbUp) return null
        return base.ancestorOrSelf()
    }

    protected open fun isSuitable(element: PsiElement): Boolean {
        val crate = (element as? RsElement)?.containingCrate?.asNotFake
        return crate?.origin == PackageOrigin.WORKSPACE
    }

    protected fun isIgnoredTest(element: PsiElement): Boolean =
        element is RsFunction && element.findOuterAttr("ignore") != null

    companion object {
        private val PsiDirectory.targets: List<RsFile>
            get() {
                val rootPath = virtualFile.pathAsPath
                return project
                    .cargoProjects
                    .allTargets
                    .filter { it.pkg.origin == PackageOrigin.WORKSPACE }
                    .mapNotNull { it.crateRoot }
                    .distinct()
                    .filter { it.pathAsPath.startsWith(rootPath) }
                    .mapNotNull { it.toPsiFile(project) as? RsFile }
                    .toList()
            }

        // We need to chop off heading colon `::`, since `crateRelativePath` always returns fully-qualified path
        @JvmStatic
        protected fun String?.configPath(): String? =
            this?.removePrefix("::")?.split("::")?.joinToString("::", transform = ::escapePathFragment)

        private fun escapePathFragment(fragment: String): String {
            return if (RsNamesValidator.isKeyword(fragment)) {
                "$RS_RAW_PREFIX$fragment"
            } else {
                fragment
            }
        }

        fun getSourceElement(element: RsQualifiedNamedElement): RsQualifiedNamedElement? =
            if (element is RsModDeclItem) element.reference.resolve() as? RsQualifiedNamedElement else element
    }
}

interface TestConfig {
    val commandName: String
    val path: String
    val exact: Boolean
    val targets: List<CargoWorkspace.Target>
    val configurationName: String
    val sourceElement: PsiElement
    val originalElement: PsiElement get() = sourceElement
    val isIgnored: Boolean get() = false
    val isDoctest: Boolean get() = false

    fun cargoCommandLine(): CargoCommandLine {
        var commandLine = CargoCommandLine.forTargets(targets, commandName, listOf(path), isDoctest = isDoctest)
        if (exact) {
            commandLine = commandLine.withPositionalArgument("--exact")
        }
        if (isIgnored) {
            commandLine = commandLine.withPositionalArgument("--ignored")
        }
        return commandLine
    }
}

private class DirectoryTestConfig(
    override val commandName: String,
    override val targets: List<CargoWorkspace.Target>,
    override val sourceElement: PsiDirectory
) : TestConfig {
    override val path: String = ""
    override val exact = false

    override val configurationName: String
        get() = "${StringUtil.pluralize(commandName).capitalized()} in '${sourceElement.name}'"
}

private class MultipleFileTestConfig(
    override val commandName: String,
    override val targets: List<CargoWorkspace.Target>,
    override val sourceElement: PsiElement
) : TestConfig {
    override val path: String = ""
    override val exact: Boolean = false

    override val configurationName: String
        get() = "${commandName.capitalized()} multiple selected files"
}

 class SingleItemTestConfig(
    override val commandName: String,
    override val path: String,
    val target: CargoWorkspace.Target,
    override val sourceElement: RsElement,
    override val originalElement: RsElement,
    override val isIgnored: Boolean
) : TestConfig {
    override val exact: Boolean
        get() = sourceElement is RsFunction

    override val targets: List<CargoWorkspace.Target>
        get() = listOf(target)

    override val configurationName: String
        get() = buildString {
            append(commandName.capitalized())
            append(" ")

            if (sourceElement !is RsMod) {
                append(path)
                return@buildString
            }

            if (sourceElement.modName in listOf("test", "tests")) {
                append(sourceElement.`super`?.modName ?: "")
                append("::")
            }
            append(sourceElement.modName)
        }
}
