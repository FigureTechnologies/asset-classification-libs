dependencies {
    api(project(":client"))

    listOf(
        // Bundles
        libs.bundles.coroutines,
        libs.bundles.eventStream,
        libs.bundles.scarlet,
        libs.bundles.blockApi,

        // Libraries
        libs.okHttp3,
    ).forEach(::api)

    listOf(
        // Bundles
        libs.bundles.test,

        // Libraries
        libs.coroutinesTest,
    ).forEach(::testImplementation)
}
