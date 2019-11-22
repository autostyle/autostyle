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
package com.github.autostyle.java;

import org.junit.Test;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.SerializableEqualityTester;
import com.github.autostyle.StepHarness;
import com.github.autostyle.TestProvisioner;

public class RemoveUnusedImportsStepTest {
  @Test
  public void behavior() throws Exception {
    FormatterStep step = RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral());
    StepHarness.forStep(step)
        .testResource("java/removeunusedimports/JavaCodeUnformatted.test", "java/removeunusedimports/JavaCodeFormatted.test")
        .testResource("java/removeunusedimports/JavaCodeWithLicenseUnformatted.test", "java/removeunusedimports/JavaCodeWithLicenseFormatted.test")
        .testResource("java/removeunusedimports/JavaCodeWithLicensePackageUnformatted.test", "java/removeunusedimports/JavaCodeWithLicensePackageFormatted.test")
        .testResource("java/removeunusedimports/JavaCodeWithPackageUnformatted.test", "java/removeunusedimports/JavaCodeWithPackageFormatted.test");
  }

  @Test
  public void equality() throws Exception {
    new SerializableEqualityTester() {
      @Override
      protected void setupTest(API api) {
        api.areDifferentThan();
      }

      @Override
      protected FormatterStep create() {
        return RemoveUnusedImportsStep.create(TestProvisioner.mavenCentral());
      }
    }.testEquals();
  }
}
