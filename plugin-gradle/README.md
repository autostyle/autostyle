# Autostyle: format your code automatically with Gradle

<!---freshmark shields
output = [
  link(shield('Gradle plugin', 'plugins.gradle.org', 'com.github.autostyle', 'blue'), 'https://plugins.gradle.org/plugin/com.github.autostyle'),
  link(shield('Maven central', 'mavencentral', 'com.github.autostyle.gradle:autostyle', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.autostyle%22%20AND%20a%3A%22autostyle-plugin-gradle%22'),
  link(shield('Javadoc', 'javadoc', '{{released.version}}', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/autostyle-plugin-gradle/{{released.version}}/'),
  '',
  link(shield('Changelog', 'changelog', '{{released.version}}', 'brightgreen'), 'CHANGES.md'),
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.github.autostyle-blue.svg)](https://plugins.gradle.org/plugin/com.github.autostyle)
[![Maven central](https://img.shields.io/badge/mavencentral-com.github.autostyle.gradle%3Aautostyle-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.autostyle%22%20AND%20a%3A%22autostyle-plugin-gradle%22)
[![Javadoc](https://img.shields.io/badge/javadoc-3.1-blue.svg)](https://autostyle.github.io/autostyle/javadoc/autostyle-plugin-gradle/3.1/)

[![Changelog](https://img.shields.io/badge/changelog-3.1-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/autostyle/autostyle.svg?branch=master)](https://travis-ci.org/autostyle/autostyle)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/autostyle/autostyle)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/autostyle-plugin-gradle/', '/', '{{released.version}}')
-->

Autostyle is a general-purpose formatting plugin.  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

Autostyle requires Gradle 5.4+ for execution.

To people who use your build, it looks like this:

```
cmd> gradlew build
...
:autostyleJavaCheck FAILED
> The following files have format violations:
  src\main\java\com\github\autostyle\gradle\FormatExtension.java
    @@ -109,7 +109,7 @@
    ...
    -\t\t····if·(targets.length·==·0)·{
    +\t\tif·(targets.length·==·0)·{
    ...
  You might want to adjust -PmaxCheckMessageLines=50 -PmaxFilesToList=10 -PminLinesPerFile=4 to see more violations
  Run './gradlew autostyleApply' to fix the violations.

cmd> gradlew autostyleApply
:autostyleApply
BUILD SUCCESSFUL

cmd> gradlew build
BUILD SUCCESSFUL
```

To use it in your buildscript, just [add the Autostyle dependency](https://plugins.gradle.org/plugin/com.github.autostyle.gradle), and configure it like so:

```gradle
autostyle {
  format 'misc', {
    filter.include '**/*.gradle', '**/*.md', '**/.gitignore'

    trimTrailingWhitespace()
    indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
    endWithNewline()
  }
  format 'cpp', {
    filter.include '**/*.hpp', '**/*.cpp'

    replace      'Not enough space after if', 'if(', 'if ('
    replaceRegex 'Too much space after if', 'if +\\(', 'if ('

    licenseHeader 'Licensed under Apache-2.0'
  }
}
```

Autostyle can check and apply formatting to any plain-text file, using simple rules ([javadoc](https://autostyle.github.io/autostyle/javadoc/autostyle-plugin-gradle/3.1/com/github/autostyle/gradle/FormatExtension.html)) like those above.  It also supports more powerful formatters:

* Eclipse's [CDT](#eclipse-cdt) C/C++ code formatter
* Eclipse's java code formatter (including style and import ordering)
* Eclipse's [WTP](#eclipse-wtp) HTML, XML, ... code formatters
* Google's [google-java-format](https://github.com/google/google-java-format)
* [Groovy Eclipse](#groovy-eclipse)'s groovy code formatter
* [FreshMark](https://github.com/diffplug/freshmark) (markdown with variables)
* [ktlint](https://github.com/pinterest/ktlint)
* [scalafmt](https://github.com/olafurpg/scalafmt)
* [DBeaver sql format](https://dbeaver.jkiss.org/)
* [Prettier: An opinionated code formatter](https://prettier.io)
* [TypeScript Formatter (tsfmt)](https://github.com/vvakame/typescript-formatter)
* Any user-defined function which takes an unformatted string and outputs a formatted version.

Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.

Autostyle requires Gradle to be running on JRE 8+.<sup>See [issue #7](https://github.com/autostyle/autostyle/issues/7) for details.</sup>

<a name="java"></a>

## Applying to Java source

By default, all Java source sets will be formatted. To change this,
set the `target` parameter as described in the [Custom rules](#custom) section.

```gradle
apply plugin: 'java'
...

autostyle {
  java {
    // Note: it will be automatically commented according to file extension
    licenseHeader 'Licensed under Apache-2.0'  // License header

    importOrder 'java', 'javax', 'org', 'com', 'com.diffplug', ''  // A sequence of package names
    importOrderFile 'autostyle.importorder'        // An import ordering file, exported from Eclipse
    // As before, you can't specify both importOrder and importOrderFile at the same time
    // You probably want an empty string at the end - all of the imports you didn't specify
    // explicitly will go there.

    removeUnusedImports() // removes any unused imports

    eclipse {
      configFile 'autostyle.eclipseformat.xml'  // XML file dumped out by the Eclipse formatter
    }
    // If you have Eclipse preference or property files, you can use them too.
    // eclipse('4.7.1') to specify a specific version of eclipse,
    // available versions are: https://github.com/autostyle/autostyle/tree/master/lib-extra/src/main/resources/com/github/autostyle/autostyle/extra/eclipse_jdt_formatter
  }
}
```

See [ECLIPSE_SCREENSHOTS](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the eclipseFormatFile and importOrderFile mentioned above.

### Configuring License Headers

Autostyle is capable of formatting copyright headers for several files.
For instance: XML (header must be present after `<?xml...?`>), Shell (header must be present after `#!/...`), Java, Kotlin, Bat, and so on.

It uses file extension to identify the comment style.

For instance, here's how you can configure license headers:

```kotlin
// Note: Autostyle automatically adds relevant comments depending on the file type
val license = "Licensed under Apache-2.0"

autostyle {
    format("xml") {
        filter.include("**/*.xml")
        licenseHeader(license)
    }
    format("batch") {
        filter.include("**/*.bat", "**/*.cmd")
        licenseHeader(license) {
            // Note: below are for illustration purposes only
            // Autostyle has default formatting for lots of extensions (see ImprovedLicenseHeaderStep.kt)
            copyrightStyle("bat", com.github.autostyle.generic.DefaultCopyrightStyle.PAAMAYIM_NEKUDOTAYIM)
            copyrightStyle("cmd", "AT_REM")
        }
    }
}
``` 

The output will be like

`test.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Licensed under Apache-2.0
  -->
...
```

`test.cmd` (`AT_REM` formatting)

```
@rem
@rem Licensed under Apache-2.0
@rem
...
```

`test.bat` (`PAAMAYIM_NEKUDOTAYIM` formatting)

```shell script
::
:: Licensed under Apache-2.0
::
...
```


<a name="android"></a>

### Applying to Android Java source
Be sure to add `filter.include '**/*.java'` otherwise Autostyle will not detect Java code inside Android modules.

```gradle
autostyle {
  java {
    // ...
    filter.include '**/*.java'
    // ...
  }
}
```

<a name="google-java-format"></a>

### Applying to Java source with [google-java-format](https://github.com/google/google-java-format)

```gradle
autostyle {
  java {
    googleJavaFormat()
    // optional: you can specify a specific version and/or switch to AOSP style
    googleJavaFormat('1.1') {
      aosp()
    }
    // you can then layer other format steps, such as
    licenseHeader 'autostyle.license.java'
  }
}
```

<a name="groovy"></a>

## Applying to Groovy source

Configuration for Groovy is similar to [Java](#java).  Most java steps, like `licenseHeader` and `importOrder`, support Groovy as well as Java.

The groovy formatter's default behavior is to format all `.groovy` and `.java` files found in the Groovy source directories.  If you would like to exclude the `.java` files, set the parameter `excludeJava`, or you can set the `target` parameter as described in the [Custom rules](#custom) section.

Due to cyclic ambiguities of groovy formatter results, e.g. for nested closures, the use of [paddedCell()](../PADDEDCELL.md) and/or [Custom rules](#custom) is recommended to bandaid over this third-party formatter problem.

```gradle
apply plugin: 'groovy'
...

autostyle {
  java {
    licenseHeader 'Licensed under Apache-2.0'
    googleJavaFormat() // use a specific formatter for Java files
  }
  groovy {
    licenseHeader 'Licensed under Apache-2.0'
    excludeJava() // excludes all Java sources within the Groovy source dirs from formatting
    paddedCell() // Avoid cyclic ambiguities
    // the Groovy Eclipse formatter extends the Java Eclipse formatter,
    // so it formats Java files by default (unless `excludeJava` is used).
    greclipse {
      configFile('greclipse.properties')
    }
  }
  groovyGradle {
    // same as groovy, but for .gradle (defaults to '*.gradle')
    filter.include '*.gradle', 'additionalScripts/*.gradle'
    greclipse {
      configFile('greclipse.properties')
    }
  }
}
```
<a name="groovy-eclipse"></a>

### [Groovy-Eclipse](https://github.com/groovy/groovy-eclipse) formatter

The Groovy formatter uses some of the Eclipse [Java formatter](#java) configuration parameters in addition to Groovy-Eclipse specific ones. All parameters can be configured within a single file, like the Java properties file [greclipse.properties](../testlib/src/main/resources/groovy/greclipse/format/greclipse.properties) in the previous example. The formatter step can also load the [exported Eclipse properties](../ECLIPSE_SCREENSHOTS.md) and augment it with the `.metadata/.plugins/org.eclipse.core.runtime/.settings/org.codehaus.groovy.eclipse.ui.prefs` from your Eclipse workspace as shown below.


```gradle
autostyle {
  groovy {
    // Use the default version and Groovy-Eclipse default configuration
    greclipse()
    // optional: you can specify a specific version or config file(s)
    // available versions: https://github.com/autostyle/autostyle/tree/master/lib-extra/src/main/resources/com/github/autostyle/autostyle/extra/groovy_eclipse_formatter
    greclipse('2.3.0') {
      configFile('autostyle.eclipseformat.xml', 'org.codehaus.groovy.eclipse.ui.prefs')
    }
  }
}
```

Groovy-Eclipse formatting errors/warnings lead per default to a build failure. This behavior can be changed by adding the property/key value `ignoreFormatterProblems=true` to a configuration file. In this scenario, files causing problems, will not be modified by this formatter step.

<a name="freshmark"></a>

## Applying [FreshMark](https://github.com/diffplug/freshmark) to markdown files

Freshmark lets you generate markdown in the comments of your markdown.  This helps to keep badges and links up-to-date (see the source for this file), and can
also be helpful for generating complex tables (see the source for [the parent readme](../README.md)).

To apply freshmark to all of the `.md` files in your project, with all of your project's properties available for templating, use this snippet:

```gradle
autostyle {
  freshmark {
    target file('README.md'), file('CONTRIBUTING.md')  // defaults to '**/*.md'
    propertiesFile('gradle.properties')    // loads all the properties in the given file
    properties {
      it.put('key', 'value')        // specify other properties manually
    }
  }
}
```

<a name="scala"></a>

## Applying [scalafmt](https://olafurpg.github.io/scalafmt/#Scalafmt-codeformatterforScala) to Scala files

```gradle
autostyle {
  scala {
    scalafmt()
    // optional: you can specify a specific version or config file
    scalafmt('0.5.1') {
      configFile('scalafmt.conf')
    }
  }
}
```

<a name="ktlint"></a>

## Applying [ktlint](https://github.com/pinterest/ktlint) to Kotlin files

```gradle
autostyle {
  kotlin {
    // optionally takes a version
    ktlint()
    // Optional user arguments can be set as such:
    ktlint {
      userData(['indent_size': '2', 'continuation_indent_size' : '2'])
    }

    // also supports license headers
    licenseHeader 'Licensed under Apache-2.0'  // License header
  }
  kotlinGradle {
    // same as kotlin, but for .gradle.kts files (defaults to '*.gradle.kts')
    filter.include 'additionalScripts/*.gradle.kts'

    ktlint()

    // Optional user arguments can be set as such:
    ktlint {
      userData(['indent_size': '2', 'continuation_indent_size' : '2'])
    }

    // also supports license headers
    licenseHeader 'Licensed under Apache-2.0'  // License header
  }
}
```

<a name="sql-dbeaver"></a>

## Applying [DBeaver](https://dbeaver.jkiss.org/) to SQL scripts

```gradle
autostyle {
  sql {
    // default value for target files
    target '**/*.sql'
    // configFile is optional, arguments available here: https://github.com/autostyle/autostyle/blob/master/lib/src/main/java/com/github/autostyle/autostyle/sql/dbeaver/DBeaverSQLFormatterConfiguration.java
    dbeaver {
      configFile('dbeaver.props')
    }
  }
}
```

Default configuration file:
```properties
# case of the keywords (UPPER, LOWER or ORIGINAL)
sql.formatter.keyword.case=UPPER
# Statement delimiter
sql.formatter.statement.delimiter=;
# Indentation style (space or tab)
sql.formatter.indent.type=space
# Number of identation characters
sql.formatter.indent.size=4
```

<a name="cpp"></a>

## Applying to C/C++ sources

```gradle
autostyle {
  cpp {
    filter.include '**/*.CPP' // Change file filter. By default files with 'c', 'h', 'C', 'cpp', 'cxx', 'cc', 'c++', 'h', 'hpp', 'hh', 'hxx' and 'inc' extension are supported
    eclipse {
      configFile 'autostyle.eclipseformat.xml'  // XML file dumped out by the Eclipse formatter
    }
    // If you have Eclipse preference or property files, you can use them too.
    // eclipse('4.7.1') to specify a specific version of Eclipse,
    // available versions are: https://github.com/autostyle/autostyle/tree/master/lib-extra/src/main/resources/com/github/autostyle/autostyle/extra/eclipse_cdt_formatter
    licenseHeader 'Licensed under Apache'  // License header
  }
}
```

<a name="eclipse-cdt"></a>

### Eclipse [CDT](https://www.eclipse.org/cdt/) formatter

Use the Eclipse to define the *Code Style preferences* (see [Eclipse documentation](https://www.eclipse.org/documentation/)). Within the preferences *Edit...* dialog, you can export your configuration as XML file, which can be used as a `configFile`. If no `configFile` is provided, the CDT default configuration is used.

<a name="typescript"></a>

## Applying to Typescript source

To use tsfmt, you first have to specify the files that you want it to apply to.
Then you specify `tsfmt()`, and optionally how you want to apply it.

By default, all typescript source sets will be formatted. To change this,
set the `target` parameter as described in the [Custom rules](#custom) section.

```gradle
autostyle {
  typescript {
    // using existing config files
    tsfmt {
      tslintFile('/path/to/repo/tslint.json')
    }
    // tsfmt('7.2.2') to specify specific version of tsfmt
    // tsfmt(['typescript-formatter': '7.2.2', 'typescript': '3.3.3', 'tslint': '5.12.1') to specify all of the npm dependencies that you want
  }
}
```
Supported config file types are `tsconfigFile`, `tslintFile`, `vscodeFile` and `tsfmtFile`. They are corresponding to the respective
[tsfmt-parameters](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/index.ts#L27L34).

*Please note:*
The auto-discovery of config files (up the file tree) will not work when using tsfmt within Autostyle,
  hence you are required to provide resolvable file paths for config files.

... or alternatively provide the configuration inline ...

```gradle
autostyle {
  typescript {
    // custom file-set
    target 'src/main/resources/**/*.ts'
    // provide config inline
    tsfmt {
      config(['indentSize': 1, 'convertTabsToSpaces': true])
    }
  }
}
```

See [tsfmt's default config settings](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32) for what is available.

... and it is also possible to apply `prettier()` instead of `tsfmt()` as formatter. For details see the section about [prettier](#typescript-prettier).

### Prerequisite: tsfmt requires a working NodeJS version

tsfmt is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running autostyle.
Autostyle will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```gradle
autostyle {
  typescript {
    tsfmt {
      npmExecutable('/usr/bin/npm')
      config(...)
    }
  }
}
```

Autostyle uses npm to install necessary packages locally. It runs tsfmt using [J2V8](https://github.com/eclipsesource/J2V8) internally after that.

<a name="prettier"></a>

## Applying [Prettier](https://prettier.io) to javascript | flow | typeScript | css | scss | less | jsx | graphQL | yaml | etc.

Prettier is a formatter that can format [multiple file types](https://prettier.io/docs/en/language-support.html).

To use prettier, you first have to specify the files that you want it to apply to.  Then you specify prettier, and how you want to apply it.

```gradle
autostyle {
  format 'styling', {
    target '**/*.css', '**/*.scss'

    // at least provide the parser to use
    prettier {
      config(['parser': 'postcss'])
    }
    // prettier('1.16.4') to specify specific version of prettier
    // prettier(['my-prettier-fork': '1.16.4']) to specify exactly which npm packages to use

    // or provide a typical filename
    prettier {
      config(['filepath': 'style.scss'])
    }
  }
}
```

Supported config options are documented on [prettier.io](https://prettier.io/docs/en/options.html).

It is also possible to specify the config via file:

```gradle
autostyle {
  format 'styling', {
    filter.include '**/*.css', '**/*.scss'

    prettier {
      configFile('/path-to/.prettierrc.yml')
    }

    // or provide both (config options take precedence over configFile options)
    prettier {
      config(['parser': 'postcss'])
      configFile('path-to/.prettierrc.yml')
    }
  }
}
```

Supported config file variants are documented on [prettier.io](https://prettier.io/docs/en/configuration.html).
*Please note:*
- The auto-discovery of config files (up the file tree) will not work when using prettier within autostyle.
- Prettier's override syntax is not supported when using prettier within autostyle.

To apply prettier to more kinds of files, just add more formats

```gradle
autostyle {
  format 'javascript', {
    filter.include 'src/main/resources/**/*.js'
    prettier {
      config(['filepath': 'file.js'])
    }
  }
}
```

<a name="typescript-prettier"></a>
Prettier can also be applied from within the [typescript config block](#typescript-formatter):

```gradle
autostyle {
  typescript {
    // no parser or filepath needed
    // -> will default to 'typescript' parser when used in the typescript block
    prettier()
  }
}
```

### Prerequisite: prettier requires a working NodeJS version

Prettier, like tsfmt, is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running autostyle.
Autostyle will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```gradle
autostyle {
  format 'javascript', {
    prettier {
      npmExecutable('/usr/bin/npm')
      config(...)
    }
  }
}
```

Autostyle uses npm to install necessary packages locally. It runs prettier using [J2V8](https://github.com/eclipsesource/J2V8) internally after that.

<a name="eclipse-wtp"></a>

## Applying [Eclipse WTP](https://www.eclipse.org/webtools/) to css | html | etc.

The Eclipse [WTP](https://www.eclipse.org/webtools/) formatter can be applied as follows:

```gradle
autostyle {
  format 'xml', {
    filter {
      include '**/*.xml', '**/*.xsd'
      exclude '**/build/**'
    }
    // Use for example eclipseWtp('xml', '4.7.3a') to specify a specific version of Eclipse,
    // available versions are: https://github.com/autostyle/autostyle/tree/master/lib-extra/src/main/resources/com/github/autostyle/autostyle/extra/eclipse_wtp_formatters
    eclipseWtp('xml') {
      configFile 'autostyle.xml.prefs', 'autostyle.common.properties'
    }
  }
}
```
The WTP formatter accept multiple configuration files. All Eclipse configuration file formats are accepted as well as simple Java property files. Omit the `configFile` entirely to use the default Eclipse configuration. The following formatters and configurations are supported:

| Type | Configuration       | File location
| ---- | ------------------- | -------------
| CSS  | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
|      | cleanup preferences | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
| HTML | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.html.core.prefs
|      | cleanup preferences | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.html.core.prefs
|      | embedded CSS        | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
|      | embedded JS         | Use the export in the Eclipse editor configuration dialog
| JS   | editor preferences  | Use the export in the Eclipse editor configuration dialog
| JSON | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.json.core.prefs
| XML  | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.xml.core.prefs

Note that `HTML` should be used for `X-HTML` sources instead of `XML`.

The Eclipse XML catalog cannot be configured for the Autostyle WTP formatter, instead a
user defined catalog file can be specified using the property `userCatalog`. Catalog versions
1.0 and 1.1 are supported by Autostyle.

Unlike Eclipse, Autostyle WTP ignores per default external URIs in schema location hints and
external entities. To allow the access of external URIs, set the property `resolveExternalURI`
to true.

<a name="license-header"></a>

## License header options

License hader is automatically formatted according to the file extension.

<a name="custom"></a>

## Custom rules

Autostyle is a generic system for specifying a sequence of steps which are applied to a set of files.

```gradle
autostyle {
  // this will create two tasks: autostyleMiscCheck and autostyleMiscApply
  format 'misc', {
    // target determines the root(s) of the filetree for which this format will apply to
    // - if you pass a string or a list of strings, they will be treated
    //       as 'include' parameters to a fileTree in the root directory
    // - if you pass a FileCollection, it will pass through untouched
    //       e.g. project.files('build.gradle', 'settings.gradle')
    // - if you pass anything else, it will be sent to project.files(yourArg)
    // By default target is set to the project.rootDir 
    target '**/*.gradle', '**/*.md', '**/.gitignore'

    // You can specify exclude-include patters with
    filter {
      include '**/*.yaml'
      exclude '**/*.xml'
    }

    // Autostyle has built-in rules for the most basic formatting tasks
    trimTrailingWhitespace()
    indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
    endWithNewline()

    // you can also call out to your own function
    // Note: Gradle does not know when the function is changed, so it needs an extra input
    // to understand when the formatter needs to be re-run. This is why the second argument
    // is needed. You need to bump it every time you alter implementation
    custom 'superFormatter', 3, {
      // when writing a custom step, it will be helpful to know
      // how the formatting process works, which is as follows:

      // 1) Load each target file, and convert it to unix-style line endings ('\n')
      // 2) Pass its content through a series of steps, feeding the output of each step to the next
      // 3) Put the correct line endings back on, then either check or apply

      // each step receives a string as input, and should output
      // a formatted string as output.  Each step can trust that its
      // input will have unix newlines, and it must promise to output
      // only unix newlines.  Other than that, anything is fair game!
    }
  }
}
```

If you use `custom` or `customLazy`, you might want to take a look at [this javadoc](https://autostyle.github.io/autostyle/javadoc/autostyle-plugin-gradle/3.1/com/github/autostyle/gradle/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-) for a big performance win.

See [`JavaExtension.java`](src/main/java/com/github/autostyle/gradle/JavaExtension.java) if you'd like to see how a language-specific set of custom rules is implemented.  We'd love PR's which add support for other languages.

<a name="invisible"></a>

## Line endings and encodings (invisible stuff)

Autostyle uses UTF-8 by default, but you can use [any encoding which Java supports](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html).  You can set it globally, and you can also set it per-format.

```gradle
autostyle {
  java {
    ...
    encoding 'Cp1252' // java will have Cp1252
  }
  encoding 'US-ASCII'   // but all other formats will be interpreted as US-ASCII
}
```

Line endings can also be set globally or per-format using the `lineEndings` property.  Autostyle supports four line ending modes: `UNIX`, `WINDOWS`, `PLATFORM_NATIVE`, and `GIT_ATTRIBUTES`.  The default value is `GIT_ATTRIBUTES`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Autostyle and git disagree, then you're going to have a bad time.

You can easily set the line endings of different files using [a `.gitattributes` file](https://help.github.com/articles/dealing-with-line-endings/).  Here's an example `.gitattributes` which sets all files to unix newlines: `* text eol=lf`.

<a name="enforceCheck"></a>

## Disabling warnings and error messages

The `check` task is Gradle's built-in task for grouping all verification tasks - unit tests, static analysis, etc.  By default, `autostyleCheck` is added as a dependency to `check`.

You might want to disable this behavior.  We [recommend against this](https://github.com/autostyle/autostyle/issues/79#issuecomment-290844602), but it's easy to do if you'd like:

```gradle
autostyle {
  enforceCheck false
}
```

When a misformatted file throws an exception, it will be for one of two reasons:

1) Autostyle calculated the properly formatted version, and it is different than the current contents.
2) One of the formatters threw an exception while attempting to calculate the properly formatted version.

You can fix (1) by excluding the file from formatting using the `targetExclude` method, see the [custom rules](#custom) section for details.  You can fix (2) and turn these exceptions into warnings like this:

```gradle
autostyle {
  java {
    googleJavaFormat()
    custom 'my-glitchy-step', { }
  }
}
```

<a name="preview"></a>

## How do I preview what `autostyleApply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before Autostyle"`
- Run `gradlew autostyleApply`
- View the changes with `git diff`
- If you don't like what Autostyle did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

<a name="examples"></a>

## Can I apply Autostyle to specific files?

Not yet.

## Can I apply Autostyle to multi-module project?

Yes.
By default Autostyle operates only on files relevant to a single module, so you can apply it inside
`allprojects` block:

```kotlin
plugins {
    id("com.github.autostyle")
}

allprojects {
    apply(plugin = "com.github.autostyle")
    // Note: even though Autostyle uses `filter.include("*.gradle.kts")` for kotlinGradle,
    // It will not descend to folders of other projects, so :autostyleCheck task in the root
    // project would validate only Gradle scripts for the root project.
    autostyle {
        kotlinGradle {
            ktlint()
        }
    }
}
```

If that does not suit for you, you can use `excludeSubprojects` property as follows.
Note: the possibility is there, however you should probably refrain from using this approach.
Having project-specific tasks enables fine-grained control, and enables to use build cache.

```kotlin
// Declare Autostyle for the root project
autostyle {
    format("markdown") {
        filter.include("**/*.md")
        // Default is true
        // It will process all the *.md files in the source tree
        excludeSubprojects.set(false)
    }
}
```

## Example configurations (from real-world projects)

Autostyle is hosted on Maven Central, and it can be used as a regular Gradle plugin: `id("com.github.autostyle")`

* [GitHub search for Autostyle](https://github.com/search?l=gradle&q=autostyle&type=Code)
* [Apache Calcite](https://github.com/apache/calcite) (see [Spotless -> Autostyle](https://github.com/apache/calcite/pull/1682))
* [Apache Calcite Avatica](https://github.com/apache/calcite-avatica) (see [Spotless -> Autostyle](https://github.com/apache/calcite-avatica/pull/118))
* [Apache JMeter](https://github.com/apache/jmeter) (see [Spotless -> Autostyle](https://github.com/apache/jmeter/pull/549))
* [vlsi-release-plugins](https://github.com/vlsi/vlsi-release-plugins) (see [Spotless -> Autostyle](https://github.com/vlsi/vlsi-release-plugins/commit/8f9353cfd788508bdcecf6469c0054757afb295b))
* Your project here

<!---freshmark /javadoc -->
