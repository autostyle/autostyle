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
package com.github.autostyle.gradle;

import static com.github.autostyle.gradle.Tasks.execute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.LineEnding;
import com.github.autostyle.ResourceHarness;
import com.github.autostyle.TestProvisioner;
import com.github.autostyle.extra.integration.DiffMessageFormatter;

public class DiffMessageFormatterTest extends ResourceHarness {
  private AutostyleTask create(File... files) throws IOException {
    return create(Arrays.asList(files));
  }

  private AutostyleTask create(List<File> files) throws IOException {
    Project project = TestProvisioner.gradleProject(rootFolder());
    AutostyleTask task = project.getTasks().create("underTest", AutostyleCheckTask.class);
    task.getLineEndingsPolicy().set(LineEnding.UNIX.createPolicy());
    task.getSourceFiles().from(files);
    return task;
  }

  private void assertTaskFailure(AutostyleTask task, String... expectedLines) throws Exception {
    String msg = getTaskErrorMessage(task);

    String firstLine = "The following files have format violations:\n";
    String lastLine = "\nRun 'gradlew autostyleApply' to fix these violations.";
    Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);

    String middle = msg.substring(firstLine.length(), msg.length() - lastLine.length());
    String expectedMessage = StringPrinter.buildStringFromLines(expectedLines);
    Assertions.assertThat(middle).isEqualTo(expectedMessage.substring(0, expectedMessage.length() - 1));
  }

  protected String getTaskErrorMessage(AutostyleTask task) throws Exception {
    try {
      execute(task);
      throw new AssertionError("Expected a GradleException");
    } catch (GradleException e) {
      return e.getMessage();
    }
  }

  @Test
  public void lineEndingProblem() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent("A\r\nB\r\nC\r\n"));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,3 +1,3 @@",
      "        -A␍␊",
      "        -B␍␊",
      "        -C␍␊",
      "        +A␊",
      "        +B␊",
      "        +C␊");
  }

  @Test
  public void whitespaceProblem() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent("A \nB\t\nC  \n"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
      Pattern pattern = Pattern.compile("[ \t]+$", Pattern.UNIX_LINES | Pattern.MULTILINE);
      return pattern.matcher(input).replaceAll("");
    }));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,3 +1,3 @@",
      "        -A·␊",
      "        -B⇥␊",
      "        -C··␊",
      "        +A␊",
      "        +B␊",
      "        +C␊");
  }

  @Test
  public void whitespaceProblem2() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "u0\nu 1\nu 2\nu 3\n" +
        "\t leading space\n" +
        "trailing space\t \n" +
        "u 4\n" +
        "  leading and trailing space  \n" +
        "u 5\nu 6"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
      Pattern pattern = Pattern.compile("(^[ \t]+)|([ \t]+$)", Pattern.UNIX_LINES | Pattern.MULTILINE);
      return pattern.matcher(input).replaceAll("");
    }));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -2,9 +2,9 @@",
      "         u 1",
      "         u 2",
      "         u 3",
      "        -⇥·leading·space␊",
      "        -trailing·space⇥·␊",
      "        +leading·space␊",
      "        +trailing·space␊",
      "         u 4",
      "        -··leading·and·trailing·space··␊",
      "        +leading·and·trailing·space␊",
      "         u 5",
      "         u 6");
  }

  @Test
  public void whitespaceProblemDiffChaining() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "u0\ntrailing space\t \n" +
        "u1\nu2\nu3\nu4\nu5\nu6\nu7\n" +
        "trailing space2 \nu8"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
      Pattern pattern = Pattern.compile("(^[ \t]+)|([ \t]+$)", Pattern.UNIX_LINES | Pattern.MULTILINE);
      return pattern.matcher(input).replaceAll("");
    }));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,11 +1,11 @@",
      "         u0",
      "        -trailing·space⇥·␊",
      "        +trailing·space␊",
      "         u1",
      "         u2",
      "         u3",
      "         u4",
      "         u5",
      "         u6",
      "         u7",
      "        -trailing·space2·␊",
      "        +trailing·space2␊",
      "         u8");
  }

  @Test
  public void whitespaceProblemDiffChaining2() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "u0\ntrailing space\t \n" +
        "u1\nu2\nu3\nu4\nu5\nu6\nu7\nu8\n" +
        "trailing space2 \nu9"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
      Pattern pattern = Pattern.compile("(^[ \t]+)|([ \t]+$)", Pattern.UNIX_LINES | Pattern.MULTILINE);
      return pattern.matcher(input).replaceAll("");
    }));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,5 +1,5 @@",
      "         u0",
      "        -trailing·space⇥·␊",
      "        +trailing·space␊",
      "         u1",
      "         u2",
      "         u3",
      "        @@ -8,5 +8,5 @@",
      "         u6",
      "         u7",
      "         u8",
      "        -trailing·space2·␊",
      "        +trailing·space2␊",
      "         u9");
  }

  @Test
  public void singleLineCr() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "line without line ending"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing",
      input -> input.endsWith("\n") ? input : input + "\n"));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1 +1 @@",
      "        -line·without·line·ending",
      "        +line·without·line·ending␊");
  }

  @Test
  public void singleLineUnnecessaryCr() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "line without line ending\r\n"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing",
      input -> input.replaceAll("[\r\n]+$", "")));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1 +1 @@",
      "        -line·without·line·ending␍␊",
      "        +line·without·line·ending");
  }

  @Test
  public void trailingWhitespaceAndCRLF() throws Exception {
    AutostyleTask task = create(setFile("testFile").toContent(
      "line 1\ntrailing whitespace  \nline with CRLF\r\nline 2"));
    task.addStep(FormatterStep.createNeverUpToDate("trimTrailing",
      input -> input.replaceAll("[\r ]+\n", "\n")));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,4 +1,4 @@",
      "         line 1",
      "        -trailing·whitespace··␊",
      "        -line·with·CRLF␍␊",
      "        +trailing·whitespace␊",
      "        +line·with·CRLF␊",
      "         line 2");
  }

  @Test
  public void multipleFiles() throws Exception {
    AutostyleTask task = create(
      setFile("A").toContent("1\r\n2\r\n"),
      setFile("B").toContent("3\n4\r\n"));
    assertTaskFailure(task,
      "    A",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    B",
      "        @@ -1,2 +1,2 @@",
      "         3",
      "        -4␍␊",
      "        +4␊");
  }

  @Test
  public void manyFiles() throws Exception {
    List<File> testFiles = new ArrayList<>();
    for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST - 1; ++i) {
      testFiles.add(setFile(Integer.toString(i) + ".txt").toContent("1\r\n2\r\n"));
    }
    AutostyleTask task = create(testFiles);
    assertTaskFailure(task,
      "    0.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    1.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    10.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    11.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    12.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    13.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    14.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    15.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    16.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "    ... (2 more lines that didn't fit)",
      "Violations also present in:",
      "    17.txt",
      "    2.txt",
      "    3.txt",
      "    4.txt",
      "    5.txt",
      "    6.txt",
      "    7.txt",
      "    8.txt",
      "    9.txt");
  }

  @Test
  public void manyManyFiles() throws Exception {
    List<File> testFiles = new ArrayList<>();
    for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST; ++i) {
      testFiles.add(setFile(i + ".txt").toContent("1\r\n2\r\n"));
    }
    AutostyleTask task = create(testFiles);
    assertTaskFailure(task,
      "    0.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    1.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    10.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    11.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    12.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    13.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    14.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    15.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "        +1␊",
      "        +2␊",
      "    16.txt",
      "        @@ -1,2 +1,2 @@",
      "        -1␍␊",
      "        -2␍␊",
      "    ... (2 more lines that didn't fit)",
      "Violations also present in " + DiffMessageFormatter.MAX_FILES_TO_LIST + " other files.");
  }

  @Test
  public void longFile() throws Exception {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 1000; ++i) {
      builder.append(i);
      builder.append("\r\n");
    }
    AutostyleTask task = create(setFile("testFile").toContent(builder.toString()));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,1000 +1,1000 @@",
      "        -0␍␊",
      "        -1␍␊",
      "        -2␍␊",
      "        -3␍␊",
      "        -4␍␊",
      "        -5␍␊",
      "        -6␍␊",
      "        -7␍␊",
      "        -8␍␊",
      "        -9␍␊",
      "        -10␍␊",
      "        -11␍␊",
      "        -12␍␊",
      "        -13␍␊",
      "        -14␍␊",
      "        -15␍␊",
      "        -16␍␊",
      "        -17␍␊",
      "        -18␍␊",
      "        -19␍␊",
      "        -20␍␊",
      "        -21␍␊",
      "        -22␍␊",
      "        -23␍␊",
      "        -24␍␊",
      "        -25␍␊",
      "        -26␍␊",
      "        -27␍␊",
      "        -28␍␊",
      "        -29␍␊",
      "        -30␍␊",
      "        -31␍␊",
      "        -32␍␊",
      "        -33␍␊",
      "        -34␍␊",
      "        -35␍␊",
      "        -36␍␊",
      "        -37␍␊",
      "        -38␍␊",
      "        -39␍␊",
      "        -40␍␊",
      "        -41␍␊",
      "        -42␍␊",
      "        -43␍␊",
      "        -44␍␊",
      "        -45␍␊",
      "        -46␍␊",
      "        -47␍␊",
      "    ... (1952 more lines that didn't fit)");
  }

  @Test
  public void oneMoreLineThatDidntFit() throws Exception {
    // com.github.autostyle.extra.integration.DiffMessageFormatter.MAX_CHECK_MESSAGE_LINES
    // defaults to 50, so we create a diff that would be exactly 50 lines long
    // The test is to ensure diff does not contain "1 more lines that didn't fit"
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 25; ++i) {
      builder.append(i);
      builder.append(i < 1 ? "\n" : "\r\n");
    }
    AutostyleTask task = create(setFile("testFile").toContent(builder.toString()));
    assertTaskFailure(task,
      "    testFile",
      "        @@ -1,25 +1,25 @@",
      "         0",
      "        -1␍␊",
      "        -2␍␊",
      "        -3␍␊",
      "        -4␍␊",
      "        -5␍␊",
      "        -6␍␊",
      "        -7␍␊",
      "        -8␍␊",
      "        -9␍␊",
      "        -10␍␊",
      "        -11␍␊",
      "        -12␍␊",
      "        -13␍␊",
      "        -14␍␊",
      "        -15␍␊",
      "        -16␍␊",
      "        -17␍␊",
      "        -18␍␊",
      "        -19␍␊",
      "        -20␍␊",
      "        -21␍␊",
      "        -22␍␊",
      "        -23␍␊",
      "        -24␍␊",
      "        +1␊",
      "        +2␊",
      "        +3␊",
      "        +4␊",
      "        +5␊",
      "        +6␊",
      "        +7␊",
      "        +8␊",
      "        +9␊",
      "        +10␊",
      "        +11␊",
      "        +12␊",
      "        +13␊",
      "        +14␊",
      "        +15␊",
      "        +16␊",
      "        +17␊",
      "        +18␊",
      "        +19␊",
      "        +20␊",
      "        +21␊",
      "        +22␊",
      "        +23␊",
      "        +24␊");
  }
}
