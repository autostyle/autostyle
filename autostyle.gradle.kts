import com.github.autostyle.gradle.AutostyleExtension

apply(plugin = "com.github.autostyle")

repositories {
    mavenCentral()
}

plugins.withType<JavaPlugin> {
    configure<AutostyleExtension> {
        java {
            removeUnusedImports()
            importOrder("static ", "static java.", "static javax.", "", "java.", "javax.")
            trimTrailingWhitespace()
            indentWithSpaces(2)
            endWithNewline()
        }
    }
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    configure<AutostyleExtension> {
        kotlin {
            ktlint {
                userData(mapOf(
                    "disabled_rules" to "no-wildcard-imports,import-ordering"
                ))
            }
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

configure<AutostyleExtension> {
    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    freshmark {
        filter.include("**/*.md")
        propertiesFile("$rootDir/gradle.properties")
        properties {
            put("yes", ":+1:")
            put("no", ":white_large_square:")
            put("stableGradle", get("autostyle.version") as String)
        }
    }
}
