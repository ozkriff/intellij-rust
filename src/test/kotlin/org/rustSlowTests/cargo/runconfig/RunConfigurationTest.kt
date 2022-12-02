/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rustSlowTests.cargo.runconfig

import org.rust.fileTree

class RunConfigurationTest : RunConfigurationTestBase() {

    fun `test application configuration`() {
        fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    fn main() {
                        println!("Hello, world!");
                    }
                """)
            }
        }.create()
        val configuration = createConfiguration()
        val result = executeAndGetOutput(configuration)

        check("Hello, world!" in result.stdout)
    }

    // TODO: fix & move to some other place!
    fun `test build script configuration`() {
        fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello-build"
                version = "0.1.0"
                authors = []
            """)
            rust("build.rs", """
                fn main() {
                    println!("Hello, world!");
                }
            """)
            dir("src") {
                rust("lib.rs", """
                    // TODO: ?
                """)
            }
        }.create()
        val configuration = createConfiguration()
        val result = executeAndGetOutput(configuration)

        check("Hello, world!" in result.stdout)
    }

    fun `test single test configuration 1`() {
        val testProject = fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    fn main() {
                        println!("Hello, world!");
                    }

                    #[test]
                    fn foo() {
                        /*caret*/
                    }
                """)
            }
        }.create()
        myFixture.configureFromTempProjectFile(testProject.fileWithCaret)

        val configuration = createTestRunConfigurationFromContext()
        val result = executeAndGetOutput(configuration)
        check("""{ "type": "test", "event": "started", "name": "foo" }""" in result.stdout)
    }

    fun `test single test configuration 2`() {
        val testProject = fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    fn main() {
                        println!("Hello, world!");
                    }

                    #[cfg(test)]
                    mod tests {
                        #[test]
                        fn foo() {
                            /*caret*/
                        }

                        #[test]
                        fn bar() {}
                    }
                """)
            }
        }.create()
        myFixture.configureFromTempProjectFile(testProject.fileWithCaret)

        val configuration = createTestRunConfigurationFromContext()
        val result = executeAndGetOutput(configuration)
        check("""{ "type": "test", "event": "started", "name": "tests::foo" }""" in result.stdout)
        check("""{ "type": "test", "event": "started", "name": "tests::bar" }""" !in result.stdout)
    }

    fun `test mod test configuration`() {
        val testProject = fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    fn main() {
                        println!("Hello, world!");
                    }

                    #[cfg(test)]
                    mod tests {
                        /*caret*/
                        #[test]
                        fn foo() {}

                        #[test]
                        fn bar() {}
                    }
                """)
            }
        }.create()
        myFixture.configureFromTempProjectFile(testProject.fileWithCaret)

        val configuration = createTestRunConfigurationFromContext()
        val result = executeAndGetOutput(configuration)
        check("""{ "type": "test", "event": "started", "name": "tests::foo" }""" in result.stdout)
        check("""{ "type": "test", "event": "started", "name": "tests::bar" }""" in result.stdout)
    }

    fun `test redirect input (absolute path)`() {
        fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    use std::io::{self, BufRead};

                    fn main() {
                        let stdin = io::stdin();
                        let mut iter = stdin.lock().lines();
                        println!("{}", iter.next().unwrap().unwrap());
                        println!("{}", iter.next().unwrap().unwrap());
                    }
                """)
            }

            file("in.txt", """
                1. aaa
                2. bbb
                3. ccc
            """)
        }.create()

        val configuration = createConfiguration()
            .apply {
                isRedirectInput = true
                redirectInputPath = workingDirectory?.resolve("in.txt").toString()
            }

        val result = executeAndGetOutput(configuration)
        val stdout = result.stdout
        check("1. aaa" in stdout)
        check("2. bbb" in stdout)
        check("3. ccc" !in stdout)
    }

    fun `test redirect input (relative path)`() {
        fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    use std::io::{self, BufRead};

                    fn main() {
                        let stdin = io::stdin();
                        let mut iter = stdin.lock().lines();
                        println!("{}", iter.next().unwrap().unwrap());
                        println!("{}", iter.next().unwrap().unwrap());                    }
                """)
            }

            file("in.txt", """
                1. aaa
                2. bbb
                3. ccc
            """)
        }.create()

        val configuration = createConfiguration()
            .apply {
                isRedirectInput = true
                redirectInputPath = "in.txt"
            }

        val result = executeAndGetOutput(configuration)
        val stdout = result.stdout
        check("1. aaa" in stdout)
        check("2. bbb" in stdout)
        check("3. ccc" !in stdout)
    }

    fun `test toolchain override`() {
        fileTree {
            toml("Cargo.toml", """
                [package]
                name = "hello"
                version = "0.1.0"
                authors = []
            """)

            dir("src") {
                rust("main.rs", """
                    #![feature(if_let)]

                    fn main() {
                        println!("Hello, world!");
                    }
                """)
            }
        }.create()
        val configuration = createConfiguration("+nightly run")
        val result = executeAndGetOutput(configuration)

        check("Hello, world!" in result.stdout)
    }
}
