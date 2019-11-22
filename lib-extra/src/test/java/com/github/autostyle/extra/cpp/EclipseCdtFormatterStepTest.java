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
package com.github.autostyle.extra.cpp;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.TestProvisioner;
import com.github.autostyle.extra.EclipseBasedStepBuilder;
import com.github.autostyle.extra.eclipse.EclipseCommonTests;

public class EclipseCdtFormatterStepTest extends EclipseCommonTests {

  @Override
  protected String[] getSupportedVersions() {
    return new String[]{"4.7.3a", "4.11.0", "4.12.0", "4.13.0"};
  }

  @Override
  protected String getTestInput(String version) {
    return "#include <a.h>;\nint main(int argc,   \nchar *argv[]) {}";
  }

  @Override
  protected String getTestExpectation(String version) {
    return "#include <a.h>;\nint main(int argc, char *argv[]) {\n}\n";
  }

  @Override
  protected FormatterStep createStep(String version) {
    EclipseBasedStepBuilder builder = EclipseCdtFormatterStep.createBuilder(TestProvisioner.mavenCentral());
    builder.setVersion(version);
    return builder.build();
  }

}
