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
import com.github.autostyle.java.GoogleJavaFormatStep
import com.github.autostyle.java.ImportOrderStep
import com.github.autostyle.java.RemoveUnusedImportsStep
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.findByType
import javax.inject.Inject

open class JavaExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        target.conv(root.providers.provider {
            val java = project.extensions.findByType<JavaPluginExtension>()
                ?: throw GradleException("You must apply the java plugin before the Autostyle plugin if you are using the java extension.")
            java.sourceSets.map { it.allJava }
        })
    }

    fun importOrder(vararg importOrder: String) {
        addStep(ImportOrderStep.forJava().createFrom(*importOrder))
    }

    /** Removes any unused imports.  */
    fun removeUnusedImports() {
        addStep(RemoveUnusedImportsStep.create(project.asProvisioner()))
    }

    fun googleJavaFormat(action: Action<GoogleJavaFormatConfig>) {
        googleJavaFormat(GoogleJavaFormatStep.defaultVersion(), action)
    }

    /**
     * Uses the given version of [google-java-format](https://github.com/google/google-java-format) to format source code.
     *
     * Limited to published versions.  See [issue #33](https://github.com/diffplug/spotless/issues/33#issuecomment-252315095)
     * for an workaround for using snapshot versions.
     */
    /** Uses the [google-java-format](https://github.com/google/google-java-format) jar to format source code.  */
    @JvmOverloads
    fun googleJavaFormat(
        version: String = GoogleJavaFormatStep.defaultVersion(),
        action: Action<GoogleJavaFormatConfig>? = null
    ) {
        GoogleJavaFormatConfig(version, project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class GoogleJavaFormatConfig internal constructor(val version: String, val project: Project) {
        var style: String = GoogleJavaFormatStep.defaultStyle()

        fun style(style: String) {
            this.style = style
        }

        fun aosp() {
            style("AOSP")
        }

        internal fun createStep(): FormatterStep = GoogleJavaFormatStep.create(
            version,
            style,
            project.asProvisioner()
        )
    }
}
