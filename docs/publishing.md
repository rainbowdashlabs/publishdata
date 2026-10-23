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
- registers the task `publishChannel`,
- fills in the POM name and description of every Maven publication,
- publishes only `.md5` and `.sha1` checksums.

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

## POM name and description

Every Maven publication, also those of other plugins, gets the project name as POM `name` and the project
`description` as POM `description`. Both are defaults: a value the build sets wins.

The description is read when the POM is written, so it may be set anywhere in the build script, also below the
`plugins` block of a module whose convention plugin configures the POM:

```kotlin
plugins {
    id("my.publishing-conventions")
}

description = "The core module"
```

A convention plugin that assigns `description = project.description` in its `pom { }` block reads the description
before the module sets it. That assigns nothing, and the default still applies.

## Checksums

Gradle writes `.md5`, `.sha1`, `.sha256` and `.sha512` files next to every published file. Maven repositories and
Maven Central only need `.md5` and `.sha1`, so PublishData leaves out the other two. Turn that off to get all four:

```kotlin
publishData {
    onlyRequiredChecksums = false
}
```

Gradle reads this switch from a system property that applies to the whole build while it publishes. Turning it off in
one project therefore keeps the extra checksums only as long as no other project of the build that keeps it on has
started publishing. Turn it off in every project, for example in the
[settings plugin](./multi-module/settings-plugin.md) defaults, to get all four everywhere.

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
