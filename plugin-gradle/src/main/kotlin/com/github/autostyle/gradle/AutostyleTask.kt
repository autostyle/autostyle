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

import com.github.autostyle.*
import com.github.autostyle.gradle.ext.conv
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject

@CacheableTask
abstract class AutostyleTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    init {
        if (System.getenv("JITPACK")?.toBoolean() == true) {
            // It makes no sense to verify code style on JitPack builds
            enabled = false
        }
    }

    // set by AutostyleExtension, but possibly overridden by FormatExtension
    @get:Input
    val encoding = objects.property<String>().conv("UTF-8")

    @get:Input
    val lineEndingsPolicy = objects.property<LineEnding.Policy>()
        .conv(LineEnding.UNIX.createPolicy())

    @get:Input
    val steps = objects.listProperty<FormatterStep>()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: ConfigurableFileCollection = project.files()

    fun addStep(step: FormatterStep) = steps.add(step)

    @OutputDirectory
    val outputDirectory = objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("autostyle/$name/formatted"))

    @OutputDirectory
    val divergingDirectory = objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("autostyle/$name/diverging"))

    private val projectDirectory = project.projectDir

    @get:Internal
    val formatter: Formatter
        get() =
            Formatter.builder()
                .lineEndingsPolicy(lineEndingsPolicy.get())
                .encoding(Charset.forName(encoding.get()))
                .rootDir(project.rootDir.toPath())
                .steps(steps.get())
                .build()

    @TaskAction
    fun run(inputChanges: InputChanges) {
        val outputDir = outputDirectory.get().asFile
        if (!inputChanges.isIncremental) {
            project.delete(outputDir)
        }
        project.mkdir(outputDir)
        val divergingDir = divergingDirectory.get().asFile
        project.delete(divergingDir)
        project.mkdir(divergingDir)

        val filesToCheck = mutableListOf<File>()
        inputChanges.getFileChanges(sourceFiles).forEach {
            if (it.changeType == ChangeType.REMOVED) {
                val outFile = outputDir.resolve(it.file.relativeTo(projectDirectory))
                project.delete(outFile)
            }

            if (it.changeType != ChangeType.REMOVED && it.fileType == FileType.FILE) {
                filesToCheck.add(it.file)
            }
        }

        formatter.use { formatFiles(it, filesToCheck) }
    }

    private fun formatFiles(formatter: Formatter, filesToCheck: Collection<File>) {
        val outputDir = outputDirectory.get().asFile
        val divergingDir = divergingDirectory.get().asFile
        val convergenceAnalyzer = ConvergenceAnalyzer(formatter)
        val diverges = mutableListOf<String>()
        val cycles = mutableListOf<String>()
        for (file in filesToCheck) {
            logger.debug("Applying format to {}", file)
            val result = convergenceAnalyzer.analyze(file)
            val relativeFile = file.relativeTo(projectDirectory)
            val outFile = outputDir.resolve(relativeFile)
            outFile.parentFile.mkdirs()
            when (result) {
                is ConvergenceResult.Clean ->
                    project.delete(outFile)
                is ConvergenceResult.Convergence ->
                    outFile.writeText(result.formatted, formatter.encoding)
                is ConvergenceResult.Cycle -> {
                    storeCycle(formatter, divergingDir, relativeFile, result.cycle)
                    cycles += relativeFile.toString()
                }
                is ConvergenceResult.Divergence -> {
                    storeCycle(formatter, divergingDir, relativeFile, result.cycle)
                    diverges += relativeFile.toString()
                }
            }
        }
        if (diverges.isEmpty() && cycles.isEmpty()) {
            return
        }
        throw GradleException(
            ("Formatting ${
                cycles.joinToString(prefix = "cycles for ", postfix = ", ")
                    .removeSuffix("cycles for , ")
            }" +
                    diverges.joinToString(prefix = "diverges for ").removeSuffix("diverges for "))
                .removeSuffix(", ")
        )
    }

    private fun storeCycle(
        formatter: Formatter,
        divergingDir: File,
        relativeFile: File,
        cycle: List<String>
    ) {
        val outFile = divergingDir.resolve(relativeFile)
        project.mkdir(outFile.parentFile)
        val outPath = outFile.absolutePath
        for ((index, value) in cycle.withIndex()) {
            File(outPath + "." + index.toString().padStart(2, '0'))
                .writeText(value, formatter.encoding)
        }
    }
}
