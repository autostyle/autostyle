dependencies {
    api(project(":autostyle-lib"))
    // misc useful utilities
    implementation("com.diffplug.durian:durian-core")
    implementation("com.diffplug.durian:durian-collect")
    // needed by GitAttributesLineEndings
    implementation("org.eclipse.jgit:org.eclipse.jgit") {
        exclude("com.jcraft", "jsch")
        exclude("org.bouncycastle")
    }
    implementation("com.googlecode.concurrent-trees:concurrent-trees")
    // used for xml parsing in EclipseFormatter
    implementation("org.codehaus.groovy:groovy-xml")

    // testing
    testImplementation(project(":autostyle-testlib"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.diffplug.durian:durian-testlib")
    // EclipseCommonTests is hard to migrate to JUnit5 :(
    testImplementation("junit:junit")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

// we'll hold the core lib to a high standard
// spotbugs { reportLevel = 'low' } // low|medium|high (low = sensitive to even minor mistakes)
