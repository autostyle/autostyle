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
import com.github.autostyle.FormatterStep
import com.github.autostyle.LineEnding
import com.github.autostyle.extra.integration.DiffMessageFormatter
import com.github.autostyle.gradle.ext.conv
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.work.InputChanges
import java.io.File
import java.nio.charset.Charset
import java.util.*
import javax.inject.Inject

abstract class AutostyleTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    init {
        // The task produces no output, so we need to provide upToDateWhen
        outputs.upToDateWhen { true }
    }

    // set by AutostyleExtension, but possibly overridden by FormatExtension
    @get:Input
    val encoding = objects.property<String>().conv("UTF-8")

    @get:Input
    val lineEndingsPolicy = objects.property<LineEnding.Policy>()
        .conv(LineEnding.UNIX.createPolicy())

    // set by FormatExtension
    @get:Input
    val paddedCell = objects.property<Boolean>().conv(false)

    @get:Input
    val steps = objects.listProperty<FormatterStep>()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: ConfigurableFileCollection = project.files()

    fun addStep(step: FormatterStep) = steps.add(step)

    @TaskAction
    abstract fun performAction(inputChanges: InputChanges)

    /** Returns the name of this format.  */
    fun formatName(): String {
        val name = name
        return if (name.startsWith(AutostyleExtension.EXTENSION)) {
            name.substring(AutostyleExtension.EXTENSION.length).toLowerCase(Locale.ROOT)
        } else {
            name
        }
    }

    @get:Input
    val formatter: Formatter get() =
        Formatter.builder()
            .lineEndingsPolicy(lineEndingsPolicy.get())
            .encoding(Charset.forName(encoding.get()))
            .rootDir(project.rootDir.toPath())
            .steps(steps.get())
            .build()

    /** Returns an exception which indicates problem files nicely.  */
    fun formatViolationsFor(
        formatter: Formatter,
        problemFiles: List<File>
    ) = GradleException(
        "The following files have format violations:\n" +
                DiffMessageFormatter(formatter)
                    .diff(problemFiles.sorted(), paddedCell.get())
                    .append("Run 'gradlew autostyleApply' to fix these violations.")
                    .toString()
    )
}
