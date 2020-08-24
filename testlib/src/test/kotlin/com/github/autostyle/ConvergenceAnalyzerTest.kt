/*
 * Copyright 2020 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
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
package com.github.autostyle

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.function.BiConsumer

class ConvergenceAnalyzerTest {
    private lateinit var tempDir: File

    @BeforeEach
    fun createFolder(@TempDir tempDir: File) {
        this.tempDir = tempDir
    }

    private fun testCase(
        input: String,
        expected: ConvergenceResult,
        formatted: String?,
        formatterFunc: (String) -> String
    ) {
        val step = FormatterFunc { formatterFunc(it) }
        val formatterSteps: MutableList<FormatterStep> = ArrayList()
        formatterSteps.add(FormatterStep.createNeverUpToDate("step", step))
        Formatter.builder()
            .lineEndingsPolicy(LineEnding.UNIX.createPolicy())
            .encoding(StandardCharsets.UTF_8)
            .rootDir(tempDir.toPath())
            .steps(formatterSteps).build().use { formatter ->
                val file = File.createTempFile("input", "txt", tempDir)
                Files.write(file.toPath(), input.toByteArray(StandardCharsets.UTF_8))
                val analyzer = ConvergenceAnalyzer(formatter)
                val actual = analyzer.analyze(file)
                Assertions.assertEquals(expected, actual) {
                    "input: $input"
                }
                if (expected == ConvergenceResult.Clean) {
                    return
                }
                if (formatted == null) {
                    Assertions.assertThrows(IllegalStateException::class.java) {
                        actual.formatted
                    }
                } else {
                    Assertions.assertEquals(formatted, actual.formatted) {
                        "input: $input"
                    }
                }
            }
    }

    @Test
    fun wellBehaved() {
        testCase("CCC", ConvergenceResult.Clean, null) { it }
        testCase("CCC", ConvergenceResult.Convergence(listOf("A")), "A") { "A" }
    }

    @Test
    fun pingPong() {
        testCase(
            "CCC",
            ConvergenceResult.Cycle(listOf("A", "B")),
            "A"
        ) { input: String -> if (input == "A") "B" else "A" }
    }

    @Test
    fun fourState() {
        testCase("CCC", ConvergenceResult.Cycle(listOf("A", "B", "C", "D")), "A") {
            when (it) {
                "A" -> "B"
                "B" -> "C"
                "C" -> "D"
                else -> "A"
            }
        }
    }

    @Test
    fun converging() {
        testCase("CCC", ConvergenceResult.Convergence(listOf("CC", "C", "")), "") {
            if (it.isEmpty()) {
                it
            } else {
                it.substring(0, it.length - 1)
            }
        }
    }

    @Test
    fun diverging() {
        testCase(
            "",
            ConvergenceResult.Divergence(listOf(" ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          ")),
            null
        ) { "$it " }
    }

    @Test
    fun cycleOrder() {
        val testCase = BiConsumer<String, String> { unorderedStr: String, canonical: String? ->
            val unordered = unorderedStr.split(",".toRegex())
            for (i in unordered.indices) { // try every rotation of the list
                Collections.rotate(unordered, 1)
                val result = ConvergenceResult.Cycle(unordered)
                // make sure the canonical result is always the appropriate one
                Assertions.assertEquals(canonical, result.formatted)
            }
        }
        // alphabetic
        testCase.accept("a,b,c", "a")
        // length
        testCase.accept("a,aa,aaa", "a")
        // length > alphabetic
        testCase.accept("b,aa,aaa", "b")
    }
}
