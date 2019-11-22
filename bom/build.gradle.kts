/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    `java-platform`
}

val String.v: String get() = rootProject.extra["$this.version"] as String

// Note: Gradle allows to declare dependency on "bom" as "api",
// and it makes the contraints to be transitively visible
// However Maven can't express that, so the approach is to use Gradle resolution
// and generate pom files with resolved versions
// See https://github.com/gradle/gradle/issues/9866

fun DependencyConstraintHandlerScope.apiv(
        notation: String,
        versionProp: String = notation.substringAfterLast(':')
) =
        "api"(notation + ":" + versionProp.v)

fun DependencyConstraintHandlerScope.runtimev(
        notation: String,
        versionProp: String = notation.substringAfterLast(':')
) =
        "runtime"(notation + ":" + versionProp.v)

dependencies {
    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    (constraints) {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        apiv("com.diffplug.durian:durian-collect", "durian")
        apiv("com.diffplug.durian:durian-core", "durian")
        apiv("com.diffplug.durian:durian-testlib", "durian")
        apiv("com.diffplug.durian:durian-io", "durian")
        apiv("org.exparity:hamcrest-date")
        apiv("org.hamcrest:hamcrest")
        apiv("org.hamcrest:hamcrest-core", "hamcrest")
        apiv("org.hamcrest:hamcrest-library", "hamcrest")
        apiv("org.eclipse.jgit:org.eclipse.jgit")
        apiv("com.googlecode.concurrent-trees:concurrent-trees")
        apiv("org.codehaus.groovy:groovy-xml")
        apiv("junit:junit", "junit4")
        apiv("org.assertj:assertj-core")
        apiv("org.junit.jupiter:junit-jupiter-api", "junit5")
        apiv("org.junit.jupiter:junit-jupiter-params", "junit5")
        apiv("org.slf4j:slf4j-api", "slf4j")
        apiv("org.slf4j:slf4j-log4j12", "slf4j")
        runtimev("org.junit.jupiter:junit-jupiter-engine", "junit5")
        runtimev("org.junit.vintage:junit-vintage-engine", "junit5")
    }
}
