/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.rust.ide.icons.RsIcons

class CustomBuildConfigurationType : ConfigurationTypeBase(
    "CustomBuildRunConfiguration",
    "custom-build",
    "custom build run configuration",
    RsIcons.RUST // TODO: ?
) {
    init {
        addFactory(CustomBuildConfigurationFactory(this))
    }

    val factory: ConfigurationFactory get() = configurationFactories.single()

    companion object {
        fun getInstance(): CustomBuildConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(CustomBuildConfigurationType::class.java)
    }
}

class CustomBuildConfigurationFactory(type: CustomBuildConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        //  val p2 = project as? CargoProject
        // CargoConstants.ProjectLayout.target

        // val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(workingDirectory) ?: return
        // val targetDir = root.findChild(CargoConstants.ProjectLayout.target) ?: return

        return CustomBuildCommandConfiguration(project, id, this)
    }

    companion object {
        const val ID: String = "custom-build"
    }
}
