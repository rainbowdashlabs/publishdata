---
title: Versioning
sidebar_position: 2
---

# Versioning

PublishData owns `project.version`. A tag build takes its version from the tag. Every other build needs a base
version, which the [channel](./channels.md) of the build decorates, for example with `-SNAPSHOT`.

## The base version

Set it in `gradle.properties`:

```properties
version=1.5.0
```

or in the build script:

```kotlin
publishData {
    baseVersion = "1.5.0"
}
```

Use the next version you plan to release. After releasing `1.4.0`, the base version becomes `1.5.0` (or `1.4.1`).

:::warning
Do not assign `version = …` in the build script. It replaces the version PublishData resolved, and the plugin warns
about it.
:::

A base version that ends in `-SNAPSHOT` or `-DEV` is cut back to `1.5.0`, with a warning. The channels add those
suffixes themselves.

## Which build gets which version

With the default channels:

| Build | Version |
|---|---|
| tag `v1.4.0` or `1.4.0` | `1.4.0` |
| any branch, pull or merge request | `1.5.0-SNAPSHOT` |
| local build | `1.5.0-SNAPSHOT` |

Releases come from tags. A branch only releases when the release channel names it, see
[Channels](./channels.md#release-from-branches).

## Local builds never release

A build on your own machine is matched as if it ran on a branch called `local`, without tags. It can never become a
release, even when `HEAD` has a tag.

To build like the CI does, set the environment variable `PUBLIC_BUILD=true`. Then the local branch and the tags on
`HEAD` count. Setting [`PUBLISHDATA_REF`](./ci.md#any-ci) has the same effect, because the plugin then treats the
build as a CI build.

## Without a base version

Builds that are not tag builds get Gradle's `unspecified`, with a warning. Everything except publishing to a
repository keeps working. Publishing fails and asks for a base version.

## When the version is fixed

The version is fixed once the build script has run. Changes to the configuration after that, for example in an
`afterEvaluate` block, no longer change it. `baseVersion` and `hashLength` can then no longer be set.

## Print the version

```bash
./gradlew -q printVersion
```

prints the resolved version. Use it in CI for Docker tags or release names. If the build script registers a
`printVersion` task itself, that task wins.

## Why this version

Run any task with `--info` to see how the version was chosen:

```text
PublishData resolved version 1.4.0 of :core in channel 'release' on GITHUB: tag 'v1.4.0' matches 'v?(?<version>\d.*)'
```

The resolution is also available to your build script:

```kotlin
publishData.resolution.get().let { println("${it.channel} ${it.version} ${it.repository}") }
```
