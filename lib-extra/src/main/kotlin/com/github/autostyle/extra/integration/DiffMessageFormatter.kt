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

import com.diffplug.common.base.Splitter
import com.github.autostyle.Formatter
import com.github.autostyle.LineEnding
import com.github.autostyle.PaddedCell
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.HistogramDiff
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** Prints a human-readable diff that represents the missing changes.  */
class DiffMessageFormatter constructor(
    val formatter: Formatter,
    val sb: StringBuilder = StringBuilder(MAX_CHECK_MESSAGE_LINES * 64)
) {
    companion object {
        private const val MAX_CHECK_MESSAGE_LINES = 50
        const val MAX_FILES_TO_LIST = 10
        private const val MIN_LINES_PER_FILE = 4
        private val NEWLINE_SPLITTER = Splitter.on('\n')
        private const val NORMAL_INDENT = "  "
        private const val DIFF_INDENT = NORMAL_INDENT + NORMAL_INDENT
    }

    var maxCheckMessageLines: Int = MAX_CHECK_MESSAGE_LINES
    var maxFilesToList: Int = MAX_FILES_TO_LIST
    var minLinesPerFile: Int = MIN_LINES_PER_FILE

    private var numLines = 0

    private fun File.relativize(): String {
        return formatter.rootDir.relativize(toPath()).toString()
    }

    fun diff(files: List<File>, paddedCell: Boolean = false): StringBuilder {
        val problemIter = files.listIterator()
        while (problemIter.hasNext() && numLines < maxCheckMessageLines) {
            val file = problemIter.next()
            addFile(file.relativize() + "\n" + diff(file, paddedCell))
        }
        if (problemIter.hasNext()) {
            val remainingFiles = files.size - problemIter.nextIndex()
            if (remainingFiles >= maxFilesToList) {
                sb.append("Violations also present in ").append(remainingFiles)
                    .append(" other files.\n")
            } else {
                sb.append("Violations also present in:\n")
                while (problemIter.hasNext()) {
                    addIntendedLine(NORMAL_INDENT, problemIter.next().relativize())
                }
            }
            sb.append("You might want to adjust")
            sb.append(" -PmaxCheckMessageLines=").append(maxCheckMessageLines)
            sb.append(" -PmaxFilesToList=").append(maxFilesToList)
            sb.append(" -PminLinesPerFile=").append(minLinesPerFile)
            sb.append(" to see more violations\n")
        }
        return sb
    }

    private fun addFile(arg: String) { // at the very least, we'll print this about a file:
        //     0.txt
        //         @@ -1,2 +1,2 @@,
        //         -1\\r\\n,
        //         -2\\r\\n,
        //     ... (more lines that didn't fit)
        val lines = NEWLINE_SPLITTER.splitToList(arg)
        if (lines.isNotEmpty()) {
            addIntendedLine(NORMAL_INDENT, lines[0])
        }
        for (i in 1 until lines.size.coerceAtMost(minLinesPerFile)) {
            addIntendedLine(DIFF_INDENT, lines[i])
        }
        // then we'll print the rest that can fit
        val iter = lines.listIterator(lines.size.coerceAtMost(minLinesPerFile))
        // lines.size() - iter.nextIndex() == 1 means "just one line left", and we just print the line
        // instead of "1 more lines that didn't fit"
        while (iter.hasNext() &&
            (numLines < maxCheckMessageLines || lines.size - iter.nextIndex() == 1)
        ) {
            addIntendedLine(DIFF_INDENT, iter.next())
        }
        if (numLines >= maxCheckMessageLines) { // we're out of space
            if (iter.hasNext()) {
                val linesLeft = lines.size - iter.nextIndex()
                addIntendedLine(NORMAL_INDENT, "... ($linesLeft more lines that didn't fit)")
            }
        }
    }

    private fun addIntendedLine(indent: String, line: String) {
        sb.append(indent)
        sb.append(line)
        sb.append('\n')
        ++numLines
    }

    /**
     * Returns a git-style diff between the contents of the given file and what those contents would
     * look like if formatted using the given formatter. Does not end with any newline
     * sequence (\n, \r, \r\n).
     */
    private fun diff(file: File, paddedCell: Boolean): String {
        val encoding = formatter.encoding
        val raw = file.readBytes().toString(encoding)
        val rawUnix = LineEnding.toUnix(raw)
        val formattedUnix = if (paddedCell) {
            PaddedCell.check(formatter, file, rawUnix).canonical()
        } else {
            formatter.compute(rawUnix, file)
        }
        val formatted = formatter.computeLineEndings(formattedUnix, file)
        return visualizeDiff(file, raw, formatted)
    }

    private fun visualizeDiff(file: File, raw: String, formattedBytes: String): String {
        val a = RawText(raw.toByteArray(StandardCharsets.UTF_8))
        val b = RawText(formattedBytes.toByteArray(StandardCharsets.UTF_8))
        val edits = HistogramDiff().diff(RawTextComparator.DEFAULT, a, b)

        printGitHubActionsErrors(file, edits, a, b)

        val out = ByteArrayOutputStream()
        // defaultCharset is here so the formatter could select "fancy" or "simple"
        // characters for whitespace visualization based on the capabilities of the console
        // For instance, if the app is running with file.encoding=ISO-8859-1, then
        // the console can't encode fancy whitespace characters, and the formatter would
        // resort to simple "\\r", "\\n", and so on
        WriteSpaceAwareDiffFormatter(out, Charset.defaultCharset()).format(edits, a, b)
        return out.toByteArray().toString(StandardCharsets.UTF_8)
    }

    private fun printGitHubActionsErrors(file: File, edits: EditList, a: RawText, b: RawText) {
        val workspace = System.getenv("GITHUB_WORKSPACE") ?: return
        if (System.getenv("AUTOSTYLE_SKIP_GITHUB_ACTIONS")?.toBoolean() == true) {
            return
        }
        val relativePath = file.toRelativeString(File(workspace))

        for (edit in edits) {
            when (edit.type) {
                null, Edit.Type.EMPTY -> {
                }
                Edit.Type.DELETE ->
                    issueCommand(
                        "error", relativePath, edit.beginA + 1,
                        "Remove ${edit.lengthA} line${edit.lengthA.plural}: ${edit.beginA + 1}..${edit.endA}"
                    )
                Edit.Type.INSERT ->
                    issueCommand(
                        "error", relativePath, edit.beginA + 1,
                        "Insert ${edit.lengthB} line${edit.lengthB.plural}:\n" +
                                suggestion(b, edit)
                    )
                Edit.Type.REPLACE ->
                    issueCommand(
                        "error", relativePath, edit.beginA + 1,
                        "Replace ${edit.lengthB} line${edit.lengthB.plural} ${edit.beginA + 1}..${edit.endA} with\n" +
                                suggestion(b, edit)
                    )
            }
        }
    }

    private fun suggestion(
        b: RawText,
        edit: Edit,
        maxLines: Int = 20
    ): String? {
        if (edit.lengthB <= maxLines) {
            return b.getString(edit.beginB, edit.endB, true)
        }
        val linesLeft = edit.lengthB - maxLines
        return b.getString(edit.beginB, edit.beginB + maxLines, true) +
                "\n...$linesLeft more line${linesLeft.plural}"
    }

    private val Int.plural: String get() = if (this == 1) "" else "s"

    private fun issueCommand(
        @Suppress("SameParameterValue") command: String,
        file: String,
        line: Int,
        message: String
    ) {
        println("::$command file=${file.escapeProperty()},line=$line::${message.escapeData()}")
    }

    fun String.escapeData() =
        replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")

    fun String.escapeProperty() =
        replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")
            .replace(":", "%3A")
            .replace(",", "%2C")
}
