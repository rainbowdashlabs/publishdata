package de.chojo

/**
 * Creates a new repository.
 *
 * @property identifier the regex identifier which has to match the branch name
 * @property marker the identifier which should be appended on the version. may be blank
 * @property url the remote repository url
 * @property addCommit true when the commit hash should be appended on the version
 */
class BuildRepository(
    identifier: Regex,
    marker: String,
    val url: String,
    addCommit: Boolean,
    type: ReleaseType,
) : BuildType(identifier, marker, addCommit, type) {

    companion object {
        fun master(append: String, repo: String, addCommit: Boolean): BuildRepository {
            return BuildRepository(Regex("master"), append, repo, addCommit, ReleaseType.STABLE)
        }

        fun main(append: String, repo: String, addCommit: Boolean): BuildRepository {
            return BuildRepository(Regex("main"), append, repo, addCommit, ReleaseType.STABLE)
        }

        fun dev(append: String, repo: String, addCommit: Boolean): BuildRepository {
            return BuildRepository(Regex("(?i)^dev.*"), append, repo, addCommit, ReleaseType.DEV)
        }

        fun snapshot(append: String, repo: String, addCommit: Boolean): BuildRepository {
            return BuildRepository(Regex(".*"), append, repo, addCommit, ReleaseType.SNAPSHOT)
        }
    }
}
