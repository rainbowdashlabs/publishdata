![GitHub Workflow Status](https://img.shields.io/github/workflow/status/RainbowDashLabs/SchematicBrushReborn/Verify%20state?style=for-the-badge&label=Building)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/RainbowDashLabs/SchematicBrushReborn/Publish%20to%20Nexus?style=for-the-badge&label=Publishing) \
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/maven-releases/de.chojo/publishdata?label=Release&logo=Release&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)
![Sonatype Nexus (Development)](https://img.shields.io/nexus/maven-dev/de.chojo/publishdata?label=DEV&logo=Release&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/de.chojo/publishdata?color=orange&label=Snapshot&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)

Small gradle plugin which I use to avoid boilerplate code for version detection.

This util helps to publish artifacts, sets the correction version with optional commit hash and branch identifier and
chooses the repository based on the current branch.

## Setup

```kotlin
plugins {
    id("de.chojo.publishdata") version "version"
}

publishData {
    // only if you want to publish to the eldonexus
    useEldoNexusRepos()
    // manually register a release repo
    addRepo(Repo(Regex("master"), "", "https://my-repo.com/releases", false))
    // manually register a snapshot repo which will appen -SNAPSHOT+<commit_hash>
    addRepo(Repo(Regex(".*"), "-SNAPSHOT", "https://my-repo.com/snapshots", true))
    // Add tasks which should be published
    publishTask("jar")
    publishTask("sourcesJar")
    publishTask("javadocJar")
}

publishing {
    publications.create<MavenPublication>("maven") {
        publishData.configurePublication(this)
    }

    repositories {
        maven {
            authentication {
                // Auth for the repository
                // ....
            }

            name = "MyRepo"
            url = uri(publishData.getRepository())
        }
    }
}
```
