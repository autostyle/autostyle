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

import com.github.autostyle.FormatterFunc
import com.github.autostyle.FormatterStep
import com.github.autostyle.LineEnding
import com.github.autostyle.extra.wtp.EclipseWtpFormatterStep
import com.github.autostyle.generic.*
import com.github.autostyle.gradle.ext.conv
import com.github.autostyle.npm.PrettierFormatterStep
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject

open class BaseFormatExtension @Inject constructor(
    val name: String,
    val root: AutostyleExtension
) {
    val patterns: PatternSet = PatternSet()

    protected val project: Project get() = root.project

    val paddedCell = root.objects.property<Boolean>().conv(false)

    fun paddedCell() {
        paddedCell.set(true)
    }

    val lineEndings = root.objects.property<LineEnding>()
        .conv(root.providers.provider { root.lineEndings })

    val encoding = root.objects.property<Charset>()
        .conv(root.providers.provider { root.encoding })

    fun encoding(encoding: String) {
        this.encoding.set(Charset.forName(encoding))
    }

    protected val target = root.objects.listProperty<Any>()
        .conv(root.providers.provider { listOf(project.projectDir) })

    /**
     * Sets which files should be formatted.
     */
    fun target(vararg targets: Any) {
        target.set(targets.asList())
    }

    private val steps = mutableListOf<FormatterStep>()

    fun addStep(step: FormatterStep) {
        steps += step
    }

    /** Clears all of the existing steps.  */
    fun clearSteps() {
        steps.clear()
    }

    /** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines.  */
    fun custom(
        name: String,
        stepVersion: Int,
        formatter: Closure<String>
    ) {
        custom(name, stepVersion, FormatterFunc(formatter::call))
    }

    /** Adds a custom step. Receives a string with unix-newlines, must return a string with unix newlines.  */
    fun custom(
        name: String,
        stepVersion: Int,
        formatter: FormatterFunc
    ) {
        addStep(FormatterStep.create(name, stepVersion) { formatter })
    }

    /** Highly efficient find-replace char sequence.  */
    fun replace(
        name: String,
        original: CharSequence,
        after: CharSequence
    ) {
        addStep(ReplaceStep.create(name, original, after))
    }

    /** Highly efficient find-replace regex.  */
    fun replaceRegex(
        name: String,
        regex: String,
        replacement: String
    ) {
        addStep(ReplaceRegexStep.create(name, regex, replacement))
    }

    /** Removes trailing whitespace.  */
    fun trimTrailingWhitespace() {
        addStep(TrimTrailingWhitespaceStep.create())
    }

    /** Ensures that files end with a single newline.  */
    fun endWithNewline() {
        addStep(EndWithNewlineStep.create())
    }

    /** Ensures that the files are indented using spaces.  */
    fun indentWithSpaces(numSpacesPerTab: Int) {
        addStep(IndentStep.Type.SPACE.create(numSpacesPerTab))
    }

    /** Ensures that the files are indented using spaces.  */
    fun indentWithSpaces() {
        addStep(IndentStep.Type.SPACE.create())
    }

    /** Ensures that the files are indented using tabs.  */
    fun indentWithTabs(tabToSpaces: Int) {
        addStep(IndentStep.Type.TAB.create(tabToSpaces))
    }

    /** Ensures that the files are indented using tabs.  */
    fun indentWithTabs() {
        addStep(IndentStep.Type.TAB.create())
    }

    /**
     * @param licenseHeader
     * Content that should be at the top of every file.
     * @param delimiter
     * Autostyle will look for a line that starts with this regular expression pattern to know what the "top" is.
     */
    @JvmOverloads
    fun licenseHeader(
        copyright: String,
        addBlankLineAfter: Boolean = false,
        map: Map<String, CopyrightStyle>? = null
    ) {
        addStep(
            ImprovedLicenseHeaderStep(
                copyright,
                addBlankLineAfter,
                map ?: DEFAULT_HEADER_STYLES
            )
        )
    }

    /** Uses the default version of prettier.  */
    fun prettier(action: Action<PrettierConfig>) {
        prettier(PrettierFormatterStep.defaultDevDependencies(), action)
    }

    /** Uses the specified version of prettier.  */
    @JvmOverloads
    fun prettier(version: String, action: Action<PrettierConfig>? = null) {
        prettier(PrettierFormatterStep.defaultDevDependenciesWithPrettier(version), action)
    }

    /** Uses exactly the npm packages specified in the map.  */
    @JvmOverloads
    fun prettier(devDependencies: Map<String, String>, action: Action<PrettierConfig>? = null) {
        createPrettierConfig(devDependencies).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    protected open fun createPrettierConfig(devDependencies: Map<String, String>) =
        PrettierConfig(devDependencies, root.objects, root.project)

    @JvmOverloads
    fun eclipseWtp(
        type: EclipseWtpFormatterStep,
        version: String = EclipseWtpFormatterStep.defaultVersion()
    ) =
        EclipseWtpConfig(type, version, root.project)

    /** Sets up a format task according to the values in this extension.  */
    internal open fun configureTask(task: AutostyleTask) {
        task.paddedCell.set(paddedCell)
        task.encoding.set(encoding.map { it.name() })
        task.sourceFiles.from(target.map { targetRoot ->
            targetRoot.map {
                when (it) {
                    is FileTree -> it
                    is File ->
                        if (it.isDirectory) {
                            project.fileTree(it)
                        } else {
                            project.files(it).asFileTree
                        }
                    else -> project.fileTree(it)
                }.matching(patterns)
            }
        })
        task.steps.set(steps)
        task.lineEndingsPolicy.set(lineEndings.map {
            it.createPolicy(project.projectDir) { project.files(task.sourceFiles) }
        })
    }
}
