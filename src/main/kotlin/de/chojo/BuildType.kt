package de.chojo

/**
 * Creates a new repository.
 *
 * @property identifier the regex identifier which has to match the branch name
 * @property marker the identifier which should be appended on the version. may be blank
 * @property url the remote repository url
 * @property addCommit true when the commit hash should be appended on the version
 */
open class BuildType(val identifier: Regex, val marker: String, protected  val addCommit: Boolean, val type: ReleaseType) {

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
    fun isType(branch: String): Boolean {
        return identifier.matches(branch)
    }

    companion object {
        fun master(append: String, addCommit: Boolean): BuildType {
            return BuildType(Regex("master"), append, addCommit, ReleaseType.STABLE)
        }

        fun main(append: String, addCommit: Boolean): BuildType {
            return BuildType(Regex("main"), append, addCommit, ReleaseType.STABLE)
        }

        fun dev(append: String, addCommit: Boolean): BuildType {
            return BuildType(Regex("(?i)^dev.*"), append, addCommit, ReleaseType.DEV)
        }

        fun snapshot(append: String, addCommit: Boolean): BuildType {
            return BuildType(Regex(".*"), append, addCommit, ReleaseType.SNAPSHOT)
        }
    }
}
