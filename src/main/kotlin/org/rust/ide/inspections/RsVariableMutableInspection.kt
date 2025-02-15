/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.rust.ide.fixes.RemoveMutableFix
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.psi.ext.descendantsOfType
import org.rust.lang.core.psi.ext.mutability
import org.rust.lang.core.psi.ext.selfParameter

class RsVariableMutableInspection : RsLocalInspectionTool() {
    override fun getDisplayName(): String = "No mutable required"

    override fun buildVisitor(holder: RsProblemsHolder, isOnTheFly: Boolean): RsVisitor =
        object : RsWithMacrosInspectionVisitor() {
            override fun visitPatBinding(o: RsPatBinding) {
                if (!o.mutability.isMut) return
                val block = o.ancestorStrict<RsBlock>() ?: o.ancestorStrict<RsFunction>() ?: return
                if (ReferencesSearch.search(o, LocalSearchScope(block))
                        .any { checkOccurrenceNeedMutable(it.element.parent) }) return
                if (block.descendantsOfType<RsMacroCall>().any { checkExprPosition(o, it) }) return
                holder.registerProblem(
                    o,
                    "Variable `${o.identifier.text}` does not need to be mutable",
                    RemoveMutableFix()
                )
            }
        }

    fun checkExprPosition(o: RsPatBinding, expr: RsMacroCall): Boolean = o.textOffset < expr.textOffset

    fun checkOccurrenceNeedMutable(occurrence: PsiElement): Boolean {
        when (val parent = occurrence.parent) {
            is RsUnaryExpr -> return parent.isMutable || parent.mul != null
            is RsBinaryExpr -> return parent.left == occurrence
            is RsMethodCall -> {
                val ref = parent.reference.resolve() as? RsFunction ?: return true
                val self = ref.selfParameter ?: return true
                return self.mutability.isMut
            }
            is RsTupleExpr -> {
                val expr = parent.parent as? RsUnaryExpr ?: return true
                return expr.isMutable
            }
            is RsValueArgumentList -> return false
        }
        return true
    }

    private val RsUnaryExpr.isMutable: Boolean get() = mut != null
}
