package com.github.autostyle.generic

import com.github.autostyle.FormatterStep
import com.github.autostyle.cpp.CppDefaults
import com.github.autostyle.css.CssDefaults
import com.github.autostyle.java.JavaDefaults
import java.io.File

val DEFAULT_HEADER_STYLES =
    JavaDefaults.EXTENSIONS.associateWith { DefaultCopyrightStyle.JAVA } +
            CppDefaults.EXTENSIONS.associateWith { DefaultCopyrightStyle.JAVA } +
            CssDefaults.EXTENSIONS.associateWith { DefaultCopyrightStyle.JAVA } +
            setOf("bat", "cmd").associateWith { DefaultCopyrightStyle.PAAMAYIM_NEKUDOTAYIM } +
            setOf("kts", "kt").associateWith { DefaultCopyrightStyle.JAVA } +
            setOf("groovy", "gradle").associateWith { DefaultCopyrightStyle.JAVA } +
            setOf("sh", "yml", "properties").associateWith { DefaultCopyrightStyle.SHELL } +
            setOf("xsd", "xsl", "xml", "html").associateWith { DefaultCopyrightStyle.XML }

class ImprovedLicenseHeaderStep(
    val copyright: String,
    val extraNewline: Boolean,
    val styles: Map<String, CopyrightStyle>
) : FormatterStep {
    @Transient
    private val formatted = mutableMapOf<CopyrightStyle, String>()

    override fun getName() = "licenseHeader"

    override fun format(rawUnix: String, file: File): String? {
        val style = styles[file.extension] ?: styles[""] ?: return null

        val newCopyright = formatted.getOrPut(style) {
            val v = Regex.escapeReplacement(style.licenseFormatter.apply(copyright))
            if (extraNewline) v + "\n\n" else v + '\n'
        }
        return style.replacer.replace(rawUnix, newCopyright)
    }
}
