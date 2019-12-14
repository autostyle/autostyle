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

import com.diffplug.common.collect.ImmutableMap;
import com.github.autostyle.FormatterStep;
import com.github.autostyle.StepHarness;
import com.github.autostyle.TestProvisioner;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;

public class PrettierFormatterStepTest {
  @Nested
  @Tag("npm")
  public static class PrettierFormattingOfFileTypesIsWorking extends NpmFormatterStepCommonTests {
    public static Iterable<String> formattingConfigFiles() {
      return Arrays.asList("typescript", "json", "javascript-es5", "javascript-es6", "css", "scss", "markdown", "yaml");
    }

    @ParameterizedTest
    @MethodSource("formattingConfigFiles")
    public void formattingUsingConfigFile(String fileType) throws Throwable {
      String filedir = "npm/prettier/filetypes/" + fileType + "/";

      final File prettierRc = createTestFile(filedir + ".prettierrc.yml");
      final String dirtyFile = filedir + fileType + ".dirty";
      final String cleanFile = filedir + fileType + ".clean";

      final FormatterStep formatterStep = PrettierFormatterStep.create(
          PrettierFormatterStep.defaultDevDependencies(),
          TestProvisioner.mavenCentral(),
          buildDir(),
          npmExecutable(),
          new PrettierConfig(prettierRc, null));

      try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
        stepHarness.testResource(dirtyFile, cleanFile);
      }
    }
  }

  @Nested
  @Tag("npm")
  public static class SpecificPrettierFormatterStepTests extends NpmFormatterStepCommonTests {

    @Test
    public void parserInferenceIsWorking() throws Throwable {
      String filedir = "npm/prettier/filetypes/json/";

      final String dirtyFile = filedir + "json.dirty";
      final String cleanFile = filedir + "json.clean";

      final FormatterStep formatterStep = PrettierFormatterStep.create(
          PrettierFormatterStep.defaultDevDependencies(),
          TestProvisioner.mavenCentral(),
          buildDir(),
          npmExecutable(),
          new PrettierConfig(null, ImmutableMap.of("filepath", "anyname.json"))); // should select parser based on this name

      try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
        stepHarness.testResource(dirtyFile, cleanFile);
      }
    }
  }

  @Nested
  @Tag("npm")
  public static class PrettierFormattingOptionsAreWorking extends NpmFormatterStepCommonTests {

    private static final String FILEDIR = "npm/prettier/config/";

    public void runFormatTest(PrettierConfig config, String cleanFileNameSuffix) throws Throwable {

      final String dirtyFile = FILEDIR + "typescript.dirty";
      final String cleanFile = FILEDIR + "typescript." + cleanFileNameSuffix + ".clean";

      final FormatterStep formatterStep = PrettierFormatterStep.create(
          PrettierFormatterStep.defaultDevDependencies(),
          TestProvisioner.mavenCentral(),
          buildDir(),
          npmExecutable(),
          config); // should select parser based on this name

      try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
        stepHarness.testResource(dirtyFile, cleanFile);
      }
    }

    @Test
    public void defaultsAreApplied() throws Throwable {
      runFormatTest(new PrettierConfig(null, ImmutableMap.of("parser", "typescript")), "defaults");
    }

    @Test
    public void configFileOptionsAreApplied() throws Throwable {
      runFormatTest(new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), null), "configfile");
    }

    @Test
    public void configFileOptionsCanBeOverriden() throws Throwable {
      runFormatTest(new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), ImmutableMap.of("printWidth", 300)), "override");
    }
  }
}
