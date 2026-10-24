---
title: Settings plugin
sidebar_position: 2
---

# Settings plugin

The settings plugin applies PublishData to every project, the root included. It configures each one before its build
script runs.

`settings.gradle.kts`:

```kotlin
plugins {
    id("de.chojo.publishdata.settings") version "2.3.0"
}

rootProject.name = "myproject"
include("core", "api")

publishData {
    defaults {
        channels {
            release("https://repo.example.com/releases")
            snapshot("https://repo.example.com/snapshots")
        }
        buildData()
    }
}
```

`api/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    `maven-publish`
    id("de.chojo.publishdata")
}

publishData {
    artifactId = "myproject-api"
}
```

A build script can add to the defaults or override them, for example with `channels { clear() }`. It may apply
`de.chojo.publishdata` itself, but without a version: the plugin is already on the classpath.

Gradle versions that do not generate accessors for settings extensions need the explicit form in
`settings.gradle.kts`:

```kotlin
configure<de.chojo.publishdata.PublishDataSettings> {
    defaults { … }
}
```
