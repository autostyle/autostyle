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

import static com.github.autostyle.gradle.Tasks.execute;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.LineEnding;
import com.github.autostyle.ResourceHarness;
import com.github.autostyle.TestProvisioner;
import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class FormatTaskTest extends ResourceHarness {
  private AutostyleTask checkTask;
  private AutostyleTask applyTask;

  @BeforeEach
  public void createTask() throws IOException {
    Project project = TestProvisioner.gradleProject(rootFolder());
    checkTask = project.getTasks().create("checkTaskUnderTest", AutostyleCheckTask.class);
    applyTask = project.getTasks().create("applyTaskUnderTest", AutostyleApplyTask.class);
  }

  @Test
  public void testLineEndingsCheckFail() throws Exception {
    Assertions.assertThatThrownBy(() -> {
      checkTask.getLineEndingsPolicy().set(LineEnding.UNIX.createPolicy());
      checkTask.getSourceFiles().from(Collections.singleton(setFile("testFile").toContent("\r\n")));
      execute(checkTask);
    }).isInstanceOf(GradleException.class);
  }

  @Test
  public void testLineEndingsCheckPass() throws Exception {
    checkTask.getLineEndingsPolicy().set(LineEnding.UNIX.createPolicy());
    checkTask.getSourceFiles().from(Collections.singleton(setFile("testFile").toContent("\n")));
    execute(checkTask);
  }

  @Test
  public void testLineEndingsApply() throws Exception {
    File testFile = setFile("testFile").toContent("\r\n");

    applyTask.getLineEndingsPolicy().set(LineEnding.UNIX.createPolicy());
    applyTask.getSourceFiles().from(Collections.singleton(testFile));
    execute(applyTask);

    assertFile(testFile).hasContent("\n");
  }

  @Test
  public void testStepCheckFail() throws IOException {
    File testFile = setFile("testFile").toContent("apple");
    checkTask.getSourceFiles().from(Collections.singleton(testFile));

    checkTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));

    String diff = String.join("\n",
        "    @@ -1 +1 @@",
        "    -apple",
        "    +aple");
    Assertions.assertThatThrownBy(() -> execute(checkTask)).hasStackTraceContaining(diff);

    assertFile(testFile).hasContent("apple");
  }

  @Test
  public void testStepCheckPass() throws Exception {
    File testFile = setFile("testFile").toContent("aple");
    checkTask.getSourceFiles().from(Collections.singleton(testFile));

    checkTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));
    execute(checkTask);

    assertFile(testFile).hasContent("aple");
  }

  @Test
  public void testStepApply() throws Exception {
    File testFile = setFile("testFile").toContent("apple");
    applyTask.getSourceFiles().from(Collections.singleton(testFile));

    applyTask.addStep(FormatterStep.createNeverUpToDate("double-p", content -> content.replace("pp", "p")));
    execute(applyTask);

    assertFile(testFile).hasContent("aple");
  }
}
