---
title: Publishing
sidebar_position: 6
---

# Publishing

When `maven-publish` is applied, PublishData

- creates a publication `maven` from the `java` component, unless the project declares a publication itself or
  applies `java-gradle-plugin` or `com.vanniktech.maven.publish`,
- registers a Maven repository with the URL of the resolved channel, named `PublishData` (`EldoNexus` or `GitLab`
  with the [presets](./presets.md)),
- registers the task `publishChannel`.

## publishChannel

`publishChannel` publishes to the targets of the resolved channel and nowhere else: its repository and, with
[`publishToMavenCentral()`](./maven-central.md), Maven Central. Run it in CI on every push:

```bash
./gradlew publishChannel
```

Prefer it over `publish`. `publish` also uploads to repositories that other plugins registered.

A channel without a repository publishes nowhere, and `publishChannel` says so.

## Configure the repository

`repository { }` configures the registered repository. It replaces the default credentials of a preset:

```kotlin
publishData {
    channels {
        release("https://repo.example.com/releases")
        snapshot("https://repo.example.com/snapshots")
    }
    repository {
        credentials {
            username = System.getenv("REPO_USER")
            password = System.getenv("REPO_PASSWORD")
        }
    }
}
```

## Further options

```kotlin
publishData {
    component = "javaPlatform"          // component of the automatic publication, default java
    groupId = "de.chojo.multi"          // default: group of the root project, else of this project
    artifactId = "multi-api"            // default: project name in lower case, also used in build data
    artifact(tasks.named("shadowJar"))  // additional artifacts
    autoPublication = false             // configure publications yourself
    repositoryName = "MyRepo"           // name of the registered repository
}
```

The task that publishes to the repository follows its name: `publishAllPublicationsTo<name>Repository`, for example
`publishAllPublicationsToEldoNexusRepository`.
