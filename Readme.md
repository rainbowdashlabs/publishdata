# PublishData

![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/de.chojo.publishdata?style=for-the-badge&label=Plugin%20Portal)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/RainbowDashLabs/publishdata/verify.yml?style=for-the-badge&label=Building)

A Gradle plugin that derives the version of your project from git and publishes every build to the
repository it belongs in.

- Tag `v1.4.0` and the build is version `1.4.0`, published as a release. No version bump commit needed.
- Push a branch and it becomes `1.5.0-SNAPSHOT`, published to your snapshot repository, or
  `1.5.0-feature-x-SNAPSHOT` if branches should not overwrite each other.
- Works on GitHub Actions, GitLab CI, Bitbucket Pipelines, Jenkins and any other CI, and locally
  without ever producing a release by accident.
- Optionally embeds build information (version, commit, branch, build time) into the artifact.
- Sets up `maven-publish` for you and supports the configuration cache.

Requires Gradle 8.5 or newer.

## Quick start

```kotlin
plugins {
    `java-library`
    `maven-publish`
    id("de.chojo.publishdata") version "2.0.0"
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

with the next version in `gradle.properties`:

```properties
version=1.5.0
```

`./gradlew publish` now publishes `1.5.0-SNAPSHOT` from branches and the tagged version from tags.
Without `channels`, the plugin only sets the version.

The plugin is also available from `https://eldonexus.de/repository/maven-public/` for builds that
do not use the Gradle Plugin Portal.

### Version

The plugin owns `project.version`. Builds that are not tag builds need a base version, either in
`gradle.properties`

```properties
version=1.5.0
```

or in the build script

```kotlin
publishData {
    baseVersion = "1.5.0"
}
```

Do not assign `version = …` in the build script: it replaces the resolved version, which is reported
as a warning. A base version ending in `-SNAPSHOT` or `-DEV` is cut back to `1.5.0` with a warning,
since the channels add those suffixes.

Without further configuration:

| Build                                         | Version          |
|-----------------------------------------------|------------------|
| tag `v1.4.0` or `1.4.0`                       | `1.4.0`          |
| any branch, pull or merge request             | `1.5.0-SNAPSHOT` |
| local build                                   | `1.5.0-SNAPSHOT` |

