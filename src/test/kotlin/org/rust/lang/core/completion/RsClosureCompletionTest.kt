/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion

// TODO: https://github.com/intellij-rust/intellij-rust/issues/3830 (with arguments)
// TODO: https://github.com/intellij-rust/intellij-rust/issues/4524 (auto autocomplete)

// TODO: Q: Should I implement an implicit completion?

class RsClosureCompletionTest : RsCompletionTestBase() {
    fun `test complete simple closure`() = doFirstCompletion("""
        fn f(_f: impl Fn(u8)) {}
        fn main() { f(/*caret*/); }
    """, """
        fn f(_f: impl Fn(u8)) {}
        fn main() { f(xxx/*caret*/); }
    """)

    // TODO: add a test with arguments

    /*
    fun `test complete false in variable decl`() = doSingleCompletion("""
        fn main() { let x: bool = fal/*caret*/; }
    """, """
        fn main() { let x: bool = false/*caret*/; }
    """)

    fun `test no true completion in u8 variable decl`() = checkNoCompletion("""
        fn main() { let x: u8 = tr/*caret*/; }
    """)

    fun `test no false completion in u8 variable decl`() = checkNoCompletion("""
        fn main() { let x: u8 = fal/*caret*/; }
    """)

    fun `test complete true in condition`() = doSingleCompletion("""
        fn main() { if tr/*caret*/; }
    """, """
        fn main() { if true/*caret*/; }
    """)

    fun `test complete false when assigning to bool variable`() = doSingleCompletion("""
        fn main() {
            let mut test = false;
            test = fa/*caret*/;
        }
    """, """
        fn main() {
            let mut test = false;
            test = false/*caret*/;
        }
    """)
    */
}
