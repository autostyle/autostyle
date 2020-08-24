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
package com.github.autostyle.extra.eclipse

import com.github.autostyle.FormatterStep
import com.github.autostyle.LineEnding
import com.github.autostyle.ResourceHarness
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Provides a common set of tests for all Autostyle Eclipse Formatter steps provided by
 * lib-extra.
 *
 *
 * The external Autostyle Eclipse Formatter Step implementations are responsible
 * to test the functionality of the Autostyle adaptations for the Eclipse formatter.
 * This explicitly includes a check of various forms of valid and invalid input
 * to cover all relevant execution paths during the code formatting and internal
 * exception handling.
 *
 *
 *
 * The lib-extra users, like plugin-gradle and plugin-maven are responsible
 * to test the correct provision of user settings to the generic
 * Autostyle Eclipse Formatter steps provided by lib-extra.
 *
 */
abstract class EclipseCommonTests : ResourceHarness() {
    @Rule
    @JvmField
    var folderDontUseDirectly = TemporaryFolder()

    override fun rootFolder(): File = folderDontUseDirectly.root.canonicalFile

    /** Returns the complete set of versions supported by the formatter  */
    protected abstract val supportedVersions: Array<String>

    /**
     * Returns the input which shall be used with the formatter version.
     * The input shall be very simple and supported if possible by all
     * formatter versions.
     */
    protected abstract fun getTestInput(version: String?): String

    /**
     * Returns the output which is expected from the formatter step.
     * If possible, the output shall be equal for all versions,
     * but since the default formatter preferences are used, this
     * might not be achieved for all versions.
     */
    protected abstract fun getTestExpectation(version: String?): String

    /** Create formatter step for a specific version  */
    protected abstract fun createStep(version: String?): FormatterStep?

    @Test
    fun testSupportedVersions() {
        val versions = supportedVersions
        for (version in versions) {
            val input = getTestInput(version)
            val expected = getTestExpectation(version)
            val inputFile = setFile("someInputFile").toContent(input)
            var step: FormatterStep? = null
            try {
                step = createStep(version)
            } catch (e: Exception) {
                Assertions.fail(
                    "Exception occured when instantiating step for version: $version",
                    e
                )
            }
            var output: String? = null
            try {
                output = LineEnding.toUnix(step!!.format(input, inputFile)!!)
            } catch (e: Exception) {
                Assertions.fail("Exception occured when formatting input with version: $version", e)
            }
            Assertions.assertThat(output)
                .`as`("Formatting output unexpected with version: $version").isEqualTo(expected)
        }
    }
}
