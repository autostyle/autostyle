plugins {
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
}

dependencies {
    api(project(":lib"))
    api(project(":lib-extra"))
    implementation("com.diffplug.durian:durian-core")
    implementation("com.diffplug.durian:durian-io")
    implementation("com.diffplug.durian:durian-collect")

    testImplementation(project(":testlib"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.diffplug.durian:durian-testlib")
}

/////////////////////
// SPOTLESS (fake) //
/////////////////////
//task spotlessCheck(type: JavaExec) {
//  classpath sourceSets.test.runtimeClasspath
//  main = "com.github.autostyle.gradle.SelfTestCheck"
//}
//check.dependsOn(spotlessCheck)
//
//task spotlessApply(type: JavaExec) {
//  classpath sourceSets.test.runtimeClasspath
//  main = "com.github.autostyle.gradle.SelfTestApply"
//}

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

//////////////////////////
// GRADLE PLUGIN PORTAL //
//////////////////////////
pluginBundle {
    // These settings are set for the whole plugin bundle
    website = "https://github.com/autostyle/autostyle"
    vcsUrl = "https://github.com/autostyle/autostyle"
    description = project.description

    plugins {
        create("autostylePlugin") {
            id = "com.github.vlsi.autostyle"
            displayName = "Autostyle formatting plugin"
            tags = listOf(
                    "format",
                    "style",
                    "license",
                    "header"
            )
        }
    }

    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()
    }
}
