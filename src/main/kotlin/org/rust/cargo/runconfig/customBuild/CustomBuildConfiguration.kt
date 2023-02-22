/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

class CustomBuildConfiguration(
    project: Project,
    name: String,
    factory: ConfigurationFactory
) : CargoCommandConfiguration(project, name, factory) {
    override var command: String = "run" // TODO: it's a hack. not sure if a good idea

    private var target: CargoWorkspace.Target? = null // TODO: is it ok to have a null by default here?

    // var outDir: String = project.basePath + "/target/pseudoOutDir" // TODO: un-hack
    var outDir: String? = null // TODO: let's try keeping it empty by default and only assign an actual value when used

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        CustomBuildConfigurationEditor(project)

    fun canBeFrom(target: CargoWorkspace.Target): Boolean {
        return target == this.target
    }

    fun setTarget(target: CargoWorkspace.Target) {
        this.target = target
        name = target.name + " (build.rs)" // TODO: good name? translation?
    }
}
