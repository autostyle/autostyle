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
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import java.io.File
import javax.inject.Inject

@CacheableTask
open class AutostyleCheckTask @Inject constructor(
    objects: ObjectFactory
) : AutostyleTask(objects) {

    override fun performAction(inputChanges: InputChanges) {
        val filesToCheck = mutableSetOf<File>()

        inputChanges.getFileChanges(sourceFiles).forEach {
            if (it.changeType != ChangeType.REMOVED && it.fileType == FileType.FILE) {
                filesToCheck.add(it.file)
            }
        }

        formatter.use { check(it, filesToCheck) }
    }

    private fun check(
        formatter: Formatter,
        filesToCheck: Collection<File>
    ): Collection<File> {
        val problemFiles = filesToCheck.filterNot {
            logger.debug("Checking format of {}", it)
            formatter.isClean(it)
        }
        if (paddedCell.get()) {
            PaddedCellGradle.check(this, formatter, problemFiles)
        } else if (problemFiles.isNotEmpty()) {
            // if we're not in paddedCell mode, we'll check if maybe we should be
            val cell = PaddedCellBulk.anyMisbehave(formatter, problemFiles)
            if (cell != null) {
                throw PaddedCellGradle.youShouldTurnOnPaddedCell(this, cell)
            } else {
                throw formatViolationsFor(formatter, problemFiles)
            }
        }
        return problemFiles
    }
}
