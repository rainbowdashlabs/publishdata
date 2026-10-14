package de.chojo.publishdata

import org.gradle.api.provider.Provider

/**
 * A `project.version` resolved each time it is read, so it reflects the configuration at that point
 * rather than at plugin application.
 */
internal class LazyVersion(private val version: Provider<String>) {
    override fun toString(): String = version.get()
}
