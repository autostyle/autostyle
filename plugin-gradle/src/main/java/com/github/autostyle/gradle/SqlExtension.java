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

import static com.github.autostyle.gradle.PluginGradlePreconditions.requireElementsNonNull;

import org.gradle.api.Project;

import com.github.autostyle.FormatterStep;
import com.github.autostyle.sql.DBeaverSQLFormatterStep;

public class SqlExtension extends FormatExtension {
  static final String NAME = "sql";

  public SqlExtension(SpotlessExtension rootExtension) {
    super(rootExtension);
  }

  public DBeaverSQLFormatterConfig dbeaver() {
    return new DBeaverSQLFormatterConfig();
  }

  public class DBeaverSQLFormatterConfig {
    Object[] configFiles;

    DBeaverSQLFormatterConfig() {
      configFiles = new Object[0];
      addStep(createStep());
    }

    public void configFile(Object... configFiles) {
      this.configFiles = requireElementsNonNull(configFiles);
      replaceStep(createStep());
    }

    private FormatterStep createStep() {
      Project project = getProject();
      return DBeaverSQLFormatterStep.create(project.files(configFiles).getFiles());
    }
  }

  /** If the user hasn't specified the files yet, we'll assume he/she means all of the sql files. */
  @Override
  protected void setupTask(SpotlessTask task) {
    if (target == null) {
      target("**/*.sql");
    }
    super.setupTask(task);
  }
}