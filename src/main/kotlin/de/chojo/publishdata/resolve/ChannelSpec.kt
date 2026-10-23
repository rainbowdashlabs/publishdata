/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.resolve

import java.io.Serializable

/**
 * The resolved configuration of one release channel.
 *
 * @property name the channel name, written into build data
 * @property matchers the channel applies when any of them matches
 * @property suffix appended to the version as `-suffix` when not blank
 * @property commitHash appends `+<short commit>` to the version
 * @property branchSuffix inserts the sanitised branch name before [suffix], except for the branches
 * in [branchSuffixExceptions] and for builds without a public branch
 * @property branchSuffixExceptions branches that never get a branch suffix
 * @property repository the repository to publish to, or null
 * @property mavenCentral publishes to Maven Central through `com.vanniktech.maven.publish`
 * @property includeChangeRequests whether pull and merge request builds may use this channel
 */
data class ChannelSpec(
    val name: String,
    val matchers: List<RefMatcher>,
    val suffix: String,
    val commitHash: Boolean,
    val branchSuffix: Boolean,
    val branchSuffixExceptions: List<String>,
    val repository: String?,
    val mavenCentral: Boolean,
    val includeChangeRequests: Boolean,
) : Serializable
