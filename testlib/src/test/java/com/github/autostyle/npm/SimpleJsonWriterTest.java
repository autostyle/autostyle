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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.autostyle.ResourceHarness;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

public class SimpleJsonWriterTest extends ResourceHarness {

  private SimpleJsonWriter jsonWriter = new SimpleJsonWriter();

  @Test
  public void itWritesAValidEmptyObject() {
    assertThat(jsonWriter.toJsonString().replaceAll("\\s", "")).isEqualTo("{}");
  }

  @Test
  public void itWritesABooleanProperty() {
    jsonWriter.put("mybool", true);
    assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mybool\": true\n}");
  }

  @Test
  public void itWritesAStringProperty() {
    jsonWriter.put("mystring", "stringvalue");
    assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mystring\": \"stringvalue\"\n}");
  }

  @Test
  public void itWritesAnInteger() {
    jsonWriter.put("myint", 7);
    assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"myint\": 7\n}");
  }

  @Test
  public void itFailsOnUnsupportedObject() {
    Assertions.assertThatThrownBy(() -> {
      jsonWriter.put("anyobj", new Object());
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void itHandlesSeveralOptionsSimultaneously() {
    jsonWriter.putAll(new LinkedHashMap<String, Object>(){{ put("mystring", "stringvalue"); put("intvalue", 1);}});
    assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mystring\": \"stringvalue\",\n    \"intvalue\": 1\n}");
  }

  @Test
  public void itWritesToFile() throws IOException {
    jsonWriter.put("mystring", "stringvalue");
    final File file = newFile("target.json");
    jsonWriter.toJsonFile(file);
    assertFile(file).hasContent("{\n    \"mystring\": \"stringvalue\"\n}");
  }
}
