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
package com.github.autostyle.gradle

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("npm")
class PrettierIntegrationTest : GradleIntegrationTest() {
    @Test
    fun useInlineConfig() {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            def prettierConfig = [:]
            prettierConfig['printWidth'] = 50
            prettierConfig['parser'] = 'typescript'
            autostyle {
                format 'mytypescript', {
                    target 'test.ts'
                    prettier {
                        config prettierConfig
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/prettier/config/typescript.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean")
    }

    @Test
    fun useFileConfig() {
        setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc.yml")
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            autostyle {
                format 'mytypescript', {
                    target 'test.ts'
                    prettier {
                        configFile '.prettierrc.yml'
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/prettier/config/typescript.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean")
    }
}
