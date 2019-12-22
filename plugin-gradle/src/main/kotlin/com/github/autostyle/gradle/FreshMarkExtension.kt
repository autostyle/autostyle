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

import com.github.autostyle.FormatterProperties
import com.github.autostyle.gradle.ext.asProvisioner
import com.github.autostyle.markdown.FreshMarkStep
import org.gradle.api.Action
import javax.inject.Inject

open class FreshMarkExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    val propertyActions =
        mutableListOf<Action<MutableMap<String, Any>>>()

    init {
        filter.include("**/*.md")
        addStep(FreshMarkStep.create(project.asProvisioner()) {
            mutableMapOf<String, Any>().also {
                for (action in propertyActions) {
                    action.execute(it)
                }
            }
        })
    }

    fun properties(action: Action<MutableMap<String, Any>>) {
        propertyActions.add(action)
    }

    fun propertiesFile(vararg files: Any) {
        propertyActions.add(
            Action {
                // FreshMarkStep.State serializes the properties and not the files.
                // Therefore they must be stored in a hash-map like used by Properties.
                val preferences = FormatterProperties.from(project.files(*files))
                preferences.properties.forEach { key: Any, value: Any ->
                    this[key.toString()] = value
                }
            }
        )
    }
}
