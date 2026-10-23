---
title: Channels
sidebar_position: 4
---

# Channels

Channels decide which builds get which version and where they are published. The plugin tries them in order. The
first channel that matches a build wins.

```kotlin
publishData {
    channels {
        release("https://repo.example.com/releases")          // tags
        dev("https://repo.example.com/dev")                   // dev* branches, -DEV
        channel("rc", "https://repo.example.com/rc") {        // a channel of your own
            onBranch(Regex("release/.*")).suffix("RC")
        }
        snapshot("https://repo.example.com/snapshots")        // everything else, -SNAPSHOT
    }
}
```

## The built-in channels

| Channel | Without matchers it applies to | Suffix | Pull and merge requests |
|---|---|---|---|
| `release` | tags like `v1.2.3` or `1.2.3`; the version comes from the tag | none | excluded |
| `dev` | branches starting with `dev`, in any case | `-DEV` | included |
| `snapshot` | every build | `-SNAPSHOT` | included |
| `channel(name)` | nothing, until it gets a matcher | none | included |

The repository is optional for all of them. A project can be versioned without publishing anywhere:

```kotlin
publishData {
    channels {
        release()
        dev()
        snapshot()
    }
}
```

Without any channel, `release` and `snapshot` apply without a repository.

## Matchers and options

Call them on a channel, inside its block or chained:

| Call | Effect |
|---|---|
| `onTag()` | tags like `v1.2.3` or `1.2.3`; the version is taken from the tag |
| `onTag("release-(?<version>.+)")` | tags matching your pattern; the version is the group `version`, or the whole tag |
| `onBranch("main", "master")` | exactly these branches |
| `onBranch(Regex("dev.*"))` | branches fully matching the pattern |
| `onAnyRef()` | every build |
| `suffix("RC")` | appends `-RC` |
| `branchSuffix()` | inserts the branch: `1.5.0-feature-x-SNAPSHOT`; not for `main` and `master` |
| `branchSuffix(listOf("develop"))` | the same, with these branches keeping the plain version |
| `commitHash()` | appends `+<short hash>`; the length is `publishData.hashLength` (default 7) |
| `includeChangeRequests()` | pull and merge request builds may use this channel |
| `excludeChangeRequests()` | pull and merge request builds skip this channel |
| `publishToMavenCentral()` | also publishes to Maven Central, see [Maven Central](./maven-central.md) |

A channel with matchers of its own uses only those, not the defaults of its kind.

## Order matters

A channel after one that matches every build, such as `snapshot`, is never used. Neither is a `channel(…)` without
matchers. The plugin warns about both.

## Release from branches

To release from branches as well, name them:

```kotlin
release("https://repo.example.com/releases") {
    onTag().onBranch("main", "master")
}
```

## Keep branches apart

`branchSuffix()` keeps the snapshots of different branches apart. Characters other than letters, digits, `.` and `_`
become `-`. Tag builds and local builds without `PUBLIC_BUILD` keep the plain version.

Maven snapshot repositories expect versions that end in `-SNAPSHOT`. `branchSuffix()` keeps that ending.
`commitHash()` does not, so use it only with repositories that accept any version.

## Replace the channels of a preset

`channels { }` adds to the channels that are already there, for example those of a [preset](./presets.md). A new
channel behind the preset's `snapshot` channel is never used. Call `clear()` first to start over:

```kotlin
publishData {
    eldoNexus(dev = false)
    channels {
        clear()
        release("https://eldonexus.de/repository/maven-releases/") { publishToMavenCentral() }
        snapshot("https://eldonexus.de/repository/maven-snapshots/")
    }
}
```

The preset still sets the repository name and its credentials.
