/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

/**
 * What a `plugin.yml` or `paper-plugin.yml` says about a plugin, as far as the manifest needs it.
 *
 * @property platforms `bukkit` for a `plugin.yml`, `paper` for a `paper-plugin.yml`, and `folia`
 *   when either says `folia-supported: true`
 * @property apiVersion `api-version`, the oldest Minecraft version the plugin is built for
 */
data class PluginDescriptor(
    val name: String?,
    val apiVersion: String?,
    val platforms: List<String>,
    val dependencies: List<ManifestDependency>,
) {
    companion object {
        /**
         * Reads the descriptors among [files]. A `paper-plugin.yml` wins for the name, the API version
         * and its dependencies; a `plugin.yml` beside it adds `bukkit`. Null when neither exists.
         */
        fun read(files: Collection<File>): PluginDescriptor? {
            val bukkit = files.firstOrNull { it.name == "plugin.yml" && it.isFile }?.let(::load)
            val paper = files.firstOrNull { it.name == "paper-plugin.yml" && it.isFile }?.let(::load)
            if (bukkit == null && paper == null) return null
            val primary = paper ?: bukkit!!
            val platforms =
                buildList {
                    if (bukkit != null) add("bukkit")
                    if (paper != null) add("paper")
                    if (listOfNotNull(bukkit, paper).any { it["folia-supported"] == true }) add("folia")
                }
            return PluginDescriptor(
                name = primary["name"]?.toString(),
                apiVersion = primary["api-version"]?.toString(),
                platforms = platforms,
                dependencies = if (paper != null) paperDependencies(paper) else bukkitDependencies(bukkit!!),
            )
        }

        private fun load(file: File): Map<String, Any?> {
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            @Suppress("UNCHECKED_CAST")
            return file.reader().use { yaml.load<Any?>(it) } as? Map<String, Any?> ?: emptyMap()
        }

        private fun bukkitDependencies(yaml: Map<String, Any?>): List<ManifestDependency> =
            names(yaml["depend"]).map { ManifestDependency(it, required = true) } +
                names(yaml["softdepend"]).map { ManifestDependency(it, required = false) }

        /** `dependencies.server` of a `paper-plugin.yml`, where `required` defaults to true. */
        private fun paperDependencies(yaml: Map<String, Any?>): List<ManifestDependency> {
            val server = (yaml["dependencies"] as? Map<*, *>)?.get("server") as? Map<*, *> ?: return emptyList()
            return server.map { (name, settings) ->
                ManifestDependency(name.toString(), required = (settings as? Map<*, *>)?.get("required") != false)
            }
        }

        private fun names(value: Any?): List<String> = (value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    }
}
