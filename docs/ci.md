---
title: CI
sidebar_position: 3
---

# CI

PublishData needs the branch, the tag and the commit of a build. It reads them from the first source that has them:

1. `PUBLISHDATA_REF` and `PUBLISHDATA_COMMIT`, for any CI.
2. GitHub Actions.
3. GitLab CI.
4. Bitbucket Pipelines.
5. Jenkins.
6. The local `git`.

Builds found through 1 to 5 are CI builds. Builds that use the local `git` are [local builds](./versioning.md#local-builds-never-release).

## Any CI

Set `PUBLISHDATA_REF` to the ref of the build:

| Value | Build |
|---|---|
| `refs/tags/v1.2.3` | tag `v1.2.3` |
| `refs/heads/main` | branch `main` |
| `main` | branch `main` |

`PUBLISHDATA_COMMIT` optionally gives the commit hash, for [`commitHash()`](./channels.md#matchers-and-options) and
the [build data](./build-data.md).

This also helps to check your setup locally:

```bash
PUBLISHDATA_REF=refs/tags/v1.5.0 ./gradlew -q printVersion
PUBLISHDATA_REF=refs/heads/main ./gradlew publishChannel --dry-run
```

## GitHub Actions

Tags, branches and pull requests are recognised. A pull request build counts as a build of its head branch.

A workflow that publishes every push to `main` and every tag:

```yaml
name: Publish

on:
  push:
    branches: [main]
    tags: ["v*"]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: 25
      - uses: gradle/actions/setup-gradle@v6
      - run: ./gradlew publishChannel
        env:
          REPO_USER: ${{ secrets.REPO_USER }}
          REPO_PASSWORD: ${{ secrets.REPO_PASSWORD }}
```

## GitLab CI

Tag pipelines, branch pipelines and merge request pipelines are recognised. A merge request pipeline counts as a
build of its source branch.

## Bitbucket Pipelines

Tags, branches and pull requests are recognised.

## Jenkins

Multibranch pipelines are recognised with their tags, branches and change requests. A change request counts as a
build of its source branch. Other jobs use `GIT_BRANCH` without the remote name.

## Local git

Without a known CI, the plugin asks the local `git` for the branch, the tags pointing at `HEAD` and the commit. A
directory that is not a git repository gives a build without branch, tag and commit.
