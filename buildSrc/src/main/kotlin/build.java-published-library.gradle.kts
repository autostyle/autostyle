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

plugins {
    id("java-library")
    id("build.publish-to-central")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            // Gradle feature variants can't be mapped to Maven's pom
            suppressAllPomMetadataWarnings()
        }
        // artifacts.removeIf below resolves the configuration, so it causes the following warning:
        // Mutating a configuration after it has been resolved, consumed as a variant, or used for generating published metadata
        withType<MavenPublication>().configureEach {
            // Use the resolved versions in pom.xml
            // Gradle might have different resolution rules, so we set the versions
            // that were used in Gradle build/test.
            versionMapping {
                usage(Usage.JAVA_API) {
                    fromResolutionOf("runtimeClasspath")
                }
                usage(Usage.JAVA_RUNTIME) {
                    fromResolutionResult()
                }
            }
        }
    }
}
