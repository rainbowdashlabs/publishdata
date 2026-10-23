/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata

import de.chojo.publishdata.resolve.AnyRefMatcher
import de.chojo.publishdata.resolve.BranchMatcher
import de.chojo.publishdata.resolve.ChannelSpec
import de.chojo.publishdata.resolve.RefMatcher
import de.chojo.publishdata.resolve.TagMatcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * A release channel: which builds it applies to, how their version looks and where they go.
 *
 * A channel without explicit matchers uses the defaults of its kind, see [Channels].
 */
abstract class Channel
    @Inject
    constructor(
        val name: String,
    ) {
        private var repository: String? = null
        private var defaultMatchers = emptyList<RefMatcher>()
        private var includeChangeRequests = true
        private val matchers = mutableListOf<RefMatcher>()
        private var suffix = ""
        private var commitHash = false
        private var mavenCentral = false
        private var branchSuffix = false
        private var branchSuffixExceptions = emptyList<String>()

        /** Applies to builds of a tag matching [pattern]; the version is taken from the tag. */
        @JvmOverloads
        fun onTag(pattern: String = TagMatcher.DEFAULT_PATTERN): Channel = apply { matchers += TagMatcher(pattern) }

        /** Applies to builds of any of the branches [names]. */
        fun onBranch(vararg names: String): Channel = apply { matchers += BranchMatcher.anyOf(*names) }

        /** Applies to builds of branches fully matching [pattern]. */
        fun onBranch(pattern: Regex): Channel = apply { matchers += BranchMatcher(pattern.pattern) }

        /** Applies to builds of branches fully matching [pattern], e.g. a Groovy pattern literal. */
        fun onBranch(pattern: Pattern): Channel = apply { matchers += BranchMatcher(pattern.pattern()) }

        /** Applies to every build. */
        fun onAnyRef(): Channel = apply { matchers += AnyRefMatcher }

        /** Appends `-[value]` to the version. */
        fun suffix(value: String): Channel = apply { suffix = value }

        /** Appends `+<short commit hash>` to the version. */
        fun commitHash(): Channel = apply { commitHash = true }

        /**
         * Inserts the branch name before the suffix, e.g. `1.5.0-feature-x-SNAPSHOT`, so branches do not
         * overwrite each other's artifacts. Characters other than letters, digits, `.` and `_` become `-`.
         * Tag builds, local builds without `PUBLIC_BUILD` and the branches in [except] keep the plain
         * version.
         */
        @JvmOverloads
        fun branchSuffix(except: List<String> = listOf("main", "master")): Channel =
            apply {
                branchSuffix = true
                branchSuffixExceptions = except.toList()
            }

        /** Lets pull and merge request builds use this channel. */
        fun includeChangeRequests(): Channel = apply { includeChangeRequests = true }

        /** Keeps pull and merge request builds out of this channel. */
        fun excludeChangeRequests(): Channel = apply { includeChangeRequests = false }

        /**
         * Publishes the builds of this channel to Maven Central, in addition to the channel's repository
         * if it has one. Requires the plugin `com.vanniktech.maven.publish`, which is configured as
         * usual in its `mavenPublishing` block; the `publishChannel` task runs its
         * `publishToMavenCentral`.
         */
        fun publishToMavenCentral(): Channel = apply { mavenCentral = true }

        internal fun setup(
            repository: String?,
            defaultMatchers: List<RefMatcher>,
            includeChangeRequests: Boolean,
        ): Channel =
            apply {
                this.repository = repository
                this.defaultMatchers = defaultMatchers
                this.includeChangeRequests = includeChangeRequests
            }

        internal fun toSpec() =
            ChannelSpec(
                name = name,
                matchers = matchers.ifEmpty { defaultMatchers }.toList(),
                suffix = suffix,
                commitHash = commitHash,
                branchSuffix = branchSuffix,
                branchSuffixExceptions = branchSuffixExceptions,
                repository = repository,
                mavenCentral = mavenCentral,
                includeChangeRequests = includeChangeRequests,
            )
    }
