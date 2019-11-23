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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import com.diffplug.common.base.CharMatcher;
import com.diffplug.common.base.Splitter;
import com.diffplug.common.base.StringPrinter;
import com.github.autostyle.LineEnding;

/** Tests the desired behavior from https://github.com/diffplug/spotless/issues/46. */
public class ErrorShouldRethrow extends GradleIntegrationTest {
  private void writeBuild(String... toInsert) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("plugins {");
    lines.add("    id 'com.github.autostyle.gradle'");
    lines.add("    id 'java'");
    lines.add("}");
    lines.add("autostyle {");
    lines.add("    format 'misc', {");
    lines.add("        lineEndings 'UNIX'");
    lines.add("        target file('README.md')");
    lines.add("        custom 'no swearing', {");
    lines.add("             if (it.toLowerCase(Locale.ROOT).contains('fubar')) {");
    lines.add("                 throw new RuntimeException('No swearing!');");
    lines.add("             }");
    lines.add("        }");
    lines.addAll(Arrays.asList(toInsert));
    setFile("build.gradle").toContent(String.join("\n", lines));
  }

  @Test
  public void passesIfNoException() throws Exception {
    writeBuild(
        "    } // format",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fun.");
    runWithSuccess(":autostyleMisc");
  }

  @Test
  public void anyExceptionShouldFail() throws Exception {
    writeBuild(
        "    } // format",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fubar.");
    runWithFailure(
        ":autostyleMiscStep 'no swearing' found problem in 'README.md':",
        "No swearing!",
        "java.lang.RuntimeException: No swearing!");
  }

  @Test
  public void unlessEnforceCheckIsFalse() throws Exception {
    writeBuild(
        "    } // format",
        "    enforceCheck false",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fubar.");
    runWithSuccess(":compileJava UP-TO-DATE");
  }

  @Test
  public void unlessExemptedByStep() throws Exception {
    writeBuild(
        "        ignoreErrorForStep 'no swearing'",
        "    } // format",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fubar.");
    runWithSuccess(":autostyleMisc",
        "Unable to apply step 'no swearing' to 'README.md'");
  }

  @Test
  public void unlessExemptedByPath() throws Exception {
    writeBuild(
        "        ignoreErrorForPath 'README.md'",
        "    } // format",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fubar.");
    runWithSuccess(":autostyleMisc",
        "Unable to apply step 'no swearing' to 'README.md'");
  }

  @Test
  public void failsIfNeitherStepNorFileExempted() throws Exception {
    writeBuild(
        "        ignoreErrorForStep 'nope'",
        "        ignoreErrorForPath 'nope'",
        "    } // format",
        "}     // autostyle");
    setFile("README.md").toContent("This code is fubar.");
    runWithFailure(
        ":autostyleMiscStep 'no swearing' found problem in 'README.md':",
        "No swearing!",
        "java.lang.RuntimeException: No swearing!");
  }

  private void runWithSuccess(String... messages) throws Exception {
    BuildResult result = gradleRunner().withArguments("check").build();
    assertResultAndMessages(result, TaskOutcome.SUCCESS, messages);
  }

  private void runWithFailure(String... messages) throws Exception {
    BuildResult result = gradleRunner().withArguments("check").buildAndFail();
    assertResultAndMessages(result, TaskOutcome.FAILED, messages);
  }

  private void assertResultAndMessages(BuildResult result, TaskOutcome outcome, String... messages) {
    String expectedToStartWith = StringPrinter.buildStringFromLines(messages).trim();
    int numNewlines = CharMatcher.is('\n').countIn(expectedToStartWith);
    List<String> actualLines = Splitter.on('\n').splitToList(LineEnding.toUnix(result.getOutput()));
    String actualStart = String.join("\n", actualLines.subList(0, numNewlines + 1));
    Assertions.assertThat(actualStart).isEqualTo(expectedToStartWith);
    Assertions.assertThat(result.tasks(outcome).size() + result.tasks(TaskOutcome.UP_TO_DATE).size())
        .isEqualTo(result.getTasks().size());
  }
}
