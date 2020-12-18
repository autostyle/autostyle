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

import com.github.autostyle.FormatterFunc;
import com.github.autostyle.FormatterStep;
import com.github.autostyle.JarState;
import com.github.autostyle.LineEnding;
import com.github.autostyle.Provisioner;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
public class GoogleJavaFormatStep {
  // prevent direct instantiation
  private GoogleJavaFormatStep() {}

  private static final String DEFAULT_VERSION = "1.7";
  private static final String DEFAULT_STYLE = "GOOGLE";
  static final String NAME = "google-java-format";
  static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
  static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
  static final String FORMATTER_METHOD = "formatSource";

  private static final String OPTIONS_CLASS = "com.google.googlejavaformat.java.JavaFormatterOptions";
  private static final String OPTIONS_BUILDER_METHOD = "builder";
  private static final String OPTIONS_BUILDER_CLASS = "com.google.googlejavaformat.java.JavaFormatterOptions$Builder";
  private static final String OPTIONS_BUILDER_STYLE_METHOD = "style";
  private static final String OPTIONS_BUILDER_BUILD_METHOD = "build";
  private static final String OPTIONS_Style = "com.google.googlejavaformat.java.JavaFormatterOptions$Style";

  private static final String REMOVE_UNUSED_CLASS = "com.google.googlejavaformat.java.RemoveUnusedImports";
  private static final String REMOVE_UNUSED_METHOD = "removeUnusedImports";

  private static final float REMOVE_UNUSED_IMPORT_JavadocOnlyImports_LAST_SUPPORTED = 1.7f;
  private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports = "com.google.googlejavaformat.java.RemoveUnusedImports$JavadocOnlyImports";
  private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep = "KEEP";

  private static final String IMPORT_ORDERER_CLASS = "com.google.googlejavaformat.java.ImportOrderer";
  private static final String IMPORT_ORDERER_METHOD = "reorderImports";

  /** Creates a step which formats everything - code, import order, and unused imports. */
  public static FormatterStep create(Provisioner provisioner) {
    return create(defaultVersion(), provisioner);
  }

  /** Creates a step which formats everything - code, import order, and unused imports. */
  public static FormatterStep create(String version, Provisioner provisioner) {
    return create(version, DEFAULT_STYLE, provisioner);
  }

