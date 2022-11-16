package de.chojo

import org.gradle.api.Project

class Git {
    companion object {
        /**
         * Get the current branch which is determined by GitHub or the local git repository
         */
        fun getBranch(project: Project): String = getGithubBranch() ?: determineLocalBranch(project)

        /**
         * Check if the build is a public build.
         */
        fun isPublicBuild(): Boolean {
            return (System.getenv("PUBLIC_BUILD") ?: "false").contentEquals("true")
        }

        /**
         * Get the commit hash.
         */
        fun getCommitHash(project: Project, hashLength: Int): String {
            return getGithubCommitHash(hashLength) ?: determineLocalCommitHash(project, hashLength)
        }

        private fun getGithubCommitHash(hashLength: Int): String? =
            System.getenv("GITHUB_SHA")?.substring(0, hashLength)


        private fun determineLocalCommitHash(project: Project, hashLength: Int): String {
            val localBranch = determineLocalBranchInternal(project)
            println("Building on branch $localBranch")
            if (localBranch == null) return "none"
            val hash = project.rootProject.file(".git/refs/heads/${localBranch}").useLines { it.firstOrNull() }
            return hash?.substring(0, hashLength) ?: "undefined"
        }


        private fun getGithubBranch(): String? {
            return System.getenv("GITHUB_REF")?.replace("refs/heads/", "")
        }

        private fun determineLocalBranch(project: Project): String {
            if (!isPublicBuild()) {
                println("Local build detected. Set the env variable PUBLIC_BUILD=true to build non local builds")
                return "local"
            }
            return determineLocalBranchInternal(project) ?: "none"
        }

        private fun determineLocalBranchInternal(project: Project): String? {
            val file = project.rootProject.file(".git/HEAD")
            if (!file.exists()) return null
            val branch = file.useLines { it.firstOrNull() }
            return branch?.replace("ref: refs/heads/", "") ?: "local"
        }
    }
}
