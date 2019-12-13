/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

/** Tests the desired behavior from https://github.com/diffplug/spotless/issues/46.  */
class ErrorShouldRethrowTest : GradleIntegrationTest() {
    private fun writeBuild(formatSettings: String = "", enforceCheck: Boolean? = null) {
        val build = """
            plugins {
                id 'com.github.autostyle'
                id 'java'
            }
            autostyle {
                ${enforceCheck?.let { "enforceCheck $it" } }
                format 'misc', {
                    lineEndings 'UNIX'
                    target file('README.md')
                    custom 'no fu', 1, {
                         if (it.toLowerCase(Locale.ROOT).contains('fubar')) {
                             throw new RuntimeException('No fubar!');
                         }
                    }
                    custom 'no foo', 1, {
                         if (it.toLowerCase(Locale.ROOT).contains('foobar')) {
                             throw new RuntimeException('No foobar!');
                         }
                    }
                    $formatSettings
                }
            }
        """.trimIndent()
        setFile("build.gradle").toContent(build)
    }

    @Test
    fun passesIfNoException() {
        writeBuild()
        setFile("README.md").toContent("This code is fun.")
        runWithSuccess("> Task :autostyleMiscCheck")
    }

    @Test
    fun anyExceptionShouldFail() {
        writeBuild()
        setFile("README.md").toContent("This code is fubar.")
        runWithFailure(
            ":autostyleMiscStep 'no swearing' found problem in 'README.md':",
            "No swearing!",
            "java.lang.RuntimeException: No swearing!"
        )
    }

    @Test
    fun unlessEnforceCheckIsFalse() {
        writeBuild(enforceCheck = false)
        setFile("README.md").toContent("This code is fubar.")
        // autostyleCheck is not executed executed as a part of check since enforceCheck=false
        runWithSuccess(outcome = null)
    }

    private fun runWithSuccess(vararg messages: String, outcome: TaskOutcome? = TaskOutcome.SUCCESS) {
        val result = gradleRunner().withArguments("check").build()
        assertResultAndMessages(result, outcome, *messages)
    }

    private fun runWithFailure(vararg messages: String, outcome: TaskOutcome? = TaskOutcome.FAILED) {
        val result = gradleRunner().withArguments("check").buildAndFail()
        assertResultAndMessages(result, outcome, *messages)
    }

    private fun assertResultAndMessages(
        result: BuildResult,
        outcome: TaskOutcome?,
        vararg messages: String
    ) {
        assertThat(result.task(":autostyleMiscCheck")?.outcome).isEqualTo(outcome).`as`("autostyleMiscCheck.outcome")
//        val expectedToStartWith =
//            StringPrinter.buildStringFromLines(*messages).trim { it <= ' ' }
//        val numNewlines = CharMatcher.`is`('\n').countIn(expectedToStartWith)
//        val actualLines = LineEnding.toUnix(result.output).split('\n')
//        val actualStart = actualLines.subList(0, numNewlines + 1).joinToString("\n")
//        assertThat(actualStart).isEqualTo(expectedToStartWith)
//        assertThat(result.tasks(outcome).size + result.tasks(TaskOutcome.UP_TO_DATE).size)
//            .isEqualTo(result.tasks.size)
    }
}
