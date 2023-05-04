import org.gradle.api.tasks.testing.logging.TestExceptionFormat

sourceSets {
    create("integrationTest") {
        compileClasspath += main.get().output + test.get().output + configurations.testCompileClasspath.get()
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
        libs.asset.specs,
        libs.feign.jackson,
        libs.okhttp3
    ).forEach(::api)

    testImplementation(libs.bundles.test)

    listOf(
        // Libraries
        libs.asset.model,
        libs.figure.eventstream.api,
        libs.figure.eventstream.api.model,
        libs.figure.eventstream.cli,
        libs.figure.eventstream.core,
        libs.kotlin.logging,
        libs.logback.classic,
        libs.objectstore.gateway.client,
        libs.provenance.scope.objectstore.client,

        // Bundles
        libs.bundles.coroutines,
        libs.bundles.test,
        libs.bundles.testcontainers
    ).forEach { configurations["integrationTestImplementation"].invoke(it) }
}
