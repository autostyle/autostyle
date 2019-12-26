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

import com.github.autostyle.AutostyleCache
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.util.*

class AutostylePlugin : Plugin<Project> {
    companion object {
        private const val TASK_GROUP = "Verification"
        private const val CHECK_DESCRIPTION =
            "Checks that sourcecode satisfies formatting steps."
        private const val APPLY_DESCRIPTION =
            "Applies code formatting steps to sourcecode in-place."
        private const val FILES_PROPERTY = "autostyleFiles"
        internal const val PROJECT_DIR_MAP = "_autostyleProjectDirMap_"
    }

    override fun apply(project: Project) {
        project.configurePlugin()
        project.rootProject.installProjectPathsExtra()
    }

    private fun Project.installProjectPathsExtra() {
        // When Autostyle rule (e.g. **/*.md) is declared for a project,
        // it should not descend to subprojects by default.
        // So we want to exclude all the folders that represent project dir and build dirs of the subproject
        if (extra.has(PROJECT_DIR_MAP)) {
            return
        }
        extra[PROJECT_DIR_MAP] = lazy {
            allprojects
                .asSequence()
                .flatMap { sequenceOf(it.projectDir, it.buildDir) }
                .plus(File(rootDir, "buildSrc"))
                .plus(File(rootDir, ".gradle"))
                .plus(File(rootDir, ".idea"))
                .mapTo(TreeSet()) { it.absolutePath + File.separatorChar }
        }
    }

    private fun Project.configurePlugin() {
        // make sure there's a `clean` task
        plugins.apply(BasePlugin::class.java)
        // setup the extension
        val extension = extensions.create(
            AutostyleExtension.EXTENSION,
            AutostyleExtension::class.java,
            this
        )
        configurations.detachedConfiguration()
        val checkTask = tasks.register(AutostyleExtension.EXTENSION + AutostyleExtension.CHECK) {
            group = TASK_GROUP
            description = CHECK_DESCRIPTION
            dependsOn(tasks.withType<AutostyleCheckTask>())
        }
        tasks.register(AutostyleExtension.EXTENSION + AutostyleExtension.APPLY) {
            group = TASK_GROUP
            description = APPLY_DESCRIPTION
            dependsOn(tasks.withType<AutostyleApplyTask>())
        }

        gradle.buildFinished {
            AutostyleCache.clear()
        }
        afterEvaluate {
            // Add our check task as a dependency on the global check task
            // getTasks() returns a "live" collection, so this works even if the
            // task doesn't exist at the time this call is made
            if (extension.isEnforceCheck) {
                tasks.named(JavaBasePlugin.CHECK_TASK_NAME) {
                    dependsOn(checkTask)
                }
            }
        }
    }
}
