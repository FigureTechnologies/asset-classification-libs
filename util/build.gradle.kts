dependencies {
    listOf(
        libs.bundles.jackson,
        libs.bundles.provenance,
        libs.bouncycastle,
    ).forEach(::api)

    testImplementation(libs.bundles.test)
}
