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

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class UpToDateTest : GradleIntegrationTest() {
    /** Requires that README be lowercase.  */
    private fun writeBuildFile() {
        setFile("build.gradle").toLines(
            "plugins {",
            "    id 'com.github.autostyle'",
            "}",
            "autostyle {",
            "    format 'misc', {",
            "        target file('README.md')",
            "        custom('lowercase', 1) { str -> str.toLowerCase(Locale.ROOT) }",
            "    }",
            "}"
        )
    }

    @Test
    fun testNormalCase() {
        writeBuildFile()
        setFile("README.md").toContent("ABC")
        // first time, the task runs as expected
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("abc")
        // because a file was changed (by Autostyle),
        // up-to-date is false, even though nothing is
        // going to change during this run.  This second
        // run is very fast though, because it will
        // only run on the few files that were changed.
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
        // it's not until the third run that everything
        // is totally up-to-date
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.UP_TO_DATE,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
    }

    @Test
    fun testNearPathologicalCase() {
        writeBuildFile()
        setFile("README.md").toContent("ABC")
        // first time, up-to-date is false
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("abc")

        // now we'll change the file
        setFile("README.md").toContent("AB")
        // as expected, the task will run again
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("ab")
        // and it'll take two more runs to get to up-to-date
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.UP_TO_DATE,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
    }

    @Test
    fun testPathologicalCase() {
        writeBuildFile()
        setFile("README.md").toContent("ABC")
        // first time, up-to-date is false
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("abc")

        // now we'll change the file back to EXACTLY its original content
        setFile("README.md").toContent("ABC")
        // Process task should be up to date, but apply still needs to run to apply the changes
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.UP_TO_DATE,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("abc")

        // Let's repeat.
        setFile("README.md").toContent("ABC")
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.UP_TO_DATE,
                ":autostyleMiscApply" to TaskOutcome.SUCCESS,
                ":autostyleApply" to TaskOutcome.SUCCESS
            )
        )
        assertFile("README.md").hasContent("abc")

        // and it'll take two more runs to get to up-to-date
        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.SUCCESS,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
        assertFile("README.md").hasContent("abc")

        assertTaskOutcomes(
            ":autostyleApply", mapOf(
                ":autostyleMiscProcess" to TaskOutcome.UP_TO_DATE,
                ":autostyleMiscApply" to TaskOutcome.UP_TO_DATE,
                ":autostyleApply" to TaskOutcome.UP_TO_DATE
            )
        )
        assertFile("README.md").hasContent("abc")
    }
}
