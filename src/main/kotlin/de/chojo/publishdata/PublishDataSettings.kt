package de.chojo.publishdata

import org.gradle.api.Action

/**
 * The `publishData` extension of the settings plugin: configuration shared by every project of the
 * build.
 */
abstract class PublishDataSettings {
    internal val defaults = mutableListOf<Action<in PublishDataExtension>>()

    /**
     * Configures the `publishData` extension of every project, before its build script runs. A
     * build script can add to it or override it, e.g. with `channels { clear() }`.
     */
    fun defaults(action: Action<in PublishDataExtension>) {
        defaults += action
    }
}
