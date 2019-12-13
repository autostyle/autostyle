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

import com.github.autostyle.npm.TsFmtFormatterStep
import org.gradle.api.Action
import javax.inject.Inject

open class TypescriptExtension @Inject constructor(name: String, root: AutostyleExtension) :
    BaseFormatExtension(name, root) {
    init {
        patterns.include("**/*.ts")
    }

    /** Uses the specified version of typescript-format.  */
    fun tsfmt(version: String, action: Action<TypescriptConfig>) {
        tsfmt(TsFmtFormatterStep.defaultDevDependenciesWithTsFmt(version), action)
    }

    /** Creates a `TypescriptFormatExtension` using exactly the specified npm packages.  */
    /** Uses the default version of typescript-format.  */
    @JvmOverloads
    fun tsfmt(
        devDependencies: Map<String, String> = TsFmtFormatterStep.defaultDevDependencies(),
        action: Action<TypescriptConfig>? = null
    ) {
        TypescriptConfig(devDependencies, root.objects, root.project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    override fun createPrettierConfig(devDependencies: Map<String, String>): PrettierConfig =
        super.createPrettierConfig(devDependencies).apply {
            properties.put("parser", "typescript")
        }
}
