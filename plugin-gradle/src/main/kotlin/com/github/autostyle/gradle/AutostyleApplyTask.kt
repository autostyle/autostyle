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

import com.github.autostyle.Formatter
import com.github.autostyle.PaddedCell
import com.github.autostyle.PaddedCellBulk
import com.github.autostyle.serialization.deserialize
import com.github.autostyle.serialization.serialize
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class AutostyleApplyTask @Inject constructor(
    objects: ObjectFactory
) : AutostyleTask(objects) {
    @get:Optional
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val cacheFile: File
        get() = File(project.buildDir, "autostyle/$name")

    private val prevFiles: LastViolations?
        get() = cacheFile.takeIf { it.exists() }?.deserialize()

    internal data class LastViolations(
        val violations: Set<File> = setOf(),
        val modifiedWhen: Long = if (violations.isEmpty()) 0 else System.currentTimeMillis()
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    override fun performAction(inputChanges: InputChanges) {
        val filesToCheck = mutableSetOf<File>()

        // This is the regular incremental inputs
        // In other words, it is the set of files the user specified
        inputChanges.getFileChanges(sourceFiles).forEach {
            if (it.changeType != ChangeType.REMOVED && it.fileType == FileType.FILE) {
                filesToCheck.add(it.file)
            }
        }

        // However, Gradle does not know that Apply task updates its inputs
        // Typically it is OK, however there's an edge case:
        // User might run the task, and revert the files to their original state
        // Then Gradle would think it does not need to re-execute the task, as
        // it knows it has already executed the task on exactly the same inputs,
        // and the task succeeded.

        // The solution is we create yet another file which remembers the set of files
        // the task **modified** during the last execution.
        // Then, if user reverts the files to their original state, we know which files we processed,
        // and we add the files once again to the set of files to process

        // Note: the user might adjust task configuration in-between the runs,
        // So we can't blindly add the files from the previous execution, but we check if
        // they match the current filters. That is why we have `sourceFiles.contains`.

        // Note: the extra "last processed" file is declared as **incremental** input so
        // the task is fully incremental (it enables to trigger the task if `cacheFile` changes)
        // and it enables to keep incremental processing of sourceFiles
        prevFiles?.let {
            filesToCheck += it.violations.filter { f -> sourceFiles.contains(f) }
        }

        val changedFiles = formatter.use { applyFormat(it, filesToCheck) }

        if (changedFiles.isEmpty()) {
            return
        }
        cacheFile.parentFile.mkdirs()
        cacheFile.serialize(
            LastViolations(changedFiles)
        )
    }

    private fun applyFormat(
        formatter: Formatter,
        filesToCheck: Collection<File>
    ): Set<File> {
        val changedFiles = mutableSetOf<File>()
        if (paddedCell.get()) {
            for (file in filesToCheck) {
                logger.debug("Applying format to {}", file)
                if (PaddedCellBulk.applyAnyChanged(formatter, file)) {
                    changedFiles += file
                }
            }
            return changedFiles
        }
        var anyMisbehave: PaddedCell? = null
        for (file in filesToCheck) {
            logger.debug("Applying format to {}", file)
            val unixResultIfDirty = formatter.applyToAndReturnResultIfDirty(file)
            if (unixResultIfDirty != null) {
                changedFiles += file
            }
            // because apply will count as up-to-date, it's important
            // that every call to apply will get a PaddedCell check
            if (anyMisbehave == null && unixResultIfDirty != null) {
                val onceMore = formatter.compute(unixResultIfDirty, file)
                //  f(f(input) == f(input) for an idempotent function
                if (onceMore == unixResultIfDirty) {
                    continue
                }
                // It's not idempotent. But, if it converges, then it's likely a glitch that won't reoccur,
                // so there's no need to make a bunch of noise for the user
                val result = PaddedCell.check(formatter, file, onceMore)
                if (result.type() != PaddedCell.Type.CONVERGE) {
                    // it didn't converge, so the user is going to need padded cell mode
                    anyMisbehave = result
                } else {
                    val finalResult = formatter.computeLineEndings(result.canonical(), file)
                    file.writeText(finalResult, formatter.encoding)
                }
            }
        }
        if (anyMisbehave != null) {
            throw PaddedCellGradle.youShouldTurnOnPaddedCell(this, anyMisbehave)
        }
        return changedFiles
    }
}
