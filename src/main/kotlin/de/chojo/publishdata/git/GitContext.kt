/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.git

import java.io.Serializable

/**
 * What is known about the commit a build runs on.
 *
 * @property branch the branch being built, the source branch for a change request, or null for tag
 * pipelines and detached checkouts
 * @property tags the tags pointing at the built commit, highest version first
 * @property commit the full commit hash, or null when unknown
 * @property ci the system the context was read from
 * @property changeRequest true for pull and merge request builds
 * @property public true when the build may produce publishable versions: always on CI, locally only
 * with `PUBLIC_BUILD=true`
 */
data class GitContext(
    val branch: String?,
    val tags: List<String>,
    val commit: String?,
    val ci: Ci,
    val changeRequest: Boolean,
    val public: Boolean,
) : Serializable

/** The source a [GitContext] was read from. */
enum class Ci {
    GENERIC,
    GITHUB,
    GITLAB,
    BITBUCKET,
    JENKINS,
    LOCAL,
}
