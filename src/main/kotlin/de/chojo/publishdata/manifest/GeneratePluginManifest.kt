/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the plugin manifest of the build to [outputFile]. */
@CacheableTask
abstract class GeneratePluginManifest : DefaultTask() {
    /** The processed `plugin.yml` and `paper-plugin.yml`, whichever exist. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val descriptors: ConfigurableFileCollection

    @get:Input
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val pluginName: Property<String>

    @get:Input
    @get:Optional
    abstract val platforms: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val minecraftMin: Property<String>

    @get:Input
    @get:Optional
    abstract val minecraftMax: Property<String>

    @get:Input
    @get:Optional
    abstract val java: Property<Int>

    @get:Input
    abstract val dependencies: ListProperty<ManifestDependency>

    @get:Input
    abstract val extra: MapProperty<String, String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val manifest =
            PluginManifest.of(
                PluginDescriptor.read(descriptors.files),
                version.get(),
                PluginManifest.Declared(
                    name = pluginName.orNull,
                    platforms = platforms.orNull,
                    minecraftMin = minecraftMin.orNull,
                    minecraftMax = minecraftMax.orNull,
                    java = java.orNull,
                    dependencies = dependencies.get(),
                    extra = extra.get(),
                ),
            )
        if (manifest.name == null) {
            logger.warn(
                "The plugin manifest of {} has no name: there is no plugin.yml or paper-plugin.yml, and pluginManifest sets none.",
                path,
            )
        }
        outputFile.get().asFile.writeText(manifest.toJson())
    }
}
