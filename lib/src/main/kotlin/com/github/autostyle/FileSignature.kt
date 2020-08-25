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
package com.github.autostyle

import java.io.File
import java.io.IOException
import java.io.Serializable
import java.security.MessageDigest
import java.util.*

/** Computes a signature for any needed files.  */
class FileSignature private constructor(
    keyPaths: Map<File, String>,
    /*
     * Transient because not needed to uniquely identify a FileSignature instance, and also because
     * Gradle only needs this class to be Serializable so it can compare FileSignature instances for
     * incremental builds.
     */
    @field:Transient
    private val files: List<File>,
    ordered: Boolean
) : Serializable {
    private val digest: ByteArray

    /** Returns all of the files in this signature */
    fun files(): Collection<File> = Collections.unmodifiableList(files)

    /** Returns the only file in this signature, throwing an exception if there are more or less than 1 file.  */
    val onlyFile: File get() = files.single()

    init {
        val md = MessageDigest.getInstance("SHA-1")
        val entries = keyPaths.asSequence()
            .map { it.key.canonicalPath to it.value }
            .sortedByDescending { it.first.length }
            .toList()
        val contents = files.asSequence()
            .map {
                val path = it.canonicalPath
                val relativePath = entries
                    .firstOrNull { root -> path.startsWith(root.first) }
                    ?.let { root ->
                        root.second + ":" + path.substring(root.first.length)
                    } ?: path
                relativePath to it
            }
            .run {
                if (ordered) {
                    sortedBy { it.first }
                } else {
                    this
                }
            }
        for ((relativePath, file) in contents) {
            md.update(relativePath.toByteArray())
            // 0-byte is unlikely to appear in a file path, so it should be safe for use as a delimiter
            md.update(0)
            file.forEachBlock { buffer, bytesRead -> md.update(buffer, 0, bytesRead) }
        }
        digest = md.digest()
    }

    companion object {
        private const val serialVersionUID = 1L
        private val DEFAULT_KEYS =
            mapOf(File(System.getProperty("user.home")) to "\$HOME")

        @JvmField
        val EMPTY = signAsList(DEFAULT_KEYS, listOf())

        /**
         * Creates file signature whereas order of the files remains unchanged.
         */
        @JvmStatic
        @Throws(IOException::class)
        @Deprecated("use keyPaths for PathSensitive.Relative")
        fun signAsList(vararg files: File) =
            signAsList(DEFAULT_KEYS, listOf(*files))

        /**
         * Creates file signature whereas order of the files remains unchanged.
         */
        @JvmStatic
        @Throws(IOException::class)
        @Deprecated("use keyPaths for PathSensitive.Relative")
        fun signAsList(files: Iterable<File>) =
            signAsList(DEFAULT_KEYS, files.toList())

        /** Creates file signature whereas order of the files remains unchanged.  */
        @JvmStatic
        @Throws(IOException::class)
        @Deprecated("use keyPaths for PathSensitive.Relative")
        fun signAsSet(vararg files: File) =
            signAsSet(DEFAULT_KEYS, listOf(*files))

        /**
         * Creates file signature insensitive to the order of the files.
         */
        @JvmStatic
        @Throws(IOException::class)
        @Deprecated("use keyPaths for PathSensitive.Relative")
        fun signAsSet(files: Iterable<File>) =
            signAsSet(DEFAULT_KEYS, files.toList())

        /**
         * Creates file signature whereas order of the files remains unchanged.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun signAsList(keyPaths: Map<File, String>, files: Iterable<File>) =
            FileSignature(keyPaths, files.toList(), true)

        /**
         * Creates file signature insensitive to the order of the files.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun signAsSet(keyPaths: Map<File, String>, files: Iterable<File>) =
            FileSignature(keyPaths, files.toSortedSet().toList(), false)
    }
}
