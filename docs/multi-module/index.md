---
title: Multi-module builds
sidebar_position: 1
---

# Multi-module builds

Every project that applies PublishData resolves its version on its own. With the same configuration they all agree.

Nothing is inherited between projects, with one exception: `version` in the root `gradle.properties` is the base
version for all of them.

Projects without the plugin keep that raw `version`. Apply the plugin to the root as well when the root should carry
the resolved version too, for example for aggregated Javadoc or a root artifact. Give it the same channels as the
modules. Otherwise it resolves with the default channels, and its version can differ from theirs.

There are two ways to share the configuration:

- the [settings plugin](./settings-plugin.md), which configures every project from `settings.gradle.kts`,
- [convention plugins](./convention-plugins.md) in `buildSrc`.

Use one of them, not both.
