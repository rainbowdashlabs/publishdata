---
title: Init scripts
sidebar_position: 11
---

# Init scripts

An [init script](https://docs.gradle.org/current/userguide/init_scripts.html) can apply PublishData to builds that
do not declare it, for example on a CI agent:

```kotlin
initscript {
    repositories { maven("https://eldonexus.de/repository/maven-public/") }
    dependencies { classpath("de.chojo:publishdata:2.2.0") }
}

gradle.beforeProject {
    apply<de.chojo.publishdata.PublishDataPlugin>()
    extensions.configure<de.chojo.publishdata.PublishDataExtension> {
        buildData()
    }
}
```

Configure the plugin inside the init script. The build scripts cannot see its classes.

Apply it either from the init script or from the build, not both. The init script loads its own copy of the plugin,
two copies cannot share one project, and the build stops with an explanation.
