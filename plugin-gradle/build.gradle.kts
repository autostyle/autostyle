plugins {
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    `kotlin-dsl`
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
//task autostyleCheck(type: JavaExec) {
//  classpath sourceSets.test.runtimeClasspath
//  main = "com.github.autostyle.gradle.SelfTestCheck"
//}
//check.dependsOn(autostyleCheck)
//
//task autostyleApply(type: JavaExec) {
//  classpath sourceSets.test.runtimeClasspath
//  main = "com.github.autostyle.gradle.SelfTestApply"
//}

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

//////////////////////////
// GRADLE PLUGIN PORTAL //
//////////////////////////
gradlePlugin {
    plugins {
        create("autostylePlugin") {
            id = "com.github.autostyle"
            displayName = "Autostyle formatting plugin"
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
