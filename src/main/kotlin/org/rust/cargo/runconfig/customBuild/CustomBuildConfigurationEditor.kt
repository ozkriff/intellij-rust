/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.CheckBox
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.nullize
import org.rust.openapiext.fullWidthCell
import javax.swing.JCheckBox
import javax.swing.JComponent

// TODO: move all visible text to translation files & try to improve the text

class CustomBuildConfigurationEditor(val project: Project)
    : SettingsEditor<CustomBuildConfiguration>() {

    private val customOutDir = TextFieldWithBrowseButton().apply {
        isEnabled = false
        val fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select OUT_DIR"
            // title = ExecutionBundle.message("select.working.directory.message") // TODO: tr
        }
        addBrowseFolderListener(null, null, null, fileChooser)
    }

    private val isCustomOutDir: JCheckBox = CheckBox("Custom OUT_DIR", false).apply {
        addChangeListener { customOutDir.isEnabled = isSelected }
    }

    override fun createEditor(): JComponent = panel {
        row {
            layout(RowLayout.LABEL_ALIGNED)
            cell(isCustomOutDir)
            fullWidthCell(customOutDir)
        }
    }

    override fun resetEditorFrom(configuration: CustomBuildConfiguration) {
        isCustomOutDir.isSelected = configuration.isCustomOutDir
        customOutDir.text = configuration.customOutDir ?: ""
    }

    override fun applyEditorTo(configuration: CustomBuildConfiguration) {
        configuration.isCustomOutDir = isCustomOutDir.isSelected
        configuration.customOutDir = customOutDir.text.nullize()
    }
}
