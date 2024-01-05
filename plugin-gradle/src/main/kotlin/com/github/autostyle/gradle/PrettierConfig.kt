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
import com.github.autostyle.npm.PrettierFormatterStep
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class PrettierConfig @Inject internal constructor(
    private val devDependencies: Map<String, String>,
    objects: ObjectFactory,
    private val project: Project
) : NpmStepConfig(objects) {
    val configFile = objects.property<Any>()
    val config = objects.mapProperty<String, Any>()

    fun configFile(value: Any) {
        configFile.set(value)
    }

    fun config(value: Map<String, Any>) {
        config.set(value)
    }

    override fun createStep(): FormatterStep = PrettierFormatterStep.create(
        devDependencies,
        project.asProvisioner(),
        project.layout.buildDirectory.get().asFile,
        npmExecutable.orNull?.let { project.file(it) },
        com.github.autostyle.npm.PrettierConfig(
            configFile.orNull?.let { project.file(it) },
            config.get()
        )
    )
}
