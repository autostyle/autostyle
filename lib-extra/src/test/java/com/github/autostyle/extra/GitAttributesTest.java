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
package com.github.autostyle.extra;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GitAttributesTest {
  private File tempDir;

  @BeforeEach
  public void createFolder(@TempDir File tempDir) {
    this.tempDir = tempDir;
  }

  private void write(String path, String... content) throws IOException {
    File file = file(path);
    file.getParentFile().mkdirs();
    Files.write(file.toPath(), Arrays.asList(content));
  }

  private File file(String path) {
    return new File(tempDir, path);
  }

  @Test
  public void cacheTest() throws IOException {
    write(".gitattributes", "* eol=lf", "*.MF eol=crlf");
    {
      GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
      Assertions.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
      Assertions.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
      Assertions.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
      Assertions.assertEquals("crlf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));

      // write out a .gitattributes for the subfolder
      write("subfolder/.gitattributes", "* eol=lf");

      // it shouldn't change anything, because it's cached
      Assertions.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
      Assertions.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
      Assertions.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
      Assertions.assertEquals("crlf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));
    }

    {
      // but if we make a new cache, it should change
      GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
      Assertions.assertEquals("lf", cache.valueFor(file("someFile"), "eol"));
      Assertions.assertEquals("lf", cache.valueFor(file("subfolder/someFile"), "eol"));
      Assertions.assertEquals("crlf", cache.valueFor(file("MANIFEST.MF"), "eol"));
      Assertions.assertEquals("lf", cache.valueFor(file("subfolder/MANIFEST.MF"), "eol"));
    }
  }
}
