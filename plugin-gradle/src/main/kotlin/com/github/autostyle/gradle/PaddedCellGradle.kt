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

import com.github.autostyle.Formatter
import com.github.autostyle.PaddedCell
import com.github.autostyle.PaddedCellBulk
import org.gradle.api.GradleException
import java.io.File

/**
 * Incorporates the PaddedCell machinery into AutostyleTask.apply() and AutostyleTask.check().
 *
 * Here's the general workflow:
 *
 * ### Identify that paddedCell is needed
 *
 * Initially, paddedCell is off.  That's the default, and there's no need for users to know about it.
 *
 * If they encounter a scenario where `autostyleCheck` fails after calling `autostyleApply`, then they would
 * justifiably be frustrated.  Luckily, every time `autostyleCheck` fails, it passes the failed files to
 * [.anyMisbehave], which checks to see if any of the rules are causing a cycle
 * or some other kind of mischief.  If they are, it throws a special error message,
 * [.youShouldTurnOnPaddedCell] which tells them to turn on paddedCell.
 *
 * ### autostyleCheck with paddedCell on
 *
 * Autostyle check behaves as normal, finding a list of problem files, but then passes that list
 * to [.check].  If there were no problem files, then `paddedCell`
 * is no longer necessary, so users might as well turn it off, so we give that info as a warning.
 */
// TODO: Cleanup this javadoc - it's a copy of the javadoc of PaddedCellBulk, so some info
// is out-of-date (like the link to #anyMisbehave(Formatter, List))
internal object PaddedCellGradle {
    /** URL to a page which describes the padded cell thing.  */
    private const val URL =
        "https://github.com/autostyle/autostyle/blob/master/PADDEDCELL.md"

    fun youShouldTurnOnPaddedCell(
        task: AutostyleTask,
        cell: PaddedCell
    ): GradleException {
        val rootPath = task.project.rootDir.toPath()
        val diagnoseDir = diagnoseDir(task)
        diagnoseDir.mkdirs()
        val name = cell.file().nameWithoutExtension
        val extension = cell.file().extension

        for ((index, step) in cell.steps().withIndex()) {
            File(diagnoseDir, "$name.${index + 1}.$extension").writeText(step)
        }

        return GradleException(
            """
            You have a misbehaving rule which can't make up its mind.
            This means that autostyleCheck will fail even after autostyleApply has run.

            The file in question is ${rootPath.relativize(cell.file().toPath())}
            Formatting ${cell.userMessage()}
            You can find intermediate results in ${rootPath.relativize(diagnoseDir.toPath())}

            This is a bug in a formatting rule, not Autostyle itself, but Autostyle can
            work around this bug and generate helpful bug reports for the broken rule
            if you add 'paddedCell()' to your build.gradle as such:

                autostyle {
                    format 'someFormat', {
                        ...
                        paddedCell()
                    }
                }

            For details see $URL
            """.trimIndent()
        )
    }

    private fun diagnoseDir(task: AutostyleTask) = File(
        task.project.buildDir,
        "autostyle-diagnose-" + task.formatName()
    )

    fun check(
        task: AutostyleTask,
        formatter: Formatter,
        problemFiles: List<File>
    ) {
        if (problemFiles.isEmpty()) {
            // if the first pass was successful, then paddedCell() mode is unnecessary
            task.logger.info(
                """
                ${task.name} is in paddedCell() mode, but it doesn't need to be.
                If you remove that option, Autostyle will run ~2x faster.
                For details see $URL
                """.trimIndent()
            )
        }
        val diagnoseDir = diagnoseDir(task)
        val rootDir = task.project.rootDir
        val stillFailing = PaddedCellBulk.check(rootDir, diagnoseDir, formatter, problemFiles)
        if (stillFailing.isNotEmpty()) {
            throw task.formatViolationsFor(formatter, problemFiles)
        }
    }
}
