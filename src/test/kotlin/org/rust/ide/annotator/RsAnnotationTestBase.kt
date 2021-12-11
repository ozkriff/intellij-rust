/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator

import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase
import org.rust.fileTreeFromText
import org.rust.openapiext.Testmark

abstract class RsAnnotationTestBase : RsTestBase() {

    protected lateinit var annotationFixture: RsAnnotationTestFixture<Unit>

    override fun setUp() {
        super.setUp()
        annotationFixture = createAnnotationFixture()
        annotationFixture.setUp()
    }

    override fun tearDown() {
        annotationFixture.tearDown()
        super.tearDown()
    }

    protected abstract fun createAnnotationFixture(): RsAnnotationTestFixture<Unit>

    protected fun checkHighlighting(@Language("Rust") text: String, ignoreExtraHighlighting: Boolean = true) =
        annotationFixture.checkHighlighting(text, ignoreExtraHighlighting)

    protected fun checkInfo(@Language("Rust") text: String) = annotationFixture.checkInfo(text)
    protected fun checkWarnings(@Language("Rust") text: String) = annotationFixture.checkWarnings(text)
    protected fun checkErrors(@Language("Rust") text: String) = annotationFixture.checkErrors(text)

    protected fun checkByText(
        @Language("Rust") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
        testmark: Testmark? = null
    ) = annotationFixture.checkByText(text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting, testmark)

    protected fun checkFixByText(
        fixName: String,
        @Language("Rust") before: String,
        @Language("Rust") after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixByText(fixName, before, after, checkWarn, checkInfo, checkWeakWarn, testmark)

    protected fun checkFixByTextWithoutHighlighting(
        fixName: String,
        @Language("Rust") before: String,
        @Language("Rust") after: String,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixByTextWithoutHighlighting(fixName, before, after, testmark)

    protected fun checkByFileTree(
        @Language("Rust") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
        stubOnly: Boolean = true,
        testmark: Testmark? = null
    ) = annotationFixture.checkByFileTree(text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting, stubOnly, testmark)

    protected fun checkFixByFileTree(
        fixName: String,
        @Language("Rust") before: String,
        @Language("Rust") after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        stubOnly: Boolean = true,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixByFileTree(fixName, before, after, checkWarn, checkInfo, checkWeakWarn, stubOnly, testmark)

    protected fun checkFixByFileTreeWithoutHighlighting(
        fixName: String,
        @Language("Rust") before: String,
        @Language("Rust") after: String,
        stubOnly: Boolean = true,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixByFileTreeWithoutHighlighting(fixName, before, after, stubOnly, testmark)

    // TODO: extend this?
    protected fun checkFixByTextWithLiveTemplate(
        fixName: String,
        @Language("Rust") before: String,
        toType: String,
        @Language("Rust") after: String,
        fileName: String = "main.rs",
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        testmark: Testmark? = null
    ) {
        checkByTextWithLiveTemplate(before, after, toType, fileName) {
            annotationFixture.checkFixByTextOzkriffExperiment(fixName, before, after, checkWarn, checkInfo, checkWeakWarn, testmark)
            // codeInsightFixture.configureByText(baseFileName, replaceCaretMarker(text.trimIndent()))
            // TODO: check more stuff here?
            // annotationFixture.checkFixByText(fixName, before, after, checkWarn, checkInfo, checkWeakWarn, testmark)
        }

//        TemplateManagerImpl.setTemplateTesting(testRootDisposable)
//        InlineFile(before.trimIndent(), fileName).withCaret()
//
//        annotationFixture.checkFixByText(fixName, before, after, checkWarn, checkInfo, checkWeakWarn, testmark)
//
//        assertNotNull(TemplateManagerImpl.getTemplateState(myFixture.editor))
//        myFixture.type(toType)
//        assertNull(TemplateManagerImpl.getTemplateState(myFixture.editor))
//        myFixture.checkResult(replaceCaretMarker(after.trimIndent()))


    }

    protected fun checkFixIsUnavailable(
        fixName: String,
        @Language("Rust") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixIsUnavailable(fixName, text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting, testmark)

    protected fun checkFixIsUnavailableByFileTree(
        fixName: String,
        @Language("Rust") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
        stubOnly: Boolean = true,
        testmark: Testmark? = null
    ) = annotationFixture.checkFixIsUnavailableByFileTree(fixName, text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting, stubOnly, testmark)

    protected fun checkDontTouchAstInOtherFiles(@Language("Rust") text: String, checkInfo: Boolean = false, filePath: String? = null) {
        fileTreeFromText(text).create()
        val testFilePath = filePath ?: "main.rs"
        (myFixture as CodeInsightTestFixtureImpl) // meh
            .setVirtualFileFilter { !it.path.endsWith(testFilePath) }

        myFixture.configureFromTempProjectFile(testFilePath)
        myFixture.testHighlighting(false, checkInfo, false)
    }
}
