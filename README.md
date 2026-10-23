# PublishData

[![Release](https://img.shields.io/nexus/maven-releases/de.chojo/publishdata?label=Release&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)][release]
[![Snapshot](https://img.shields.io/nexus/s/de.chojo/publishdata?color=orange&label=Snapshot&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)][snapshot]

A Gradle plugin that derives the version of your project from git and publishes every build to the repository it
belongs in: tags as releases, branches as snapshots. It can also embed build data in the artifact and publish a
manifest beside the jar of a Minecraft plugin.

## Requirements

Gradle 8.5 or newer, running on Java 21 or newer (Java 17 up to 2.1.0).

## Usage

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}
```

`build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    `maven-publish`
    id("de.chojo.publishdata") version "2.1.0"
}

publishData {
    channels {
        release("https://repo.example.com/releases")
        snapshot("https://repo.example.com/snapshots")
    }
}
```

The next version goes into `gradle.properties` (`version=1.5.0`), and CI runs `./gradlew publishChannel`.

Documentation: https://docs.chojo.dev/publishdata

[release]: https://eldonexus.de/#browse/browse:maven-releases:de%2Fchojo%2Fpublishdata
[snapshot]: https://eldonexus.de/#browse/browse:maven-snapshots:de%2Fchojo%2Fpublishdata
