/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Writes a manifest of the plugin a build makes - its Minecraft versions, platforms, Java version and
 * dependencies - and publishes it beside the jar with the classifier `manifest`, so a repository or
 * a download site can read what a build needs without opening it.
 *
 * Fills what it can from the processed `plugin.yml` or `paper-plugin.yml`; `pluginManifest { }` adds
 * what those cannot say and overrides the rest.
 */
class PluginManifestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val declared = project.extensions.create(EXTENSION, PluginManifestExtension::class.java)
        val task =
            project.tasks.register(TASK, GeneratePluginManifest::class.java) {
                group = "build"
                description = "Writes the plugin manifest published beside the jar."
                version.set(project.provider { project.version.toString() })
                pluginName.set(declared.name)
                platforms.set(declared.platforms)
                minecraftMin.set(declared.minecraftMin)
                minecraftMax.set(declared.minecraftMax)
                java.set(declared.java)
                dependencies.set(declared.dependencies)
                extra.set(declared.extra)
                outputFile.set(project.layout.buildDirectory.file("pluginManifest/manifest.json"))
            }
        project.pluginManager.withPlugin("java") {
            val processResources = project.tasks.named("processResources", ProcessResources::class.java)
            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            task.configure {
                descriptors.from(
                    project.files(processResources).asFileTree.matching { include("plugin.yml", "paper-plugin.yml") },
                )
                java.set(
                    declared.java
                        .orElse(javaExtension.toolchain.languageVersion.map { it.asInt() })
                        .orElse(project.provider { javaExtension.targetCompatibility.majorVersion.toInt() }),
                )
            }
        }
        project.pluginManager.withPlugin("maven-publish") {
            project.extensions
                .getByType(PublishingExtension::class.java)
                .publications
                .withType(MavenPublication::class.java)
                .matching { !it.name.endsWith("PluginMarkerMaven") }
                .configureEach {
                    artifact(task.flatMap { it.outputFile }) {
                        classifier = CLASSIFIER
                        extension = "json"
                    }
                }
        }
    }

    companion object {
        const val EXTENSION = "pluginManifest"
        const val TASK = "generatePluginManifest"
        const val CLASSIFIER = "manifest"
    }
}
