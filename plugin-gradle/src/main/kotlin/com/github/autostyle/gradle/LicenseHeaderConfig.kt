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
import com.github.autostyle.generic.CopyrightStyle
import com.github.autostyle.generic.DEFAULT_HEADER_STYLES
import com.github.autostyle.generic.DefaultCopyrightStyle
import com.github.autostyle.generic.ImprovedLicenseHeaderStep
import com.github.autostyle.gradle.ext.conv
import org.gradle.api.Project
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import java.util.*

open class LicenseHeaderConfig internal constructor(
    project: Project
) {
    val copyright = project.objects.property<String>()
    val addBlankLineAfter = project.objects.property<Boolean>().conv(false)
    val copyrightStyles =
        project.objects.mapProperty<String, CopyrightStyle>().apply { set(DEFAULT_HEADER_STYLES) }

    fun copyrightStyle(extension: String, style: CopyrightStyle) {
        copyrightStyles.put(extension, style)
    }

    fun copyrightStyle(extension: String, style: String) {
        copyrightStyle(extension, DefaultCopyrightStyle.valueOf(style.uppercase(Locale.ENGLISH)))
    }

    internal fun createStep(): FormatterStep =
        ImprovedLicenseHeaderStep(
            copyright.get(),
            addBlankLineAfter.get(),
            copyrightStyles.get()
        )
}
