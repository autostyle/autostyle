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

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.github.autostyle.FormatterStep;
import com.github.autostyle.ResourceHarness;
import com.github.autostyle.SerializableEqualityTester;
import com.github.autostyle.StepHarness;
import com.github.autostyle.TestProvisioner;

public class GoogleJavaFormatStepTest extends ResourceHarness {
  @Test
  public void behavior() throws Throwable {
    FormatterStep step = GoogleJavaFormatStep.create("1.2", TestProvisioner.mavenCentral());
    StepHarness.forStep(step)
        .testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormatted.test")
        .testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormatted.test")
        .testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormatted.test")
        .testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormatted.test");
  }

  @Test
  public void behaviorWithAospStyle() throws Throwable {
    FormatterStep step = GoogleJavaFormatStep.create("1.2", "AOSP", TestProvisioner.mavenCentral());
    StepHarness.forStep(step)
        .testResource("java/googlejavaformat/JavaCodeUnformatted.test", "java/googlejavaformat/JavaCodeFormattedAOSP.test")
        .testResource("java/googlejavaformat/JavaCodeWithLicenseUnformatted.test", "java/googlejavaformat/JavaCodeWithLicenseFormattedAOSP.test")
        .testResource("java/googlejavaformat/JavaCodeWithLicensePackageUnformatted.test", "java/googlejavaformat/JavaCodeWithLicensePackageFormattedAOSP.test")
        .testResource("java/googlejavaformat/JavaCodeWithPackageUnformatted.test", "java/googlejavaformat/JavaCodeWithPackageFormattedAOSP.test");
  }

  @Test
  public void equality() throws Throwable {
    new SerializableEqualityTester() {
      String version = "1.2";
      String style = "";

      @Override
      protected void setupTest(API api) {
        // same version == same
        api.areDifferentThan();
        // change the version, and it's different
        version = "1.1";
        api.areDifferentThan();
        // change the style, and it's different
        style = "AOSP";
        api.areDifferentThan();
      }

      @Override
      protected FormatterStep create() {
        String finalVersion = this.version;
        return GoogleJavaFormatStep.create(finalVersion, style, TestProvisioner.mavenCentral());
      }
    }.testEquals();
  }

  @Test
  public void fixWindowsBugForGfj1Point1() {
    fixWindowsBugTestcase("");
    fixWindowsBugTestcase(
        "",
        "import somepackage;",
        "");
    fixWindowsBugTestcase(
        "import somepackage;",
        "",
        "public class SomeClass {}");
    fixWindowsBugTestcase(
        "/** Some license */",
        "import somepackage;",
        "",
        "public class SomeClass {}");
    fixWindowsBugTestcase(
        "package thispackage;",
        "",
        "import somepackage;",
        "",
        "public class SomeClass {}");
    fixWindowsBugTestcase(
        "/*",
        " * A License.",
        " */",
        "",
        "package thispackage;",
        "",
        "import somepackage;",
        "",
        "public class SomeClass {}");
  }

  private void fixWindowsBugTestcase(String... lines) {
    String input = StringPrinter.buildStringFromLines(lines);
    Assert.assertEquals(input, GoogleJavaFormatStep.fixWindowsBug(input, "1.1"));
  }
}
