/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.git

/**
 * Reads the [GitContext] of a local working copy.
 *
 * @param git runs `git` with the given arguments and returns its trimmed output, or null when the
 * command failed or printed nothing
 */
internal class LocalGit(
    private val git: (List<String>) -> String?,
) {
    /** Reads branch, tags and commit of `HEAD`. A missing repository yields an empty context. */
    fun detect(public: Boolean): GitContext =
        GitContext(
            branch = git(listOf("symbolic-ref", "--short", "-q", "HEAD")),
            tags =
                git(listOf("tag", "--points-at", "HEAD", "--sort=-v:refname"))
                    ?.lines()
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    .orEmpty(),
            commit = git(listOf("rev-parse", "HEAD")),
            ci = Ci.LOCAL,
            changeRequest = false,
            public = public,
        )
}
