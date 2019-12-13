import com.github.vlsi.gradle.properties.dsl.props
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.github.ben-manes.versions")
    id("com.gradle.plugin-publish") apply false
    id("com.jfrog.bintray") apply false
    id("org.jdrupes.mdoclet") apply false
    id("com.github.vlsi.gradle-extensions")
    kotlin("jvm") apply false
}

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "autostyle".v + "-SNAPSHOT"

allprojects {
    group = "com.github.vlsi.autostyle"
    version = buildVersion

    val javaUsed = file("src/main/java").isDirectory
    val kotlinUsed = file("src/main/kotlin").isDirectory || file("src/test/kotlin").isDirectory
    if (javaUsed) {
        apply(plugin = "java-library")
        apply(plugin = "org.jdrupes.mdoclet")
        dependencies {
            val compileOnly by configurations
            compileOnly("net.jcip:jcip-annotations:1.0")
            compileOnly("com.github.spotbugs:spotbugs-annotations:3.1.6")
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
            implementation(platform(project(":bom")))
        }
    }

    val hasTests = file("src/test/java").isDirectory || file("src/test/kotlin").isDirectory
    if (hasTests) {
        // Add default tests dependencies
        dependencies {
            val testImplementation by configurations
            val testRuntimeOnly by configurations
            testImplementation("org.junit.jupiter:junit-jupiter-api")
            testImplementation("org.junit.jupiter:junit-jupiter-params")
            testImplementation("org.hamcrest:hamcrest")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
            if (props.bool("junit4", default = true)) {
                // Allow projects to opt-out of junit dependency, so they can be JUnit5-only
                testImplementation("junit:junit")
                testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
            }
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        // Ensure builds are reproducible
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = "775".toInt(8)
        fileMode = "664".toInt(8)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        sourceCompatibility = "unused"
        targetCompatibility = "unused"
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginConvention> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        repositories {
            mavenCentral()
        }

        apply(plugin = "maven-publish")

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
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
                passProperty("junit.jupiter.execution.timeout.default", "5 m")
            }
            configure<PublishingExtension> {
                if (project.path == ":") {
                    // Do not publish "root" project. Java plugin is applied here for DSL purposes only
                    return@configure
                }
                publications {
                    create<MavenPublication>(project.name) {
                        artifactId = project.name
                        version = rootProject.version.toString()
                        description = project.description
                        from(project.components.get("java"))

                        //if (!skipJavadoc) {
                        // Eager task creation is required due to
                        // https://github.com/gradle/gradle/issues/6246
                        //  artifact(sourcesJar.get())
                        //  artifact(javadocJar.get())
                        //}

                        // Use the resolved versions in pom.xml
                        // Gradle might have different resolution rules, so we set the versions
                        // that were used in Gradle build/test.
                        versionMapping {
                            usage(Usage.JAVA_RUNTIME) {
                                fromResolutionResult()
                            }
                            usage(Usage.JAVA_API) {
                                fromResolutionOf("runtimeClasspath")
                            }
                        }
                        pom {
                            withXml {
                                val sb = asString()
                                var s = sb.toString()
                                // <scope>compile</scope> is Maven default, so delete it
                                s = s.replace("<scope>compile</scope>", "")
                                // Cut <dependencyManagement> because all dependencies have the resolved versions
                                s = s.replace(
                                    Regex(
                                        "<dependencyManagement>.*?</dependencyManagement>",
                                        RegexOption.DOT_MATCHES_ALL
                                    ),
                                    ""
                                )
                                sb.setLength(0)
                                sb.append(s)
                                // Re-format the XML
                                asNode()
                            }
                            name.set(
                                (project.findProperty("artifact.name") as? String)
                                    ?: "Autostyle ${project.name.capitalize()}"
                            )
                            description.set(
                                project.description
                                    ?: "Autostyle ${project.name.capitalize()}"
                            )
                            inceptionYear.set("2019")
                            url.set("https://github.com/autostyle/autostyle")
                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    comments.set("A business-friendly OSS license")
                                    distribution.set("repo")
                                }
                            }
                            issueManagement {
                                system.set("GitHub")
                                url.set("https://github.com/autostyle/autostyle/issues")
                            }
                            scm {
                                connection.set("scm:git:https://github.com/autostyle/autostyle.git")
                                developerConnection.set("scm:git:https://github.com/autostyle/autostyle.git")
                                url.set("https://github.com/autostyle/autostyle")
                                tag.set("HEAD")
                            }
                        }
                    }
                }
            }
        }
    }
}
