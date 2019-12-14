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
class TypescriptExtensionTest : GradleIntegrationTest() {
    @Test
    fun allowToSpecifyFormatterVersion() {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            def tsfmtconfig = [:]
            tsfmtconfig['indentSize'] = 1
            tsfmtconfig['convertTabsToSpaces'] = true
            autostyle {
                typescript {
                    target 'test.ts'
                    tsfmt('7.2.1') {
                        config(tsfmtconfig)
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean")
    }

    @Test
    fun allowToSpecifyMultipleVersionStrings() {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            def tsfmtconfig = [:]
            tsfmtconfig['indentSize'] = 1
            tsfmtconfig['convertTabsToSpaces'] = true
            autostyle {
                typescript {
                    target 'test.ts'
                    tsfmt(['typescript-formatter': '7.2.1', 'tslint': '5.1.0', 'typescript': '2.9.2']) {
                        config(tsfmtconfig)
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean")
    }

    @Test
    fun useTsfmtInlineConfig() {
        setFile("build.gradle").toLines(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            def tsfmtconfig = [:]
            tsfmtconfig['indentSize'] = 1
            tsfmtconfig['convertTabsToSpaces'] = true
            autostyle {
                typescript {
                    target 'test.ts'
                    tsfmt {
                        config(tsfmtconfig)
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean")
    }

    @Test
    fun useTsfmtFileConfig() {
        setFile("tsfmt.json").toContent(
            """
            {
                "indentSize": 1,
                "convertTabsToSpaces": true
            }
            """.trimIndent()
        )
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            autostyle {
                typescript {
                    target 'test.ts'
                    tsfmt {
                        tsfmtFile('tsfmt.json')
                    }
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean")
    }

    @Test
    fun usePrettier() {
        setFile("build.gradle").toContent(
            """
            plugins {
                id 'com.github.autostyle'
            }
            repositories { mavenCentral() }
            autostyle {
                typescript {
                    target 'test.ts'
                    prettier()
                }
            }
            """.trimIndent()
        )
        setFile("test.ts").toResource("npm/prettier/filetypes/typescript/typescript.dirty")
        gradleRunner().withArguments("--stacktrace", "autostyleApply").build()
        assertFile("test.ts").sameAsResource("npm/prettier/filetypes/typescript/typescript.clean")
    }
}
