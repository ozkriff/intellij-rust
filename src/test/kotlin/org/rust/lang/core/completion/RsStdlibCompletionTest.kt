/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion

import com.intellij.openapi.util.SystemInfo
import org.rust.*
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.lang.core.macros.MacroExpansionScope

@ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
class RsStdlibCompletionTest : RsCompletionTestBase() {
    fun `test complete simple closure`() = doFirstCompletion("""
        fn f(_f: fn()) {}
        fn main() { f(/*caret*/); }
    """, """
        fn f(_f: fn()) {}
        fn main() { f(||/*caret*/); }
    """)

    fun `test complete simple closure next`() = doFirstCompletion("""
        fn f(_f: fn(n: u8)) {}
        fn main() { f(/*caret*/); }
    """, """
        fn f(_f: fn(n: u8)) {}
        fn main() { f(|_|/*caret*/); }
    """)

    fun `test complete simple closure next2`() = doFirstCompletion("""
        fn f(_f: fn(n1: u8, n2: i32, n3: String)) {}
        fn main() { f(/*caret*/); }
    """, """
        fn f(_f: fn(n1: u8, n2: i32, n3: String)) {}
        fn main() { f(|_, _, _|/*caret*/); }
    """)

    fun `test prelude`() = doFirstCompletion("""
        fn main() {
            dr/*caret*/
        }
    """, """
        fn main() {
            drop(/*caret*/)
        }
    """)

    fun `test prelude visibility`() = checkNoCompletion("""
        mod m {}
        fn main() {
            m::dr/*caret*/
        }
    """)

    fun `test iter`() = @Suppress("DEPRECATION") checkSingleCompletion("iter_mut()", """
        fn main() {
            let vec: Vec<i32> = Vec::new();
            let iter = vec.iter_m/*caret*/
        }
    """)

    fun `test derived trait method`() = @Suppress("DEPRECATION") checkSingleCompletion("fmt", """
        #[derive(Debug)]
        struct Foo;
        fn bar(foo: Foo) {
            foo.fm/*caret*/
        }
    """)

    fun `test macro`() = doSingleCompletion("""
        fn main() { unimpl/*caret*/ }
    """, """
        fn main() { unimplemented!(/*caret*/) }
    """)

    fun `test macro with square brackets`() = doFirstCompletion("""
        fn main() { vec/*caret*/ }
    """, """
        fn main() { vec![/*caret*/] }
    """)

    fun `test macro with braces`() = doFirstCompletion("""
       thread_lo/*caret*/
    """, """
       thread_local! {/*caret*/}
    """)

    fun `test macro in use item`() = doSingleCompletion("""
       #![feature(use_extern_macros)]

       pub use std::unimpl/*caret*/
    """, """
       #![feature(use_extern_macros)]

       pub use std::unimplemented;/*caret*/
    """)

    fun `test rustc doc only macro from prelude`() = doSingleCompletion("""
        fn main() { stringif/*caret*/ }
    """, """
        fn main() { stringify!(/*caret*/) }
    """)

    fun `test rustc doc only macro from std`() = doSingleCompletion("""
        fn main() { std::stringif/*caret*/ }
    """, """
        fn main() { std::stringify!(/*caret*/) }
    """)

    @MockEdition(CargoWorkspace.Edition.EDITION_2015)
    fun `test complete all in std in 'use' in crate root`() = checkContainsCompletion("vec", """
        use std::/*caret*/;
    """)

    @ExpandMacros(MacroExpansionScope.ALL, "std")
    @ProjectDescriptor(WithActualStdlibRustProjectDescriptor::class)
    fun `test complete items from 'os' module unix`() {
        if (!SystemInfo.isUnix) return
        doSingleCompletion("""
            use std::os::uni/*caret*/
        """, """
            use std::os::unix/*caret*/
        """)
    }

    @ExpandMacros(MacroExpansionScope.ALL, "std")
    @ProjectDescriptor(WithActualStdlibRustProjectDescriptor::class)
    fun `test complete items from 'os' module windows`() {
        if (!SystemInfo.isWindows) return
        doSingleCompletion("""
            use std::os::win/*caret*/
        """, """
            use std::os::windows/*caret*/
        """)
    }
}

