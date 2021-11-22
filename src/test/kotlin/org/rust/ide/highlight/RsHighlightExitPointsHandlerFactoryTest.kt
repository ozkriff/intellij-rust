/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import org.intellij.lang.annotations.Language
import org.rust.MockEdition
import org.rust.ProjectDescriptor
import org.rust.RsTestBase
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.cargo.project.workspace.CargoWorkspace

class RsHighlightExitPointsHandlerFactoryTest : RsTestBase() {

    fun `test highlight all returns`() = doTest("""
        fn main() {
            if true {
                /*caret*/return 1;
            }
            return 0;
        }
    """, "return 1", "return 0")

    fun `test highlight all returns with caret at fn keyword`() = doTest("""
        /*caret*/fn main() {
            if true {
                return 1;
            }
            return 0;
        }
    """, "return 1", "return 0")

    fun `test no highlighting with caret at fn keyword in fn pointer type`() = doTest("""
        fn main() {
            let _: /*caret*/fn();
            if true {
                return 1;
            }
            return 0;
        }
    """, "fn")

    fun `test highlight all returns with caret at returning type arrow`() = doTest("""
        fn main() /*caret*/-> i32 {
            if true {
                return 1;
            }
            return 0;
        }
    """, "return 1", "return 0")

    fun `test no highlighting with caret at arrow in a path`() = doTest("""
        fn main() {
            let _: Fn() /*caret*/-> i32;
            if true {
                return 1;
            }
            return 0;
        }
    """)

    fun `test no highlighting with caret at arrow in an fn pointer`() = doTest("""
        fn main() {
            let _: fn() /*caret*/-> i32;
            if true {
                return 1;
            }
            return 0;
        }
    """)

    fun `test highlight correct cfg block with an arrow`() = doTest("""
        fn foo() /*caret*/-> u8 {
            #[cfg(not(undeclared))]
            { 0 }

            #[cfg(undeclared)]
            { 1 }
        }

        fn main() {}
    """, "0")

