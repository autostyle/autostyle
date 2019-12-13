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

import com.diffplug.common.base.StringPrinter;

public class GroovyGradleExtensionTest extends GradleIntegrationTest {
  private static final String HEADER = "My tests header";
  private static final String FORMATTED_HEADER = "/*\n * My tests header\n */\n";

  @Test
  public void defaultTarget() throws IOException {
    testTarget(true);
  }

  @Test
  public void customTarget() throws IOException {
    testTarget(false);
  }

  private void testTarget(boolean useDefaultTarget) throws IOException {
    String target = useDefaultTarget ? "" : "target file('other.gradle')";
    String buildContent = StringPrinter.buildStringFromLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    groovyGradle {",
        target,
        "        licenseHeader('" + HEADER + "')",
        "    }",
        "}");
    setFile("build.gradle").toContent(buildContent);

    gradleRunner().withArguments("autostyleApply", "--info").build();

    if (useDefaultTarget) {
      assertFile("build.gradle").hasContent(FORMATTED_HEADER + buildContent);
    } else {
      assertFile("build.gradle").hasContent(buildContent);
    }
  }
}
