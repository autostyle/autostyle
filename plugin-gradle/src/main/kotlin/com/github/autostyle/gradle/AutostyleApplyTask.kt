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

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import javax.inject.Inject

open class AutostyleApplyTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputDirectory = objects.directoryProperty()

    @TaskAction
    fun run() {
        val projectDir = project.projectDir
        var hasUpdates = false
        project.fileTree(inputDirectory).visit {
            if (!isDirectory) {
                hasUpdates = true
                println("Apply: $path => ${projectDir.resolve(path)}")
                projectDir.resolve(path).outputStream().use { copyTo(it) }
            }
        }
        didWork = hasUpdates
    }
}
