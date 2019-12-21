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
import com.github.autostyle.sql.DBeaverSQLFormatterStep
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

open class SqlExtension @Inject constructor(name: String, root: AutostyleExtension) : BaseFormatExtension(name, root) {
    init {
        patterns.include("**/*.sql")
    }

    @JvmOverloads
    fun dbeaver(action: Action<DBeaverSQLFormatterConfig>? = null) {
        DBeaverSQLFormatterConfig(root.project).also {
            action?.execute(it)
            addStep(it.createStep())
        }
    }

    class DBeaverSQLFormatterConfig internal constructor(
        private val project: Project
    ) {
        val configFiles = project.objects.setProperty<Any>()

        fun configFile(vararg files: Any) {
            configFiles.addAll(files)
        }

        internal fun createStep(): FormatterStep =
            DBeaverSQLFormatterStep.create(project.files(configFiles))
    }
}
