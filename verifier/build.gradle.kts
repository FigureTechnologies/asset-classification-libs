plugins {
    `maven-publish`
    java
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(project(":client"))

    listOf(
        // Bundles
        libs.bundles.blockapi,
        libs.bundles.coroutines,
        libs.bundles.eventstream,
        libs.bundles.scarlet,

        // Libraries
        libs.okhttp3
    ).forEach(::api)

    listOf(
        // Bundles
        libs.bundles.test,

        // Libraries
        libs.coroutines.test
    ).forEach(::testImplementation)
}

// disable javadoc lint since it spams the console for generated sources
tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://nexus.figure.com/repository/figure")
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASS")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
