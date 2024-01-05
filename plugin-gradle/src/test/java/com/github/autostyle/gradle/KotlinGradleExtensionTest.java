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

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class KotlinGradleExtensionTest extends GradleIntegrationTest {
  @Test
  public void integration() throws IOException {
    testInDirectory(null);
  }

  @Test
  public void integration_script_in_subdir() throws IOException {
    testInDirectory("companionScripts");
  }

  private void testInDirectory(final String directory) throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'org.jetbrains.kotlin.jvm' version '1.9.22'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlinGradle {",
        "        ktlint()",
        "    }",
        "}");
    String filePath = "configuration.gradle.kts";
    if (directory != null) {
      filePath = directory + "/" + filePath;
    }
    setFile(filePath).toResource("kotlin/ktlint/basic.dirty");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile(filePath).sameAsResource("kotlin/ktlint/basic.clean");
  }

  @Test
  public void integration_default() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'org.jetbrains.kotlin.jvm' version '1.9.22'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlinGradle {",
        "        ktlint()",
        "    }",
        "}");
    setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/basic.clean");
  }

  @Test
  public void indentStep() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'org.jetbrains.kotlin.jvm' version '1.9.22'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlinGradle {",
        "        ktlint {",
        "            userData(['indent_size': '6'])",
        "        }",
        "    }",
        "}");
    setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
    BuildResult result = gradleRunner().withArguments("autostyleApply").buildAndFail();
    assertThat(result.getOutput()).contains("Unexpected indentation (4) (it should be 6)");
  }

  @Test
  public void integration_lint_script_files_without_top_level_declaration() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'org.jetbrains.kotlin.jvm' version '1.9.22'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlinGradle {",
        "        ktlint()",
        "    }",
        "}");
    setFile("configuration.gradle.kts").toContent("buildscript {}");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("configuration.gradle.kts").hasContent("buildscript {}");
  }
}
