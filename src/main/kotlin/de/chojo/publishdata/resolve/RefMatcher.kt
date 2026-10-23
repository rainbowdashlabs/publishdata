/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.resolve

import de.chojo.publishdata.git.GitContext
import java.io.Serializable

/** Decides whether a channel applies to a [GitContext]. */
sealed interface RefMatcher : Serializable {
    /** Returns the match against [context], or null when it does not apply. */
    fun match(context: GitContext): RefMatch?
}

/**
 * A successful [RefMatcher] result.
 *
 * @property tag the matched tag, null for branch matches
 * @property tagVersion the version taken from [tag], null for branch matches
 * @property reason why the matcher applied, for logging
 */
data class RefMatch(
    val tag: String?,
    val tagVersion: String?,
    val reason: String,
)

/**
 * Matches the first tag of the build that fully matches [pattern]. The version is the named group
 * `version` when the pattern declares one, the whole tag otherwise.
 */
data class TagMatcher(
    val pattern: String,
) : RefMatcher {
    override fun match(context: GitContext): RefMatch? {
        val regex = Regex(pattern)
        val hasVersionGroup = "(?<version>" in pattern
        return context.tags.firstNotNullOfOrNull { tag ->
            regex.matchEntire(tag)?.let { result ->
                val version = if (hasVersionGroup) result.groups["version"]?.value ?: tag else tag
                RefMatch(tag, version, "tag '$tag' matches '$pattern'")
            }
        }
    }

    companion object {
        /** Accepts `1.2.3` and `v1.2.3` with any suffix, and yields the version without the `v`. */
        const val DEFAULT_PATTERN = """v?(?<version>\d.*)"""
    }
}

/** Matches when the branch name fully matches [pattern]. */
data class BranchMatcher(
    val pattern: String,
) : RefMatcher {
    override fun match(context: GitContext): RefMatch? =
        context.branch
            ?.takeIf { Regex(pattern).matches(it) }
            ?.let { RefMatch(null, null, "branch '$it' matches '$pattern'") }

    companion object {
        private val SPECIAL = Regex("""[\\^$.|?*+()\[\]{}]""")

        /** Matches exactly one of [names]. */
        fun anyOf(vararg names: String) = BranchMatcher(names.joinToString("|", transform = ::escape))

        private fun escape(name: String) = SPECIAL.replace(name) { "\\" + it.value }
    }
}

/** Matches every build. */
data object AnyRefMatcher : RefMatcher {
    override fun match(context: GitContext): RefMatch = RefMatch(null, null, "the channel matches every build")
}
