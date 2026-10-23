---
title: Migrating from 1.x
sidebar_position: 12
---

# Migrating from 1.x


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

The task that publishes to the repository follows its name: with `eldoNexus()` it stays
`publishAllPublicationsToEldoNexusRepository`. Prefer [`publishChannel`](./publishing.md#publishchannel), which
publishes only to the targets of the resolved channel.
