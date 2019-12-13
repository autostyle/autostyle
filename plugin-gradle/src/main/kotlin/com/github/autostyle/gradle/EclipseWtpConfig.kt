package com.github.autostyle.gradle

import com.github.autostyle.extra.wtp.EclipseWtpFormatterStep
import org.gradle.api.Project

class EclipseWtpConfig internal constructor(
    type: EclipseWtpFormatterStep,
    version: String,
    project: Project
) : EclipseBasedConfig(version, project, { type.createBuilder(it) })
