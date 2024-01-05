pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun PluginDependenciesSpec.idv(id: String, key: String = id) = id(id) version key.v()

        idv("com.github.autostyle", "released")
        id("com.github.vlsi.crlf") version "1.90"
        id("com.github.vlsi.gradle-extensions") version "1.90"
        id("com.github.vlsi.ide") version "1.90"
        id("com.github.vlsi.license-gather") version "1.90"
        id("com.github.vlsi.stage-vote-release") version "1.90"
        id("com.gradle.plugin-publish") version "1.2.1"
        kotlin("jvm") version "1.9.20"
    }
}

if (JavaVersion.current() < JavaVersion.VERSION_17) {
    throw UnsupportedOperationException("Please use Java 17 or 21 for launching Gradle, the current Java is ${JavaVersion.current().majorVersion}")
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
            classpath("org.eclipse.jgit:org.eclipse.jgit:5.6.0.201912101111-r")
            classpath("com.googlecode.concurrent-trees:concurrent-trees:2.6.1")
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
