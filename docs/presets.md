---
title: Presets
sidebar_position: 5
---

# Presets

Presets add ready-made [channels](./channels.md), a repository name and credentials for some hosts.

## EldoNexus

```kotlin
publishData {
    eldoNexus()                     // release (tags), dev (dev* branches, -DEV), snapshot
    eldoNexus(dev = false)          // without the dev channel
    eldoNexus(internal = true)      // the internal repositories
}
```

| Channel | Repository |
|---|---|
| `release` | `https://eldonexus.de/repository/maven-releases/` |
| `dev` | `https://eldonexus.de/repository/maven-dev/` |
| `snapshot` | `https://eldonexus.de/repository/maven-snapshots/` |

With `internal = true` every repository gets the suffix `-internal`, for example `maven-releases-internal`.

The repository is named `EldoNexus`. Its credentials are the Gradle properties `EldoNexusUsername` and
`EldoNexusPassword`. On CI, set them as the environment variables `ORG_GRADLE_PROJECT_EldoNexusUsername` and
`ORG_GRADLE_PROJECT_EldoNexusPassword`. They are only needed when something is published.

## GitLab

```kotlin
publishData {
    gitLab(projectId = "177")                          // on gitlab.com
    gitLab("177", "https://gitlab.example.com/")       // on your own GitLab
}
```

Tags go to the project's package registry as releases, everything else as snapshots, both to
`<url>/api/v4/projects/<projectId>/packages/maven`. The repository is named `GitLab` and authenticates with the
pipeline's `CI_JOB_TOKEN`.

## Other credentials

[`repository { }`](./publishing.md#configure-the-repository) replaces the credentials of a preset.
