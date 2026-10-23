/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

/** Collects the links of one dependency in the `requires { }` and `optional { }` blocks. */
class DependencySpec internal constructor() {
    internal val links = mutableListOf<DependencyLink>()

    /** The project on Modrinth, by its slug or id. */
    fun modrinth(project: String) {
        links += DependencyLink.modrinth(project)
    }

    /** The resource on SpigotMC, by its numeric id. */
    fun spigot(resource: Int) {
        links += DependencyLink.spigot(resource)
    }

    /** The project on Hangar, as `owner/slug`. */
    fun hangar(project: String) {
        links += DependencyLink.hangar(project)
    }

    /** Any other http or https address. */
    fun link(address: String) {
        links += DependencyLink.url(address)
    }
}