Releases come from tags. A branch only releases when the release channel names it, see
[Channels](#channels).

A local build is only treated like a CI build with the environment variable `PUBLIC_BUILD=true`, so a
release cannot be built by accident on a developer machine.

Without a base version, non-tag builds get Gradle's `unspecified` with a warning; everything but
publishing to a repository keeps working.

The version is fixed once the build script has run. Later changes to the configuration, e.g. in an
`afterEvaluate` block, do not change it anymore, and `baseVersion` and `hashLength` can no longer be
set.

`./gradlew -q printVersion` prints the version, e.g. for Docker tags or release names in CI. A
`printVersion` task of the build script itself takes precedence.

`--info` logs how the version was chosen:

```
PublishData resolved version 1.4.0 of :core in channel 'release' on GITHUB: tag 'v1.4.0' matches 'v?(?<version>\d.*)'
```

### Where the git state comes from

1. `PUBLISHDATA_REF` (`refs/tags/v1.2.3`, `refs/heads/main` or a branch name) and optionally
   `PUBLISHDATA_COMMIT`, for any CI.
2. GitHub Actions: tags, branches and pull requests.
3. GitLab CI: tag pipelines, branch pipelines and merge request pipelines.
4. Bitbucket Pipelines: tags, branches and pull requests.
5. Jenkins: tags, branches and change requests of multibranch pipelines; `GIT_BRANCH` for other jobs.
6. The local `git`, including tags pointing at `HEAD`.

### Channels

Channels decide which builds get which version and where they are published. They are tried in
order; the first match wins.

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

Matchers and options of a channel:

| Call                          | Effect                                                                     |
|-------------------------------|----------------------------------------------------------------------------|
| `onTag()`                     | tags like `v1.2.3` / `1.2.3`; the version is taken from the tag            |
| `onTag("release-(?<version>.+)")` | custom tag pattern; the version is the group `version` or the whole tag |
| `onBranch("main", "master")`  | these branches                                                             |
| `onBranch(Regex("dev.*"))`    | branches matching the pattern                                              |
| `onAnyRef()`                  | every build                                                                |
| `suffix("DEV")`               | appends `-DEV`                                                             |
| `branchSuffix()`              | inserts the branch, `1.5.0-feature-x-SNAPSHOT`; not for `main` / `master`  |
| `branchSuffix(listOf("develop"))` | same, with other branches keeping the plain version                    |
| `commitHash()`                | appends `+<short hash>`; length via `publishData.hashLength` (default 7)   |
| `excludeChangeRequests()`     | pull and merge request builds skip this channel (default for `release`)    |

`release` without matchers applies to tags, `dev` to branches starting with `dev` (any case) with
suffix `DEV`, and `snapshot` to every build. The repository is optional for all of them, so a
project can be versioned without publishing anywhere:

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

Order matters: a channel after one that matches every build, such as `snapshot`, is never used, and
neither is a `channel(…)` without matchers. Both are reported as a warning.

To release from branches as well, name them:

```kotlin
release("https://repo.example.com/releases") {
    onTag().onBranch("main", "master")
}
```

`branchSuffix()` keeps snapshots of different branches apart. Characters other than letters, digits,
`.` and `_` become `-`. Tag builds and local builds without `PUBLIC_BUILD` keep the plain version.

Maven snapshot repositories expect versions ending in `-SNAPSHOT`. `branchSuffix()` keeps that
ending; `commitHash()` does not, so use it only with repositories that accept any version.

#### Presets

Ready-made channels for some hosts:

```kotlin
publishData {
    gitLab(projectId = "177")       // release and snapshot to the project's package registry
    gitLab("177", "https://gitlab.example.com/")
    eldoNexus()                     // eldonexus.de: release (tags), dev (dev* branches, -DEV), snapshot
    eldoNexus(internal = true)      // its internal repositories
    eldoNexus(dev = false)          // without the dev channel
}
```

### Publishing

When `maven-publish` is applied, the plugin

- creates a publication `maven` from the `java` component, unless the project declares a publication
  itself or uses `java-gradle-plugin`,
- registers a Maven repository with the URL of the resolved channel, named `PublishData`
  (`EldoNexus` / `GitLab` with the presets).

The presets bring their credentials:

- `eldoNexus()` reads the gradle properties `EldoNexusUsername` and `EldoNexusPassword`, on CI e.g.
  from the environment variables `ORG_GRADLE_PROJECT_EldoNexusUsername` and
  `ORG_GRADLE_PROJECT_EldoNexusPassword`. They are only required when publishing to the repository.
- `gitLab(…)` authenticates with the pipeline's `CI_JOB_TOKEN`.

`repository { }` replaces those defaults and configures the repository freely:

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

Further options:

```kotlin
publishData {
    component = "javaPlatform"          // component of the automatic publication
    groupId = "de.chojo.multi"          // default: group of the root project, else of this project
    artifactId = "multi-api"            // default: project name in lower case, also used in build data
    artifact(tasks.named("shadowJar"))  // additional artifacts
    autoPublication = false             // configure publications yourself
    repositoryName = "MyRepo"
}
```

The resolution is available for anything else:

```kotlin
publishData.resolution.get().let { println("${it.channel} ${it.version} ${it.repository}") }
```

### Build data

```kotlin
publishData {
    buildData()
}
```

adds `build.data` to the main resources:

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

## Multi-module builds

Every project that applies the plugin resolves its version on its own. With the same configuration
they all agree. Nothing is inherited between projects, except that `version` in the root
`gradle.properties` is the base version for all of them. Projects without the plugin keep that raw
`version`, so apply the plugin to the root as well when it should carry the resolved version too,
e.g. for aggregated javadoc or a root artifact. Give it the same channels as the modules, or it
resolves with the default channels and its version can differ from theirs.

Share the configuration through convention plugins. Keep the channels in one convention that the
root applies too, so the root resolves exactly like the modules:

`buildSrc/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("de.chojo:publishdata:2.0.0")
}
```

`buildSrc/src/main/kotlin/versioning-conventions.gradle.kts`

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

`buildSrc/src/main/kotlin/publishing-conventions.gradle.kts`

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

`build.gradle.kts` of the root

```kotlin
plugins {
    id("versioning-conventions")
}
```

`api/build.gradle.kts`

```kotlin
plugins {
    id("publishing-conventions")
}

publishData {
    artifactId = "myproject-api"
}
```

Give the build data file a name per module when modules end up in one shaded jar; otherwise every
module writes `build.data` and only one survives.

## Migrating from 1.x

| 1.x                                                       | 2.x                                                          |
|-----------------------------------------------------------|--------------------------------------------------------------|
| `version = "1.5.0"` in the build script                   | `version=1.5.0` in `gradle.properties` or `publishData.baseVersion` |
| `useEldoNexusRepos()` / `useInternalEldoNexusRepos()`     | `eldoNexus()` / `eldoNexus(internal = true)`                 |
| `main` / `master` publish releases                        | tags publish releases; `release { onTag().onBranch("main", "master") }` restores the old behaviour |
| credentials in `publishing { repositories { … } }`        | `EldoNexusUsername` / `EldoNexusPassword` gradle properties, or `publishData { repository { … } }` |
| `useGitlabReposForProject("177", url)`                    | `gitLab("177", url)`                                         |
| `addMainRepo` / `addMasterRepo` / `addDevRepo` / `addSnapshotRepo` | `channels { release(…); dev(…); snapshot(…) }` |
| `publishTask("jar")`                                      | `java` component is used by default; `artifact(tasks.named(…))` |
| `publishComponent("java")`                                | `component = "java"`                                         |
| `publishData.configurePublication(this)`                  | not needed                                                   |
| `url = uri(publishData.getRepository())`                  | not needed                                                   |
| `addBuildData(mapOf(…))`                                  | `buildData { put(…) }`                                       |
| `publishData.getVersion()`                                | `project.version`, or `./gradlew -q printVersion` in CI      |
| `-SNAPSHOT` / `-DEV` in `version`                         | still stripped, now with a warning; remove it                |
| `PATREON=true` / `KOFI=true`                              | `BUILD_TYPE=PATREON` / `BUILD_TYPE=KOFI`                     |

The CI publish task follows the repository name: with `eldoNexus()` it stays
`publishAllPublicationsToEldoNexusRepository`.
