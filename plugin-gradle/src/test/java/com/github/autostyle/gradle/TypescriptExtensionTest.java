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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("npm")
public class TypescriptExtensionTest extends GradleIntegrationTest {
  @Test
  public void allowToSpecifyFormatterVersion() throws IOException {
    setFile("build.gradle").toLines(
        "buildscript { repositories { mavenCentral() } }",
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "def tsfmtconfig = [:]",
        "tsfmtconfig['indentSize'] = 1",
        "tsfmtconfig['convertTabsToSpaces'] = true",
        "autostyle {",
        "    typescript {",
        "        target 'test.ts'",
        "        tsfmt('7.2.1').config(tsfmtconfig)",
        "    }",
        "}");
    setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
    gradleRunner().withArguments("--stacktrace", "autostyleApply").build();
    assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
  }

  @Test
  public void allowToSpecifyMultipleVersionStrings() throws IOException {
    setFile("build.gradle").toLines(
        "buildscript { repositories { mavenCentral() } }",
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "def tsfmtconfig = [:]",
        "tsfmtconfig['indentSize'] = 1",
        "tsfmtconfig['convertTabsToSpaces'] = true",
        "autostyle {",
        "    typescript {",
        "        target 'test.ts'",
        "        tsfmt(['typescript-formatter': '7.2.1', 'tslint': '5.1.0', 'typescript': '2.9.2']).config(tsfmtconfig)",
        "    }",
        "}");
    setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
    gradleRunner().withArguments("--stacktrace", "autostyleApply").build();
    assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
  }

  @Test
  public void useTsfmtInlineConfig() throws IOException {
    setFile("build.gradle").toLines(
        "buildscript { repositories { mavenCentral() } }",
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "def tsfmtconfig = [:]",
        "tsfmtconfig['indentSize'] = 1",
        "tsfmtconfig['convertTabsToSpaces'] = true",
        "autostyle {",
        "    typescript {",
        "        target 'test.ts'",
        "        tsfmt().config(tsfmtconfig)",
        "    }",
        "}");
    setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
    gradleRunner().withArguments("--stacktrace", "autostyleApply").build();
    assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
  }

  @Test
  public void useTsfmtFileConfig() throws IOException {
    setFile("tsfmt.json").toLines(
        "{",
        "    \"indentSize\": 1,",
        "    \"convertTabsToSpaces\": true",
        "}");
    setFile("build.gradle").toLines(
        "buildscript { repositories { mavenCentral() } }",
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    typescript {",
        "        target 'test.ts'",
        "        tsfmt().tsfmtFile('tsfmt.json')",
        "    }",
        "}");
    setFile("test.ts").toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
    gradleRunner().withArguments("--stacktrace", "autostyleApply").build();
    assertFile("test.ts").sameAsResource("npm/tsfmt/tsfmt/tsfmt.clean");
  }

  @Test
  public void usePrettier() throws IOException {
    setFile("build.gradle").toLines(
        "buildscript { repositories { mavenCentral() } }",
        "plugins {",
        "    id 'com.github.autostyle'",
        "}",
        "autostyle {",
        "    typescript {",
        "        target 'test.ts'",
        "        prettier()",
        "    }",
        "}");
    setFile("test.ts").toResource("npm/prettier/filetypes/typescript/typescript.dirty");
    gradleRunner().withArguments("--stacktrace", "autostyleApply").build();
    assertFile("test.ts").sameAsResource("npm/prettier/filetypes/typescript/typescript.clean");
  }
}
