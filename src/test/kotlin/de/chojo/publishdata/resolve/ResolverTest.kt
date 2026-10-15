package de.chojo.publishdata.resolve

import de.chojo.publishdata.Channels
import de.chojo.publishdata.git.Ci
import de.chojo.publishdata.git.GitContext
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class ResolverTest {
    private val objects = ProjectBuilder.builder().build().objects

    private fun channels(configure: Channels.() -> Unit): Channels =
        objects.newInstance(Channels::class.java).apply(configure)

    private val eldoNexusLike = channels {
        release("releases")
        dev("dev")
        snapshot("snapshots")
    }.toSpecs()

    private fun ci(branch: String? = null, vararg tags: String, changeRequest: Boolean = false) =
        GitContext(branch, tags.toList(), "0123456789abcdef", Ci.GITHUB, changeRequest, public = true)

    private fun resolve(context: GitContext, channels: List<ChannelSpec> = eldoNexusLike, base: String? = "1.5.0") =
        Resolver.resolve(context, channels, base, 7)

    @Test
    fun `tag pipeline is a release with the version of the tag`() {
        val resolution = resolve(ci(null, "v1.4.0"))
        assertThat(resolution.channel).isEqualTo("release")
        assertThat(resolution.version).isEqualTo("1.4.0")
        assertThat(resolution.repository).isEqualTo("releases")
        assertThat(resolution.tag).isEqualTo("v1.4.0")
        assertThat(resolution.reason).isEqualTo("tag 'v1.4.0' matches 'v?(?<version>\\d.*)'")
    }

    @Test
    fun `tag without base version`() {
        assertThat(resolve(ci(null, "2.0.0-rc.1"), base = null).version).isEqualTo("2.0.0-rc.1")
    }

    @Test
    fun `main branch is a snapshot unless release opts in`() {
        assertThat(resolve(ci("main")).channel).isEqualTo("snapshot")

        val channels = channels { release { onTag().onBranch("main", "master") }; snapshot() }.toSpecs()
        val resolution = resolve(ci("main"), channels)
        assertThat(resolution.channel).isEqualTo("release")
        assertThat(resolution.version).isEqualTo("1.5.0")
        assertThat(resolution.reason).isEqualTo("branch 'main' matches 'main|master'")
    }

    @Test
    fun `branch names are matched literally`() {
        val channels = channels { channel("x") { onBranch("release/1.x") } }.toSpecs()
        assertThat(resolve(ci("release/1.x"), channels).channel).isEqualTo("x")
        assertThat(resolve(ci("release/1yx"), channels).channel).isEqualTo(Resolver.NO_CHANNEL)
    }

    @Test
    fun `channel suffixes are removed from the base version`() {
        assertThat(resolve(ci("feature"), base = "1.5.0-SNAPSHOT").version).isEqualTo("1.5.0-SNAPSHOT")
        assertThat(resolve(ci("dev"), base = "1.5.0-DEV").version).isEqualTo("1.5.0-DEV")
        assertThat(Resolver.cleanBaseVersion("2.0.0-rc.1")).isEqualTo("2.0.0-rc.1")
    }

    @Test
    fun `branch suffix keeps branches apart`() {
        val channels = channels { snapshot { branchSuffix() } }.toSpecs()
        assertThat(resolve(ci("feature/JIRA-12 fix"), channels).version).isEqualTo("1.5.0-feature-JIRA-12-fix-SNAPSHOT")
        assertThat(resolve(ci("main"), channels).version).isEqualTo("1.5.0-SNAPSHOT")
        val local = GitContext("feature/x", emptyList(), null, Ci.LOCAL, false, public = false)
        assertThat(resolve(local, channels).version).isEqualTo("1.5.0-SNAPSHOT")
    }

    @Test
    fun `branch suffix exceptions are configurable`() {
        val channels = channels { snapshot { branchSuffix(listOf("develop")) } }.toSpecs()
        assertThat(resolve(ci("develop"), channels).version).isEqualTo("1.5.0-SNAPSHOT")
        assertThat(resolve(ci("main"), channels).version).isEqualTo("1.5.0-main-SNAPSHOT")
    }

    @Test
    fun `dev branch`() {
        val resolution = resolve(ci("dev/feature"))
        assertThat(resolution.version).isEqualTo("1.5.0-DEV")
        assertThat(resolution.repository).isEqualTo("dev")
    }

    @Test
    fun `other branch is a snapshot`() {
        val resolution = resolve(ci("feature/x"))
        assertThat(resolution.channel).isEqualTo("snapshot")
        assertThat(resolution.version).isEqualTo("1.5.0-SNAPSHOT")
    }

    @Test
    fun `change request from main is not a release`() {
        assertThat(resolve(ci("main", changeRequest = true)).channel).isEqualTo("snapshot")
    }

    @Test
    fun `local build never releases`() {
        val context = GitContext("main", listOf("v1.4.0"), "abc", Ci.LOCAL, false, public = false)
        val resolution = resolve(context)
        assertThat(resolution.channel).isEqualTo("snapshot")
        assertThat(resolution.reason).startsWith("local build without PUBLIC_BUILD")
        assertThat(resolution.version).isEqualTo("1.5.0-SNAPSHOT")
        assertThat(resolution.branch).isEqualTo("main")
    }

    @Test
    fun `public local build on a tagged commit releases`() {
        val context = GitContext("feature", listOf("v1.4.1"), "abc", Ci.LOCAL, false, public = true)
        assertThat(resolve(context).version).isEqualTo("1.4.1")
    }

    @Test
    fun `commit hash is shortened and appended`() {
        val channels = channels { snapshot { commitHash() } }.toSpecs()
        assertThat(resolve(ci("x"), channels).version).isEqualTo("1.5.0-SNAPSHOT+0123456")
    }

    @Test
    fun `custom tag pattern`() {
        val channels = channels { release { onTag("release-(?<version>.+)") } }.toSpecs()
        assertThat(resolve(ci(null, "release-3.1"), channels).version).isEqualTo("3.1")
        val unmatched = resolve(ci(null, "v3.1"), channels)
        assertThat(unmatched.channel).isEqualTo(Resolver.NO_CHANNEL)
        assertThat(unmatched.reason).isEqualTo("no channel matched tags [v3.1]")
    }

    @Test
    fun `without channels release and snapshot apply`() {
        val defaults = channels {}.toSpecs()
        assertThat(resolve(ci(null, "v1.0.0"), defaults).version).isEqualTo("1.0.0")
        assertThat(resolve(ci("feature"), defaults).version).isEqualTo("1.5.0-SNAPSHOT")
        assertThat(resolve(ci("feature"), defaults).repository).isNull()
    }

    @Test
    fun `missing base version is unspecified`() {
        val resolution = resolve(ci("feature"), base = null)
        assertThat(resolution.version).isEqualTo(Resolver.UNSPECIFIED)
        assertThat(resolution.hasVersion).isFalse()
    }

    @Test
    fun `dev channel matches dev branches with suffix DEV`() {
        val channels = channels { dev(); snapshot() }.toSpecs()
        assertThat(resolve(ci("Dev/next"), channels).version).isEqualTo("1.5.0-DEV")
        assertThat(resolve(ci("develop"), channels).channel).isEqualTo("dev")
        assertThat(resolve(ci("feature"), channels).channel).isEqualTo("snapshot")
    }

    @Test
    fun `channels behind a match-all channel are unreachable`() {
        val channels = channels { snapshot(); release(); channel("empty") }.toSpecs()
        assertThat(Resolver.unreachableChannels(channels).map { it.name }).containsExactly("release", "empty")
    }

    @Test
    fun `channel without matchers is unreachable`() {
        val channels = channels { release(); channel("empty"); snapshot() }.toSpecs()
        assertThat(Resolver.unreachableChannels(channels).map { it.name }).containsExactly("empty")
    }

    @Test
    fun `match-all channel without change requests leaves change requests reachable`() {
        val channels = channels {
            channel("all") { onAnyRef().excludeChangeRequests() }
            channel("requests") { onAnyRef() }
            release()
        }.toSpecs()
        assertThat(Resolver.unreachableChannels(channels).map { it.name }).containsExactly("release")
    }

    @Test
    fun `presets have no unreachable channels`() {
        assertThat(Resolver.unreachableChannels(eldoNexusLike)).isEmpty()
        assertThat(Resolver.unreachableChannels(channels {}.toSpecs())).isEmpty()
    }

    @Test
    fun `maven central target is carried into the resolution`() {
        val channels = channels { release { publishToMavenCentral() }; snapshot("snapshots") }.toSpecs()
        val release = resolve(ci(null, "v1.0.0"), channels)
        assertThat(release.mavenCentral).isTrue()
        assertThat(release.repository).isNull()
        val snapshot = resolve(ci("feature"), channels)
        assertThat(snapshot.mavenCentral).isFalse()
        assertThat(snapshot.repository).isEqualTo("snapshots")
    }
}
