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

import org.assertj.core.api.AbstractCharSequenceAssert
import org.assertj.core.api.Assertions
import org.assertj.core.util.CheckReturnValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.UnaryOperator

open class ResourceHarness {
    private var tempDir: File? = null
    @BeforeEach
    fun createFolder(@TempDir tempDir: File?) {
        this.tempDir = tempDir
    }

    companion object {
        /** Returns the contents of the given file from the src/test/resources directory.  */
        @JvmStatic
        fun getTestResource(filename: String): String {
            val url = ResourceHarness::class.java.getResource("/$filename")
                ?: throw IllegalArgumentException("No such resource $filename")
            return url.readText()
        }
    }

    /** Returns the root folder (canonicalized to fix OS X issue)  */
    protected open fun rootFolder(): File = tempDir!!.canonicalFile

    /** Returns a new child of the root folder.  */
    @Throws(IOException::class)
    protected fun newFile(subpath: String?): File {
        return File(rootFolder(), subpath)
    }

    /** Creates and returns a new child-folder of the root folder.  */
    @Throws(IOException::class)
    protected fun newFolder(subpath: String?): File {
        val targetDir = newFile(subpath)
        if (!targetDir.mkdir()) {
            throw IOException("Failed to create $targetDir")
        }
        return targetDir
    }

    @Throws(IOException::class)
    protected fun read(path: String?): String {
        return read(newFile(path).toPath(), StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    protected fun read(path: Path?, encoding: Charset?): String {
        return String(Files.readAllBytes(path), encoding!!)
    }

    @Throws(IOException::class)
    protected fun replace(
        path: String,
        toReplace: String,
        replaceWith: String
    ) {
        val before = read(path)
        val after: String = before.replace(toReplace, replaceWith)
        require(before != after) { "Replace was ineffective! '$toReplace' was not found in $path" }
        setFile(path).toContent(after)
    }

    /** Returns Files (in a temporary folder) which has the contents of the given file from the src/test/resources directory.  */
    @Throws(IOException::class)
    protected fun createTestFiles(vararg filenames: String): List<File> {
        val files: MutableList<File> =
            ArrayList(filenames.size)
        for (filename in filenames) {
            files.add(createTestFile(filename))
        }
        return files
    }
    /**
     * Returns a File (in a temporary folder) which has the contents, possibly processed, of the given file from the
     * src/test/resources directory.
     */
    /** Returns a File (in a temporary folder) which has the contents of the given file from the src/test/resources directory.  */
    @JvmOverloads
    @Throws(IOException::class)
    protected fun createTestFile(
        filename: String,
        fileContentsProcessor: UnaryOperator<String> = UnaryOperator.identity()
    ): File {
        val lastSlash = filename.lastIndexOf('/')
        val name = if (lastSlash >= 0) filename.substring(lastSlash) else filename
        val file = newFile(name)
        file.parentFile.mkdirs()
        Files.write(
            file.toPath(),
            fileContentsProcessor.apply(getTestResource(filename)).toByteArray(
                StandardCharsets.UTF_8
            )
        )
        return file
    }

    /** Reads the given resource from "before", applies the step, and makes sure the result is "after".  */
    @Throws(Throwable::class)
    protected fun assertOnResources(
        step: FormatterStep,
        unformattedPath: String,
        expectedPath: String
    ) {
        assertOnResources(FormatterFunc {
            step.format(it, File(""))
        }, unformattedPath, expectedPath)
    }

    /** Reads the given resource from "before", applies the step, and makes sure the result is "after".  */
    @Throws(Throwable::class)
    protected fun assertOnResources(
        step: FormatterFunc,
        unformattedPath: String,
        expectedPath: String
    ) {
        val unformatted = LineEnding.toUnix(
            getTestResource(unformattedPath)
        ) // unix-ified input
        val formatted = step.apply(unformatted)
        // no windows newlines
        Assertions.assertThat(formatted).doesNotContain("\r")
        // unix-ify the test resource output in case git screwed it up
        val expected = LineEnding.toUnix(
            getTestResource(expectedPath)
        ) // unix-ified output
        Assertions.assertThat(formatted).isEqualTo(expected)
    }

    @CheckReturnValue
    @Throws(IOException::class)
    protected fun assertFile(path: String?): ReadAsserter {
        return ReadAsserter(newFile(path))
    }

    @CheckReturnValue
    @Throws(IOException::class)
    protected fun assertFile(file: File): ReadAsserter {
        return ReadAsserter(file)
    }

    class ReadAsserter(private val file: File) {
        @JvmOverloads
        fun hasContent(
            expected: String?,
            charset: Charset? = StandardCharsets.UTF_8
        ) {
            Assertions.assertThat(file).usingCharset(charset)
                .hasContent(expected)
        }

        fun hasLines(vararg lines: String?) {
            hasContent(java.lang.String.join("\n", Arrays.asList<String>(*lines)))
        }

        @Throws(IOException::class)
        fun sameAsResource(resource: String) {
            hasContent(getTestResource(resource))
        }

        @Throws(IOException::class)
        fun matches(conditions: Consumer<AbstractCharSequenceAssert<*, String?>?>) {
            val content = String(
                Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8
            )
            conditions.accept(Assertions.assertThat(content))
        }
    }

    @Throws(IOException::class)
    protected fun setFile(path: String?): WriteAsserter {
        return WriteAsserter(newFile(path))
    }

    class WriteAsserter(private val file: File) {
        init {
            file.parentFile.mkdirs()
        }

        @Throws(IOException::class)
        fun toLines(vararg lines: String?): File {
            return toContent(lines.joinToString("\n"))
        }

        @JvmOverloads
        @Throws(IOException::class)
        fun toContent(
            content: String,
            charset: Charset = StandardCharsets.UTF_8
        ): File {
            Files.write(file.toPath(), content.toByteArray(charset))
            return file
        }

        @Throws(IOException::class)
        fun toResource(path: String): File {
            Files.write(
                file.toPath(),
                getTestResource(path).toByteArray(StandardCharsets.UTF_8)
            )
            return file
        }
    }
}
