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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildCacheTest : GradleIntegrationTest() {
    /** Requires that README be lowercase.  */
    private fun writeBuildFile() {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            autostyle {
                format 'misc', {
                    target file('README.md')
                    custom('lowercase', 1) { str -> str.toLowerCase(Locale.ROOT) }
                }
            }
            """.trimIndent()
        )
        setFile("gradle.properties").toContent(
            """
                org.gradle.caching=true
                org.gradle.caching.debug=true
            """.trimIndent()
        )
        setFile("settings.gradle").toContent(
            """
                buildCache {
                    local {
                        directory '${rootFolder().toURI()}'
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun `autostyleMiscProcess is cacheable`() {
        writeBuildFile()
        setFile("README.md").toContent("abc")

        val runner = gradleRunner()

        runner.assertTaskOutcome(
            ":autostyleMiscCheck",
            mapOf(":autostyleMiscProcess" to TaskOutcome.SUCCESS)
        ) {
            "The first execution of :autostyleMiscCheck"
        }

        setFile("README.md").toContent("abcd")

        runner.assertTaskOutcome(":autostyleMiscCheck",
            mapOf(":autostyleMiscProcess" to TaskOutcome.SUCCESS)) {
            "The file modified => need execution"
        }

        setFile("README.md").toContent("abc")

        runner.assertTaskOutcome(":autostyleMiscCheck",
            mapOf(":autostyleMiscProcess" to TaskOutcome.FROM_CACHE)) {
            "The file was reverted to a contents which was already seen"
        }
    }

    private fun GradleRunner.assertTaskOutcome(
        taskName: String,
        expected: Map<String, TaskOutcome>,
        message: () -> String
    ) {
        Assertions.assertEquals(
            expected,
            withArguments(taskName, "--build-cache").build().let { result ->
                expected.mapValues { result.task(it.key)?.outcome }
            },
            message
        )
    }
}
