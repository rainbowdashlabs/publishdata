package de.chojo.publishdata.git

/**
 * Reads the [GitContext] from the environment variables a CI system sets.
 *
 * The generic `PUBLISHDATA_REF` / `PUBLISHDATA_COMMIT` pair takes precedence, so any CI can state
 * its ref explicitly. Then GitHub Actions, GitLab CI, Bitbucket Pipelines and Jenkins are recognised
 * by their own markers.
 */
internal object CiEnvironment {
    private const val TAG_PREFIX = "refs/tags/"
    private const val BRANCH_PREFIX = "refs/heads/"

    /** Returns the context of the CI build described by [env], or null outside a known CI. */
    fun detect(env: (String) -> String?): GitContext? =
        generic(env) ?: gitHub(env) ?: gitLab(env) ?: bitbucket(env) ?: jenkins(env)

    private fun generic(env: (String) -> String?): GitContext? {
        val ref = env("PUBLISHDATA_REF").nonBlank() ?: return null
        val commit = env("PUBLISHDATA_COMMIT").nonBlank()
        return if (ref.startsWith(TAG_PREFIX)) {
            tagBuild(ref.removePrefix(TAG_PREFIX), commit, Ci.GENERIC)
        } else {
            branchBuild(ref.removePrefix(BRANCH_PREFIX), commit, Ci.GENERIC, changeRequest = false)
        }
    }

    private fun gitHub(env: (String) -> String?): GitContext? {
        if (env("GITHUB_ACTIONS") != "true") return null
        val commit = env("GITHUB_SHA").nonBlank()
        val headRef = env("GITHUB_HEAD_REF").nonBlank()
        val refName = env("GITHUB_REF_NAME").nonBlank()
        return when {
            headRef != null -> branchBuild(headRef, commit, Ci.GITHUB, changeRequest = true)
            env("GITHUB_REF_TYPE") == "tag" && refName != null -> tagBuild(refName, commit, Ci.GITHUB)
            else -> branchBuild(refName, commit, Ci.GITHUB, changeRequest = false)
        }
    }

    private fun gitLab(env: (String) -> String?): GitContext? {
        if (env("GITLAB_CI") != "true") return null
        val commit = env("CI_COMMIT_SHA").nonBlank()
        val tag = env("CI_COMMIT_TAG").nonBlank()
        val mergeRequestSource = env("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME").nonBlank()
        return when {
            tag != null -> tagBuild(tag, commit, Ci.GITLAB)
            mergeRequestSource != null -> branchBuild(mergeRequestSource, commit, Ci.GITLAB, changeRequest = true)
            else -> branchBuild(env("CI_COMMIT_BRANCH").nonBlank(), commit, Ci.GITLAB, changeRequest = false)
        }
    }

    private fun bitbucket(env: (String) -> String?): GitContext? {
        val commit = env("BITBUCKET_COMMIT").nonBlank() ?: return null
        val tag = env("BITBUCKET_TAG").nonBlank()
        val branch = env("BITBUCKET_BRANCH").nonBlank()
        return when {
            tag != null -> tagBuild(tag, commit, Ci.BITBUCKET)
            else -> branchBuild(branch, commit, Ci.BITBUCKET, changeRequest = env("BITBUCKET_PR_ID").nonBlank() != null)
        }
    }

    /**
     * Multibranch pipelines set `TAG_NAME`, `BRANCH_NAME` and for pull requests `CHANGE_BRANCH`.
     * Plain jobs only have `GIT_BRANCH`, prefixed with the remote.
     */
    private fun jenkins(env: (String) -> String?): GitContext? {
        if (env("JENKINS_URL").nonBlank() == null) return null
        val commit = env("GIT_COMMIT").nonBlank()
        val tag = env("TAG_NAME").nonBlank()
        val changeBranch = env("CHANGE_BRANCH").nonBlank()
        return when {
            tag != null -> tagBuild(tag, commit, Ci.JENKINS)
            changeBranch != null -> branchBuild(changeBranch, commit, Ci.JENKINS, changeRequest = true)
            else -> branchBuild(
                env("BRANCH_NAME").nonBlank() ?: env("GIT_BRANCH").nonBlank()?.substringAfter('/'),
                commit,
                Ci.JENKINS,
                changeRequest = false,
            )
        }
    }

    private fun tagBuild(tag: String, commit: String?, ci: Ci) =
        GitContext(branch = null, tags = listOf(tag), commit = commit, ci = ci, changeRequest = false, public = true)

    private fun branchBuild(branch: String?, commit: String?, ci: Ci, changeRequest: Boolean) =
        GitContext(branch = branch, tags = emptyList(), commit = commit, ci = ci, changeRequest = changeRequest, public = true)

    private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
}
