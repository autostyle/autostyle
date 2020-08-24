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
package com.github.autostyle.npm;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.StepHarness;
import com.github.autostyle.TestProvisioner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TsFmtFormatterStepTest {

  @Nested
  @Tag("npm")
  public static class TsFmtUsingVariousFormattingFilesTest extends NpmFormatterStepCommonTests {
    public static Iterable<String> formattingConfigFiles() {
      return Arrays.asList("vscode/vscode.json", "tslint/tslint.json", "tsfmt/tsfmt.json", "tsconfig/tsconfig.json");
    }

    @ParameterizedTest
    @MethodSource("formattingConfigFiles")
    public void formattingUsingConfigFile(String formattingConfigFile) throws Throwable {
      String configFileName = formattingConfigFile.substring(formattingConfigFile.lastIndexOf('/') >= 0 ? formattingConfigFile.lastIndexOf('/') + 1 : 0);
      String configFileNameWithoutExtension = configFileName.substring(0, configFileName.lastIndexOf('.'));
      String filedir = "npm/tsfmt/" + configFileNameWithoutExtension + "/";

      final File configFile = createTestFile(filedir + configFileName);
      final String dirtyFile = filedir + configFileNameWithoutExtension + ".dirty";
      final String cleanFile = filedir + configFileNameWithoutExtension + ".clean";

      // some config options expect to see at least one file in the baseDir, so let's write one there
      Files.write(new File(configFile.getParentFile(), configFileNameWithoutExtension + ".ts").toPath(), getTestResource(dirtyFile).getBytes(StandardCharsets.UTF_8));

      final FormatterStep formatterStep = TsFmtFormatterStep.create(
          TsFmtFormatterStep.defaultDevDependencies(),
          TestProvisioner.mavenCentral(),
          buildDir(),
          npmExecutable(),
          TypedTsFmtConfigFile.named(configFileNameWithoutExtension, configFile),
          Collections.emptyMap());

      try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
        stepHarness.testResource(dirtyFile, cleanFile);
      }
    }
  }


  @Nested
  @Tag("npm")
  public static class TsFmtUsingInlineConfigTest extends NpmFormatterStepCommonTests {
    @Test
    public void formattingUsingInlineConfigWorks() throws Throwable {

      final Map<String, Object> inlineConfig = new LinkedHashMap<String, Object>() {{
        put("indentSize", 1);
        put("convertTabsToSpaces", true);
      }};

      final FormatterStep formatterStep = TsFmtFormatterStep.create(
          TsFmtFormatterStep.defaultDevDependencies(),
          TestProvisioner.mavenCentral(),
          buildDir(),
          npmExecutable(),
          null,
          inlineConfig);

      try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
        stepHarness.testResource("npm/tsfmt/tsfmt/tsfmt.dirty", "npm/tsfmt/tsfmt/tsfmt.clean");
      }
    }
  }
}
