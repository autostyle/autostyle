/*
 * Copyright 2026 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
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

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TargetGlobTest extends GradleIntegrationTest {
  /**
   * A String target such as {@code target("**&#47;*.md")} must be treated as an Ant-style
   * include glob relative to the project directory, matching files in the root and in nested
   * directories. Before the fix, the String was passed to {@code Project.files()}, which does
   * not expand globs, so the target silently matched nothing and no file was formatted.
   *
   * <p>The assertions read the whole file (including the trailing newline) rather than using
   * {@code hasContent}, which compares line by line and would ignore a missing final newline.
   */
  @Test
  public void stringGlobMatchesFilesRecursively() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    format('misc') {",
        "        target('**/*.md')",
        "        endWithNewline()",
        "    }",
        "}");
    // Files without a trailing newline, in the root and in nested directories.
    setFile("README.md").toContent("root");
    setFile("docs/a.md").toContent("a");
    setFile("docs/nested/b.md").toContent("b");
    // A file that does not match the glob must be left untouched (no trailing newline added).
    setFile("docs/keep.txt").toContent("keep");

    gradleRunner().withArguments("autostyleApply").build();

    assertFile("README.md").matches(it -> it.isEqualTo("root\n"));
    assertFile("docs/a.md").matches(it -> it.isEqualTo("a\n"));
    assertFile("docs/nested/b.md").matches(it -> it.isEqualTo("b\n"));
    assertFile("docs/keep.txt").matches(it -> it.isEqualTo("keep"));
  }
}
