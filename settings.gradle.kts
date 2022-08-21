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
