import org.gradle.api.tasks.testing.logging.TestExceptionFormat

sourceSets {
    create("integrationTest") {
        compileClasspath += main.get().output + test.get().output + configurations.testCompileClasspath
        runtimeClasspath += main.get().output + test.get().output + compileClasspath
        java.srcDir("src/integrationTest")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Run integration tests"
    group = "Integration Testing"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath =
        sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath + sourceSets["integrationTest"].runtimeClasspath
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
    useJUnitPlatform {
        includeEngines = setOf("junit-jupiter", "junit-vintage")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Always re-run tests
    outputs.upToDateWhen { false }
}


dependencies {
    listOf(
        project(":client"),
        libs.assetSpecs,
        libs.feignJackson,
        libs.okHttp3,
    ).forEach(::api)

    testImplementation(libs.bundles.test)

    listOf(
        // Libraries
        libs.kotlinLogging,
        libs.logbackClassic,
        libs.provenanceEventStreamApi,
        libs.provenanceEventStreamApiModel,
        libs.provenanceEventStreamCli,
        libs.provenanceEventStreamCore,
        libs.provenanceGatewayClient,
        libs.provenanceScopeOsClient,

        // Bundles
        libs.bundles.coroutines,
        libs.bundles.test,
        libs.bundles.testContainers,
    ).forEach { configurations["integrationTestImplementation"].invoke(it) }
}
