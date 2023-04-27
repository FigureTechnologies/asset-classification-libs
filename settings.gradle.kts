rootProject.name = "asset-classification-libs"

include("util", "client", "localtools", "verifier")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

gradle.rootProject {
    val libraryVersion = rootProject.property("libraryVersion") ?: error("Missing libraryVersion - check gradle.properties")
    allprojects {
        group = "tech.figure.classification.asset"
        version = libraryVersion
        description = "Various tools for interacting with the Asset Classification smart contract"
    }
}

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "1.1.7"
}

gitHooks {
    preCommit {
        from {
            """
                echo "Running pre-commit ktlint check"
                ./gradlew ktlintCheck
            """.trimIndent()
        }
    }
    createHooks()
}
