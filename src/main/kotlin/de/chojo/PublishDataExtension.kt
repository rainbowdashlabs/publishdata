package de.chojo

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class PublishDataExtension(private val project: Project) {
    var versionCleaner: Regex = Regex("-SNAPSHOT|-DEV")
    var hashLength: Int = 7
    var repos: MutableSet<Repo> = mutableSetOf()
    var components: MutableSet<String> = mutableSetOf()
    var tasks: MutableSet<String> = mutableSetOf()
    var repo: Repo? = null

    /**
     * Registers a repository.
     *
     * Order matters. The first registered repository has the highest priority.
     * The first repo which matches will be the one to be used.
     */
    fun addRepo(repo: Repo) {
        println("Registered repository ${repo.url} with identifier \"${repo.marker}\" matching \"${repo.identifier}\"")
        repos.add(repo)
    }

    /**
     * Configures the repositories to use the eldonexus repositories as defined in [Repo.master], [Repo.dev] and [Repo.snapshot]
     */
    fun useEldoNexusRepos(useMain: Boolean = false) {
        if (useMain) {
            addRepo(Repo.main("", "https://eldonexus.de/repository/maven-releases/", false))
        } else {
            addRepo(Repo.master("", "https://eldonexus.de/repository/maven-releases/", false))
        }
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
            return repo
        }
        val branch = Git.getBranch(project)
        val first = repos.firstOrNull { r -> r.isRepo(branch) }
        println(if (first == null) "Could not detect release type" else "Detected release of ${first.identifier}")
        return first
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
        publication.version = getVersion()
        publication.artifactId = getProjectName()
        publication.groupId = getGroupId()
    }

    private fun getGroupId(): String {
        var version = (project.rootProject.group as String)
        if (version.isBlank()) {
            version = (project.group as String)
        }
        return version
    }

    private fun getProjectName(): String {
        var version = (project.name as String)
        if (version.isBlank()) {
            version = (project.rootProject.name as String)
        }
        return version.lowercase()
    }


    /**
     * Get the version with the optional [Repo.marker] without the commit hash
     */
    fun getVersion(): String = getVersion(false)

    /**
     * Get the version with the optional [Repo.marker]. Will append the commit hash when [Repo.addCommit] and [appendCommit] is set to true
     */
    fun getVersion(appendCommit: Boolean): String {
        return getReleaseType()?.append(getVersionString(), Git.getCommitHash(project, hashLength), appendCommit) ?: "undefined"
    }

    private fun getVersionString(): String {
        var version = project.version as String
        if (version.isBlank() || version == "unspecified") {
            version = (project.rootProject.version as String)
        }
        return version.replace(versionCleaner, "")
    }

    /**
     * Get the [Repo.url]
     */
    fun getRepository(): String = getReleaseType()?.url ?: ""

}
