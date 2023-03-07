plugins {
    kotlin("jvm") version "1.8.10"
    `maven-publish`
    `java-gradle-plugin`
}

group = "de.chojo"
version = "1.1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("stdlib"))
}

gradlePlugin {
    plugins {
        create("publishData") {
            id = "de.chojo.publishdata"
            implementationClass = "de.chojo.PublishData"
        }
    }
}

publishing {
    repositories {
        maven {
            group = project.group as String
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }

            val branch = System.getenv("GITHUB_REF")?.replace("refs/heads/", "") ?: ""

            val isSnap = !(branch == "main" || branch == "master")

            val releasesRepoUrl = "https://eldonexus.de/repository/maven-releases/"
            val snapshotsRepoUrl = "https://eldonexus.de/repository/maven-snapshots/"
            url = uri(if (isSnap) snapshotsRepoUrl else releasesRepoUrl)
            name = "EldoNexus"
        }
    }
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
