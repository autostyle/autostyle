dependencies {
    api(project(":lib"))
    api("com.diffplug.durian:durian-core")
    api("com.diffplug.durian:durian-testlib")
    api("junit:junit")
    api("org.assertj:assertj-core")

    implementation("com.diffplug.durian:durian-io")
    implementation("com.diffplug.durian:durian-collect")
    implementation(gradleTestKit())
}

// we"ll hold the testlib to a low standard (prize brevity)
//spotbugs { reportLevel = "high" } // low|medium|high (low = sensitive to even minor mistakes)

tasks.named<Test>("test") {
    useJUnit {
        excludeCategories("com.github.autostyle.category.NpmTest")
    }
}

val npmTest by tasks.registering(Test::class) {
    useJUnit {
        includeCategories("com.github.autostyle.category.NpmTest")
    }
}
