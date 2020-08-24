/*
 * Copyright 2016 DiffPlug
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

import com.github.autostyle.ResourceHarness
import com.github.autostyle.extra.integration.DiffMessageFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class DiffMessageFormatterTest : ResourceHarness() {
    val sb = StringBuilder()
    val writer by lazy { DiffMessageFormatter(rootFolder(), sb) }

    private fun assertDiff(expected: String) {
        val msg = if (writer.finishWithoutErrors()) "EMPTY" else sb.toString()
        assertThat(msg.trimEnd()).isEqualTo(expected)
    }

    private fun addFile(name: String, old: String, new: String) {
        val a = setFile(name).toContent(old)
        val b = setFile("$name-new").toContent(new)
        writer.addDiff(a, b, StandardCharsets.UTF_8)
    }

    private fun addFile(name: String, old: String, transformer: (String) -> String = { it }) {
        addFile(name, old, transformer(old.replace("\r\n", "\n")))
    }

    @Test
    fun lineEndingProblem() {
        addFile("testFile", "A\r\nB\r\nC\r\n", "A\nB\nC\n")
        assertDiff(
            """
            testFile
              @@ -1,3 +1,3 @@
              -A␍␊
              -B␍␊
              -C␍␊
              +A␊
              +B␊
              +C␊
            """.replaceIndent("  ")
        )
    }

    @Test
    fun whitespaceProblem() {
        addFile("testFile", "A \nB\t\nC  \n") { input ->
            val pattern = Pattern.compile(
                "[ \t]+$",
                Pattern.UNIX_LINES or Pattern.MULTILINE
            )
            pattern.matcher(input).replaceAll("")
        }
        assertDiff(
            """
            testFile
              @@ -1,3 +1,3 @@
              -A·␊
              -B⇥␊
              -C··␊
              +A␊
              +B␊
              +C␊
            """.replaceIndent("  ")
        )
    }

    @Test
    fun whitespaceProblem2() {
        addFile(
            "testFile",
            "u0\nu 1\nu 2\nu 3\n" +
                    "\t leading space\n" +
                    "trailing space\t \n" +
                    "u 4\n" +
                    "  leading and trailing space  \n" +
                    "u 5\nu 6"
        ) { input ->
            val pattern = Pattern.compile(
                "(^[ \t]+)|([ \t]+$)",
                Pattern.UNIX_LINES or Pattern.MULTILINE
            )
            pattern.matcher(input).replaceAll("")
        }
        assertDiff(
            """
            testFile
              @@ -2,9 +2,9 @@
               u 1
               u 2
               u 3
              -⇥·leading·space␊
              -trailing·space⇥·␊
              +leading·space␊
              +trailing·space␊
               u 4
              -··leading·and·trailing·space··␊
              +leading·and·trailing·space␊
               u 5
               u 6
            """.replaceIndent("  ")
        )
    }

    @Test
    fun whitespaceProblemDiffChaining() {
        addFile(
            "testFile",
            "u0\ntrailing space\t \n" +
                    "u1\nu2\nu3\nu4\nu5\nu6\nu7\n" +
                    "trailing space2 \nu8"
        ) { input ->
            val pattern = Pattern.compile(
                "(^[ \t]+)|([ \t]+$)",
                Pattern.UNIX_LINES or Pattern.MULTILINE
            )
            pattern.matcher(input).replaceAll("")
        }
        assertDiff(
            """
            testFile
              @@ -1,11 +1,11 @@
               u0
              -trailing·space⇥·␊
              +trailing·space␊
               u1
               u2
               u3
               u4
               u5
               u6
               u7
              -trailing·space2·␊
              +trailing·space2␊
               u8
            """.replaceIndent("  ")
        )
    }

    @Test
    fun whitespaceProblemDiffChaining2() {
        addFile(
            "testFile",
            "u0\ntrailing space\t \n" +
                    "u1\nu2\nu3\nu4\nu5\nu6\nu7\nu8\n" +
                    "trailing space2 \nu9"
        ) { input ->
            val pattern = Pattern.compile(
                "(^[ \t]+)|([ \t]+$)",
                Pattern.UNIX_LINES or Pattern.MULTILINE
            )
            pattern.matcher(input).replaceAll("")
        }
        assertDiff(
            """
            testFile
              @@ -1,5 +1,5 @@
               u0
              -trailing·space⇥·␊
              +trailing·space␊
               u1
               u2
               u3
              @@ -8,5 +8,5 @@
               u6
               u7
               u8
              -trailing·space2·␊
              +trailing·space2␊
               u9
            """.replaceIndent("  ")
        )
    }

    @Test
    fun singleLineCr() {
        addFile(
            "testFile",
            "line without line ending"
        ) { input: String -> if (input.endsWith("\n")) input else input + "\n" }
        assertDiff(
            """
            testFile
              @@ -1 +1 @@
              -line·without·line·ending
              +line·without·line·ending␊
            """.replaceIndent("  ")
        )
    }

    @Test
    fun singleLineUnnecessaryCr() {
        addFile(
            "testFile",
            "line without line ending\r\n"
        ) { input: String -> input.replace("[\r\n]+$".toRegex(), "") }
        assertDiff(
            """
            testFile
              @@ -1 +1 @@
              -line·without·line·ending␍␊
              +line·without·line·ending
            """.replaceIndent("  ")
        )
    }

    @Test
    fun trailingWhitespaceAndCRLF() {
        addFile(
            "testFile",
            "line 1\ntrailing whitespace  \nline with CRLF\r\nline 2"
        ) { input: String -> input.replace("[\r ]+\n".toRegex(), "\n") }
        assertDiff(
            """
            testFile
              @@ -1,4 +1,4 @@
               line 1
              -trailing·whitespace··␊
              -line·with·CRLF␍␊
              +trailing·whitespace␊
              +line·with·CRLF␊
               line 2
            """.replaceIndent("  ")
        )
    }

    @Test
    fun multipleFiles() {
        addFile("A", "1\r\n2\r\n")
        addFile("B", "3\n4\r\n")
        assertDiff(
            """
            A
              @@ -1,2 +1,2 @@
              -1␍␊
              -2␍␊
              +1␊
              +2␊
            B
              @@ -1,2 +1,2 @@
               3
              -4␍␊
              +4␊
            """.replaceIndent("  ")
        )
    }

    @Test
    fun manyFiles() {
        for (i in 0 until 9 + DiffMessageFormatter.MAX_FILES_TO_LIST - 1) {
            addFile("$i.txt", "1\r\n2\r\n")
        }
        assertDiff(
            """
              0.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              1.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              10.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              11.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              12.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              13.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              14.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              15.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              16.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
              ... (2 more lines that didn't fit)
            Violations also present in:
              17.txt
              2.txt
              3.txt
              4.txt
              5.txt
              6.txt
              7.txt
              8.txt
              9.txt
            """.trimIndent()
        )
    }

    @Test
    fun manyManyFiles() {
        for (i in 0 until 10 + DiffMessageFormatter.MAX_FILES_TO_LIST) {
            addFile("$i.txt", "1\r\n2\r\n")
        }
        assertDiff(
            """
              0.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              1.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              10.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              11.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              12.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              13.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              14.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              15.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
                +1␊
                +2␊
              16.txt
                @@ -1,2 +1,2 @@
                -1␍␊
                -2␍␊
              ... (2 more lines that didn't fit)
            Violations also present in ${DiffMessageFormatter.MAX_FILES_TO_LIST + 1} other files.
            """.trimIndent()
        )
    }

    @Test
    fun longFile() {
        val builder = StringBuilder()
        for (i in 0..999) {
            builder.append(i)
            builder.append("\r\n")
        }
        addFile("testFile", builder.toString())
        assertDiff(
            """
            testFile
              @@ -1,1000 +1,1000 @@
              -0␍␊
              -1␍␊
              -2␍␊
              -3␍␊
              -4␍␊
              -5␍␊
              -6␍␊
              -7␍␊
              -8␍␊
              -9␍␊
              -10␍␊
              -11␍␊
              -12␍␊
              -13␍␊
              -14␍␊
              -15␍␊
              -16␍␊
              -17␍␊
              -18␍␊
              -19␍␊
              -20␍␊
              -21␍␊
              -22␍␊
              -23␍␊
              -24␍␊
              -25␍␊
              -26␍␊
              -27␍␊
              -28␍␊
              -29␍␊
              -30␍␊
              -31␍␊
              -32␍␊
              -33␍␊
              -34␍␊
              -35␍␊
              -36␍␊
              -37␍␊
              -38␍␊
              -39␍␊
              -40␍␊
              -41␍␊
              -42␍␊
              -43␍␊
              -44␍␊
              -45␍␊
              -46␍␊
              -47␍␊
            ... (1952 more lines that didn't fit)
            """.replaceIndent("  ")
        )
    }

    @Test
    fun oneMoreLineThatDidntFit() {
        // com.github.autostyle.extra.integration.DiffMessageFormatter.MAX_CHECK_MESSAGE_LINES
        // defaults to 50, so we create a diff that would be exactly 50 lines long
        // The test is to ensure diff does not contain "1 more lines that didn't fit"
        val builder = StringBuilder()
        for (i in 0..24) {
            builder.append(i)
            builder.append(if (i < 1) "\n" else "\r\n")
        }
        addFile("testFile", builder.toString())
        assertDiff(
            """
            testFile
              @@ -1,25 +1,25 @@
               0
              -1␍␊
              -2␍␊
              -3␍␊
              -4␍␊
              -5␍␊
              -6␍␊
              -7␍␊
              -8␍␊
              -9␍␊
              -10␍␊
              -11␍␊
              -12␍␊
              -13␍␊
              -14␍␊
              -15␍␊
              -16␍␊
              -17␍␊
              -18␍␊
              -19␍␊
              -20␍␊
              -21␍␊
              -22␍␊
              -23␍␊
              -24␍␊
              +1␊
              +2␊
              +3␊
              +4␊
              +5␊
              +6␊
              +7␊
              +8␊
              +9␊
              +10␊
              +11␊
              +12␊
              +13␊
              +14␊
              +15␊
              +16␊
              +17␊
              +18␊
              +19␊
              +20␊
              +21␊
              +22␊
              +23␊
              +24␊
            """.replaceIndent("  ")
        )
    }
}
