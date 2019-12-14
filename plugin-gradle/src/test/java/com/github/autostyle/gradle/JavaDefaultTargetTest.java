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

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class JavaDefaultTargetTest extends GradleIntegrationTest {
  @Test
  public void integration() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "repositories { mavenCentral() }",
        "",
        "apply plugin: 'groovy'",
        "",
        "autostyle {",
        "    java {",
        "        googleJavaFormat('1.2')",
        "    }",
        "}");
    setFile("src/main/java/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
    setFile("src/main/groovy/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
    setFile("src/main/groovy/test.groovy").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

    gradleRunner().withArguments("autostyleApply").build();

    assertFile("src/main/java/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
    assertFile("src/main/groovy/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
    assertFile("src/main/groovy/test.groovy").sameAsResource("java/googlejavaformat/JavaCodeUnformatted.test");
  }
}
