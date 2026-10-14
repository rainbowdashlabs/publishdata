package de.chojo.publishdata

import de.chojo.publishdata.resolve.ChannelSpec
import de.chojo.publishdata.resolve.Resolution
import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.authentication.http.HttpHeaderAuthentication
import javax.inject.Inject

/**
 * The `publishData` extension.
 *
 * The plugin owns `project.version`: it is resolved from [baseVersion] and the channel matching the
 * build. Assigning `version` in the build script overrides it and is reported as a warning.
 *
 * @param onBuildDataEnabled registers the build data task the first time [buildData] is called
 */
abstract class PublishDataExtension @Inject constructor(
    objects: ObjectFactory,
    private val onBuildDataEnabled: () -> Unit,
) {
    /** Version of builds that do not take it from a tag. Defaults to the `version` gradle property. */
    abstract val baseVersion: Property<String>

    /** Length commit hashes are shortened to. Defaults to 7. */
    abstract val hashLength: Property<Int>

    /**
     * Whether a `maven` publication is created when `maven-publish` is applied and the project
     * declares no publication itself. Defaults to true.
     */
    abstract val autoPublication: Property<Boolean>

    /** Software component of the automatic publication. Defaults to `java`. */
    abstract val component: Property<String>

    /**
     * Group of the automatic publication. Defaults to the group of the root project, or of this
     * project when the root has none.
     */
    abstract val groupId: Property<String>

    /**
     * Artifact of the automatic publication, also written into build data. Defaults to the project
     * name in lower case.
     */
    abstract val artifactId: Property<String>

    /** Name of the repository registered for the resolved channel. Defaults to `PublishData`. */
    abstract val repositoryName: Property<String>

    /** The release channels, in order of precedence. */
    val channels: Channels = objects.newInstance(Channels::class.java)

    /** The build data configuration. Only used once [buildData] is called. */
    val buildData: BuildDataSpec = objects.newInstance(BuildDataSpec::class.java)

    /** The resolved channel, version and git state of the build. */
    lateinit var resolution: Provider<Resolution>
        internal set

    internal val artifacts = mutableListOf<Any>()
    internal val repositoryActions = mutableListOf<Action<in MavenArtifactRepository>>()
    internal var defaultRepositoryAction: Action<in MavenArtifactRepository>? = null
    internal var frozenResolution: Resolution? = null
    private var buildDataEnabled = false

    init {
        hashLength.convention(7)
        autoPublication.convention(true)
        component.convention("java")
        repositoryName.convention("PublishData")
    }

    /** Configures the release channels. */
    fun channels(action: Action<Channels>) = action.execute(channels)

    /** Adds the build data file to the main resources. */
    fun buildData() = buildData {}

    /** Adds the build data file to the main resources and configures it. */
    fun buildData(action: Action<BuildDataSpec>) {
        action.execute(buildData)
        if (buildDataEnabled) return
        buildDataEnabled = true
        onBuildDataEnabled()
    }

    /** Adds an artifact to the automatic publication, in any notation `MavenPublication.artifact` accepts. */
    fun artifact(notation: Any) {
        artifacts += notation
    }

    /**
     * Configures the repository registered for the resolved channel, e.g. its credentials. Replaces
     * the default credentials of a preset.
     */
    fun repository(action: Action<in MavenArtifactRepository>) {
        repositoryActions += action
    }

    /**
     * Publishes to the EldoNexus: tags to releases, `dev*` branches to the dev repository with suffix
     * `DEV`, everything else to snapshots. The repository is named `EldoNexus`; unless [repository] is
     * used, its credentials are read from the gradle properties `EldoNexusUsername` and
     * `EldoNexusPassword`, e.g. via `ORG_GRADLE_PROJECT_EldoNexusUsername` on CI.
     *
     * @param internal use the internal repositories
     * @param dev add the dev channel
     */
    @JvmOverloads
    fun eldoNexus(internal: Boolean = false, dev: Boolean = true) {
        val variant = if (internal) "-internal" else ""
        repositoryName.convention("EldoNexus")
        defaultRepositoryAction = Action { credentials(PasswordCredentials::class.java) }
        channels {
            release("https://eldonexus.de/repository/maven-releases$variant/")
            if (dev) {
                channel("dev", "https://eldonexus.de/repository/maven-dev$variant/") {
                    onBranch(Regex("(?i)dev.*"))
                    suffix("DEV")
                }
            }
            snapshot("https://eldonexus.de/repository/maven-snapshots$variant/")
        }
    }

    /**
     * Publishes to the package registry of a GitLab project: tags as releases, everything else as
     * snapshots. The repository is named `GitLab`; unless [repository] is used, it authenticates with
     * the `CI_JOB_TOKEN` of the pipeline.
     */
    @JvmOverloads
    fun gitLab(projectId: String, url: String = "https://gitlab.com/") {
        val registry = "${url.removeSuffix("/")}/api/v4/projects/$projectId/packages/maven"
        repositoryName.convention("GitLab")
        channels {
            release(registry)
            snapshot(registry)
        }
        defaultRepositoryAction = Action {
            credentials(HttpHeaderCredentials::class.java) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication.create("header", HttpHeaderAuthentication::class.java)
        }
    }

    internal fun channelSpecs(): List<ChannelSpec> = channels.toSpecs()
}
