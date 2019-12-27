package com.github.autostyle.generic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class CopyrightReplacerTest {
    companion object {
        const val NEW_COPYRIGHT = "Updated Copyright\nLine 2"

        @JvmStatic
        fun javaSources() = ArrayList<Arguments>().apply {
            add(
                arguments(
                    """
                        /*
                         * Copyright ACME
                         */
                        package test
                    """.trimIndent(),
                    """
                        /*
                         * Updated Copyright
                         * Line 2
                         */
                        package test
                    """.trimIndent()
                )
            )
            add(
                arguments(
                    """
                        /*Copyright ACME*/
                        // comment
                        package test
                    """.trimIndent(),
                    """
                        /*
                         * Updated Copyright
                         * Line 2
                         */

                        // comment
                        package test
                    """.trimIndent()
                )
            )
            add(
                arguments(
                    """
                        // Line1
                        // Line2

                        // comment
                        package test
                    """.trimIndent(),
                    """
                        /*
                         * Updated Copyright
                         * Line 2
                         */

                        // comment
                        package test
                    """.trimIndent()
                )
            )
        }

        @JvmStatic
        fun shellSources() = ArrayList<Arguments>().apply {
            add(
                arguments(
                    """
                        #!/bin/bash
                        # Copyright ACME
                        A=1
                    """.trimIndent(),
                    """
                        #!/bin/bash
                        #${' '}
                        # Updated Copyright
                        # Line 2
                        #${' '}
                        A=1
                    """.trimIndent(),
                    "#!/bin/bash"
                )
            )
            add(
                arguments(
                    """
                        # Copyright ACME
                        #!/bin/bash
                        A=2
                    """.trimIndent(),
                    """
                        #${' '}
                        # Updated Copyright
                        # Line 2
                        #${' '}
                        A=2
                    """.trimIndent()
                )
            )
            add(
                arguments(
                    """
                        # Copyright ACME

                        # Let A be 42
                        A=42
                    """.trimIndent(),
                    """
                        #${' '}
                        # Updated Copyright
                        # Line 2
                        #${' '}

                        # Let A be 42
                        A=42
                    """.trimIndent()
                )
            )
        }

        @JvmStatic
        fun batSources() = ArrayList<Arguments>().apply {
            add(
                arguments(
                    """
                        @echo off
                        rem Copyright 1
                        rem Copyright 2
                        A=1
                    """.trimIndent(),
                    """
                        @echo off
                        ::${' '}
                        :: Updated Copyright
                        :: Line 2
                        ::${' '}
                        A=1
                    """.trimIndent(),
                    "#!/bin/bash"
                )
            )
            add(
                arguments(
                    """
                        @rem Copyright 1
                        @rem Copyright 2
                        A=2
                    """.trimIndent(),
                    """
                        ::${' '}
                        :: Updated Copyright
                        :: Line 2
                        ::${' '}
                        A=2
                    """.trimIndent()
                )
            )
        }

        @JvmStatic
        fun xmlSources() = ArrayList<Arguments>().apply {
            val expected = """
                <?xml version="1.0" encoding="utf-8"?>
                <!--
                  ~ Updated Copyright
                  ~ Line 2
                  -->
                <root>
            """.trimIndent()
            add(
                arguments(
                    """
                        <?xml version="1.0" encoding="utf-8"?>
                        <!--
                          ~ Copyright ACME
                          -->
                        <root>
                    """.trimIndent(),
                    expected
                )
            )
            add(
                arguments(
                    """
                        <?xml version="1.0" encoding="utf-8"?>
                        <root>
                    """.trimIndent(),
                    expected
                )
            )
            add(
                arguments(
                    """
                        <root>
                    """.trimIndent(),
                    """
                        <!--
                          ~ Updated Copyright
                          ~ Line 2
                          -->
                        <root>
                    """.trimIndent()
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("javaSources")
    internal fun java(input: String, expected: String) {
        assertReplaceCopyright(input, expected, DefaultCopyrightStyle.JAVA)
    }

    @ParameterizedTest
    @MethodSource("shellSources")
    internal fun shell(input: String, expected: String) {
        assertReplaceCopyright(input, expected, DefaultCopyrightStyle.SHELL)
    }

    @ParameterizedTest
    @MethodSource("batSources")
    internal fun bat(input: String, expected: String) {
        assertReplaceCopyright(input, expected, DefaultCopyrightStyle.PAAMAYIM_NEKUDOTAYIM)
    }

    @ParameterizedTest
    @MethodSource("xmlSources")
    internal fun xml(input: String, expected: String) {
        assertReplaceCopyright(input, expected, DefaultCopyrightStyle.XML)
    }

    private fun assertReplaceCopyright(
        input: String,
        expected: String,
        style: CopyrightStyle
    ) {
        val new = style.licenseFormatter.apply(NEW_COPYRIGHT) + '\n'
        val replaced = style.replacer.replace(input, new)
        assertEquals(expected, replaced, input)
        val replaced2 = style.replacer.replace(replaced, new)
        assertEquals(replaced, replaced2) { "second pass for <$input>" }
    }
}
