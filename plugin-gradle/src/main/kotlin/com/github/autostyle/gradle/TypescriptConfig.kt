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
import com.github.autostyle.npm.TsConfigFileType
import com.github.autostyle.npm.TsFmtFormatterStep
import com.github.autostyle.npm.TypedTsFmtConfigFile
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import java.util.*
import javax.inject.Inject

class TypescriptConfig @Inject internal constructor(
    private val devDependencies: Map<String, String>,
    objects: ObjectFactory,
    private val project: Project
) : NpmStepConfig(objects) {
    private var config = emptyMap<String, Any>()
    private lateinit var configFile: TypedTsFmtConfigFile

    fun config(config: Map<String, Any>) {
        this.config = TreeMap(config)
    }

    fun tsconfigFile(path: Any) {
        configFile(TsConfigFileType.TSCONFIG, path)
    }

    fun tslintFile(path: Any) {
        configFile(TsConfigFileType.TSLINT, path)
    }

    fun vscodeFile(path: Any) {
        configFile(TsConfigFileType.VSCODE, path)
    }

    fun tsfmtFile(path: Any) {
        configFile(TsConfigFileType.TSFMT, path)
    }

    private fun configFile(configFileType: TsConfigFileType, path: Any) {
        configFile = TypedTsFmtConfigFile(
            configFileType,
            project.file(path)
        )
    }

    public override fun createStep(): FormatterStep = TsFmtFormatterStep.create(
        devDependencies,
        project.asProvisioner(),
        project.buildDir,
        npmExecutable.orNull?.let { project.file(it) },
        configFile,
        config
    )
}
