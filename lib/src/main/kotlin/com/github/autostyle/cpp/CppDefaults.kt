package com.github.autostyle.cpp

object CppDefaults {
    /**
     * Filter based on Eclipse-CDT `org.eclipse.core.contenttype.contentTypes`
     * extension `cSource`, `cHeader`, `cxxSource` and `cxxHeader`.
     */
    val EXTENSIONS = listOf(
        "c",
        "h",
        "C",
        "cpp",
        "cxx",
        "cc",
        "c++",
        "h",
        "hpp",
        "hh",
        "hxx",
        "inc"
    )
}
