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
import com.github.autostyle.PaddedCellBulk
import com.github.autostyle.gradle.ext.serialize
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputFile
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class AutostyleCheckTask @Inject constructor(
    objects: ObjectFactory
) : AutostyleTask(objects) {
    @get:OutputFile
    val cacheFile: File
        get() = File(project.buildDir, "autostyle/$name")

    override fun performAction(inputChanges: InputChanges) {
        val filesToCheck = mutableSetOf<File>()

        inputChanges.getFileChanges(sourceFiles).forEach {
            if (it.changeType != ChangeType.REMOVED && it.file.isFile) {
                filesToCheck.add(it.file)
            }
        }

//        if (inputChanges.isIncremental && cacheFile.exists()) {
//            val lastViolations = cacheFile.deserialize<LastViolations>()
//            filesToCheck.addAll(lastViolations.violations)
//        }

        val filesWithViolations = formatter.use { check(it, filesToCheck) }

        // Save violations so the next task execution reports them as well
        cacheFile.parentFile.mkdirs()
        cacheFile.serialize(LastViolations().apply {
            violations.addAll(filesWithViolations)
        })
    }

    internal class LastViolations : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }

        val violations = mutableListOf<File>()
    }

    private fun check(
        formatter: Formatter,
        filesToCheck: Collection<File>
    ): Collection<File> {
        val problemFiles = filesToCheck.filterNot {
            logger.debug("Applying format to {}", it)
            formatter.isClean(it)
        }
        if (paddedCell.get()) {
            PaddedCellGradle.check(this, formatter, problemFiles)
        } else if (problemFiles.isNotEmpty()) {
            // if we're not in paddedCell mode, we'll check if maybe we should be
            if (PaddedCellBulk.anyMisbehave(formatter, problemFiles)) {
                throw PaddedCellGradle.youShouldTurnOnPaddedCell(this)
            } else {
                throw formatViolationsFor(formatter, problemFiles)
            }
        }
        return problemFiles
    }
}
