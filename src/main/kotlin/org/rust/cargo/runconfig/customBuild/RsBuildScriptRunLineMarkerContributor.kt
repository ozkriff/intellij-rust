/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.customBuild

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.rust.lang.core.psi.RsElementTypes.IDENTIFIER
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.elementType

// TODO: test project for now is ~/CLionProjects/build-rs-run-debug

// NOTE от Эльдара
// > есть вариант: подсунуть врапер программу (через переменные окружения или еще что)
// > и вставлять вызов gdb сервера.
// > кажется, что-то похожее в котлине делает крилл шмагов.
// > безобидное изменение протащить в апстрим карго.
// > контракт: что-то, что вызывает build-rs скрипт
// > в gdb есть настройка wrapper program - посмотри ее доки.

// TODO: Do  really need Rs prefix here?
// TODO: Rename to CustomBuildRunLineMarkerContributor

class RsBuildScriptRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != IDENTIFIER) return null
        val fn = element.parent as? RsFunction ?: return null

        if (!CustomBuildRunConfigurationProducer.isBuildScriptMainFunction(fn)) return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { psiElement -> actions.mapNotNull { getText(it, psiElement) }.joinToString("\n") },
            *actions
        )
    }
}
