/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints

import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.ide.inspections.RsInspectionsTestBase

class RsUnusedMustUseInspectionTest : RsInspectionsTestBase(RsUnusedMustUseInspection::class) {
    fun `test unused must_use with simple function call`() = checkByText("""
        #[must_use]
        fn foo() -> bool { false }

        fn main() {
            <weak_warning descr="Unused return value of foo that must be used">foo()</weak_warning>;
        }
    """)

    fun `test unused must_use with simple function call and inner attribute`() = checkByText("""
        fn foo() -> bool {
            #![must_use]
            false
        }

        fn main() {
            <weak_warning descr="Unused return value of foo that must be used">foo()</weak_warning>;
        }
    """)

    fun `test unused must_use with custom struct`() = checkByText("""
        #[must_use]
        struct S;

        fn main() {
            <weak_warning descr="Unused S that must be used">S</weak_warning>;
        }
    """)

    fun `test unused must_use with method call though nested struct literal`() = checkByText("""
        struct S;

        impl S {
            #[must_use]
            fn foo(&self) -> S { S }
        }

        fn main() {
            <weak_warning descr="Unused return value of foo that must be used">S.foo()</weak_warning>;
        }
    """)

    fun `test unused must_use with marked struct returned from function`() = checkByText("""
        #[must_use]
        struct S;

        fn foo() -> S { S }

        fn main() {
            <weak_warning descr="Unused S that must be used">foo()</weak_warning>;
        }
    """)

    fun `test unused must_use with method call`() = checkByText("""
        struct S;

        impl S {
            #[must_use]
            fn foo(&self) -> S { S }
        }

        struct S2 { s: S }

        fn main() {
            <weak_warning descr="Unused return value of foo that must be used">S2 { s: S }.s.foo()</weak_warning>;
        }
    """)

    fun `test unused must_use block disabled by cfg`() = checkByText("""
        #[must_use]
        struct S;

        fn xyz() -> S {
            #[cfg(undeclared_feature)]
            { S }

            <weak_warning descr="Unused S that must be used">#[cfg(not(undeclared_feature))]
            { S }</weak_warning>

            { S }
        }
    """)

    // TODO: add a new test that show the actual thing
//    !!!

    fun `test no warning on reverse cfg disabled blocks`() = checkByText("""
        #[must_use]
        struct S;

        fn xyz() -> S {
            #[cfg(not(undeclared_feature))]
            { S }

            #[cfg(undeclared_feature)]
            { S }
        }
    """)

    fun `test fixing by adding assigning to _`() = checkFixByText("Add `let _ =`","""
        #[must_use]
        fn foo() -> bool { false }

        fn main() {
            <weak_warning descr="Unused return value of foo that must be used">/*caret*/foo()</weak_warning>;
        }
    """, """
        #[must_use]
        fn foo() -> bool { false }

        fn main() {
            let _ = foo();
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test fixing unused result by adding unwrap`() = checkFixByText("Add `.unwrap()`","""
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            <weak_warning descr="Unused Result<bool, ()> that must be used">/*caret*/foo()</weak_warning>;
        }
    """, """
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            foo().unwrap();
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test fixing unused result by adding expect`() = checkFixByText("Add `.expect(\"\")`","""
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            <weak_warning descr="Unused Result<bool, ()> that must be used">/*caret*/foo()</weak_warning>;
        }
    """, """
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            foo().expect("TODO: panic message");
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test fixing unused result by adding expect with template`() = checkFixByTextWithLiveTemplate("Add `.expect(\"\")`","""
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            <weak_warning descr="Unused Result<bool, ()> that must be used">/*caret*/foo()</weak_warning>;
        }
    """, "abc", """"
        fn foo() -> Result<bool, ()> { false }

        fn main() {
            foo().expect("abc");
        }
    """)
}
