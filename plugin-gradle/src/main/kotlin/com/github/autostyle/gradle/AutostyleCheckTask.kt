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

import com.github.autostyle.extra.integration.DiffMessageFormatter
import com.github.autostyle.gradle.ext.conv
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.property
import java.nio.charset.Charset
import javax.inject.Inject

open class AutostyleCheckTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputDirectory = objects.directoryProperty()

    @Input
    val encoding = objects.property<String>().conv("UTF-8")

    @Console
    val maxCheckMessageLines = objects.property<Int>().convention(
        project.intProperty("maxCheckMessageLines") ?: 50
    )

    @Console
    val maxFilesToList = objects.property<Int>().convention(
        project.intProperty("maxFilesToList") ?: 10
    )

    @Console
    val minLinesPerFile = objects.property<Int>().convention(
        project.intProperty("minLinesPerFile") ?: 4
    )

    private fun Project.stringProperty(name: String) =
        when (extra.has(name)) {
            true -> extra.get(name) as? String
            else -> null
        }

    private fun Project.intProperty(name: String) = stringProperty(name)?.toInt()

    @TaskAction
    fun run() {
        val sb = StringBuilder()
        sb.append("The following files have format violations:\n")
        val writer = DiffMessageFormatter(
            project.projectDir,
            sb,
            maxCheckMessageLines = maxCheckMessageLines.get(),
            maxFilesToList = maxFilesToList.get(),
            minLinesPerFile = minLinesPerFile.get()
        )
        val projectDir = project.projectDir
        val encoding = Charset.forName(encoding.get())
        project.fileTree(inputDirectory).visit {
            if (!isDirectory) {
                writer.addDiff(projectDir.resolve(path), file, encoding)
            }
        }
        if (writer.finishWithoutErrors()) {
            didWork = false
            return
        }
        didWork = true
        sb.append("You might want to adjust")
        sb.append(" -PmaxCheckMessageLines=").appendln(maxCheckMessageLines.get())
        sb.append(" -PmaxFilesToList=").appendln(maxFilesToList.get())
        sb.append(" -PminLinesPerFile=").appendln(minLinesPerFile.get())
        sb.append(" to see more violations\n")
        sb.append("Run './gradlew autostyleApply' to fix the violations.")
        throw GradleException(sb.toString())
    }
}
