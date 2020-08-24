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

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Represents the line endings which should be written by the tool.
 */
public enum LineEnding {
  // @formatter:off
  /** Uses the same line endings as Git, using `.gitattributes` and the `core.eol` property. */
  GIT_ATTRIBUTES {
    /** .gitattributes is path-specific, so you must use {@link LineEnding#createPolicy(File, File, Supplier)}. */
    @Override @Deprecated
    public Policy createPolicy() {
      return super.createPolicy();
    }
  },
  /** `\n` on unix systems, `\r\n` on windows systems. */
  PLATFORM_NATIVE,
  /** `\r\n` */
  WINDOWS,
  /** `\n` */
  UNIX;
  // @formatter:on

  /** Returns a {@link Policy} appropriate for files which are contained within the given rootFolder. */
  public Policy createPolicy(File rootDir, File projectDir, Supplier<Iterable<File>> toFormat) {
    Objects.requireNonNull(projectDir, "projectDir");
    Objects.requireNonNull(toFormat, "toFormat");
    if (this != GIT_ATTRIBUTES) {
      return createPolicy();
    } else {
      Method method = LineEnding.gitAttributesPolicyCreator;
      if (method == null) {
        try {
          Class<?> clazz = Class.forName("com.github.autostyle.extra.GitAttributesLineEndings");
          method = clazz.getMethod("create", File.class, File.class, Supplier.class);
          LineEnding.gitAttributesPolicyCreator = method;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
          throw new IllegalStateException("LineEnding.GIT_ATTRIBUTES requires the autostyle-lib-extra library, but it is not on the classpath", e);
        }
      }
      try {
        return (Policy) method.invoke(null, rootDir, projectDir, toFormat);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Can't call GitAttributesLineEndings#create", e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException("Error in GitAttributesLineEndings#create", e.getCause());
      }
    }
  }

  private static volatile @Nullable Method gitAttributesPolicyCreator;

  // @formatter:off
  /** Should use {@link #createPolicy(File, File, Supplier)} instead, but this will work iff its a path-independent LineEnding policy. */
  public Policy createPolicy() {
    switch (this) {
    case PLATFORM_NATIVE:  return _platformNativePolicy;
    case WINDOWS:      return WINDOWS_POLICY;
    case UNIX:        return UNIX_POLICY;
    default:  throw new UnsupportedOperationException(this + " is a path-specific line ending.");
    }
  }

  static class ConstantLineEndingPolicy extends NoLambda.EqualityBasedOnSerialization implements Policy {
    private static final long serialVersionUID = 1L;

    final String lineEnding;

    ConstantLineEndingPolicy(String lineEnding) {
      this.lineEnding = lineEnding;
    }

    @Override
    public String getEndingFor(File file) {
      return lineEnding;
    }
  }

  private static final Policy WINDOWS_POLICY = new ConstantLineEndingPolicy(WINDOWS.str());
  private static final Policy UNIX_POLICY = new ConstantLineEndingPolicy(UNIX.str());
  private static final String _platformNative = System.getProperty("line.separator");
  private static final Policy _platformNativePolicy = new ConstantLineEndingPolicy(_platformNative);

  /** Returns the standard line ending for this policy. */
  public String str() {
    switch (this) {
    case PLATFORM_NATIVE:  return _platformNative;
    case WINDOWS:      return "\r\n";
    case UNIX:        return "\n";
    default:  throw new UnsupportedOperationException(this + " is a path-specific line ending.");
    }
  }
  // @formatter:on

  /** A policy for line endings which can vary based on the specific file being requested. */
  public interface Policy extends Serializable, NoLambda {
    /** Returns the line ending appropriate for the given file. */
    String getEndingFor(File file);

    /** Returns true iff this file has unix line endings. */
    default boolean isUnix(File file) {
      Objects.requireNonNull(file);
      String ending = getEndingFor(file);
      return ending.equals(UNIX.str());
    }
  }

  /** Returns a string with exclusively unix line endings. */
  public static String toUnix(String input) {
    int firstNewline = input.lastIndexOf('\n');
    if (firstNewline == -1) {
      // fastest way to detect if a string is already unix-only
      return input;
    } else {
      return input.replace("\r", "");
    }
  }
}
