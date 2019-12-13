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
package com.github.autostyle.gradle.ext

import com.github.autostyle.Provisioner
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

fun Project.asProvisioner() =
    Provisioner { withTransitives: Boolean, mavenCoords: Collection<String> ->
        try {
            val dependencies = project.dependencies
            val deps = mavenCoords
                .map { dependencies.create(it) }
                .toTypedArray()
            project.configurations.detachedConfiguration(*deps).apply {
                description = mavenCoords.toString()
                isTransitive = withTransitives
            }.resolve()
        } catch (e: Exception) {
            logger.info("Failed to resolve dependencies for Autostyle. Please add relevant buildscript { repositories { ... } } to $project: $e")
            throw e
        }
    }

val gradleGe51 = GradleVersion.current() >= GradleVersion.version("5.1")

fun <T> Property<T>.conv(v: T) = if (gradleGe51) convention(v) else apply { set(v) }
fun <T> Property<T>.conv(v: Provider<out T>) = if (gradleGe51) convention(v) else apply { set(v) }

fun <T> ListProperty<T>.conv(v: Iterable<out T>) = if (gradleGe51) convention(v) else apply { set(v) }
fun <T> ListProperty<T>.conv(v: Provider<out Iterable<out T>>) = if (gradleGe51) convention(v) else apply { set(v) }
