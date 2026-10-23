/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.manifest

import java.io.Serializable

/**
 * A plugin a build needs or can use.
 *
 * @property min the lowest version it works with, inclusive, or null when any does
 * @property max the highest version it works with, inclusive, or null when there is no known bound
 */
data class ManifestDependency(
    val name: String,
    val required: Boolean,
    val min: String? = null,
    val max: String? = null,
    val links: List<DependencyLink> = emptyList(),
) : Serializable {
    /** This dependency with what [refinement] says on top: its requirement, and versions and links it sets. */
    fun refinedBy(refinement: ManifestDependency) =
        copy(
            required = refinement.required,
            min = refinement.min ?: min,
            max = refinement.max ?: max,
            links = links + refinement.links.filter { it !in links },
        )

    private companion object {
        private const val serialVersionUID = 1L
    }
}
