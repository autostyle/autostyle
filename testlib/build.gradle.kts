dependencies {
    api(project(":lib"))
    api("com.diffplug.durian:durian-core")
    api("com.diffplug.durian:durian-testlib")
    api("org.assertj:assertj-core")
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.junit.jupiter:junit-jupiter-params")

    implementation("com.diffplug.durian:durian-io")
    implementation("com.diffplug.durian:durian-collect")
    implementation(gradleTestKit())
}

// we"ll hold the testlib to a low standard (prize brevity)
//spotbugs { reportLevel = "high" } // low|medium|high (low = sensitive to even minor mistakes)

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("npm")
    }
}

val npmTest by tasks.registering(Test::class) {
    useJUnitPlatform {
        includeTags("npm")
    }
}
