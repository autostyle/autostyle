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
    }

    @Test
    fun `autostyleCheck is cacheable`() {
        writeBuildFile()
        setFile("README.md").toContent("abc")

        val runner = gradleRunner()

        runner.assertTaskOutcome(":autostyleMiscCheck", TaskOutcome.SUCCESS) {
            "The first execution of :autostyleMiscCheck"
        }

        setFile("README.md").toContent("abcd")

        runner.assertTaskOutcome(":autostyleMiscCheck", TaskOutcome.SUCCESS) {
            "The file modified => need execution"
        }

        setFile("README.md").toContent("abc")

        runner.assertTaskOutcome(":autostyleMiscCheck", TaskOutcome.FROM_CACHE) {
            "The file was reverted to a contents which was already seen"
        }
    }

    @Test
    fun `autostyleApply is not cacheable`() {
        writeBuildFile()
        setFile("README.md").toContent("abc")

        val runner = gradleRunner()

        runner.assertTaskOutcome(":autostyleMiscApply", TaskOutcome.SUCCESS) {
            "The first execution of :autostyleMiscApply"
        }

        setFile("README.md").toContent("abcd")

        runner.assertTaskOutcome(":autostyleMiscApply", TaskOutcome.SUCCESS) {
            "The file modified => need execution"
        }

        setFile("README.md").toContent("abc")

        runner.assertTaskOutcome(":autostyleMiscApply", TaskOutcome.SUCCESS) {
            "The file was reverted to a contents which was already seen, however the task is not cacheable"
        }
    }

    private fun GradleRunner.assertTaskOutcome(
        taskName: String,
        taskOutcome: TaskOutcome,
        message: () -> String
    ) {
        Assertions.assertEquals(
            taskOutcome,
            withArguments(taskName, "--build-cache").build().task(taskName)?.outcome,
            message
        )
    }
}
