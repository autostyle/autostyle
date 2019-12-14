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
package com.github.autostyle

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.function.BiConsumer

class PaddedCellTest {
    private lateinit var tempDir: File

    @BeforeEach
    fun createFolder(@TempDir tempDir: File) {
        this.tempDir = tempDir
    }

    private fun misbehaved(
        step: FormatterFunc,
        input: String,
        expectedOutputType: PaddedCell.Type,
        steps: String,
        canonical: String?
    ) {
        testCase(step, input, expectedOutputType, steps, canonical, true)
    }

    private fun wellBehaved(
        step: FormatterFunc,
        input: String,
        expectedOutputType: PaddedCell.Type,
        canonical: String
    ) {
        testCase(step, input, expectedOutputType, canonical, canonical, false)
    }

    private fun testCase(
        step: FormatterFunc,
        input: String,
        expectedOutputType: PaddedCell.Type,
        expectedSteps: String,
        canonical: String?,
        misbehaved: Boolean
    ) {
        val formatterSteps: MutableList<FormatterStep> = ArrayList()
        formatterSteps.add(FormatterStep.createNeverUpToDate("step", step))
        Formatter.builder()
            .lineEndingsPolicy(LineEnding.UNIX.createPolicy())
            .encoding(StandardCharsets.UTF_8)
            .rootDir(tempDir.toPath())
            .steps(formatterSteps).build().use { formatter ->
                val file = File.createTempFile("input", "txt", tempDir)
                Files.write(file.toPath(), input.toByteArray(StandardCharsets.UTF_8))
                val result = PaddedCell.check(formatter, file)
                Assertions.assertEquals(misbehaved, result.misbehaved())
                Assertions.assertEquals(expectedOutputType, result.type())
                val actual = java.lang.String.join(",", result.steps())
                Assertions.assertEquals(expectedSteps, actual)
                if (canonical == null) {
                    try {
                        result.canonical()
                        Assertions.fail<Any>("Expected exception")
                    } catch (expected: IllegalArgumentException) {
                    }
                } else {
                    Assertions.assertEquals(canonical, result.canonical())
                }
            }
    }

    @Test
    fun wellBehaved() {
        wellBehaved(FormatterFunc { it }, "CCC", PaddedCell.Type.CONVERGE, "CCC")
        wellBehaved(FormatterFunc { "A" }, "CCC", PaddedCell.Type.CONVERGE, "A")
    }

    @Test
    fun pingPong() {
        misbehaved(
            FormatterFunc { input: String -> if (input == "A") "B" else "A" },
            "CCC",
            PaddedCell.Type.CYCLE,
            "A,B",
            "A"
        )
    }

    @Test
    fun fourState() {
        misbehaved(FormatterFunc {
            when (it) {
                "A" -> "B"
                "B" -> "C"
                "C" -> "D"
                else -> "A"
            }
        }, "CCC", PaddedCell.Type.CYCLE, "A,B,C,D", "A")
    }

    @Test
    fun converging() {
        misbehaved(FormatterFunc {
            if (it.isEmpty()) {
                it
            } else {
                it.substring(0, it.length - 1)
            }
        }, "CCC", PaddedCell.Type.CONVERGE, "CC,C,", "")
    }

    @Test
    fun diverging() {
        misbehaved(
            FormatterFunc { "$it " },
            "",
            PaddedCell.Type.DIVERGE,
            " ,  ,   ,    ,     ,      ,       ,        ,         ,          ",
            null
        )
    }

    @Test
    fun cycleOrder() {
        val testCase = BiConsumer<String, String> { unorderedStr: String, canonical: String? ->
            val unordered = unorderedStr.split(",".toRegex())
            for (i in unordered.indices) { // try every rotation of the list
                Collections.rotate(unordered, 1)
                val result = PaddedCell.Type.CYCLE.create(tempDir, unordered)
                // make sure the canonical result is always the appropriate one
                Assertions.assertEquals(canonical, result.canonical())
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
