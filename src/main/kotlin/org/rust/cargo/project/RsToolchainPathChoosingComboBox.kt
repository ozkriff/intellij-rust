/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBoxWithWidePopup
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import org.rust.openapiext.pathAsPath
import java.nio.file.Path
import javax.swing.plaf.basic.BasicComboBoxEditor

/**
 * A combobox with browse button for choosing a path to a toolchain, also capable of showing progress indicator.
 * To toggle progress indicator visibility use [setBusy] method.
 *
 * To fill this box in async mode use [addToolchainsAsync]
 */
class RsToolchainPathChoosingComboBox : ComponentWithBrowseButton<ComboBoxWithWidePopup<Path>>(ComboBoxWithWidePopup(), null) {
    private val busyIconExtension: ExtendableTextComponent.Extension =
        ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    private val editor: BasicComboBoxEditor = object : BasicComboBoxEditor() {
        override fun createEditorComponent() = ExtendableTextField().apply { isEditable = false }
    }

    init {
        childComponent.apply { ComboboxSpeedSearch(this) }
        addActionListener {
            // Select directory with Cargo binary
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            FileChooser.chooseFile(descriptor, null, null) { virtualFile ->
                val path = virtualFile.pathAsPath
                childComponent.selectedItem = items.find { it == path }
                    ?: path.apply { childComponent.insertItemAt(this, 0) }
            }
        }
    }

    var selectedPath: Path?
        get() = childComponent.selectedItem as? Path?
        set(value) {
            // TODO:
            if (value in items) {
                childComponent.selectedItem = value
            }
        }

    val items: List<Path>
        get() = (0 until childComponent.itemCount).map { childComponent.getItemAt(it) }

    fun setBusy(busy: Boolean) {
        if (busy) {
            childComponent.isEditable = true
            childComponent.editor = editor
            (childComponent.editor.editorComponent as ExtendableTextField).addExtension(busyIconExtension)
        } else {
            (childComponent.editor.editorComponent as ExtendableTextField).removeExtension(busyIconExtension)
            childComponent.isEditable = false
        }
        repaint()
    }
}
