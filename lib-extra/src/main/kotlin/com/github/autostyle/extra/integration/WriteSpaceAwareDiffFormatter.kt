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
package com.github.autostyle.extra.integration

import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.util.IntList
import org.eclipse.jgit.util.RawParseUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets

/**
 * Formats the diff in Git-like style, however it makes whitespace visible for
 * edit-like diffs (when one fragment is replaced with another).
 */
internal class WriteSpaceAwareDiffFormatter(
    private val out: ByteArrayOutputStream,
    charset: Charset
) {
    private val middleDot: ByteArray
    private val cr: ByteArray
    private val lf: ByteArray
    private val tab: ByteArray

    companion object {
        private const val CONTEXT_LINES = 3
        private const val MIDDLE_DOT = "\u00b7"
        private const val CR = "\u240d"
        private const val LF = "\u240a"
        private const val TAB = "\u21e5"
        private val MIDDLE_DOT_UTF8 = MIDDLE_DOT.toByteArray(StandardCharsets.UTF_8)
        private val CR_UTF8 = CR.toByteArray(StandardCharsets.UTF_8)
        private val LF_UTF8 = LF.toByteArray(StandardCharsets.UTF_8)
        private val TAB_UTF8 = TAB.toByteArray(StandardCharsets.UTF_8)
        private val SPACE_SIMPLE = byteArrayOf(' '.toByte())
        private val CR_SIMPLE = byteArrayOf('\\'.toByte(), 'r'.toByte())
        private val LF_SIMPLE = byteArrayOf('\\'.toByte(), 'n'.toByte())
        private val TAB_SIMPLE = byteArrayOf('\\'.toByte(), 't'.toByte())
        private val isWindows = "win" in System.getProperty("os.name").toLowerCase()
        private fun replacementFor(
            charsetEncoder: CharsetEncoder,
            value: String,
            fancy: ByteArray,
            simple: ByteArray
        ): ByteArray {
            return if (!isWindows && charsetEncoder.canEncode(value)) fancy else simple
        }
    }

    init {
        val charsetEncoder = charset.newEncoder()
        middleDot = replacementFor(charsetEncoder, MIDDLE_DOT, MIDDLE_DOT_UTF8, SPACE_SIMPLE)
        cr = replacementFor(charsetEncoder, CR, CR_UTF8, CR_SIMPLE)
        lf = replacementFor(charsetEncoder, LF, LF_UTF8, LF_SIMPLE)
        tab = replacementFor(charsetEncoder, TAB, TAB_UTF8, TAB_SIMPLE)
    }

    /**
     * Formats the diff.
     * @param edits the list of edits to format
     * @param a input text a, with \n line endings, with UTF-8 encoding
     * @param b input text b, with \n line endings, with UTF-8 encoding
     * @throws IOException if formatting fails
     */
    @Throws(IOException::class)
    fun format(edits: EditList, a: RawText, b: RawText) {
        val linesA = RawParseUtils.lineMap(a.rawContent, 0, a.rawContent.size)
        val linesB = RawParseUtils.lineMap(b.rawContent, 0, b.rawContent.size)
        var firstLine = true
        var i = 0
        while (i < edits.size) {
            var edit = edits[i]
            var lineA = (edit.beginA - CONTEXT_LINES).coerceAtLeast(0)
            var lineB = (edit.beginB - CONTEXT_LINES).coerceAtLeast(0)
            val endIdx = findCombinedEnd(edits, i)
            val endEdit = edits[endIdx]
            val endA = (endEdit.endA + CONTEXT_LINES).coerceAtMost(a.size())
            val endB = (endEdit.endB + CONTEXT_LINES).coerceAtMost(b.size())
            if (firstLine) {
                firstLine = false
            } else {
                out.write('\n'.toInt())
            }
            header(lineA, endA, lineB, endB)
            var showWhitespace = edit.type == Edit.Type.REPLACE
            while (lineA < endA || lineB < endB) {
                when {
                    lineA < edit.beginA -> { // Common part before the diff
                        line(' ', a, lineA, linesA, false)
                        lineA++
                        lineB++
                    }
                    lineA < edit.endA -> {
                        line('-', a, lineA, linesA, showWhitespace)
                        lineA++
                    }
                    lineB < edit.endB -> {
                        line('+', b, lineB, linesB, showWhitespace)
                        lineB++
                    }
                    else -> { // Common part after the diff
                        line(' ', a, lineA, linesA, false)
                        lineA++
                        lineB++
                    }
                }
                if (lineA == edit.endA && lineB == edit.endB && i < endIdx) {
                    i++
                    edit = edits[i]
                    showWhitespace = edit.type == Edit.Type.REPLACE
                }
            }
            i++
        }
    }

    /**
     * There might be multiple adjacent diffs, so we need to figure out the latest one in the group.
     * @param edits list of edits
     * @param startingEdit starting edit
     * @return the index of the latest edit in the group
     */
    private fun findCombinedEnd(edits: EditList, startingEdit: Int): Int {
        var i = startingEdit
        while (i < edits.size - 1) {
            val current = edits[i]
            val next = edits[i + 1]
            if (next.beginA - current.endA > 2 * CONTEXT_LINES + 1 &&
                next.beginB - current.endB > 2 * CONTEXT_LINES + 1
            ) {
                break
            }
            i++
        }
        return i
    }

    private fun header(lineA: Int, endA: Int, lineB: Int, endB: Int) {
        out.write('@'.toInt())
        out.write('@'.toInt())
        range('-', lineA + 1, endA - lineA)
        range('+', lineB + 1, endB - lineB)
        out.write(' '.toInt())
        out.write('@'.toInt())
        out.write('@'.toInt())
    }

    private fun range(prefix: Char, begin: Int, length: Int) {
        out.write(' '.toInt())
        out.write(prefix.toInt())
        if (length == 0) {
            writeInt(begin - 1)
            out.write(','.toInt())
            out.write('0'.toInt())
        } else {
            writeInt(begin)
            if (length > 1) {
                out.write(','.toInt())
                writeInt(length)
            }
        }
    }

    private fun writeInt(num: Int) {
        val str = num.toString()
        var i = 0
        val len = str.length
        while (i < len) {
            out.write(str[i].toInt())
            i++
        }
    }

    private fun line(
        prefix: Char,
        a: RawText,
        lineA: Int,
        lines: IntList,
        showWhitespace: Boolean
    ) {
        out.write('\n'.toInt())
        out.write(prefix.toInt())
        if (!showWhitespace) {
            a.writeLine(out, lineA)
            return
        }
        val bytes = a.rawContent
        var i = lines[lineA + 1]
        val end = lines[lineA + 2]
        while (i < end) {
            when (val b = bytes[i]) {
                ' '.toByte() -> {
                    out.write(middleDot)
                }
                '\t'.toByte() -> {
                    out.write(tab)
                }
                '\r'.toByte() -> {
                    out.write(cr)
                }
                '\n'.toByte() -> {
                    out.write(lf)
                }
                else -> {
                    out.write(b.toInt())
                }
            }
            i++
        }
    }
}
