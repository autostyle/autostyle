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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringWriter
import java.util.*

class GradleIncrementalResolutionTest : GradleIntegrationTest() {
    @Test
    fun failureDoesntTriggerAll() {
        setFile("build.gradle").toLines(
            "plugins {",
            "    id 'com.github.autostyle'",
            "}",
            "autostyle {",
            "    format 'misc', {",
            "        filter.include '*.md'",
            "        custom 'lowercase', 1, { str ->",
            "            String result = str.toLowerCase(Locale.ROOT)",
            "            println(\"<\${result.trim()}>\")",
            "            return result",
            "        }",
            "    }",
            "}"
        )
        // test our harness (build makes things lower case)
        writeState("ABC")
        assertState("ABC")
        writeState("aBc")
        assertState("aBc")
        // First run => need to process all three files
        checkRanAgainst("abc")
        // Files processed => no need to process them anymore
        checkRanAgainst("")
        checkRanAgainst("")

        // Apply won't re-execute formatters
        applyRanAgainst("")
        // the second time, it will only run on the file that was changes
        applyRanAgainst("b")
        // and nobody the last time
        applyRanAgainst("")
        checkRanAgainst("")

        // if we change just one file
        writeState("Abc")
        // Only A is changed, so check is executed against a only
        checkRanAgainst("a")
        // No files modified => no need to re-format
        checkRanAgainst("")
        // and so does apply
        applyRanAgainst()
        applyRanAgainst("a")
        // until the issue has been fixed
        applyRanAgainst("")
    }

    private fun filename(name: String): String {
        return name.toLowerCase(Locale.ROOT) + ".md"
    }

    private fun writeState(state: String) {
        for (c in state.toCharArray()) {
            val letter = String(charArrayOf(c))
            val exists = File(rootFolder(), filename(letter)).exists()
            val needsChanging = exists && read(filename(letter)).trim() != letter
            if (!exists || needsChanging) {
                setFile(filename(letter)).toContent(letter)
            }
        }
    }

    private fun assertState(state: String) {
        for (c in state.toCharArray()) {
            val letter = String(charArrayOf(c))
            if (Character.isLowerCase(c)) {
                Assertions.assertEquals(
                    letter.toLowerCase(Locale.ROOT),
                    read(filename(letter)).trim())
            } else {
                Assertions.assertEquals(
                    letter.toUpperCase(Locale.ROOT),
                    read(filename(letter)).trim())
            }
        }
    }

    private fun applyRanAgainst(vararg ranAgainst: String) {
        taskRanAgainst("autostyleApply", *ranAgainst)
    }

    private fun checkRanAgainst(vararg ranAgainst: String) {
        taskRanAgainst("autostyleCheck", *ranAgainst)
    }

    private fun taskRanAgainst(task: String, vararg ranAgainst: String) {
        pauseForFilesystem()
        val console = StringWriter().also { writer ->
            val runner = gradleRunner().withArguments(task).forwardStdOutput(writer)
            if (task == "autostyleCheck" && !isClean) {
                runner.buildAndFail()
            } else {
                runner.build()
            }
        }.toString()
        val added = TreeSet<String>()
        for (line in console.split("\n".toRegex()).toTypedArray()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                added.add(trimmed.substring(1, trimmed.length - 1))
            }
        }
        Assertions.assertEquals(ranAgainst.joinToString(""), added.joinToString(""))
    }

    private val isClean: Boolean
        get() = rootFolder().listFiles { it: File ->
            it.isFile && it.name.length == 4 && it.name.endsWith(".md")
        }
            ?.all { read(it.name).let { c -> c == c.toLowerCase(Locale.ROOT) } } == true
}
