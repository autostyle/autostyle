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

import com.github.autostyle.FormatterStep
import com.github.autostyle.gradle.ext.asProvisioner
import com.github.autostyle.gradle.ext.conv
import com.github.autostyle.scala.ScalaFmtStep
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.ScalaSourceDirectorySet
import org.gradle.kotlin.dsl.findByType
import javax.inject.Inject

open class ScalaExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        filter.include("**/*.scala", "**/*.sc")
        target.conv(root.providers.provider {
            val java = project.extensions.findByType<JavaPluginExtension>()
                ?: throw GradleException("You must apply the java plugin before the Autostyle plugin if you are using the java extension.")
            java.sourceSets.mapNotNull { sourceSet ->
                sourceSet.extensions.findByType<ScalaSourceDirectorySet>()
            }
        })
    }

    fun scalafmt(
        action: Action<ScalaFmtConfig>
    ) {
        scalafmt(ScalaFmtStep.defaultVersion(), action)
    }

    @JvmOverloads
    fun scalafmt(
        version: String = ScalaFmtStep.defaultVersion(),
        action: Action<ScalaFmtConfig>? = null
    ) {
        ScalaFmtConfig(version, project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class ScalaFmtConfig internal constructor(
        private val version: String,
        private val project: Project
    ) {
        var configFile: Any? = null

        fun configFile(configFile: Any) {
            this.configFile = configFile
        }

        internal fun createStep(): FormatterStep {
            val resolvedConfigFile = configFile?.let { project.file(it) }
            return ScalaFmtStep.create(
                version,
                project.asProvisioner(),
                resolvedConfigFile
            )
        }
    }
}
