package de.chojo

/**
 * Creates a new repository.
 *
 * @property identifier the regex identifier which has to match the branch name
 * @property marker the identifier which should be appended on the version. may be blank
 * @property url the remote repository url
 * @property addCommit true when the commit hash should be appended on the version
 */
class Repo(val identifier: Regex, val marker: String, val url: String, private val addCommit: Boolean, val type: Type) {

    enum class Type {
        STABLE, DEV, SNAPSHOT
    }

    /**
     * Append the optional [marker] and [commitHash] on the version.
     *
     * @param name the [name] of the branch
     * @param commitHash the hash of the commit
     * @param appendCommit if the commit hash should be added to the version. The commit will only be appended when [addCommit] is true.
     *
     * @return the version with the optional marker and commit hash
     */
    fun append(name: String, commitHash: String, appendCommit: Boolean = addCommit): String =
        name.plus(if (marker.isBlank() ) "" else "-$marker" ).plus(if (appendCommit && addCommit) "+".plus(commitHash) else "")

    /**
     * Checks if the [branch] matches the [identifier]
     */
    fun isRepo(branch: String): Boolean {
        return identifier.matches(branch)
    }

    companion object {
        fun master(append: String, repo: String, addCommit: Boolean): Repo {
            return Repo(Regex("master"), append, repo, addCommit, Type.STABLE)
        }

        fun main(append: String, repo: String, addCommit: Boolean): Repo {
            return Repo(Regex("main"), append, repo, addCommit, Type.STABLE)
        }

        fun dev(append: String, repo: String, addCommit: Boolean): Repo {
            return Repo(Regex("(?i)^dev.*"), append, repo, addCommit, Type.DEV)
        }

        fun snapshot(append: String, repo: String, addCommit: Boolean): Repo {
            return Repo(Regex(".*"), append, repo, addCommit, Type.SNAPSHOT)
        }
    }
}
