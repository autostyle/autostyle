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
import com.github.autostyle.gradle.ext.deserialize
import com.github.autostyle.gradle.ext.serialize
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

        inputChanges.getFileChanges(sourceFiles).forEach {
            if (it.changeType != ChangeType.REMOVED && it.file.isFile) {
                filesToCheck.add(it.file)
            }
        }
        prevFiles?.let { filesToCheck += it.violations }

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
                logger.debug("Applying format to $file")
                PaddedCellBulk.applyAnyChanged(formatter, file)
            }
            return changedFiles
        }
        var anyMisbehave = false
        for (file in filesToCheck) {
            logger.info("Applying format to {}", file)
            val unixResultIfDirty = formatter.applyToAndReturnResultIfDirty(file)
            if (unixResultIfDirty != null) {
                changedFiles += file
            }
            // because apply will count as up-to-date, it's important
            // that every call to apply will get a PaddedCell check
            if (!anyMisbehave && unixResultIfDirty != null) {
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
                    anyMisbehave = true
                } else {
                    val finalResult = formatter.computeLineEndings(result.canonical(), file)
                    file.writeText(finalResult, formatter.encoding)
                }
            }
        }
        if (anyMisbehave) {
            throw PaddedCellGradle.youShouldTurnOnPaddedCell(this)
        }
        return changedFiles
    }
}
