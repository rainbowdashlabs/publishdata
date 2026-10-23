package de.chojo.publishdata.resolve

import de.chojo.publishdata.git.GitContext

/**
 * Selects the channel of a build and derives its version.
 *
 * Channels are tried in order and the first match wins. A build that is not public is matched as
 * if it ran on the branch [LOCAL_BRANCH] without tags, so it can never produce a release.
 *
 * A build that needs a base version but has none gets [UNSPECIFIED], Gradle's own placeholder,
 * instead of failing: Gradle reads `project.version` during configuration on its own, and a failure
 * there would break builds that never publish.
 */
object Resolver {
    /** The branch name non-public builds are matched with. */
    const val LOCAL_BRANCH = "local"

    /** The channel name of a build no channel matched. */
    const val NO_CHANNEL = "none"

    /** The version of a build without a base version, matching Gradle's default. */
    const val UNSPECIFIED = "unspecified"

    private val CHANNEL_SUFFIXES = Regex("(-SNAPSHOT|-DEV)+$", RegexOption.IGNORE_CASE)
    private val BRANCH_SUFFIX_CHARACTERS = Regex("[^A-Za-z0-9._]+")

    /**
     * Removes channel suffixes from a base version: those are added by the channels, and keeping them
     * would produce `1.0.0-SNAPSHOT-SNAPSHOT`.
     */
    fun cleanBaseVersion(baseVersion: String): String = baseVersion.replace(CHANNEL_SUFFIXES, "")

    /**
     * Resolves [context] against [channels].
     *
     * @param baseVersion the version of builds that do not take it from a tag; cleaned with
     * [cleanBaseVersion]
     * @param hashLength the length commit hashes are shortened to
     */
    fun resolve(context: GitContext, channels: List<ChannelSpec>, baseVersion: String?, hashLength: Int): Resolution {
        val subject = if (context.public) context else context.copy(branch = LOCAL_BRANCH, tags = emptyList())
        val commit = context.commit?.take(hashLength)
        val base = baseVersion?.let(::cleanBaseVersion)?.takeIf { it.isNotBlank() && it != UNSPECIFIED }
        val visibility =
            if (context.public) "" else "local build without PUBLIC_BUILD, matched as branch '$LOCAL_BRANCH': "
        for (channel in channels) {
            if (subject.changeRequest && !channel.includeChangeRequests) continue
            val match = channel.matchers.firstNotNullOfOrNull { it.match(subject) } ?: continue
            val publicBranch = context.branch.takeIf { context.public && match.tag == null }
            val version = (match.tagVersion ?: base)?.let { decorate(it, channel, publicBranch, commit) } ?: UNSPECIFIED
            return Resolution(
                channel = channel.name,
                version = version,
                repository = channel.repository,
                mavenCentral = channel.mavenCentral,
                branch = context.branch,
                tag = match.tag,
                commit = commit,
                ci = context.ci,
                public = context.public,
                reason = visibility + match.reason,
            )
        }
        return Resolution(
            channel = NO_CHANNEL,
            version = base ?: UNSPECIFIED,
            repository = null,
            mavenCentral = false,
            branch = context.branch,
            tag = null,
            commit = commit,
            ci = context.ci,
            public = context.public,
            reason = visibility + "no channel matched " + describe(subject),
        )
    }

    /**
     * Returns the channels no build can reach: those without matchers, and those behind a channel
     * that matches every build, unless they serve change requests the earlier channel excludes.
     */
    fun unreachableChannels(channels: List<ChannelSpec>): List<ChannelSpec> {
        var catchesAllBuilds = false
        var catchesAllButChangeRequests = false
        return channels.filter { channel ->
            val unreachable = channel.matchers.isEmpty() ||
                catchesAllBuilds ||
                (catchesAllButChangeRequests && !channel.includeChangeRequests)
            if (AnyRefMatcher in channel.matchers) {
                if (channel.includeChangeRequests) catchesAllBuilds = true else catchesAllButChangeRequests = true
            }
            unreachable
        }
    }

    private fun decorate(base: String, channel: ChannelSpec, branch: String?, commit: String?): String = buildString {
        append(base)
        if (channel.branchSuffix && branch != null && branch !in channel.branchSuffixExceptions) {
            append('-').append(branch.replace(BRANCH_SUFFIX_CHARACTERS, "-").trim('-'))
        }
        if (channel.suffix.isNotBlank()) append('-').append(channel.suffix)
        if (channel.commitHash && commit != null) append('+').append(commit)
    }

    private fun describe(context: GitContext): String = buildList {
        context.branch?.let { add("branch '$it'") }
        if (context.tags.isNotEmpty()) add("tags ${context.tags}")
        if (context.changeRequest) add("as a change request")
    }.ifEmpty { listOf("a build without branch or tag") }.joinToString(", ")
}
