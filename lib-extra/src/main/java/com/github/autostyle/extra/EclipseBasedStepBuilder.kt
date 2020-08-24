/*
 * Copyright 2016 DiffPlug
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
package com.github.autostyle.extra

import com.github.autostyle.*
import com.github.autostyle.extra.EclipseBasedStepBuilder.State
import java.io.File
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Generic Eclipse based formatter step [State] builder.
 */
class EclipseBasedStepBuilder(
    private val formatterName: String,
    private val formatterStepExt: String,
    private val jarProvisioner: Provisioner,
    private val stateToFormatter: ThrowingEx.Function<State, FormatterFunc>
) {
    companion object {
        /**
         * Resource location of Autostyle Eclipse Formatter Maven coordinate lists.
         *
         *
         * Autostyle Eclipse Formatter dependencies have fixed transitive versions, since Autostyle Eclipse Formatter
         * implementations access internal methods of the Eclipse plugins, which may change with every
         * version change, including minor and patch version changes.
         * At the resource location for each supported Autostyle Eclipse Formatter, a text file is provided, containing
         * the fixed versions for the formatter and its transitive dependencies.
         * Each line is either a comment starting with `#` or corresponds to the format
         * `<groupId>:<artifactId>[:packaging][:classifier]:<versionRestriction>`
         *
         */
        private val ECLIPSE_FORMATTER_RESOURCES =
            EclipseBasedStepBuilder::class.java.getPackage().name.replace('.', '/')
    }

    private val dependencies: MutableList<String> = ArrayList()
    private var settingsFiles: Iterable<File> = ArrayList()

    /** Initialize valid default configuration, taking latest version  */
    constructor(
        formatterName: String,
        jarProvisioner: Provisioner,
        stateToFormatter: ThrowingEx.Function<State, FormatterFunc>
    ) : this(formatterName, "", jarProvisioner, stateToFormatter)

    /** Returns the FormatterStep (whose state will be calculated lazily).  */
    fun build(): FormatterStep = FormatterStep.createLazy(
        formatterName + formatterStepExt,
        ThrowingEx.Supplier { this.get() },
        stateToFormatter
    )

    /** Set dependencies for the corresponding Eclipse version  */
    fun setVersion(version: String) {
        val url = "/" + ECLIPSE_FORMATTER_RESOURCES + "/" + formatterName.replace(
            ' ',
            '_'
        ) + "/v" + version + ".lockfile"
        val depsFile = EclipseBasedStepBuilder::class.java.getResourceAsStream(url)
            ?: throw IllegalArgumentException("No such version $version, expected at $url")
        val content = depsFile.readBytes()
        val allLines = String(content, StandardCharsets.UTF_8)
        val lines = allLines.split("\n").toTypedArray()
        dependencies.clear()
        for (line in lines) {
            if (!line.startsWith("#") && line.isNotBlank()) {
                dependencies.add(line)
            }
        }
    }

    /** Set settings files containing Eclipse preferences  */
    fun setPreferences(settingsFiles: Iterable<File>) {
        this.settingsFiles = settingsFiles
    }

    /** Creates the state of the configuration.  */
    fun get(): State {
        /*
         * The current use case is tailored for Gradle.
         * Gradle calls this method only once per execution
         * and compares the State with the one of a previous run
         * for incremental building.
         * Hence a lazy construction is not required.
         */
        return State(
            formatterStepExt,
            jarProvisioner,
            dependencies,
            settingsFiles
        )
    }

    /**
     * State of Eclipse configuration items, providing functionality to derived information
     * based on the state.
     */
    class State(
        private val formatterStepExt: String,
        jarProvisioner: Provisioner,
        dependencies: List<String>,
        settingsFiles: Iterable<File>
    ) : Serializable {
        private val jarState = JarState.withoutTransitives(dependencies, jarProvisioner)

        // The formatterStepExt assures that different class loaders are used for different step types
        private val settingsFiles =
            FileSignature.signAsList(settingsFiles) // Keep the IllegalArgumentException since it contains detailed information

        /** Get formatter preferences  */
        val preferences: Properties
            get() {
                // Keep the IllegalArgumentException since it contains detailed information
                val preferences = FormatterProperties.from(settingsFiles.files())
                return preferences.properties
            }

        companion object {
            // Not used, only the serialization output is required to determine whether the object has changed
            private const val serialVersionUID = 1L
        }

        /** Returns first coordinate from sorted set that starts with a given prefix. */
        fun getMavenCoordinate(prefix: String) =
            jarState.mavenCoordinates.firstOrNull { it.startsWith(prefix) }

        /**
         * Load class based on the given configuration of JAR provider and Maven coordinates.
         * Different class loader instances are provided in the following scenarios:
         *
         *  1. The JARs ([.jarState]) have changes (this should only occur during development)
         *  1. Different configurations ([.settingsFiles]) are used for different sub-projects
         *  1. The same Eclipse step implementation provides different formatter types ([.formatterStepExt])
         *
         */
        fun loadClass(name: String?) =
            jarState.getClassLoader(this).loadClass(name)
    }
}
