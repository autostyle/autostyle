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

import java.io.File;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.github.autostyle.TestProvisioner;

/**
 * If you'd like to step through the full Autostyle plugin,
 * these tests make that easier. Uncomment ignore to do it.
 */
@Ignore
public class SelfTest {
  enum Type {
    CHECK {
      @Override
      protected Class<? extends AutostyleTask> taskClass() {
        return AutostyleCheckTask.class;
      }

      @Override
      public <T> T checkApply(T check, T apply) {
        return check;
      }
    },
    APPLY {
      @Override
      protected Class<? extends AutostyleTask> taskClass() {
        return AutostyleApplyTask.class;
      }

      @Override
      public <T> T checkApply(T check, T apply) {
        return apply;
      }
    };

    protected abstract Class<? extends AutostyleTask> taskClass();

    public void runAllTasks(Project project) {
      project.getTasks().stream()
        .filter(task -> taskClass().isInstance(task))
        .map(task -> (AutostyleTask) task)
        .forEach(task -> Errors.rethrow().run(() -> {
          task.performAction(Mocks.mockInputChanges());
        }));
    }

    public abstract <T> T checkApply(T check, T apply);
  }

  @Test
  public void autostyleApply() throws Exception {
    runTasksManually(Type.APPLY);
  }

  @Test
  public void autostyleCheck() throws Exception {
    runTasksManually(Type.CHECK);
  }

  /** Runs a full task manually, so you can step through all the logic. */
  private static void runTasksManually(Type type) throws Exception {
    Project project = createProject(extension -> {
      extension.java(java -> {
        java.target("**/*.java");
        // TODO: license header, import order
        java.eclipse(config -> {
          config.getConfigFiles().add("autostyle.eclipseformat.xml");
        });
        java.trimTrailingWhitespace();
        java.custom("Lambda fix", 1, raw -> {
          if (!raw.contains("public class SelfTest ")) {
            // don't format this line away, lol
            return raw.replace("} )", "})").replace("} ,", "},");
          } else {
            return raw;
          }
        });
      });
      extension.format("misc", misc -> {
        misc.target("**/*.gradle", "**/*.md", "**/*.gitignore");
        misc.indentWithTabs();
        misc.trimTrailingWhitespace();
        misc.endWithNewline();
      });
    });
    type.runAllTasks(project);
  }

  /** Creates a Project which has had the AutostyleExtension setup. */
  private static Project createProject(Consumer<AutostyleExtension> test) throws Exception {
    Project project = TestProvisioner.gradleProject(new File("").getAbsoluteFile());
    // create the Autostyle plugin
    AutostylePlugin plugin = project.getPlugins().apply(AutostylePlugin.class);
    // setup the plugin
    test.accept(project.getExtensions().getByType(AutostyleExtension.class));
    // return the configured plugin
    return project;
  }

  /** Runs against the `autostyleSelfApply.gradle` file. */
  static void runWithTestKit(Type type) throws Exception {
    GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(new File(StandardSystemProperty.USER_DIR.value()).getParentFile())
        .withArguments(
            "--build-file", "autostyleSelf.gradle",
            "--project-cache-dir", ".gradle-selfapply",
            "autostyle" + type.checkApply("Check", "Apply"),
            "--stacktrace")
        .forwardOutput()
        .build();
  }
}
