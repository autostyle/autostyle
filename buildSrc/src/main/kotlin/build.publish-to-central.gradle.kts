/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.util.*

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("build.reproducible-timestamps")
    id("com.gradleup.nmcp")
}

val release = providers.gradleProperty("release").getOrElse("false").toBoolean()
val useInMemoryPgpKeys = providers.gradleProperty("useInMemoryPgpKeys").getOrElse("true").toBoolean()

if (!release) {
    publishing {
        repositories {
            maven {
                name = "centralSnapshots"
                url = uri("https://central.sonatype.com/repository/maven-snapshots")
                credentials(PasswordCredentials::class)
            }
        }
    }
} else {
    signing {
        sign(publishing.publications)
        if (!useInMemoryPgpKeys) {
            useGpgCmd()
        } else {
            val pgpPrivateKey = System.getenv("SIGNING_PGP_PRIVATE_KEY")
            val pgpPassphrase = System.getenv("SIGNING_PGP_PASSPHRASE")
            if (pgpPrivateKey.isNullOrBlank() || pgpPassphrase.isNullOrBlank()) {
                throw IllegalArgumentException("GPP private key (SIGNING_PGP_PRIVATE_KEY) and passphrase (SIGNING_PGP_PASSPHRASE) must be set")
            }
            useInMemoryPgpKeys(
                pgpPrivateKey,
                pgpPassphrase
            )
        }
    }
}

publishing {
    publications {
        if (project.path != ":autostyle-plugin-gradle") {
            create<MavenPublication>(project.name) {
                artifactId = project.name
                version = rootProject.version.toString()
                description = project.description
                from(project.components.get("java"))
            }
        }
        withType<MavenPublication> {
            // if (!skipJavadoc) {
            // Eager task creation is required due to
            // https://github.com/gradle/gradle/issues/6246
            //  artifact(sourcesJar.get())
            //  artifact(javadocJar.get())
            // }

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
                        ?: "Autostyle ${project.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                )
                description.set(
                    project.description
                        ?: "Autostyle ${project.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                )
                developers {
                    developer {
                        id.set("vlsi")
                        name.set("Vladimir Sitnikov")
                        email.set("sitnikov.vladimir@gmail.com")
                    }
                }
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
