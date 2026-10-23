/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

/**
 * The manifest of one build: what a plugin descriptor says, with what the build script declares on
 * top. The build script wins wherever it says something.
 */
data class PluginManifest(
    val name: String?,
    val version: String,
    val platforms: List<String>,
    val minecraftMin: String?,
    val minecraftMax: String?,
    val java: Int?,
    val dependencies: List<ManifestDependency>,
    val extra: Map<String, String>,
) {
    /** The manifest as JSON, in the field order readers see first what they look for most. */
    fun toJson(): String =
        ManifestJson.write(
            linkedMapOf(
                "schema" to SCHEMA,
                "name" to name,
                "version" to version,
                "platforms" to platforms,
                "minecraft" to linkedMapOf("min" to minecraftMin, "max" to minecraftMax),
                "java" to java,
                "dependencies" to
                    dependencies.map { dependency ->
                        linkedMapOf(
                            "name" to dependency.name,
                            "required" to dependency.required,
                            "min" to dependency.min,
                            "max" to dependency.max,
                            "links" to
                                dependency.links.map { linkedMapOf("type" to it.type, "id" to it.id, "url" to it.url) },
                        )
                    },
                "extra" to extra,
            ),
        ) + "\n"

    companion object {
        /** Raised when a field changes meaning, so a reader can tell a manifest it cannot read. */
        const val SCHEMA = 1

        /**
         * Joins [descriptor] and what the build script [declared]. A declared dependency refines the
         * descriptor's of the same name, compared without case, and is added after them otherwise.
         */
        fun of(
            descriptor: PluginDescriptor?,
            version: String,
            declared: Declared,
        ): PluginManifest {
            val dependencies = (descriptor?.dependencies ?: emptyList()).toMutableList()
            for (dependency in declared.dependencies) {
                val index = dependencies.indexOfFirst { it.name.equals(dependency.name, ignoreCase = true) }
                if (index < 0) {
                    dependencies += dependency
                } else {
                    dependencies[index] = dependencies[index].refinedBy(dependency)
                }
            }
            return PluginManifest(
                name = declared.name ?: descriptor?.name,
                version = version,
                platforms = declared.platforms ?: descriptor?.platforms ?: emptyList(),
                minecraftMin = declared.minecraftMin ?: descriptor?.apiVersion,
                minecraftMax = declared.minecraftMax,
                java = declared.java,
                dependencies = dependencies,
                extra = declared.extra,
            )
        }
    }

    /** What the build script declared; null wherever it said nothing. */
    data class Declared(
        val name: String? = null,
        val platforms: List<String>? = null,
        val minecraftMin: String? = null,
        val minecraftMax: String? = null,
        val java: Int? = null,
        val dependencies: List<ManifestDependency> = emptyList(),
        val extra: Map<String, String> = emptyMap(),
    )
}
