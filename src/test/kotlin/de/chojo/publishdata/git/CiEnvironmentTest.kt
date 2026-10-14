package de.chojo.publishdata.git

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CiEnvironmentTest {
    private fun detect(vararg env: Pair<String, String>) = CiEnvironment.detect(mapOf(*env)::get)

    @Test
    fun `unknown environment is not a CI`() {
        assertThat(detect("PATH" to "/bin")).isNull()
    }

    @Test
    fun `github tag build`() {
        val context = detect(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF" to "refs/tags/v1.2.3",
            "GITHUB_REF_TYPE" to "tag",
            "GITHUB_REF_NAME" to "v1.2.3",
            "GITHUB_SHA" to "abcdef0123456789",
        )
        assertThat(context).isEqualTo(GitContext(null, listOf("v1.2.3"), "abcdef0123456789", Ci.GITHUB, false, true))
    }

    @Test
    fun `github branch build`() {
        val context = detect("GITHUB_ACTIONS" to "true", "GITHUB_REF_TYPE" to "branch", "GITHUB_REF_NAME" to "dev/next")
        assertThat(context).isEqualTo(GitContext("dev/next", emptyList(), null, Ci.GITHUB, false, true))
    }

    @Test
    fun `github pull request uses the head branch`() {
        val context = detect(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF_TYPE" to "branch",
            "GITHUB_REF_NAME" to "12/merge",
            "GITHUB_HEAD_REF" to "feature/x",
        )
        assertThat(context?.branch).isEqualTo("feature/x")
        assertThat(context?.changeRequest).isTrue()
    }

    @Test
    fun `gitlab tag pipeline`() {
        val context = detect("GITLAB_CI" to "true", "CI_COMMIT_TAG" to "1.4.0", "CI_COMMIT_SHA" to "0123")
        assertThat(context).isEqualTo(GitContext(null, listOf("1.4.0"), "0123", Ci.GITLAB, false, true))
    }

    @Test
    fun `gitlab branch pipeline`() {
        val context = detect("GITLAB_CI" to "true", "CI_COMMIT_BRANCH" to "main")
        assertThat(context).isEqualTo(GitContext("main", emptyList(), null, Ci.GITLAB, false, true))
    }

    @Test
    fun `gitlab merge request pipeline`() {
        val context = detect("GITLAB_CI" to "true", "CI_MERGE_REQUEST_SOURCE_BRANCH_NAME" to "fix/y")
        assertThat(context).isEqualTo(GitContext("fix/y", emptyList(), null, Ci.GITLAB, true, true))
    }

    @Test
    fun `generic ref takes precedence`() {
        val context = detect(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF_NAME" to "main",
            "PUBLISHDATA_REF" to "refs/tags/v2.0.0",
            "PUBLISHDATA_COMMIT" to "feed",
        )
        assertThat(context).isEqualTo(GitContext(null, listOf("v2.0.0"), "feed", Ci.GENERIC, false, true))
    }

    @Test
    fun `generic plain ref is a branch`() {
        assertThat(detect("PUBLISHDATA_REF" to "refs/heads/main")?.branch).isEqualTo("main")
        assertThat(detect("PUBLISHDATA_REF" to "release")?.branch).isEqualTo("release")
    }

    @Test
    fun `bitbucket tag pipeline`() {
        val context = detect("BITBUCKET_COMMIT" to "abc", "BITBUCKET_TAG" to "v1.0.0")
        assertThat(context).isEqualTo(GitContext(null, listOf("v1.0.0"), "abc", Ci.BITBUCKET, false, true))
    }

    @Test
    fun `bitbucket pull request pipeline`() {
        val context = detect("BITBUCKET_COMMIT" to "abc", "BITBUCKET_BRANCH" to "feature/x", "BITBUCKET_PR_ID" to "7")
        assertThat(context).isEqualTo(GitContext("feature/x", emptyList(), "abc", Ci.BITBUCKET, true, true))
    }

    @Test
    fun `jenkins multibranch tag build`() {
        val context = detect("JENKINS_URL" to "https://ci", "TAG_NAME" to "v1.0.0", "BRANCH_NAME" to "v1.0.0", "GIT_COMMIT" to "abc")
        assertThat(context).isEqualTo(GitContext(null, listOf("v1.0.0"), "abc", Ci.JENKINS, false, true))
    }

    @Test
    fun `jenkins multibranch pull request uses the source branch`() {
        val context = detect("JENKINS_URL" to "https://ci", "BRANCH_NAME" to "PR-3", "CHANGE_ID" to "3", "CHANGE_BRANCH" to "fix/y")
        assertThat(context).isEqualTo(GitContext("fix/y", emptyList(), null, Ci.JENKINS, true, true))
    }

    @Test
    fun `jenkins plain job strips the remote`() {
        val context = detect("JENKINS_URL" to "https://ci", "GIT_BRANCH" to "origin/feature/z")
        assertThat(context?.branch).isEqualTo("feature/z")
    }
}
