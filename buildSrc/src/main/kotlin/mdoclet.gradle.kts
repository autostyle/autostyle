/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
val mdoclet by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    mdoclet("org.jdrupes.mdoclet:doclet:3.1.0")
}

val mdocletJarFile = "mdocletClasspath.jar"

val generateMDocletPath by tasks.registering(Jar::class) {
    description = "Creates classpath-only jar for running org.jdrupes.mdoclet"
    inputs.files(mdoclet).withNormalizer(ClasspathNormalizer::class.java).withPropertyName("mdocletClasspath")
    archiveFileName.set(mdocletJarFile)
    manifest {
        manifest {
            attributes(
                "Main-Class" to "sqlline.SqlLine",
                "Class-Path" to provider { mdoclet.joinToString(" ") { it.absolutePath } }
            )
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    dependsOn(generateMDocletPath)
    options.apply {
        doclet = "org.jdrupes.mdoclet.MDoclet"
        docletpath = listOf(layout.buildDirectory.file("libs/$mdocletJarFile").get().asFile)
        this as CoreJavadocOptions
        addStringOption("Xdoclint:-html", "-quiet")
        jFlags(
            "--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        )
    }
}
