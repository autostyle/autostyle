# Autostyle: fix code style automatically

<!---freshmark shields
output = [
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(image('CI Status', 'https://github.com/{{org}}/{{name}}/workflows/CI/badge.svg'), 'https://github.com/{{org}}/{{name}}/actions'),
  ].join('\n');
-->
[![Travis CI](https://travis-ci.org/autostyle/autostyle.svg?branch=master)](https://travis-ci.org/autostyle/autostyle)
[![CI Status](https://github.com/autostyle/autostyle/workflows/CI/badge.svg)](https://github.com/autostyle/autostyle/actions)
<!---freshmark /shields -->

Autostyle can format &lt;java | kotlin | scala | sql | groovy | javascript | flow | typeScript | css | scss | less | jsx | vue | graphql | json | yaml | markdown | license headers | anything> using &lt;Gradle>.

- [Autostyle for Gradle](plugin-gradle)
- [Other build systems](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system)

Ideally, a code formatter can do more than just find formatting errors - it should fix them as well. Such a formatter is really just a `Function<String, String>`, which returns a formatted version of its potentially unformatted input.

It's easy to build such a function, but there are some gotchas and lots of integration work (newlines, character encodings, idempotency, and build-system integration). Autostyle tackles those for you so you can focus on just a simple `Function<String, String>` which can compose with any of the other formatters and build tools in Autostyle' arsenal.

## Current feature matrix

<!---freshmark matrix
function lib(className)   { return '| [`' + className + '`](lib/src/main/java/com/github/autostyle/' + className.replace('.', '/') + '.java) | ' }
function extra(className) { return '| [`' + className + '`](lib-extra/src/main/java/com/github/autostyle/extra/' + className.replace('.', '/') + '.java) | ' }

//                                               | GRADLE        | (new)   |
output = [
'| Feature / FormatterStep                       | [plugin-gradle](plugin-gradle/README.md) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |',
'| --------------------------------------------- | ------------- | --------|',
lib('generic.EndWithNewlineStep')                +'{{yes}}       | {{no}}  |',
lib('generic.IndentStep')                        +'{{yes}}       | {{no}}  |',
lib('generic.LicenseHeaderStep')                 +'{{yes}}       | {{no}}  |',
lib('generic.ReplaceRegexStep')                  +'{{yes}}       | {{no}}  |',
lib('generic.ReplaceStep')                       +'{{yes}}       | {{no}}  |',
lib('generic.TrimTrailingWhitespaceStep')        +'{{yes}}       | {{no}}  |',
extra('cpp.EclipseFormatterStep')                +'{{yes}}       | {{no}}  |',
extra('groovy.GrEclipseFormatterStep')           +'{{yes}}       | {{no}}  |',
lib('java.GoogleJavaFormatStep')                 +'{{yes}}       | {{no}}  |',
lib('java.ImportOrderStep')                      +'{{yes}}       | {{no}}  |',
lib('java.RemoveUnusedImportsStep')              +'{{yes}}       | {{no}}  |',
extra('java.EclipseFormatterStep')               +'{{yes}}       | {{no}}  |',
lib('kotlin.KtLintStep')                         +'{{yes}}       | {{no}}  |',
lib('markdown.FreshMarkStep')                    +'{{yes}}       | {{no}}  |',
lib('npm.PrettierFormatterStep')                 +'{{yes}}       | {{no}}  |',
lib('npm.TsFmtFormatterStep')                    +'{{yes}}       | {{no}}  |',
lib('scala.ScalaFmtStep')                        +'{{yes}}       | {{no}}  |',
lib('sql.DBeaverSQLFormatterStep')               +'{{yes}}       | {{no}}  |',
extra('wtp.EclipseWtpFormatterStep')             +'{{yes}}       | {{no}}  |',
'| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | {{no}}       | {{no}}  |',
'| Fast up-to-date checking                      | {{yes}}       | {{no}}  |',
'| Automatic idempotency safeguard               | {{yes}}       | {{no}}  |',
''
].join('\n');
-->
| Feature / FormatterStep                       | [plugin-gradle](plugin-gradle/README.md) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |
| --------------------------------------------- | ------------- | --------|
| [`generic.EndWithNewlineStep`](lib/src/main/java/com/github/autostyle/generic/EndWithNewlineStep.java) | :+1:       | :white_large_square:  |
| [`generic.IndentStep`](lib/src/main/java/com/github/autostyle/generic/IndentStep.java) | :+1:       | :white_large_square:  |
| [`generic.LicenseHeaderStep`](lib/src/main/java/com/github/autostyle/generic/LicenseHeaderStep.java) | :+1:       | :white_large_square:  |
| [`generic.ReplaceRegexStep`](lib/src/main/java/com/github/autostyle/generic/ReplaceRegexStep.java) | :+1:       | :white_large_square:  |
| [`generic.ReplaceStep`](lib/src/main/java/com/github/autostyle/generic/ReplaceStep.java) | :+1:       | :white_large_square:  |
| [`generic.TrimTrailingWhitespaceStep`](lib/src/main/java/com/github/autostyle/generic/TrimTrailingWhitespaceStep.java) | :+1:       | :white_large_square:  |
| [`cpp.EclipseFormatterStep`](lib-extra/src/main/java/com/github/autostyle/extra/cpp/EclipseFormatterStep.java) | :+1:       | :white_large_square:  |
| [`groovy.GrEclipseFormatterStep`](lib-extra/src/main/java/com/github/autostyle/extra/groovy/GrEclipseFormatterStep.java) | :+1:       | :white_large_square:  |
| [`java.GoogleJavaFormatStep`](lib/src/main/java/com/github/autostyle/java/GoogleJavaFormatStep.java) | :+1:       | :white_large_square:  |
| [`java.ImportOrderStep`](lib/src/main/java/com/github/autostyle/java/ImportOrderStep.java) | :+1:       | :white_large_square:  |
| [`java.RemoveUnusedImportsStep`](lib/src/main/java/com/github/autostyle/java/RemoveUnusedImportsStep.java) | :+1:       | :white_large_square:  |
| [`java.EclipseFormatterStep`](lib-extra/src/main/java/com/github/autostyle/extra/java/EclipseFormatterStep.java) | :+1:       | :white_large_square:  |
| [`kotlin.KtLintStep`](lib/src/main/java/com/github/autostyle/kotlin/KtLintStep.java) | :+1:       | :white_large_square:  |
| [`markdown.FreshMarkStep`](lib/src/main/java/com/github/autostyle/markdown/FreshMarkStep.java) | :+1:       | :white_large_square:  |
| [`npm.PrettierFormatterStep`](lib/src/main/java/com/github/autostyle/npm/PrettierFormatterStep.java) | :+1:       | :white_large_square:  |
| [`npm.TsFmtFormatterStep`](lib/src/main/java/com/github/autostyle/npm/TsFmtFormatterStep.java) | :+1:       | :white_large_square:  |
| [`scala.ScalaFmtStep`](lib/src/main/java/com/github/autostyle/scala/ScalaFmtStep.java) | :+1:       | :white_large_square:  |
| [`sql.DBeaverSQLFormatterStep`](lib/src/main/java/com/github/autostyle/sql/DBeaverSQLFormatterStep.java) | :+1:       | :white_large_square:  |
| [`wtp.EclipseWtpFormatterStep`](lib-extra/src/main/java/com/github/autostyle/extra/wtp/EclipseWtpFormatterStep.java) | :+1:       | :white_large_square:  |
| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | :white_large_square:       | :white_large_square:  |
| Fast up-to-date checking                      | :+1:       | :white_large_square:  |
| Automatic idempotency safeguard               | :+1:       | :white_large_square:  |
<!---freshmark /matrix -->

*Why are there empty squares?* Many projects get harder to work on as they get bigger. 
Autostyle is easier to work on than ever, and one of the reasons why is that we don't require 
contributors to "fill the matrix". So far Autostyle works better with Gradle, however we would consider PRs 
even if it only supports the one formatter you use.

And if you want to add FooFormatter support, we'll happily accept the PR even if it only supports the one build system you use.

Once someone has filled in one square of the formatter/build system matrix, 
it's easy for interested parties to fill in any empty squares, since you'll now have a working example for every piece needed.

## Acknowledgements
- Thanks to contributors of [Spotless](https://github.com/diffplug/spotless).
- Built by [Gradle](https://gradle.org/).
- Tested by [JUnit](https://junit.org/).
- Maintained by [Vladimir Sitnikov](https://github.com/vlsi).
