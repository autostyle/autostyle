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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GroovyExtensionTest extends GradleIntegrationTest {

  private static final String HEADER = "My tests header";

  @Test
  public void includeJava() throws IOException {
    testIncludeExcludeOption(false);
  }

  @Test
  @Disabled
  public void excludeJava() throws IOException {
    testIncludeExcludeOption(true);
  }

  private void testIncludeExcludeOption(boolean excludeJava) throws IOException {
    String excludeStatement = excludeJava ? "excludeJava()" : "";
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "apply plugin: 'groovy'",
        "",
        "autostyle {",
        "    groovy {",
        excludeStatement,
        "        licenseHeader('" + HEADER + "')",
        "    }",
        "}");

    String withoutHeader = getTestResource("groovy/licenseheader/JavaCodeWithoutHeader.test");

    setFile("src/main/java/test.java").toContent(withoutHeader);
    setFile("src/main/groovy/test.java").toContent(withoutHeader);
    setFile("src/main/groovy/test.groovy").toContent(withoutHeader);

    gradleRunner().withArguments("autostyleApply").build();

    assertFile("src/main/java/test.java").hasContent(withoutHeader);
    String header = "/*\n * " + HEADER + "\n */\n";
    assertFile("src/main/groovy/test.groovy").hasContent(header + withoutHeader);
    if (excludeJava) {
      assertFile("src/main/groovy/test.java").hasContent(withoutHeader);
    } else {
      assertFile("src/main/groovy/test.java").hasContent(header + withoutHeader);
    }
  }

  @Test
  public void groovyPluginMissingCheck() throws IOException {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "apply plugin: 'java'",
        "",
        "autostyle {",
        "    groovy {",
        "    }",
        "}");

    try {
      gradleRunner().withArguments("autostyleApply").build();
      Assertions.fail("Exception expected when using 'groovy' without 'target' if groovy-plugin is not applied.");
    } catch (Throwable t) {
      Assertions.assertThat(t).hasMessageContaining("must apply the groovy plugin before");
    }
  }

}
