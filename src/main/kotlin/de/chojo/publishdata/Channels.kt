/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata

import de.chojo.publishdata.resolve.AnyRefMatcher
import de.chojo.publishdata.resolve.BranchMatcher
import de.chojo.publishdata.resolve.ChannelSpec
import de.chojo.publishdata.resolve.TagMatcher
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * The ordered release channels of a project. The first channel matching a build is used.
 *
 * Without any channel, a `release` channel and a `snapshot` channel without repositories apply, so
 * the version is set even when nothing is published.
 */
abstract class Channels
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) {
        private val channels = mutableListOf<Channel>()

        /**
         * Adds the `release` channel. Without matchers it applies to tags, taking the version from the
         * tag. Branches release only when added with [Channel.onBranch]. Pull and merge request builds
         * are excluded.
         */
        @JvmOverloads
        fun release(
            repository: String? = null,
            action: Action<Channel> = Action {},
        ): Channel = add(releaseChannel(repository), action)

        /** Adds the `snapshot` channel. Without matchers it applies to every build, with suffix `SNAPSHOT`. */
        @JvmOverloads
        fun snapshot(
            repository: String? = null,
            action: Action<Channel> = Action {},
        ): Channel = add(snapshotChannel(repository), action)

        /**
         * Adds the `dev` channel. Without matchers it applies to branches starting with `dev`, ignoring
         * case, with suffix `DEV`.
         */
        @JvmOverloads
        fun dev(
            repository: String? = null,
            action: Action<Channel> = Action {},
        ): Channel =
            add(
                newChannel("dev")
                    .setup(repository, listOf(BranchMatcher(DEV_BRANCHES)), includeChangeRequests = true)
                    .suffix("DEV"),
                action,
            )

        /** Adds a channel named [name]. It applies to nothing until it gets a matcher. */
        @JvmOverloads
        fun channel(
            name: String,
            repository: String? = null,
            action: Action<Channel> = Action {},
        ): Channel = add(newChannel(name).setup(repository, emptyList(), includeChangeRequests = true), action)

        /** Removes all channels, including those added by a preset. */
        fun clear() = channels.clear()

        internal fun toSpecs(): List<ChannelSpec> =
            channels.ifEmpty { listOf(releaseChannel(null), snapshotChannel(null)) }.map(Channel::toSpec)

        private fun add(
            channel: Channel,
            action: Action<Channel>,
        ): Channel {
            action.execute(channel)
            channels += channel
            return channel
        }

        private fun releaseChannel(repository: String?) =
            newChannel("release")
                .setup(repository, listOf(TagMatcher(TagMatcher.DEFAULT_PATTERN)), includeChangeRequests = false)

        private fun snapshotChannel(repository: String?) =
            newChannel(
                "snapshot",
            ).setup(repository, listOf(AnyRefMatcher), includeChangeRequests = true).suffix("SNAPSHOT")

        private fun newChannel(name: String): Channel = objects.newInstance(Channel::class.java, name)

        private companion object {
            const val DEV_BRANCHES = "(?i)dev.*"
        }
    }
