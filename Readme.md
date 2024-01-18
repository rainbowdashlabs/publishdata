![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/RainbowDashLabs/publishdata/verify.yml?style=for-the-badge&label=Building)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/RainbowDashLabs/publishdata/publish_to_nexus.yml?style=for-the-badge&label=Publishing) \
![Sonatype Nexus (Releases)](https://img.shields.io/nexus/maven-releases/de.chojo/publishdata?label=Release&logo=Release&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)
![Sonatype Nexus (Development)](https://img.shields.io/nexus/maven-dev/de.chojo/publishdata?label=DEV&logo=Release&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/de.chojo/publishdata?color=orange&label=Snapshot&server=https%3A%2F%2Feldonexus.de&style=for-the-badge)

Small gradle plugin which I use to avoid boilerplate code for version detection.

This util helps to publish artifacts, sets the correction version with optional commit hash and branch identifier and
chooses the repository based on the current branch.

## Setup

### settings.gradle.kts

Add the eldonexus as a plugin repository

```kotlin
pluginManagement {
    repositories {
        maven("https://eldonexus.de/repository/maven-public/")
    }
}
```

### build.gradle.kts

#### General

Apply the publishdata as a plugin
```
plugins {
    id("de.chojo.publishdata") version "version"
}
```

Basic configuration which tasks you want to publish
```kt
publishData {
    // Add tasks which should be published
    publishTask("jar")
    publishTask("sourcesJar")
    publishTask("javadocJar")
}
```

#### Adding repositories
You can define repositories like that:
```kt
publishData {
    // manually register a release repo for main branch
    addMainRepo("https://my-repo.com/releases")
    // manually register a snapshot repo which will append -SNAPSHOT+<commit_hash>
    addSnapshotRepo("https://my-repo.com/snapshots")
}

publishing {
    publications.create<MavenPublication>("maven") {
        // configure the publication as defined previously.
        publishData.configurePublication(this)
    }

    repositories {
        maven {
            authentication {
                // Auth for the repository
                // ....
            }

            name = "MyRepo"
            // Get the detected repository from the publishData
            url = uri(publishData.getRepository())
        }
    }
}
```

Make sure to declare them in the correct order. PublishData will take the first applicable repository. 


#### Eldonexus

Additionaly call this in the configuration to publish to the eldonexus.
This will preconfigure for main, master, dev and snapshot branches.

```kt
publishData {
    // only if you want to publish to the eldonexus. If you call this you will not need to manually add repositories
    useEldoNexusRepos()
}
```

#### GitLab
Additionaly call this in the configuration to publish to a gitlab repository and replace your repository declaration.

```kt
publishData {
    // only if you want to publish to the gitlab. If you call this you will not need to manually add repositories
    useGitlabReposForProject("177", "https://gitlab.example.com/")
}

publishing {
    publications.create<MavenPublication>("maven") {
        // configure the publication as defined previously.
        publishData.configurePublication(this)
    }

    repositories {
        maven {
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
            
            name = "Gitlab"
            // Get the detected repository from the publish data
            url = uri(publishData.getRepository())
        }
    }
}
```
