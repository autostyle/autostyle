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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;

public class GradleIncrementalResolutionTest extends GradleIntegrationTest {
  @Test
  public void failureDoesntTriggerAll() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    format 'misc', {",
        "        patterns.include '*.md'",
        "        custom 'lowercase', 1, { str ->",
        "            String result = str.toLowerCase(Locale.ROOT)",
        "            println(\"<${result.trim()}>\")",
        "            return result",
        "        }",
        "    }",
        "}");
    // test our harness (build makes things lower case)
    writeState("ABC");
    assertState("ABC");
    writeState("aBc");
    assertState("aBc");
    // check will run against all three the first time (and second and third)
    checkRanAgainst("abc");
    checkRanAgainst("abc");
    checkRanAgainst("abc");
    // apply will run against all three the first time
    applyRanAgainst("abc");
    // the second time, it will only run on the file that was changes
    applyRanAgainst("b");
    // and nobody the last time
    applyRanAgainst("");
    // TODO: check does not co-operate with apply, so it is executed against all the files
    //   So it is executed, and caches that the files are OK
    checkRanAgainst("abc");

    // if we change just one file
    writeState("Abc");
    // Only A is changed, so check is executed against a only
    checkRanAgainst("a");
    // even after failing, still just the one
    checkRanAgainst("a");
    // and so does apply
    applyRanAgainst("a", "b");
    applyRanAgainst("a");
    // until the issue has been fixed
    applyRanAgainst("");
  }

  private String filename(String name) {
    return name.toLowerCase(Locale.ROOT) + ".md";
  }

  private void writeState(String state) throws IOException {
    for (char c : state.toCharArray()) {
      String letter = new String(new char[]{c});
      boolean exists = new File(rootFolder(), filename(letter)).exists();
      boolean needsChanging = exists && !read(filename(letter)).trim().equals(letter);
      if (!exists || needsChanging) {
        setFile(filename(letter)).toContent(letter);
      }
    }
  }

  private void assertState(String state) throws IOException {
    for (char c : state.toCharArray()) {
      String letter = new String(new char[]{c});
      if (Character.isLowerCase(c)) {
        Assertions.assertEquals(letter.toLowerCase(Locale.ROOT), read(filename(letter)).trim());
      } else {
        Assertions.assertEquals(letter.toUpperCase(Locale.ROOT), read(filename(letter)).trim());
      }
    }
  }

  private void applyRanAgainst(String... ranAgainst) throws IOException {
    taskRanAgainst("autostyleApply", ranAgainst);
  }

  private void checkRanAgainst(String... ranAgainst) throws IOException {
    taskRanAgainst("autostyleCheck", ranAgainst);
  }

  private void taskRanAgainst(String task, String... ranAgainst) throws IOException {
    pauseForFilesystem();
    String console = StringPrinter.buildString(Errors.rethrow().wrap(printer -> {
      boolean expectFailure = task.equals("autostyleCheck") && !isClean();
      if (expectFailure) {
        gradleRunner().withArguments(task).forwardStdOutput(printer.toWriter()).buildAndFail();
      } else {
        gradleRunner().withArguments(task).forwardStdOutput(printer.toWriter()).build();
      }
    }));
    SortedSet<String> added = new TreeSet<>();
    for (String line : console.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
        added.add(trimmed.substring(1, trimmed.length() - 1));
      }
    }
    Assertions.assertEquals(concat(Arrays.asList(ranAgainst)), concat(added));
  }

  private String concat(Iterable<String> iterable) {
    StringBuilder result = new StringBuilder();
    for (String item : iterable) {
      result.append(item);
    }
    return result.toString();
  }

  private boolean isClean() throws IOException {
    for (File file : rootFolder().listFiles()) {
      if (file.isFile() && file.getName().length() == 4 && file.getName().endsWith(".md")) {
        String content = read(file.getName());
        if (!content.toLowerCase(Locale.ROOT).equals(content)) {
          return false;
        }
      }
    }
    return true;
  }
}
