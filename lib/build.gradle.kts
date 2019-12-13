plugins {
    id("java-library")
}

dependencies {
    // zero runtime reqs is a hard requirements for autostyle-lib
    // if you need a dep, put it in lib-extra
    testImplementation("com.diffplug.durian:durian-testlib")
}

// we"ll hold the core lib to a high standard
//spotbugs { reportLevel = "low" } // low|medium|high (low = sensitive to even minor mistakes)
