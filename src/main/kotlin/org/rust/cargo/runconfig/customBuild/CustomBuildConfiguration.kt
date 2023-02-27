/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.readString
import org.rust.cargo.runconfig.writeString

class CustomBuildConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoCommandConfiguration(project, name, factory) {
    override var command: String = "run" // TODO: it's a hack. not sure if a good idea

    private var crateRootUrl: String? = null

    var outDir: String? = null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        CustomBuildConfigurationEditor(project)

    fun canBeFrom(target: CargoWorkspace.Target): Boolean =
        target.crateRoot?.url == this.crateRootUrl

    fun setTarget(target: CargoWorkspace.Target) {
        crateRootUrl = target.crateRoot?.url
        name = target.pkg.name + "'s build.rs" // TODO: translation?
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("outDir", outDir ?: "")
        element.writeString("crateRootUrl", crateRootUrl ?: "")
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("outDir")?.let { outDir = it }
        element.readString("crateRootUrl")?.let { crateRootUrl = it }
    }
}
