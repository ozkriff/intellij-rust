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

    private var target: CargoWorkspace.Target? = null // TODO: is it ok to have a null by default here?

    var outDir: String? = null
        private set

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        CustomBuildConfigurationEditor(project)

    fun canBeFrom(target: CargoWorkspace.Target): Boolean =
        target.crateRoot?.url == this.crateRootUrl

    fun setTarget(target: CargoWorkspace.Target) {
        this.crateRootUrl = target.crateRoot?.url
        // TODO: do we need additions here orr?
        name = target.name + " (build.rs)" // TODO: good name? translation?
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("outDir", outDir ?: "")
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("outDir")?.let { outDir = it }
    }
}
