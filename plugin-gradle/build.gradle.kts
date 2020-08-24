plugins {
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    `kotlin-dsl`
}

dependencies {
    api(project(":autostyle-lib"))
    api(project(":autostyle-lib-extra"))
    implementation("com.diffplug.durian:durian-collect")
    implementation("com.diffplug.durian:durian-core")
    implementation("com.diffplug.durian:durian-io")
    implementation("org.ec4j.core:ec4j-core:0.2.2")

    testImplementation(project(":autostyle-testlib"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.diffplug.durian:durian-testlib")
}

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

gradlePlugin {
    plugins {
        create("autostylePlugin") {
            id = "com.github.autostyle"
            displayName = "Autostyle formatting plugin"
            description = "Autostyle formatting plugin"
            implementationClass = "com.github.autostyle.gradle.AutostylePlugin"
        }
    }
}
pluginBundle {
    // These settings are set for the whole plugin bundle
    website = "https://github.com/autostyle/autostyle"
    vcsUrl = "https://github.com/autostyle/autostyle"
    description = project.description

//    plugins {
//        create("autostylePlugin") {
//            id = "com.github.autostyle"
//            displayName = "Autostyle formatting plugin"
//            tags = listOf(
//                "format",
//                "style",
//                "license",
//                "header"
//            )
//        }
//    }

/*    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()
    }*/
}
