/*
 * Copyright 2016 DiffPlug
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
package com.github.autostyle

import com.github.autostyle.serialization.deserialize
import com.github.autostyle.serialization.serialize
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.attributes.Bundling
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier

object TestProvisioner {
    @JvmStatic
    fun gradleProject(dir: File): Project {
        val userHome = File(System.getProperty("user.home"))
        return ProjectBuilder.builder()
            .withGradleUserHomeDir(File(userHome, ".gradle"))
            .withProjectDir(dir)
            .build()
    }

    /**
     * Creates a Provisioner for the given repositories.
     *
     * The first time a project is created, there are ~7 seconds of configuration
     * which will go away for all subsequent runs.
     *
     * Every call to resolve will take about 1 second, even when all artifacts are resolved.
     */
    private fun createWithRepositories(repoConfig: RepositoryHandler.() -> Unit): Provisioner { // Running this takes ~3 seconds the first time it is called. Probably because of classloading.
        return Provisioner { withTransitives: Boolean, mavenCoords: Collection<String?> ->
            val tempDir = Files.createTempDirectory("autostyle-test-deps").toFile()
            val project = gradleProject(tempDir)
            project.repositories.repoConfig()
            val deps: Array<Dependency> = mavenCoords
                .map { project.dependencies.create(it as String) }
                .toTypedArray()
            fun resolve() = project.configurations.detachedConfiguration(*deps).apply {
                isTransitive = withTransitives
                description = mavenCoords.toString()
                attributes {
                    it.attribute(
                        Bundling.BUNDLING_ATTRIBUTE,
                        project.objects.named(Bundling::class.java, Bundling.EXTERNAL)
                    )
                }
            }.resolve()
            try {
                for (i in 1..5) {
                    try {
                        return@Provisioner resolve()
                    } catch (e: ResolveException) {
                        Thread.sleep(100)
                        // https://github.com/gradle/gradle/issues/11752
                        // Retry a couple of times
                        continue
                    }
                }
                resolve()
            } catch (e: ResolveException) { /* Provide Maven coordinates in exception message instead of static string 'detachedConfiguration' */
                throw ResolveException(mavenCoords.toString(), e)
            } finally { // delete the temp dir
                Files.walk(tempDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map { obj: Path -> obj.toFile() }
                    .forEach { obj: File -> obj.delete() }
            }
        }
    }

    /** Creates a Provisioner which will cache the result of previous calls.  */
    private fun caching(
        name: String,
        input: Supplier<Provisioner>
    ): Provisioner {
        val autostyleDir = File(System.getProperty("user.dir")).parentFile
        val testlib = File(autostyleDir, "testlib")
        val cacheFile = File(testlib, "build/tmp/testprovisioner.$name.cache")
        val cached = if (cacheFile.exists()) {
            cacheFile.deserialize<MutableMap<Set<String>, Set<File>>>()
        } else {
            mutableMapOf()
        }
        return Provisioner { withTransitives: Boolean, mavenCoordsRaw: Collection<String> ->
            val mavenCoords = mavenCoordsRaw.toSet()
            synchronized(TestProvisioner) {
                var result = cached[mavenCoords]
                // double-check that depcache pruning hasn't removed them since our cache cached them
                val filesExist = result?.all { it.isFile && it.length() > 0 }
                if (filesExist != true) {
                    result = input.get().provisionWithTransitives(
                        withTransitives,
                        mavenCoords
                    )
                    cached[mavenCoords] = result
                    cacheFile.serialize(cached)
                }
                result!!
            }
        }
    }

    /** Creates a Provisioner for the jcenter repo.  */
    @JvmStatic
    @Deprecated(
        message = "use mavenCentral instead",
        replaceWith = ReplaceWith("mavenCentral()"),
        level = DeprecationLevel.WARNING
    )
    fun jcenter(): Provisioner = jcenter

    @Suppress("DEPRECATION")
    private val jcenter by lazy {
        caching("jcenter", Supplier {
            createWithRepositories { jcenter() }
        })
    }

    /** Creates a Provisioner for the mavenCentral repo.  */
    @JvmStatic
    fun mavenCentral(): Provisioner = mavenCentral

    private val mavenCentral by lazy {
        caching("mavenCentral", Supplier {
            createWithRepositories { mavenCentral() }
        })
    }

    /** Creates a Provisioner for the local maven repo for development purpose.  */
    fun mavenLocal(): Provisioner = mavenLocal.get()

    private val mavenLocal = Supplier {
        createWithRepositories { mavenLocal() }
    }

    /** Creates a Provisioner for the Sonatype snapshots maven repo for development purpose.  */
    fun snapshots(): Provisioner = snapshots.get()

    private val snapshots = Supplier {
        createWithRepositories {
            maven {
                it.setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            }
        }
    }
}
