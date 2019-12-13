package com.github.autostyle.generic

import java.util.function.Function

interface CopyrightStyle {
    val licenseFormatter: Function<String, String>
    val replacer: CopyrightReplacer
}

data class SimpleCopyrightStyle(
    override val licenseFormatter: Function<String, String>,
    override val replacer: CopyrightReplacer
) : CopyrightStyle

enum class DefaultCopyrightStyle(
    override val licenseFormatter: Function<String, String>,
    override val replacer: CopyrightReplacer
) : CopyrightStyle {
    AS_IS(Function.identity(), object : CopyrightReplacer {
        override fun replace(input: String, copyright: String) = input
    }),
    JAVA(SimpleCopyrightFormatter.Java, CopyrightReplacer.Java),
    SHELL(SimpleCopyrightFormatter.Shell, CopyrightReplacer.Shell),
    REM(SimpleCopyrightFormatter.Bat, CopyrightReplacer.Bat),
    AT_REM(SimpleCopyrightFormatter.AtBat, CopyrightReplacer.Bat),
    PAAMAYIM_NEKUDOTAYIM(SimpleCopyrightFormatter.PaamayimNekudotayim, CopyrightReplacer.Bat),
    XML(SimpleCopyrightFormatter.Xml, CopyrightReplacer.Xml)
}
