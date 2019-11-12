dependencies {
  api(project(":lib"))
  // misc useful utilities
  implementation("com.diffplug.durian:durian-core")
  implementation("com.diffplug.durian:durian-collect")
  // needed by GitAttributesLineEndings
  implementation("org.eclipse.jgit:org.eclipse.jgit")
  implementation("com.googlecode.concurrent-trees:concurrent-trees")
  // used for xml parsing in EclipseFormatter
  implementation("org.codehaus.groovy:groovy-xml")

  // testing
  testImplementation(project(":testlib"))
  testImplementation("org.assertj:assertj-core")
  testImplementation("com.diffplug.durian:durian-testlib")
}

// we'll hold the core lib to a high standard
//spotbugs { reportLevel = 'low' } // low|medium|high (low = sensitive to even minor mistakes)
