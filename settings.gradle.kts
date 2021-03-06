pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun PluginDependenciesSpec.idv(id: String, key: String = id) = id(id) version key.v()

        idv("com.github.autostyle", "released")
        idv("com.github.ben-manes.versions")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.ide", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.license-gather", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("com.gradle.plugin-publish")
        kotlin("jvm") version "kotlin".v()
    }
}

for (p in listOf(
    "bom",
    "testlib",
    "lib",
    "lib-extra",
    "plugin-gradle"
)) {
    include(p)
    project(":$p").apply {
        name = "autostyle-$p"
    }
}

// See https://github.com/gradle/gradle/issues/1348#issuecomment-284758705 and
// https://github.com/gradle/gradle/issues/5321#issuecomment-387561204
// Gradle inherits Ant "default excludes", however we do want to archive those files
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitattributes")
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

fun property(name: String) =
    when (extra.has(name)) {
        true -> extra.get(name) as? String
        else -> null
    }

// This enables to use local clone of vlsi-release-plugins for debugging purposes
property("localReleasePlugins")?.ifBlank { "../vlsi-release-plugins" }?.let {
    println("Importing project '$it'")
    includeBuild(it)
}

buildscript {
    fun property(name: String) =
        when (extra.has(name)) {
            true -> extra.get(name) as? String
            else -> null
        }

    fun String.v(): String = extra["$this.version"] as String

    if (property("skipAutostyle")?.ifBlank { "true" }?.toBoolean() == true) {
        // Skip
    } else if (property("autostyleSelf")?.ifBlank { "true" }?.toBoolean() == true) {
        val ver = property("autostyle.version") + "-SNAPSHOT"

        repositories {
            gradlePluginPortal()
        }

        dependencies {
            classpath(files("plugin-gradle/build/libs/autostyle-plugin-gradle-$ver.jar",
                "lib/build/libs/autostyle-lib-$ver.jar",
                "lib-extra/build/libs/autostyle-lib-extra-$ver.jar"))
            // needed by GitAttributesLineEndings
            classpath("org.eclipse.jgit:org.eclipse.jgit:${"org.eclipse.jgit".v()}")
            classpath("com.googlecode.concurrent-trees:concurrent-trees:${"concurrent-trees".v()}")
            // used for xml parsing in EclipseFormatter
            classpath("org.codehaus.groovy:groovy-xml:${"groovy-xml".v()}")
        }
    } else {
        repositories {
            gradlePluginPortal()
        }

        dependencies {
            classpath("com.github.autostyle:com.github.autostyle.gradle.plugin:${"released".v()}")
        }
    }
}
