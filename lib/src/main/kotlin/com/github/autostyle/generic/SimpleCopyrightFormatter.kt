package com.github.autostyle.generic

import java.util.function.Function

abstract class SimpleCopyrightFormatter(
    val commentStart: String? = null,
    val commentLine: String? = null,
    val commentEnd: String? = null
): Function<String, String> {
    object Java: SimpleCopyrightFormatter("/*", " * ", " */")
    object Shell: SimpleCopyrightFormatter("# ", "# ", "# ")
    object Bat: SimpleCopyrightFormatter("rem ", "rem ", "rem ")
    object AtBat: SimpleCopyrightFormatter("@rem ", "@rem ", "@rem ")
    object PaamayimNekudotayim: SimpleCopyrightFormatter(":: ", ":: ", ":: ")
    object Xml: SimpleCopyrightFormatter("<!--", "  ~ ", "  -->")

    override fun apply(value: String): String {
        val sb = StringBuilder()
        commentStart?.let { sb.append(it).append('\n') }
        commentLine?.let {
            sb.append(it)
            sb.append(value.replace("\n", "\n" + it))
        }
        commentEnd?.let { sb.append("\n").append(it) }
        return sb.toString()
    }
}
