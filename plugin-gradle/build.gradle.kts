plugins {
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("build.java-published-library")
    `kotlin-dsl`
}

dependencies {
    api(project(":autostyle-lib"))
    api(project(":autostyle-lib-extra"))
    implementation("org.ec4j.core:ec4j-core:1.1.1")

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
            website.set("https://github.com/autostyle/autostyle")
            vcsUrl.set("https://github.com/autostyle/autostyle")
            displayName = "Autostyle formatting plugin"
            description = "Autostyle formatting plugin"
            tags.set(
                listOf(
                    "format",
                    "style",
                    "license-header",
                    "kotlin",
                    "java"
                )
            )
            implementationClass = "com.github.autostyle.gradle.AutostylePlugin"
        }
    }
}

tasks.withType<PublishToMavenRepository>()
    .configureEach {
    dependsOn(tasks.withType<Sign>())
}
