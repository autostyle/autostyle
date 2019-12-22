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
import com.github.autostyle.kotlin.KtLintStep
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject

open class KotlinGradleExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        filter.include("**/*.gradle.kts")
    }

    fun ktlint(action: Action<KotlinFormatConfig>) {
        ktlint(KtLintStep.defaultVersion(), action)
    }

    /** Adds the specified version of [ktlint](https://github.com/pinterest/ktlint).  */
    @JvmOverloads
    fun ktlint(
        version: String = KtLintStep.defaultVersion(),
        action: Action<KotlinFormatConfig>? = null
    ) {
        KotlinFormatConfig(version, project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class KotlinFormatConfig internal constructor(
        private val version: String,
        private val project: Project
    ) {
        val userData = project.objects.mapProperty<String, String>()

        fun userData(entries: Map<String, String>) = userData.putAll(entries)

        internal fun createStep(): FormatterStep {
            return KtLintStep.createForScript(
                version,
                project.asProvisioner(),
                userData.get()
            )
        }
    }
}
