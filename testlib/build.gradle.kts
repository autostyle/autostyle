dependencies {
    api(project(":autostyle-lib"))
    api(platform(project(":autostyle-bom-testing")))
    api("com.diffplug.durian:durian-testlib")
    api("org.assertj:assertj-core")
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.junit.jupiter:junit-jupiter-params")

    implementation(gradleTestKit())
}

// we"ll hold the testlib to a low standard (prize brevity)
// spotbugs { reportLevel = "high" } // low|medium|high (low = sensitive to even minor mistakes)

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("npm")
    }
    // See https://github.com/gradle/gradle/issues/18647
    // org.gradle.initialization.DefaultLegacyTypesSupport.injectEmptyInterfacesIntoClassLoader
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

val npmTest by tasks.registering(Test::class) {
    useJUnitPlatform {
        includeTags("npm")
    }
}
