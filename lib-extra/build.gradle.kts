plugins {
    id("build.java-published-library")
}

dependencies {
    api(project(":autostyle-lib"))
    // misc useful utilities
    // needed by GitAttributesLineEndings
    implementation("org.eclipse.jgit:org.eclipse.jgit")
    implementation("com.googlecode.concurrent-trees:concurrent-trees")

    // testing
    testImplementation(project(":autostyle-testlib"))
    testImplementation("org.assertj:assertj-core")
    // EclipseCommonTests is hard to migrate to JUnit5 :(
    testImplementation("junit:junit")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

// we'll hold the core lib to a high standard
// spotbugs { reportLevel = 'low' } // low|medium|high (low = sensitive to even minor mistakes)
