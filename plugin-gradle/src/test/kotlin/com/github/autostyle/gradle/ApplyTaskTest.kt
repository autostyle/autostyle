/*
 * Copyright 2020, Vladimir Sitnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.autostyle.gradle

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File

@Execution(ExecutionMode.SAME_THREAD)
class ApplyTaskTest : GradleIntegrationTest() {
    private val runner by lazy { gradleRunner() }
    lateinit var file: File

    /** Requires that README be lowercase.  */
    private fun writeBuildFile(format: String) {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            autostyle {
                format 'misc', {
                    target file('src/README.md')
                    custom('lowercase', 1) { str -> $format }
                }
            }
            """.trimIndent()
        )
        file = setFile("src/README.md").toContent("CCC")
    }

    private fun applyTask(success: Boolean = true, expected: String) {
        val runner = runner.withArguments("autostyleApply")
        if (success) {
            runner.build()
        } else {
            runner.buildAndFail()
        }
        Assertions.assertEquals(expected, file.readText())
    }

    /**
     * Escapes CR and LF as the literal text `\r` and `\n`, so a line-ending mismatch stays legible
     * in an `assertEquals` failure.
     */
    private fun String.escapeLineEndings() =
        replace("\r", "\\r").replace("\n", "\\n")

    @Test
    fun lowercase() {
        writeBuildFile("'42'")
        applyTask(true, "42")
    }

    @Test
    fun appliesWindowsLineEndings() {
        setFile("build.gradle").toContent(
            """
        plugins {
            id 'com.github.autostyle'
        }
        autostyle {
            lineEndings = com.github.autostyle.LineEnding.WINDOWS
            format 'misc', {
                target file('src/README.md')
                endWithNewline()
            }
        }
        """.trimIndent()
        )

        val source = setFile("src/README.md").toContent("first\nsecond\n")

        runner.withArguments("autostyleApply").build()

        Assertions.assertEquals(
            "first\r\nsecond\r\n".escapeLineEndings(),
            source.readText().escapeLineEndings()
        )

        runner.withArguments("autostyleCheck").build()

        val formatted = newFile(
            "build/autostyle/autostyleMiscProcess/formatted/src/README.md"
        )
        Assertions.assertFalse(formatted.exists())

        runner.withArguments("autostyleApply").build()
        Assertions.assertEquals(
            "first\r\nsecond\r\n".escapeLineEndings(),
            source.readText().escapeLineEndings()
        )

        runner.withArguments("autostyleCheck").build()
        Assertions.assertFalse(formatted.exists())
    }

    @Test
    fun appliesUnixLineEndings() {
        setFile("build.gradle").toContent(
            """
        plugins {
            id 'com.github.autostyle'
        }
        autostyle {
            lineEndings = com.github.autostyle.LineEnding.UNIX
            format 'misc', {
                target file('src/README.md')
                endWithNewline()
            }
        }
        """.trimIndent()
        )

        val source = setFile("src/README.md").toContent("first\r\nsecond\r\n")

        runner.withArguments("autostyleApply").build()

        Assertions.assertEquals(
            "first\nsecond\n".escapeLineEndings(),
            source.readText().escapeLineEndings()
        )

        runner.withArguments("autostyleCheck").build()
    }

    @Test
    fun appliesGitAttributesLineEndings() {
        // GradleIntegrationTest's @BeforeEach writes a ".gitattributes" of "* text eol=lf" into
        // every test project; this test replaces it so *.md resolves to CRLF under the default policy.
        setFile(".gitattributes").toContent("*.md text eol=crlf\n")

        setFile("build.gradle").toContent(
            """
        plugins {
            id 'com.github.autostyle'
        }
        autostyle {
            format 'misc', {
                target file('src/README.md')
                endWithNewline()
            }
        }
        """.trimIndent()
        )

        val source = setFile("src/README.md").toContent("first\nsecond\n")

        runner.withArguments("autostyleApply").build()

        Assertions.assertEquals(
            "first\r\nsecond\r\n".escapeLineEndings(),
            source.readText().escapeLineEndings()
        )

        runner.withArguments("autostyleCheck").build()
    }

    @Test
    fun cycle() {
        writeBuildFile("if (str == 'A') 'B' else 'A'")
        applyTask(false, "CCC")
    }

    @Test
    fun converge() {
        writeBuildFile("if (str == '') '' else str.substring(1)")
        applyTask(true, "")
    }

    @Test
    fun diverge() {
        writeBuildFile("str + '_'")
        applyTask(false, "CCC")
    }
}