  /** Creates a step which formats everything - code, import order, and unused imports. */
  public static FormatterStep create(String version, String style, Provisioner provisioner) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(style, "style");
    Objects.requireNonNull(provisioner, "provisioner");
    return FormatterStep.createLazy(NAME,
        () -> new State(NAME, version, style, provisioner),
        State::createFormat);
  }

  public static String defaultVersion() {
    return DEFAULT_VERSION;
  }

  public static String defaultStyle() {
    return DEFAULT_STYLE;
  }

  static final class State implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The jar that contains the eclipse formatter. */
    final JarState jarState;
    final String stepName;
    final String version;
    final String style;

    State(String stepName, String version, Provisioner provisioner) throws IOException {
      this(stepName, version, DEFAULT_STYLE, provisioner);
    }

    State(String stepName, String version, String style, Provisioner provisioner) throws IOException {
      this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
      this.stepName = stepName;
      this.version = version;
      this.style = style;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    FormatterFunc createFormat() throws Exception {
      ClassLoader classLoader = jarState.getClassLoader();

      // instantiate the formatter and get its format method
      Class<?> optionsClass = classLoader.loadClass(OPTIONS_CLASS);
      Class<?> optionsBuilderClass = classLoader.loadClass(OPTIONS_BUILDER_CLASS);
      Method optionsBuilderMethod = optionsClass.getMethod(OPTIONS_BUILDER_METHOD);
      Object optionsBuilder = optionsBuilderMethod.invoke(null);

      Class<?> optionsStyleClass = classLoader.loadClass(OPTIONS_Style);
      Object styleConstant = Enum.valueOf((Class<Enum>) optionsStyleClass, style);
      Method optionsBuilderStyleMethod = optionsBuilderClass.getMethod(OPTIONS_BUILDER_STYLE_METHOD, optionsStyleClass);
      optionsBuilderStyleMethod.invoke(optionsBuilder, styleConstant);

      Method optionsBuilderBuildMethod = optionsBuilderClass.getMethod(OPTIONS_BUILDER_BUILD_METHOD);
      Object options = optionsBuilderBuildMethod.invoke(optionsBuilder);

      Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
      Object formatter = formatterClazz.getConstructor(optionsClass).newInstance(options);
      Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

      FormatterFunc removeUnusedFormatter = createUnusedImportsFormatter(classLoader);

      Class<?> importOrdererClass = classLoader.loadClass(IMPORT_ORDERER_CLASS);
      Method importOrdererMethod = importOrdererClass.getMethod(IMPORT_ORDERER_METHOD, String.class);

      return input -> {
        String formatted = (String) formatterMethod.invoke(formatter, input);
        String removedUnused = removeUnusedFormatter.apply(formatted);
        String sortedImports = (String) importOrdererMethod.invoke(null, removedUnused);
        return fixWindowsBug(sortedImports, version);
      };
    }

    FormatterFunc createRemoveUnusedImportsOnly() throws Exception {
      ClassLoader classLoader = jarState.getClassLoader();

      FormatterFunc removeUnusedFormatter = createUnusedImportsFormatter(classLoader);

      return input -> fixWindowsBug(removeUnusedFormatter.apply(input), version);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FormatterFunc createUnusedImportsFormatter(final ClassLoader classLoader) throws Exception {
      // All current versions of google-java-format have the format 1.x
      // If any newer version introduces a different format which can't be parsed as a float,
      // we set the version to Float.MAX_VALUE i.e. we detect it as a version newer than
      // REMOVE_UNUSED_IMPORT_JavadocOnlyImports_LAST_SUPPORTED.
      float versionNumber;
      try {
        versionNumber = Float.parseFloat(version);
      } catch (NumberFormatException e) {
        versionNumber = Float.MAX_VALUE;
      }

      Class<?> removeUnusedClass = classLoader.loadClass(REMOVE_UNUSED_CLASS);
      Method removeUnusedMethod;
      Object removeJavadocConstant;
      // In version 1.8 and later the currently deprecated class REMOVE_UNUSED_IMPORT_JavadocOnlyImports has been removed.
      if (versionNumber <= REMOVE_UNUSED_IMPORT_JavadocOnlyImports_LAST_SUPPORTED) {
        Class<?> removeJavadocOnlyClass = classLoader.loadClass(REMOVE_UNUSED_IMPORT_JavadocOnlyImports);
        removeJavadocConstant = Enum.valueOf((Class<Enum>) removeJavadocOnlyClass, REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep);
        removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class, removeJavadocOnlyClass);
      } else {
        removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class);
        removeJavadocConstant = null;
      }
      return input -> {
        String removeUnused;
        try {
          if (removeJavadocConstant != null) {
            removeUnused = (String) removeUnusedMethod.invoke(null, input, removeJavadocConstant);
          } else {
            removeUnused = (String) removeUnusedMethod.invoke(null, input);
          }
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
        return removeUnused;
      };
    }
  }

  private static final boolean IS_WINDOWS = LineEnding.PLATFORM_NATIVE.str().equals("\r\n");

  /**
   * google-java-format-1.1's removeUnusedImports does *wacky* stuff on Windows.
   * The beauty of normalizing all line endings to unix!
   */
  static String fixWindowsBug(String input, String version) {
    if (IS_WINDOWS && version.equals("1.1")) {
      int firstImport = input.indexOf("\nimport ");
      if (firstImport == 0) {
        return input;
      } else if (firstImport > 0) {
        int numToTrim = 0;
        char prevChar;
        do {
          ++numToTrim;
          prevChar = input.charAt(firstImport - numToTrim);
        } while (Character.isWhitespace(prevChar) && (firstImport - numToTrim) > 0);
        if (firstImport - numToTrim == 0) {
          // import was the very first line, and we'd like to maintain a one-line gap
          ++numToTrim;
        } else if (prevChar == ';' || prevChar == '/') {
          // import came after either license or a package declaration
          --numToTrim;
        }
        if (numToTrim > 0) {
          return input.substring(0, firstImport - numToTrim + 2) + input.substring(firstImport + 1);
        }
      }
    }
    return input;
  }
}
