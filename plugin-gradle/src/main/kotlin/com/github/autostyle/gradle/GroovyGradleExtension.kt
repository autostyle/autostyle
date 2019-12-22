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
import com.github.autostyle.gradle.GroovyExtension.GrEclipseConfig
import com.github.autostyle.java.ImportOrderStep
import org.gradle.api.Action
import javax.inject.Inject

open class GroovyGradleExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        filter.include("**/*.gradle")
    }

    fun importOrder(vararg importOrder: String?) {
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
}
