import com.github.vlsi.gradle.properties.dsl.props
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.time.Duration

plugins {
    id("com.github.autostyle")
    id("com.gradle.plugin-publish") apply false
    id("com.github.vlsi.gradle-extensions")
    id("com.github.vlsi.ide")
    id("com.gradleup.nmcp.aggregation")
    kotlin("jvm") apply false
}

val String.v: String get() = rootProject.extra["$this.version"] as String

val release by props()

val buildVersion = "current".v + (if (release) "" else "-SNAPSHOT")

println("Building Autostyle $buildVersion")

val autostyleSelf by props()
val skipAutostyle by props()
val skipJavadoc by props()

nmcpAggregation {
    val centralPortalPublishingType = providers.gradleProperty("centralPortalPublishingType").orElse("AUTOMATIC")
    val centralPortalValidationTimeout = providers.gradleProperty("centralPortalValidationTimeout").map { it.toLong() }.orElse(60)

    centralPortal {
        username = providers.environmentVariable("CENTRAL_PORTAL_USERNAME")
        password = providers.environmentVariable("CENTRAL_PORTAL_PASSWORD")
        publishingType = centralPortalPublishingType
        validationTimeout = centralPortalValidationTimeout.map { Duration.ofMinutes(it) }
    }
}

dependencies {
    subprojects.forEach {
        nmcpAggregation(project(it.path))
    }
}

allprojects {
    group = "com.github.autostyle"
    version = buildVersion

    val javaUsed = file("src/main/java").isDirectory || file("src/test/java").isDirectory
    val kotlinUsed = file("src/main/kotlin").isDirectory || file("src/test/kotlin").isDirectory
    if (javaUsed) {
        apply(plugin = "java-library")
        apply(plugin = "mdoclet")
        dependencies {
            val compileOnly by configurations
            compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.4")
            compileOnly("com.google.code.findbugs:jsr305:3.0.2")
        }
    }
    if (kotlinUsed) {
        apply(plugin = "java-library")
        apply(plugin = "org.jetbrains.kotlin.jvm")
        dependencies {
            val implementation by configurations
            implementation(kotlin("stdlib"))
        }
    }
    if (javaUsed || kotlinUsed) {
        dependencies {
            val implementation by configurations
            implementation(platform(project(":autostyle-bom")))
        }
    }

    val hasTests = file("src/test/java").isDirectory || file("src/test/kotlin").isDirectory
    if (hasTests) {
        // Add default tests dependencies
        dependencies {
            val testImplementation by configurations
            val testRuntimeOnly by configurations
            testImplementation(platform(project(":autostyle-bom-testing")))
            testImplementation("org.junit.jupiter:junit-jupiter-api")
            testImplementation("org.junit.jupiter:junit-jupiter-params")
            testImplementation("org.hamcrest:hamcrest")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<KotlinProjectExtension> {
            coreLibrariesVersion = "1.8.20"
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
        compilerOptions {
            @Suppress("DEPRECATION")
            apiVersion = KotlinVersion.KOTLIN_1_8
            languageVersion = apiVersion
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs.add("-Xjvm-default=all")
            freeCompilerArgs.add("-Xjdk-release=1.8")
        }
    }

    if (!skipAutostyle) {
        if (!autostyleSelf) {
            apply(plugin = "com.github.autostyle")
        }
        // Autostyle is already published, so we can always use it, except it is broken :)
        // So there's an option to disable it
        apply(from = "$rootDir/autostyle.gradle.kts")
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
            withSourcesJar()
            if (!skipJavadoc) {
                withJavadocJar()
            }
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
                options.release.set(8)
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "Apache-2.0"
                    attributes["Implementation-Title"] = "Autostyle"
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "Autostyle"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "Autostyle"
                    attributes["Implementation-Vendor"] = "Autostyle"
                    attributes["Implementation-Vendor-Id"] = "com.github.vlsi.autostyle"
                }
            }
            withType<Test>().configureEach {
                useJUnitPlatform()
                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    showStandardStreams = true
                }
                // Pass the property to tests
                fun passProperty(name: String, default: String? = null) {
                    val value = System.getProperty(name) ?: default
                    value?.let { systemProperty(name, it) }
                }
                passProperty("junit.jupiter.execution.parallel.enabled", "true")
                passProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                passProperty("junit.jupiter.execution.timeout.default", "5 m")
            }
        }
    }
}
