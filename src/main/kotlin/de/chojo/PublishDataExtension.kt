package de.chojo

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication

open class PublishDataExtension(private val project: Project) {
    var versionCleaner: Regex = Regex("-SNAPSHOT|-DEV")
    var hashLength: Int = 7
    var repos: MutableSet<Repo> = mutableSetOf()
    var components: MutableSet<String> = mutableSetOf()
    var tasks: MutableSet<String> = mutableSetOf()
    var repo: Repo? = null;

    /**
     * Registers a repository.
     */
    fun addRepo(repo: Repo) {
        println("Registered repository ${repo.url} with identifier \"${repo.marker}\" matching \"${repo.identifier}\"")
        repos.add(repo)
    }

    /**
     * Configures the repositories to use the eldonexus repositories as defined in [Repo.master], [Repo.dev] and [Repo.snapshot]
     */
    fun useEldoNexusRepos() {
        addRepo(Repo.master("", "https://eldonexus.de/repository/maven-releases/", false))
        addRepo(Repo.dev("DEV", "https://eldonexus.de/repository/maven-dev/", true))
        addRepo(Repo.snapshot("SNAPSHOT", "https://eldonexus.de/repository/maven-snapshots/", true))
    }

    /**
     * Adds a task by name to get published
     */
    fun publishTask(task: String) {
        println("Registered task $task for publishing")
        tasks.add(task)
    }

    /**
     * Adds a task to get published
     */
    fun publishTask(task: Task) {
        publishTask(task.name)
    }

    /**
     * Adds a component to get published
     */
    fun publishComponent(component: String) {
        println("Registered component $component for publishing")
        components.add(component)
    }

    private fun getReleaseType(): Repo? {
        if (repo != null) {
            return repo;
        }
        val branch = getBranch()
        val first = repos.firstOrNull { r -> r.isRepo(branch) }
        println(if (first == null) "Could not detect release type" else "Detected release of ${first.identifier}")
        return first;
    }

    /**
     * Configure a [MavenPublication]. Adds the [tasks] and [components] and calls [MavenPublication.setVersion],
     * [MavenPublication.setArtifactId] and [MavenPublication.setGroupId]
     */
    fun configurePublication(publication: MavenPublication) {
        for (component in components) {
            publication.from(project.components.getByName(component))
        }
        for (task in tasks) {
            publication.artifact(project.tasks.getByName(task))
        }
        publication.version = getVersion();
        publication.artifactId = project.name.lowercase()
        publication.groupId = (project.rootProject.group?: project.group) as String?
    }

    private fun getGithubCommitHash(): String? =
        System.getenv("GITHUB_SHA")?.substring(0, hashLength)

    private fun getCommitHash(): String {
        return getGithubCommitHash() ?: determineLocalCommitHash()
    }

    /**
     * Get the version with the optional [Repo.marker] without the commit hash
     */
    fun getVersion(): String = getVersion(false)

    /**
     * Get the version with the optional [Repo.marker]. Will append the commit hash when [Repo.addCommit] and [appendCommit] is set to true
     */
    fun getVersion(appendCommit: Boolean): String {
        return getReleaseType()?.append(getVersionString(), getCommitHash(), appendCommit) ?: "undefined"
    }

    private fun getVersionString(): String =
        (project.version as String).replace(versionCleaner, "")

    /**
     * Get the [Repo.url]
     */
    fun getRepository(): String = getReleaseType()?.url ?: ""

    private fun determineLocalCommitHash(): String {
        val localBranch = determineLocalBranchInternal()
        println("Building on branch $localBranch")
        val hash = project.rootProject.file(".git/refs/heads/${localBranch}").useLines { it.firstOrNull() }
        return hash?.substring(0, hashLength) ?: "undefined"
    }

    private fun getBranch(): String = getGithubBranch() ?: determineLocalBranch()

    private fun getGithubBranch(): String? {
        return System.getenv("GITHUB_REF")?.replace("refs/heads/", "")
    }

    private fun determineLocalBranch(): String {
        if (!isPublicBuild()) {
            println("Local build detected. Set the env variable PUBLIC_BUILD=true to build non local builds")
            return "local"
        }
        return determineLocalBranchInternal();
    }

    private fun determineLocalBranchInternal(): String {
        val branch = project.rootProject.file(".git/HEAD").useLines { it.firstOrNull() }
        return branch?.replace("ref: refs/heads/", "") ?: "local"
    }

    private fun isPublicBuild(): Boolean {
        return (System.getenv("PUBLIC_BUILD") ?: "false").contentEquals("true");
    }
}
