plugins {
    id("java-library")
}

dependencies {
    // zero runtime reqs is a hard requirements for spotless-lib
    // if you need a dep, put it in lib-extra
    testImplementation("junit:junit")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.diffplug.durian:durian-testlib")
}

// we"ll hold the core lib to a high standard
//spotbugs { reportLevel = "low" } // low|medium|high (low = sensitive to even minor mistakes)
