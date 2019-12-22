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

import com.github.autostyle.cpp.CppDefaults
import com.github.autostyle.extra.cpp.EclipseCdtFormatterStep
import org.gradle.api.Action
import org.gradle.api.Project
import javax.inject.Inject

open class CppExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        filter.include(CppDefaults.EXTENSIONS.map { "**/*.$it" })
    }

    fun eclipse(action: Action<EclipseConfig>) {
        eclipse(EclipseCdtFormatterStep.defaultVersion(), action)
    }

    @JvmOverloads
    fun eclipse(
        version: String = EclipseCdtFormatterStep.defaultVersion(),
        action: Action<EclipseConfig>? = null
    ) {
        EclipseConfig(version, project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class EclipseConfig internal constructor(version: String, project: Project) :
        EclipseBasedConfig(version, project, { EclipseCdtFormatterStep.createBuilder(it) })
}
