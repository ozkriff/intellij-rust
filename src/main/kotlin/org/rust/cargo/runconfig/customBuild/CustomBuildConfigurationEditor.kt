/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class CustomBuildConfigurationEditor(project: Project)
    : SettingsEditor<CustomBuildCommandConfiguration>() {

    // private val pathBin: JBTextField = JBTextField()
    private val pathOutDir: JBTextField = JBTextField()

    override fun createEditor(): JComponent = panel {
        // TODO: move all visible text to translation files
        row("Path to build script executable (TODO later):") {
            // TODO: add a path text field here (see pathTextField or something similar)
            // pathBin()
        }
        row("Path to OUT_DIR (TODO later):") {
            // TODO: add a path text field here (see pathTextField or something similar)
            pathOutDir()
        }
    }

    override fun resetEditorFrom(configuration: CustomBuildCommandConfiguration) {
        pathOutDir.text = configuration.outDir
    }

    override fun applyEditorTo(configuration: CustomBuildCommandConfiguration) {
        configuration.outDir = pathOutDir.text
        // configuration.command = pathBin.text
    }
}
