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

class CustomBuildConfigurationEditor(val project: Project)
    : SettingsEditor<CustomBuildConfiguration>() {

    private val pathOutDir: JBTextField = JBTextField()

    override fun createEditor(): JComponent = panel {
        // TODO: move all visible text to translation files
        row("Path to OUT_DIR (TODO make actually work):") {
            // TODO: add a path text field here (see pathTextField or something similar)
            pathOutDir()
        }
    }

    override fun resetEditorFrom(configuration: CustomBuildConfiguration) {
        pathOutDir.text = configuration.outDir ?: ""
    }

    // !
    // TODO: wtf the id of the configuration is different every time
    //       I hit a bp in `applyEditorTo`? does it happen in a normal rust runconf?
    override fun applyEditorTo(configuration: CustomBuildConfiguration) {
        configuration.outDir = pathOutDir.text.ifEmpty { null }
        if (configuration.outDir != null) {
            val n = 1
        }
        // configuration.command = pathBin.text
    }
}

