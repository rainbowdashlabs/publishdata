---
title: PublishData
sidebar_position: 1
---

# PublishData

PublishData is a Gradle plugin. It takes the version of your project from git and publishes every build to the
repository it belongs in.

- Tag `v1.4.0` and the build is version `1.4.0`, published as a release. You do not need a commit that bumps the
  version.
- Push a branch and it becomes `1.5.0-SNAPSHOT`, published to your snapshot repository. If branches should not
  overwrite each other, it becomes `1.5.0-feature-x-SNAPSHOT`.
- It works on GitHub Actions, GitLab CI, Bitbucket Pipelines, Jenkins and any other CI. A build on your own machine
  never produces a release by accident.
- It can write build information (version, commit, branch, build time) into your artifact.
- It sets up `maven-publish` for you and supports the configuration cache.

Two more plugins come with it: a [settings plugin](./multi-module/settings-plugin.md) that configures every project
of a build at once, and the [plugin manifest](./plugin-manifest.md) for Minecraft plugins.

## Requirements

| What | Version |
|---|---|
| Gradle | 8.5 or newer |
| Java | The Java that runs Gradle: 17 or newer for 2.1.0, 21 or newer from 2.2.0 on |

## Quick start

PublishData is hosted on the EldoNexus, not on the Gradle Plugin Portal. Add the EldoNexus to the plugin repositories
in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}
```

Apply the plugin in `build.gradle.kts` and tell it where releases and snapshots go:

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
    repository {
        credentials {
            username = System.getenv("REPO_USER")
            password = System.getenv("REPO_PASSWORD")
        }
    }
}
```

Put the next version into `gradle.properties`:

```properties
version=1.5.0
```

Now run `./gradlew publishChannel` in your CI on every push. Branch builds publish `1.5.0-SNAPSHOT` to the snapshot
repository. A tag `v1.5.0` publishes `1.5.0` to the release repository.

Without `channels`, the plugin only sets the version and publishes nothing.

## Where to go next

- [Versioning](./versioning.md): how the version is chosen.
- [CI](./ci.md): where the plugin reads the branch and the tag from.
- [Channels](./channels.md): which builds go where.
- [Presets](./presets.md): ready-made channels for the EldoNexus and GitLab.
- [Publishing](./publishing.md): the publication, the repository and `publishChannel`.
- [Maven Central](./maven-central.md): releases to Maven Central.
- [Build data](./build-data.md): build information inside your artifact.
- [Plugin manifest](./plugin-manifest.md): a manifest beside the jar of a Minecraft plugin.
- [Multi-module builds](./multi-module/index.md): many projects, one configuration.
- [Init scripts](./init-scripts.md): PublishData for builds that do not declare it.
- [Migrating from 1.x](./migrating-from-1.md).
