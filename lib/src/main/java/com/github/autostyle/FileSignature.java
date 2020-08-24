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
package com.github.autostyle;

import static com.github.autostyle.MoreIterables.toNullHostileList;
import static com.github.autostyle.MoreIterables.toSortedSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Computes a signature for any needed files. */
public final class FileSignature implements Serializable {
  private static final long serialVersionUID = 1L;

  /*
   * Transient because not needed to uniquely identify a FileSignature instance, and also because
   * Gradle only needs this class to be Serializable so it can compare FileSignature instances for
   * incremental builds.
   */
  @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
  private final transient List<File> files;

  private final byte[] digest;

  private static final Map<File, String> DEFAULT_KEYS =
    Collections.singletonMap(new File(System.getProperty("user.home")), "$HOME");

  /**
   * Creates file signature whereas order of the files remains unchanged.
   */
  @Deprecated
  public static FileSignature signAsList(File... files) throws IOException {
    return new FileSignature(DEFAULT_KEYS, Arrays.asList(files), true);
  }

  /**
   * Creates file signature whereas order of the files remains unchanged.
   */
  @Deprecated
  public static FileSignature signAsList(Iterable<File> files) throws IOException {
    return new FileSignature(DEFAULT_KEYS, toNullHostileList(files), true);
  }

  /** Creates file signature whereas order of the files remains unchanged. */
  @Deprecated
  public static FileSignature signAsSet(File... files) throws IOException {
    return new FileSignature(DEFAULT_KEYS, Arrays.asList(files), false);
  }

  /**
   * Creates file signature insensitive to the order of the files.
   */
  @Deprecated
  public static FileSignature signAsSet(Iterable<File> files) throws IOException {
    return new FileSignature(DEFAULT_KEYS, toSortedSet(files), false);
  }

  /**
   * Creates file signature whereas order of the files remains unchanged.
   */
  public static FileSignature signAsList(Map<File, String> keyPaths, File... files) throws IOException {
    return new FileSignature(keyPaths, Arrays.asList(files), true);
  }

  /**
   * Creates file signature whereas order of the files remains unchanged.
   */
  public static FileSignature signAsList(Map<File, String> keyPaths, Iterable<File> files) throws IOException {
    return new FileSignature(keyPaths, toNullHostileList(files), true);
  }

  /**
   * Creates file signature whereas order of the files remains unchanged.
   */
  public static FileSignature signAsSet(Map<File, String> keyPaths, File... files) throws IOException {
    return new FileSignature(keyPaths, Arrays.asList(files), false);
  }

  /**
   * Creates file signature insensitive to the order of the files.
   */
  public static FileSignature signAsSet(Map<File, String> keyPaths, Iterable<File> files) throws IOException {
    return new FileSignature(keyPaths, toSortedSet(files), false);
  }

  private FileSignature(Map<File, String> keyPaths, final List<File> files, boolean ordered) throws IOException {
    this.files = files;

    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 is not available", e);
    }

    List<Map.Entry<String, String>> entries = keyPaths.entrySet().stream().map(e -> {
      try {
        return (Map.Entry<String, String>) new AbstractMap.SimpleEntry<>(e.getKey().getCanonicalPath(), e.getValue());
      } catch (IOException ioException) {
        throw new RuntimeException("Unable to get canonical path of " + e.getKey(), ioException);
      }
    }).sorted(Map.Entry.<String, String>comparingByKey().reversed()).collect(Collectors.toList());

    List<Map.Entry<String, File>> contents = new ArrayList<>();
    for (File file : files) {
      String path = file.getCanonicalPath();
      for (Map.Entry<String, String> entry : entries) {
        if (path.startsWith(entry.getKey())) {
          path = entry.getValue() + ":" + path.substring(entry.getKey().length());
          break;
        }
      }
      contents.add(new AbstractMap.SimpleEntry<>(path, file));
    }
    if (!ordered) {
      contents.sort(Map.Entry.comparingByKey());
    }
    byte[] buffer = new byte[4096];
    for (Map.Entry<String, File> entry : contents) {
      md.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
      try (InputStream is = Files.newInputStream(entry.getValue().toPath())) {
        int read;
        while ((read = is.read(buffer)) != -1) {
          md.update(buffer, 0, read);
        }
      }
    }
    digest = md.digest();
  }

  /** Returns all of the files in this signature, throwing an exception if there are more or less than 1 file. */
  public Collection<File> files() {
    return Collections.unmodifiableList(files);
  }

  /** Returns the only file in this signature, throwing an exception if there are more or less than 1 file. */
  public File getOnlyFile() {
    if (files.size() == 1) {
      return files.iterator().next();
    } else {
      throw new IllegalArgumentException("Expected one file, but was " + files.size());
    }
  }

}
