---
title: Maven Central
sidebar_position: 7
---

# Maven Central

Channels can publish to Maven Central through
[gradle-maven-publish-plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) 0.34.0 or newer.
Apply and configure that plugin as usual. PublishData sets the version and routes each build:

```kotlin
plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("de.chojo.publishdata") version "2.2.0"
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    pom { … }
}

publishData {
    channels {
        release { publishToMavenCentral() }                  // tags to Maven Central
        snapshot("https://repo.example.com/snapshots")       // branches to your snapshot repository
    }
}
```

```bash
./gradlew publishChannel
```

runs `publishToMavenCentral` on a tag and publishes to the snapshot repository otherwise. A channel can have a
repository and `publishToMavenCentral()` at once. It then publishes to both.

## Required POM elements

Maven Central rejects a deployment whose POM lacks a name, description, url, license, developer or scm url.
On builds whose channel publishes to Maven Central, every publish task waits for the task `validatePoms`, which fails
before anything is uploaded and lists what each publication misses:

```text
The POMs of :core lack elements Maven Central requires: 'maven' lacks description. Set them in the pom block of each publication.
```

The name and description default to the project's, see [Publishing](./publishing.md#pom-name-and-description).
Builds of other channels are not checked. Run `./gradlew validatePoms` to check a project at any time.

## Things to know

- Use `publishChannel`, not `publish`. The vanniktech plugin registers its own Central repository, and `publish`
  would upload branch builds there as well.
- The publication and its coordinates come from the vanniktech plugin. Set them with its `coordinates(…)`.
  `publishData.groupId` and `artifactId` only apply to the publication PublishData creates itself.
- Credentials and signing are configured as the vanniktech plugin documents them (`mavenCentralUsername`,
  `signingInMemoryKey`, …). The snapshot repository takes its credentials through
  [`repository { }`](./publishing.md#configure-the-repository).
- With `signAllPublications()`, versions ending in `-SNAPSHOT` are published unsigned when no key is configured.
  Every other version needs the signing key, also outside Maven Central. A `dev` channel (`1.5.0-DEV`) therefore
  needs the key on dev branches. `branchSuffix()` does not.
- Release uploads to Maven Central do not support the configuration cache yet, a limit of the vanniktech plugin.
  Run them with `--no-configuration-cache`. Snapshots work with it.
- The deployment contains only `.md5` and `.sha1` checksums, see [Checksums](./publishing.md#checksums).
- `publishToMavenCentral()` without the vanniktech plugin is reported as a warning, and `publishChannel` fails on
  the builds that would go to Maven Central.
