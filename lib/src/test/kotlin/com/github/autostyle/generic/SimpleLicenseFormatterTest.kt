package com.github.autostyle.generic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleLicenseFormatterTest {
    @Test
    internal fun java() {
        val txt = """
            Line 1
            Line 2
        """.trimIndent()

        assertEquals(
            SimpleCopyrightFormatter.Java.apply(txt), """
                /*
                 * Line 1
                 * Line 2
                 */
            """.trimIndent(), txt
        );
    }

    @Test
    internal fun paamayimNekudotayim() {
        val txt = """
            Line 1
            Line 2
        """.trimIndent()

        val s = ' '
        assertEquals(
            SimpleCopyrightFormatter.PaamayimNekudotayim.apply(txt), """
                ::$s
                :: Line 1
                :: Line 2
                ::$s
            """.trimIndent(), txt
        );
    }
}
