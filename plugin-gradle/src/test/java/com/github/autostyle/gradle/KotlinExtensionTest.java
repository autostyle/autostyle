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

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

public class KotlinExtensionTest extends GradleIntegrationTest {
  private static final String HEADER = "License Header";
  private static final String HEADER_WITH_YEAR = "License Header $YEAR";
  private static final String FORMATTED_HEADER = "/*\n * License Header\n */\n";

  @Test
  public void integration() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'nebula.kotlin' version '1.0.6'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlin {",
        "        ktlint()",
        "    }",
        "}");
    setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktlint/basic.clean");
  }

  @Test
  public void testWithIndentation() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'nebula.kotlin' version '1.0.6'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { jcenter() }",
        "autostyle {",
        "    kotlin {",
        "        ktlint('0.21.0') {",
        "            userData(['indent_size': '6'])",
        "        }",
        "    }",
        "}");
    setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
    BuildResult result = gradleRunner().withArguments("autostyleApply").buildAndFail();
    assertThat(result.getOutput()).contains("Unexpected indentation (4) (it should be 6)");
  }

  @Test
  public void testWithHeader() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'nebula.kotlin' version '1.0.6'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlin {",
        "        licenseHeader('" + HEADER + "')",
        "        ktlint()",
        "    }",
        "}");
    setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("src/main/kotlin/test.kt").hasContent(FORMATTED_HEADER + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
  }

  @Test
  public void testWithCustomHeaderSeparator() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'nebula.kotlin' version '1.0.6'",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "autostyle {",
        "    kotlin {",
        "        licenseHeader ('" + HEADER + "')",
        "        ktlint()",
        "    }",
        "}");
    setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
    gradleRunner().withArguments("autostyleApply", "--info").build();
    assertFile("src/main/kotlin/test.kt").hasContent(FORMATTED_HEADER + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
  }
}
