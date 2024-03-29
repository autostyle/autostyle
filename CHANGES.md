### Version 4.0
* Drop Eclipse-based formatters
* Bump Gradle to 8.5
* Bump build Java version to 17
* Bump Kotlin target to 1.4
* Bump release-plugins: 1.70 -> 1.90

### Version 3.2
* Support Gradle build cache. The tasks were not cached previously since they produced no output
* Move file processing to `...Process` task for better integration with Gradle caches
* Produce helpful error messages by default (deprecate `paddedCell()` option)
* Print GitHub Actions friendly warnings. The warnings would be visible as diff annotations
* Bump release-plugins: 1.54 -> 1.70
* Bump Gradle: 6.1 -> 6.9.2
* Bump JUnit to 5.8.2 and 4.13.2

### Version 3.1
* Added CI regression tests with Apache Calcite, Apache JMeter, and vlsi-release-plugins projects
* Support build chache for autostyleCheck task
* Skip execution when JitPack is detected  ([#17](https://github.com/autostyle/autostyle/issues/17))
* Fix lib-extra dependencies: it should have transitives for org.eclipse.jgit

### Version 3.0

* The first release after fork of https://github.com/diffplug/spotless
* Gradle plugin: Visualize CR and LF symbols ([#2](https://github.com/autostyle/autostyle/issues/2))
* Gradle plugin: make autostyleCheck/Apply tasks operate only on the files that belong to the relevant projects ([#1](https://github.com/autostyle/autostyle/issues/1))
* Gradle plugin: simplify configuration of license header style
* Gradle plugin: log intermediate steps when formatting cannot converge
* Gradle plugin: provide options to see more violations when running check (maxCheckMessageLines, maxFilesToList, minLinesPerFile)
* License formatter: add a blank line if license header is followed with a comment
* Diff formatter: Use simple characters when rendering diff on Windows ([#4](https://github.com/autostyle/autostyle/issues/4))
* Reduce merge conflicts on `CHANGES.md` by leveraging `merge=union` Git strategy

Default versions:
* ktlint [0.36.0](https://github.com/pinterest/ktlint/releases/tag/0.36.0)
