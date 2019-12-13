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

import org.junit.Test;

public class BumpThisNumberIfACustomStepChangesTest extends GradleIntegrationTest {

  private void writeBuildFile(String toInsert) throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    format 'misc', {",
        "        target file('README.md')",
        "        custom('lowercase'," + toInsert + ") { str -> str.toLowerCase(Locale.ROOT) }",
        "    }",
        "}");
  }

  private void writeContentWithBadFormatting() throws IOException {
    setFile("README.md").toContent("ABC");
  }

  @Override
  protected void applyIsUpToDate(boolean upToDate) throws IOException {
    super.applyIsUpToDate(upToDate);
    assertFile("README.md").hasContent("abc");
  }

  @Test
  public void customRuleNeverUpToDate() throws IOException {
    writeBuildFile("0");
    writeContentWithBadFormatting();
    applyIsUpToDate(false);
    checkIsUpToDate(false);
    checkIsUpToDate(true);
  }

  @Test
  public void unlessBumpThisNumberIfACustomStepChanges() throws IOException {
    writeBuildFile("1");
    writeContentWithBadFormatting();
    applyIsUpToDate(false);
    applyIsUpToDate(false);
    applyIsUpToDate(true);
    checkIsUpToDate(false);
    checkIsUpToDate(true);

    writeContentWithBadFormatting();
    applyIsUpToDate(false);
    // The input has been just formatted, so check is up to date
    checkIsUpToDate(true);
  }

  @Test
  public void andRunsAgainIfNumberChanges() throws IOException {
    writeBuildFile("1");
    writeContentWithBadFormatting();
    applyIsUpToDate(false);
    checkIsUpToDate(false);
    checkIsUpToDate(true);

    writeBuildFile("2");
    checkIsUpToDate(false);
    checkIsUpToDate(true);
  }
}
