import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    `maven-publish`
    `java-library`
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

val projectGroup = rootProject.group
val projectVersion = project.property("version")?.takeIf { it != "unspecified" }?.toString() ?: "1.0-SNAPSHOT"

configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(findProject("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProject("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set("3180ca260b82a7") // prevents querying for the staging profile id, performance optimization
        }
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("kotlin")
        plugin("java-library")
        plugin("signing")
        plugin("core-config")
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    // Add an "ac-" prefix to each library's name.  This will prevent jar collisions with other libraries that have
    // ambiguously-named resources like this one, eg: client-1.0.0.jar == bad
    val artifactName = "ac-$name"
    val artifactVersion = projectVersion.toString()

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                groupId = projectGroup.toString()
                artifactId = artifactName
                version = artifactVersion

                from(components["java"])

                pom {
                    name.set("Provenance Asset Classification Kotlin Libraries")
                    description.set("Various tools for interacting with the Asset Classification smart contract")
                    url.set("https://figure.com")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("hyperschwartz")
                            name.set("Jacob Schwartz")
                            email.set("jschwartz@figure.com")
                        }
                        developer {
                            id.set("piercetrey-figure")
                            name.set("Pierce Trey")
                            email.set("ptrey@figure.com")
                        }
                    }

                    scm {
                        developerConnection.set("git@github.com:FigureTechnologies/asset-classification-libs.git")
                        connection.set("https://github.com/FigureTechnologies/asset-classification-libs.git")
                        url.set("https://github.com/FigureTechnologieso/asset-classification-libs")
                    }
                }
            }
        }

        configure<SigningExtension> {
            sign(publications["maven"])
        }
    }
}
