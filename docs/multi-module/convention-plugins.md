---
title: Convention plugins
sidebar_position: 3
---

# Convention plugins

Without the settings plugin, share the configuration through convention plugins. Keep the channels in one convention
that the root applies too, so the root resolves exactly like the modules.

`buildSrc/build.gradle.kts`:

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://eldonexus.de/repository/maven-public/")
}

dependencies {
    implementation("de.chojo:publishdata:2.2.0")
}
```

`buildSrc/src/main/kotlin/versioning-conventions.gradle.kts`:

```kotlin
plugins {
    id("de.chojo.publishdata")
}

publishData {
    channels {
        release("https://repo.example.com/releases")
        snapshot("https://repo.example.com/snapshots")
    }
}
```

`buildSrc/src/main/kotlin/publishing-conventions.gradle.kts`:

```kotlin
plugins {
    `java-library`
    `maven-publish`
    id("versioning-conventions")
}

publishData {
    buildData {
        fileName = "${project.name}.data"
    }
    repository {
        credentials {
            username = System.getenv("REPO_USER")
            password = System.getenv("REPO_PASSWORD")
        }
    }
}
```

`build.gradle.kts` of the root:

```kotlin
plugins {
    id("versioning-conventions")
}
```

`api/build.gradle.kts`:

```kotlin
plugins {
    id("publishing-conventions")
}

publishData {
    artifactId = "myproject-api"
}
```

A plugin on the `buildSrc` classpath is applied without a version, as above.

Give the [build data](../build-data.md) file a name per module when modules end up in one shaded jar. Otherwise every
module writes `build.data` and only one survives.
