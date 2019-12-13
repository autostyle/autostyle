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

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

public class EncodingTest extends GradleIntegrationTest {
  @Test
  public void defaultIsUtf8() throws Exception {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    java {",
        "        target file('test.java')",
        "        custom 'replaceMicro', 1, { it.replace('µ', 'A') }",
        "    }",
        "}");
    setFile("test.java").toContent("µ");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("test.java").hasContent("A");
  }

  @Test
  public void globalIsRespected() throws Exception {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    java {",
        "        target file('test.java')",
        "        custom 'replaceMicro', 1, { it.replace('µ', 'A') }",
        "    }",
        "    encoding 'US-ASCII'",
        "}");
    setFile("test.java").toContent("µ");
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("test.java").hasContent("??");
  }

  @Test
  public void globalIsRespectedButCanBeOverridden() throws Exception {
    setFile("build.gradle").toLines(
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    java {",
        "        target file('test.java')",
        "        custom 'replaceMicro', 1, { it.replace('µ', 'A') }",
        "    }",
        "    format 'utf32', {",
        "        target file('utf32.encoded')",
        "        custom 'replaceMicro', 1, { it.replace('µ', 'A') }",
        "        encoding 'UTF-32'",
        "    }",
        "    encoding 'US-ASCII'",
        "}");
    setFile("test.java").toContent("µ");
    setFile("utf32.encoded").toContent("µ", Charset.forName("UTF-32"));
    gradleRunner().withArguments("autostyleApply").build();
    assertFile("test.java").hasContent("??");
    assertFile("utf32.encoded").hasContent("A", Charset.forName("UTF-32"));
  }
}