    fun `test highlight correct cfg block with an arrow reverse order`() = doTest("""
        fn foo() /*caret*/-> u8 {
            #[cfg(undeclared)]
            { 0 }

            #[cfg(not(undeclared))]
            { 1 }
        }

        fn main() {}
    """, "1")

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test highlight try macro as return`() = doTest("""
        fn main() {
            if true {
                try!(Err(()))
            }
            /*caret*/return 0;
        }
    """, "try!(Err(()))", "return 0")

    fun `test highlight diverging macros as return`() = doTest("""
        fn main() {
            if true {
                panic!("test")
            } else {
                unimplemented!()
            }
            /*caret*/return 0;
        }
    """, "panic!(\"test\")", "unimplemented!()", "return 0")

    fun `test highlight diverging expressions as return`() = doTest("""
        fn diverge() -> ! { unimplemented!() }

        fn main() {
            if true {
                diverge();
            }
            /*caret*/return 0;
        }
    """, "diverge()", "return 0")

    fun `test highlight diverging expressions as return 2`() = doTest("""
        struct S;

        impl S {
           fn diverge(&self) -> ! { panic!() }
        }

        fn main() {
           let s = S;

           if true {
              /*caret*/return;
           }

           s.diverge();
        }
    """, "return", "s.diverge()")

    fun `test highlight ? operator as return`() = doTest("""
        fn main() {
            if true {
                Err(())?
            }
            return/*caret*/ 0;
        }
    """, "?", "return 0")

    fun `test highlight ? operator as return with caret at ?`() = doTest("""
        fn main() {
            if true {
                Err(())?/*caret*/
            }
            return 0;
        }
    """, "?", "return 0")

    fun `test highlight complex return as return`() = doTest("""
        struct S;
        impl S {
            fn foo(self) -> Result<S, i32> {Ok(self)}
            fn bar(self) -> S {self}
        }
        fn main() {
            let s = S;
            s.foo()?.bar().foo()?;
            return/*caret*/ 0;
        }
    """, "?", "?", "return 0")

    fun `test highlight last stmt lit as return`() = doTest("""
        fn test() {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            test();
            0
        }
    """, "return 1", "0")

    fun `test highlight last stmt call as return`() = doTest("""
        fn test() -> i32 {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            test()
        }
    """, "return 1", "test()")

    fun `test highlight last macro call as return`() = doTest("""
        macro_rules! test {
            () => { () }
        }
        fn main() {
            if true {
                /*caret*/return;
            }
            test!()
        }
    """, "return", "test!()")

    fun `test highlight should not highlight inner function`() = doTest("""
        fn main() {
            fn bar() {
                return 2;
            }
            /*caret*/return 1;
        }
    """, "return 1")

    fun `test highlight should not highlight inner lambda`() = doTest("""
        fn main() {
            let one = || { return 1; };
            /*caret*/return 2;
        }
    """, "return 2")

    fun `test highlight should not highlight outer function`() = doTest("""
        fn main() {
            let one = || { /*caret*/return 1; };
            return 2;
        }
    """, "return 1")

    fun `test highlight should not highlight outer function with caret at lambda return type arrow`() = doTest("""
        fn main() {
            let one = || /*caret*/-> i32 { return 1; };
            return 2;
        }
    """, "return 1")

    fun `test highlight last stmt if as return`() = doTest("""
        fn test() -> i32 {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            if false { 2 } else { 3 }
        }
    """, "return 1", "2", "3")

    fun `test highlight last stmt if in if and match as return`() = doTest("""
        fn test() -> i32 {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            if false {
                match None {
                    Some(_) => 2,
                    None => 3,
                }
            } else {
                if true {
                    4
                } else {
                    5
                }
            }
        }
    """, "return 1", "2", "3", "4", "5")

    fun `test highlight last stmt match as return`() = doTest("""
        fn test() -> i32 {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            match Some("test") {
                Some(s) => { 2 }
                _ => 3,
            }
        }
    """, "return 1", "2", "3")

    fun `test highlight last stmt match with inner as return`() = doTest("""
        fn test() -> Result<i32,i32> {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            match test()? {
                Some(s) => 2,
                _ => 3,
            }
        }
    """, "return 1", "?", "2", "3")

    fun `test highlight last stmt match with pat as return`() = doTest("""
        fn test() -> Result<i32,i32> {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            match "test" {
                "test" => 2,
                _ => 3,
            }
        }
    """, "return 1", "2", "3")

    fun `test highlight last stmt match in match and if as return`() = doTest("""
        fn test() -> Result<i32,i32> {}
        fn main() {
            if true {
                /*caret*/return 1;
            }
            match None {
                Some(_) => match "test" {
                    "test" => 2,
                    _ => 3,
                },
                _ => if true {
                    4
                } else {
                    5
                },
            }
        }
    """, "return 1", "2", "3", "4", "5")

    @MockEdition(CargoWorkspace.Edition.EDITION_2018)
    fun `test no highlight for ? in try `() = doTest("""
        fn main(){
            let a = try {
                Err(())?;
                return 0;
            }
            /*caret*/return 1;
        }
    """, "return 0", "return 1")

    fun `test highlight nothing on ? in try `() = doTest("""
        fn main(){
            let a = try {
                Err(())?/*caret*/;
                return 0;
            }
            return 1;
        }
    """)

    fun `test nested try`() = doTest("""
        fn foo() -> i32 {
            let a: Result<i32, ()> = try {
                let tmp: Result<i32, ()> = try { Err(())? };
                tmp? + Ok(42)? // wrong highlighting currently
            };
            return/*caret*/ 1;
        }
    """, "return 1")

    fun `test async block outside`() = doTest("""
        fn main(){
            let a = async {
                Err(())?;
                return 0;
            }
            return/*caret*/ 1;
        }
    """, "return 1")

    fun `test async block inside`() = doTest("""
        fn main(){
            let a = async {
                Err(())?;
                return/*caret*/ 0;
            }
            return 1;
        }
    """, "?", "return 0")


    fun `test ? in macro`() = doTest("""
        fn main(){
            macrocall![ ?/*caret*/ ];
            return 0;
        }
    """)

    fun `test return in macro`() = doTest("""
        macro_rules! foo {
            () => { /*caret*/return }
        }
    """)

    fun `test loop as return on exit break`() = doTest("""
        fn main() -> i32 {
            let a = loop { break 0; };
            return 1;
            'outer: loop {
                break/*caret*/ 2;
                return 3;
                loop {
                    break 5;
                    break 'outer 4;
                }
                6
            }
        }
    """, "return 1", "break 2", "return 3", "break 'outer 4")

    fun `test loop as return on not exit break`() = doTest("""
        fn main() -> i32 {
            return 1;
            'outer: loop {
                break 2;
                return 3;
                loop {
                    break/*caret*/ 5;
                    break 'outer 4;
                }
                6
            }
        }
    """)

    // Issue https://github.com/intellij-rust/intellij-rust/issues/7833
    fun `test string literals in macros are not highlighted`() = doTest("""
        fn foo() -> i32 {
            foo!("foobar");
            /*caret*/return 1;
        }
    """, "return 1")

    private fun doTest(@Language("Rust") check: String, vararg usages: String) {
        InlineFile(check)
        HighlightUsagesHandler.invoke(myFixture.project, myFixture.editor, myFixture.file)
        val highlighters = myFixture.editor.markupModel.allHighlighters
        val actual = highlighters.map { myFixture.file.text.substring(it.startOffset, it.endOffset) }.toList()
        assertSameElements(actual, usages.toList())
    }
}
