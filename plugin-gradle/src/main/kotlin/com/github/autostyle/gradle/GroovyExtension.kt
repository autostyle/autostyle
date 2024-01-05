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

import com.github.autostyle.extra.groovy.GrEclipseFormatterStep
import com.github.autostyle.gradle.ext.conv
import com.github.autostyle.java.ImportOrderStep
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class GroovyExtension @Inject constructor(name: String, root: AutostyleExtension) : BaseFormatExtension(name, root) {
    val excludeJava = root.objects.property<Boolean>().conv(false)

    init {
        target.conv(
            root.providers.provider {
                val java = project.extensions.findByType<JavaPluginExtension>()
                if (java == null || !project.plugins.hasPlugin(GroovyBasePlugin::class)) {
                    throw GradleException("You must apply the groovy plugin before the Autostyle plugin if you are using the groovy extension.")
                }
                java.sourceSets.mapNotNull { sourceSet ->
                    sourceSet.extensions.findByType<GroovySourceDirectorySet>()
                }
            }
        )
    }

    override fun configureTask(task: AutostyleTask) {
        // TODO: pass "excludeJava"
        super.configureTask(task)
    }

    /** Determines whether to exclude .java files, to focus on only .groovy files.  */
    /** Excludes .java files, to focus on only .groovy files.  */
    @JvmOverloads
    fun excludeJava(excludeJava: Boolean = true) {
        this.excludeJava.set(excludeJava)
    }

    fun importOrder(vararg importOrder: String) {
        addStep(ImportOrderStep.forGroovy().createFrom(*importOrder))
    }

    fun greclipse(action: Action<GrEclipseConfig>) {
        greclipse(GrEclipseFormatterStep.defaultVersion(), action)
    }

    @JvmOverloads
    fun greclipse(
        version: String = GrEclipseFormatterStep.defaultVersion(),
        action: Action<GrEclipseConfig>? = null
    ) {
        GrEclipseConfig(version, root.project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class GrEclipseConfig internal constructor(version: String, project: Project) :
        EclipseBasedConfig(version, project, { GrEclipseFormatterStep.createBuilder(it) })
}
