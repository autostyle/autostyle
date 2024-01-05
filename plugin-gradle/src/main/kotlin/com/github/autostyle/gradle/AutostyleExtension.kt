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

import com.github.autostyle.LineEnding
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

open class AutostyleExtension @Inject constructor(
    val project: Project
) {
    companion object {
        const val EXTENSION = "autostyle"
        const val PROCESS = "Process"
        const val CHECK = "Check"
        const val APPLY = "Apply"
    }
    internal val objects: ObjectFactory = project.objects

    internal val providers = project.providers

    /** Line endings (if any).  */
    var lineEndings = LineEnding.GIT_ATTRIBUTES

    /** Returns the encoding to use.  */
    var encoding = StandardCharsets.UTF_8

    /**
     * Configures Gradle's `check` task to run `autostyleCheck` if `true` (default),
     * but to not do so if `false`.
     */
    var isEnforceCheck = true

    /** Sets encoding to use (defaults to UTF_8).  */
    fun setEncoding(name: String) {
        encoding = Charset.forName(name)
    }

    /** Sets encoding to use (defaults to UTF_8).  */
    fun encoding(charset: String) {
        setEncoding(charset)
    }

    private val fmts = project.container<BaseFormatExtension>().apply {
        whenObjectAdded {
            val prefix = EXTENSION + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val processTask = project.tasks.register<AutostyleTask>(prefix + PROCESS) {
                this@whenObjectAdded.configureTask(this)
            }
            val applyTask = project.tasks.register<AutostyleApplyTask>(prefix + APPLY) {
                inputDirectory.set(processTask.flatMap { it.outputDirectory })
            }
            project.tasks.register<AutostyleCheckTask>(prefix + CHECK) {
                inputDirectory.set(processTask.flatMap { it.outputDirectory })
                mustRunAfter(applyTask)
            }
        }
        whenObjectRemoved {
            val prefix = EXTENSION + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val tasks = project.tasks
            tasks.remove(tasks.findByName(prefix + PROCESS))
            tasks.remove(tasks.findByName(prefix + CHECK))
            tasks.remove(tasks.findByName(prefix + APPLY))
        }
    }

    /** Configures the special java-specific extension.  */
    fun java(action: Action<JavaExtension>) {
        configure("java", action)
    }

    /** Configures the special scala-specific extension.  */
    fun scala(action: Action<ScalaExtension>) {
        configure("scala", action)
    }

    /** Configures the special kotlin-specific extension.  */
    fun kotlin(action: Action<KotlinExtension>) {
        getOrCreate<KotlinExtension>("kotlin").apply {
            kotlinDefaults()
            action.execute(this)
        }
    }

    /** Configures the special Gradle Kotlin DSL specific extension.  */
    fun kotlinGradle(action: Action<KotlinExtension>) {
        getOrCreate<KotlinExtension>("kotlinGradle").apply {
            kotlinGradleDefaults()
            action.execute(this)
        }
    }

    /** Configures the special freshmark-specific extension.  */
    fun freshmark(action: Action<FreshMarkExtension>) {
        configure("freshmark", action)
    }

    /** Configures the special groovy-specific extension.  */
    fun groovy(action: Action<GroovyExtension>) {
        configure("groovy", action)
    }

    /** Configures the special groovy-specific extension for Gradle files.  */
    fun groovyGradle(action: Action<GroovyGradleExtension>) {
        configure("groovyGradle", action)
    }

    /** Configures the special sql-specific extension for SQL files.  */
    fun sql(action: Action<SqlExtension>) {
        configure("sql", action)
    }

    /** Configures the special C/C++-specific extension.  */
    fun cpp(action: Action<CppExtension>) {
        configure("cpp", action)
    }

    /** Configures the special typescript-specific extension for typescript files.  */
    fun typescript(action: Action<TypescriptExtension>) {
        configure("typescript", action)
    }

    /** Configures a custom extension.  */
    fun format(name: String, action: Action<BaseFormatExtension>) {
        configure(name, action)
    }

    /** Makes it possible to remove a format which was created earlier.  */
    fun removeFormat(name: String) {
        fmts.findByName(name)?.let {
            fmts.remove(it)
        }
    }

    private inline fun <reified T : BaseFormatExtension> configure(
        name: String,
        action: Action<T>
    ) {
        action.execute(getOrCreate(name))
    }

    private inline fun <reified T : BaseFormatExtension> getOrCreate(
        name: String
    ): T = fmts.findByName(name)?.let { it as T }
        ?: objects.newInstance<T>(name, this).also { fmts.add(it) }
}
