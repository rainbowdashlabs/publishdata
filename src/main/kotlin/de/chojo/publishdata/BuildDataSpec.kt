/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/** Configuration of the build data file added to the main resources. */
abstract class BuildDataSpec {
    /** Name of the file in the resource root. Defaults to `build.data`. */
    abstract val fileName: Property<String>

    /**
     * Whether `time` and `unix` are written. Defaults to true. Without them the file depends only on
     * its inputs, so the task is cacheable and the artifact reproducible.
     */
    abstract val includeTime: Property<Boolean>

    /** Additional entries, written after the built-in ones. */
    abstract val entries: MapProperty<String, String>

    init {
        fileName.convention("build.data")
        includeTime.convention(true)
    }

    /** Adds the entry [key] with [value]. */
    fun put(
        key: String,
        value: String,
    ) = entries.put(key, value)

    /** Adds the entry [key] with the lazily resolved [value]. */
    fun put(
        key: String,
        value: Provider<String>,
    ) = entries.put(key, value)
}
