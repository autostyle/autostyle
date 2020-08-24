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

import com.github.autostyle.LineEnding
import com.github.autostyle.ResourceHarness
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.IOException
import java.util.function.Predicate
import java.util.stream.Collectors

@Execution(ExecutionMode.SAME_THREAD)
open class GradleIntegrationTest : ResourceHarness() {
    companion object {
        private val IS_UNIX = LineEnding.PLATFORM_NATIVE.str() == "\n"
        private val FILESYSTEM_RESOLUTION_MS = if (IS_UNIX) 2000 else 150
        fun buildResultToString(result: BuildResult) =
            result.tasks.joinToString("\n") { it.path + " " + it.outcome }
    }

    /**
     * Each test gets its own temp folder, and we create a gradle
     * build there and run it.
     *
     * Because those test folders don't have a .gitattributes file,
     * git (on windows) will default to \r\n. So now if you read a
     * test file from the Autostyle test resources, and compare it
     * to a build result, the line endings won't match.
     *
     * By sticking this .gitattributes file into the test directory,
     * we ensure that the default Autostyle line endings policy of
     * GIT_ATTRIBUTES will use \n, so that tests match the test
     * resources on win and linux.
     */
    @BeforeEach
    @Throws(IOException::class)
    fun gitAttributes() {
        setFile(".gitattributes").toContent("* text eol=lf")
    }

    @Throws(IOException::class)
    protected fun gradleRunner(): GradleRunner {
        // Minimal supported version is 4.8
        return GradleRunner.create()
            .withGradleVersion("5.4") // InputChanges
            .withProjectDir(rootFolder()) // .forwardOutput()
            .withPluginClasspath()
    }

    /** Dumps the complete file contents of the folder to the console.  */
    @get:Throws(IOException::class)
    protected val contents: String
        get() = getContents { subPath: String -> !subPath.startsWith(".gradle") }

    @Throws(IOException::class)
    protected fun getContents(subpathsToInclude: Predicate<String>): String {
        val files = rootFolder().walk().filter { it.isFile }.toList()
        val iterator = files.listIterator(files.size)
        val rootLength = rootFolder().absolutePath.length + 1
        return StringBuilder().apply {
            while (iterator.hasPrevious()) {
                val file = iterator.previous()
                val subPath = file.absolutePath.substring(rootLength)
                if (subpathsToInclude.test(subPath)) {
                    appendln("### $subPath ###")
                    appendln(read(subPath))
                }
            }
        }.toString()
    }

    @Throws(IOException::class)
    protected fun checkRunsThenUpToDate() {
        checkIsUpToDate(false)
        checkIsUpToDate(true)
    }

    @Throws(IOException::class)
    protected open fun applyIsUpToDate(upToDate: Boolean) {
        taskIsUpToDate("autostyleApply", upToDate)
    }

    @Throws(IOException::class)
    protected fun checkIsUpToDate(upToDate: Boolean) {
        taskIsUpToDate("autostyleCheck", upToDate)
    }

    fun pauseForFilesystem() {
        Thread.sleep(FILESYSTEM_RESOLUTION_MS.toLong())
    }

    @Throws(IOException::class)
    private fun taskIsUpToDate(task: String, upToDate: Boolean) {
        val buildResult = gradleRunner().withArguments(task).build()
        val result =
            buildResult.tasks.stream().filter { x: BuildTask -> x.path.endsWith("Process") }
                .findFirst().orElse(null)
        Assertions.assertEquals(
            if (upToDate) TaskOutcome.UP_TO_DATE else TaskOutcome.SUCCESS,
            result?.outcome
        ) { "...Process task outcome. All outcomes are " + buildResultToString(buildResult) }
    }

    @Throws(IOException::class)
    fun assertTaskOutcomes(task: String?, expected: Map<String?, TaskOutcome?>?) {
        pauseForFilesystem()
        val buildResult = gradleRunner().withArguments(task).build()
        Assertions.assertEquals(
            expected,
            buildResult.tasks
                .stream()
                .collect(Collectors.toMap({ obj: BuildTask -> obj.path }) { obj: BuildTask -> obj.outcome })
        )
    }
}
