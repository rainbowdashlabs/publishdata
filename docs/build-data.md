---
title: Build data
sidebar_position: 8
---

# Build data

```kotlin
publishData {
    buildData()
}
```

adds a file `build.data` to the main resources. Your code can read it at runtime, for example to show its version or
to check for updates.

```properties
time=ISO 8601 timestamp of the build
unix=unix timestamp of the build
type=BUILD_TYPE environment variable, else PUBLIC or LOCAL
branch=branch, or none
commit=short commit hash, or none
artifactVersion=version
artifact=artifact name
runtime=Java runtime of the build
tag=tag the version was taken from, or none
channel=resolved channel
```

`type` is `PUBLIC` for CI builds and for local builds with `PUBLIC_BUILD=true`, `LOCAL` otherwise.

## Options

```kotlin
publishData {
    buildData {
        fileName = "myplugin.data"         // default build.data
        includeTime = false                // reproducible, cacheable output
        put("discord", "https://discord.gg/…")
        put("build", providers.environmentVariable("GITHUB_RUN_NUMBER"))
    }
}
```

- Without `time` and `unix` the file only depends on its inputs. The task is then cacheable and the artifact
  reproducible.
- Your own entries are written after the built-in ones.
- The task is `generateBuildData`. Its output lands in `build/generated/publishdata`.

In a [multi-module build](./multi-module/convention-plugins.md) whose modules end up in one shaded jar, give the file
a name per module. Otherwise every module writes `build.data` and only one survives.
