package com.github.autostyle.generic

import org.intellij.lang.annotations.Language

@Language("RegExp")
private const val JAVA_MULTILINE_COMMENT = "/[*](?>[^\\\\*]++|[*][^/]|\\\\[*])*+[*]/"
@Language("RegExp")
private const val JAVA_SINGLE_LINE_COMMENT_BLOCK = "//[^\n]++(?:\n//[^\n]++)++"

@Language("RegExp")
private const val SHELL_BANG_LINE = "#![^\n]*+[^\n]*+"
@Language("RegExp")
private const val SHELL_COMMENT_LINE = "#[^\n]*+"
@Language("RegExp")
private const val SHELL_COMMENT_BLOCK = "$SHELL_COMMENT_LINE(?:\n$SHELL_COMMENT_LINE)*+"

@Language("RegExp")
private const val BAT_ECHO_LINE = "@?+echo[^\n]*+"
@Language("RegExp")
private const val BAT_COMMENT_LINE = "(?>::|rem|@rem)[^\n]*+"
@Language("RegExp")
private const val BAT_COMMENT_BLOCK = "$BAT_COMMENT_LINE(?:\n$BAT_COMMENT_LINE)*+"

@Language("RegExp")
private const val XML_PI = "<[?](?>[^?]++|[?][^>])*+[?]>"
@Language("RegExp")
private const val XML_COMMENT = "<!--(?>[^-]++|(?!-->)[^<>])*+-->"

interface CopyrightReplacer {
    fun replace(input: String, copyright: String): String

    object Java : TextAndHeaderCopyrightReplacer(
        1, listOf(),
        Regex("^\\s*+(?>$JAVA_MULTILINE_COMMENT|$JAVA_SINGLE_LINE_COMMENT_BLOCK)?\\s*+"),
        Regex("^/[/*]")
    )

    object Shell : TextAndHeaderCopyrightReplacer(
        2, listOf(1),
        Regex("^\\s*+($SHELL_BANG_LINE)?+\\s*+($SHELL_COMMENT_BLOCK)?\\s*+"),
        Regex("^#")
    )

    object Bat : TextAndHeaderCopyrightReplacer(
        2, listOf(1),
        Regex("^\\s*+($BAT_ECHO_LINE)?+\\s*+($BAT_COMMENT_BLOCK)?\\s*+"),
        Regex("^(?>::|rem|@rem)")
    )

    object Xml : TextAndHeaderCopyrightReplacer(
        2, listOf(1, 3),
        Regex("^\\s*+($XML_PI)?+\\s*+($XML_COMMENT)?\\s*+($XML_PI)?+\\s*+"),
        Regex("^<!--")
    )
}

abstract class SimpleCopyrightReplacer(val regex: Regex) : CopyrightReplacer {
    override fun replace(input: String, copyright: String) =
        input.replace(regex, copyright)
}

abstract class TextAndHeaderCopyrightReplacer(
    val valueGroup: Int,
    val headerGroups: List<Int>,
    val regex: Regex,
    val commentStartRegex: Regex
) : CopyrightReplacer {
    override fun replace(input: String, copyright: String): String {
        val match = regex.find(input) ?: return copyright + "\n" + input
        val headers = headerGroups.mapNotNull { match.groups[it]?.value }
        val sb = StringBuilder()
        for (h in headers) {
            sb.append(h).append('\n')
        }
        sb.append(copyright)
        val lastIndex = match.range.last + 1
        if (lastIndex < input.length) {
            if (!sb.endsWith("\n\n")) {
                val prefix = input.subSequence(lastIndex, input.length.coerceAtMost(lastIndex + 10))
                if (commentStartRegex.find(prefix) != null) {
                    // If comment follows the copyright header, then they should be separated with a blank line
                    // This especially helps for shell-like comments
                    sb.append('\n')
                }
            }
            sb.append(input, lastIndex, input.length)
        }
        return sb.toString()
    }
}
