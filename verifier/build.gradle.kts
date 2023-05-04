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
