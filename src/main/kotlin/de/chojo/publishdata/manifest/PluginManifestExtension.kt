/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * The `pluginManifest { }` block: what the manifest says beyond the plugin descriptor, and what of
 * the descriptor it overrides.
 */
abstract class PluginManifestExtension {
    /** The plugin's name. Defaults to `name` of the plugin descriptor. */
    abstract val name: Property<String>

    /** Where the plugin runs, such as `paper` or `folia`. Defaults to what the descriptor implies. */
    abstract val platforms: ListProperty<String>

    /** The oldest supported Minecraft version, inclusive. Defaults to the descriptor's `api-version`. */
    abstract val minecraftMin: Property<String>

    /** The newest supported Minecraft version, inclusive. Unset means no known upper bound. */
    abstract val minecraftMax: Property<String>

    /** The Java version the plugin needs. Defaults to the toolchain's, else the target compatibility. */
    abstract val java: Property<Int>

    /** Dependencies declared here, added to the descriptor's or refining one of the same name. */
    abstract val dependencies: ListProperty<ManifestDependency>

    /** Entries of your own, written under `extra`. */
    abstract val extra: MapProperty<String, String>

    init {
        platforms.convention(null as Iterable<String>?)
    }

    /** The supported Minecraft versions, both inclusive; [max] null for no known upper bound. */
    fun minecraft(
        min: String,
        max: String? = null,
    ) {
        minecraftMin.set(min)
        minecraftMax.set(max)
    }

    /** Replaces the platforms the descriptor implies. */
    fun platforms(vararg names: String) {
        platforms.set(names.toList())
    }

    /** A plugin this one cannot run without. */
    @JvmOverloads
    fun requires(
        name: String,
        min: String? = null,
        max: String? = null,
        action: Action<DependencySpec> = Action {},
    ) {
        dependencies.add(dependency(name, required = true, min, max, action))
    }

    /** A plugin this one uses when it is there. */
    @JvmOverloads
    fun optional(
        name: String,
        min: String? = null,
        max: String? = null,
        action: Action<DependencySpec> = Action {},
    ) {
        dependencies.add(dependency(name, required = false, min, max, action))
    }

    /** Adds the entry [key] with [value] to `extra`. */
    fun put(
        key: String,
        value: String,
    ) = extra.put(key, value)

    /** Adds the entry [key] with the lazily resolved [value] to `extra`. */
    fun put(
        key: String,
        value: Provider<String>,
    ) = extra.put(key, value)

    private fun dependency(
        name: String,
        required: Boolean,
        min: String?,
        max: String?,
        action: Action<DependencySpec>,
    ): ManifestDependency {
        require(name.isNotBlank()) { "A dependency needs a name" }
        val spec = DependencySpec().also(action::execute)
        return ManifestDependency(name, required, min?.ifBlank { null }, max?.ifBlank { null }, spec.links.toList())
    }
}
