package de.chojo.publishdata

import de.chojo.publishdata.git.GitContextSource
import de.chojo.publishdata.resolve.Resolution
import de.chojo.publishdata.resolve.Resolver
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.SourceSetContainer

/**
 * Sets the project version from the git state of the build, optionally adds a build data file to
 * the main resources, and wires `maven-publish` to the repository of the resolved channel.
 */
class PublishDataPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "publishData",
            PublishDataExtension::class.java,
            { registerBuildData(project) },
        )
        extension.baseVersion.convention(project.providers.gradleProperty("version"))
        extension.groupId.convention(project.provider { defaultGroupId(project) })
        extension.artifactId.convention(project.name.lowercase())

        val gitContext = project.providers.of(GitContextSource::class.java) {
            parameters.workingDirectory.set(project.layout.projectDirectory)
        }
        extension.resolution = gitContext.map { context ->
            extension.frozenResolution
                ?: Resolver.resolve(context, extension.channelSpecs(), extension.baseVersion.orNull, extension.hashLength.get())
        }
        project.version = LazyVersion(extension.resolution.map(Resolution::version))

        project.afterEvaluate {
            freezeResolution(project, extension)
            registerPrintVersion(project, extension)
        }
        project.pluginManager.withPlugin("maven-publish") {
            project.afterEvaluate { configurePublishing(project, extension) }
            failPublishingWithoutVersion(project, extension)
        }
    }

    /**
     * Fixes the resolution once the build script has run, so every later read of the version, the
     * publication and the build data agree. Reports how the version was chosen.
     */
    private fun freezeResolution(project: Project, extension: PublishDataExtension) {
        extension.baseVersion.finalizeValue()
        extension.hashLength.finalizeValue()
        val resolution = extension.resolution.get()
        extension.frozenResolution = resolution
        project.logger.info(
            "PublishData resolved version {} of {} in channel '{}' on {}: {}",
            resolution.version,
            project.path,
            resolution.channel,
            resolution.ci,
            resolution.reason,
        )
        warnAboutBaseVersion(project, extension, resolution)
        warnIfVersionOverridden(project)
    }

    private fun warnAboutBaseVersion(project: Project, extension: PublishDataExtension, resolution: Resolution) {
        if (!resolution.hasVersion) {
            project.logger.warn(MISSING_VERSION, project.path)
            return
        }
        val baseVersion = extension.baseVersion.orNull ?: return
        val cleaned = Resolver.cleanBaseVersion(baseVersion)
        if (resolution.tag != null || cleaned == baseVersion) return
        project.logger.warn(
            "The base version {} of {} ends with a channel suffix. PublishData uses {}; remove the suffix, the channels add it.",
            baseVersion,
            project.path,
            cleaned,
        )
    }

    private fun warnIfVersionOverridden(project: Project) {
        if (project.version is LazyVersion) return
        project.logger.warn(
            "The build script of {} sets version = {}, which replaces the version resolved by PublishData. " +
                "Set publishData.baseVersion or 'version' in gradle.properties instead.",
            project.path,
            project.version,
        )
    }

    private fun registerPrintVersion(project: Project, extension: PublishDataExtension) {
        if (PRINT_VERSION in project.tasks.names) return
        val version = extension.resolution.map(Resolution::version)
        project.tasks.register(PRINT_VERSION) {
            group = "help"
            description = "Prints the version resolved by PublishData."
            doLast { println(version.get()) }
        }
    }

    private fun failPublishingWithoutVersion(project: Project, extension: PublishDataExtension) {
        val hasVersion = extension.resolution.map(Resolution::hasVersion)
        val path = project.path
        project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
            doFirst {
                if (!hasVersion.get()) throw GradleException(MISSING_VERSION.replace("{}", path))
            }
        }
    }

    private fun registerBuildData(project: Project) {
        val extension = project.extensions.getByType(PublishDataExtension::class.java)
        val buildType = project.providers.environmentVariable("BUILD_TYPE")
        val task = project.tasks.register("generateBuildData", GenerateBuildData::class.java) {
            outputDirectory.set(project.layout.buildDirectory.dir("generated/publishdata"))
            fileName.set(extension.buildData.fileName)
            includeTime.set(extension.buildData.includeTime)
            entries.putAll(
                extension.resolution
                    .zip(extension.artifactId) { resolution, artifact -> resolution to artifact }
                    .zip(buildType.orElse("")) { (resolution, artifact), type -> builtInEntries(resolution, type, artifact) }
            )
            entries.putAll(extension.buildData.entries)
        }
        project.pluginManager.withPlugin("java") {
            project.extensions.getByType(SourceSetContainer::class.java).named("main") {
                resources.srcDir(task)
            }
        }
    }

    private fun builtInEntries(resolution: Resolution, buildType: String, artifact: String) = linkedMapOf(
        "type" to buildType.ifBlank { if (resolution.public) "PUBLIC" else "LOCAL" },
        "branch" to (resolution.branch ?: NONE),
        "commit" to (resolution.commit ?: NONE),
        "artifactVersion" to resolution.version,
        "artifact" to artifact,
        "runtime" to Runtime.version().toString(),
        "tag" to (resolution.tag ?: NONE),
        "channel" to resolution.channel,
    )

    private fun configurePublishing(project: Project, extension: PublishDataExtension) {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        if (shouldCreatePublication(project, extension, publishing)) createPublication(project, extension, publishing)
        val repository = extension.resolution.get().repository ?: return
        publishing.repositories.maven {
            name = extension.repositoryName.get()
            setUrl(repository)
            if (extension.repositoryActions.isEmpty()) extension.defaultRepositoryAction?.execute(this)
            extension.repositoryActions.forEach { it.execute(this) }
        }
    }

    private fun shouldCreatePublication(
        project: Project,
        extension: PublishDataExtension,
        publishing: PublishingExtension,
    ): Boolean = extension.autoPublication.get() &&
        publishing.publications.isEmpty() &&
        !project.pluginManager.hasPlugin("java-gradle-plugin")

    private fun createPublication(project: Project, extension: PublishDataExtension, publishing: PublishingExtension) {
        val component = extension.component.orNull?.let(project.components::findByName)
        if (component == null && extension.artifacts.isEmpty()) {
            project.logger.info("PublishData creates no publication for {}: no component and no artifacts.", project.path)
            return
        }
        publishing.publications.create("maven", MavenPublication::class.java) {
            component?.let(::from)
            extension.artifacts.forEach(::artifact)
            groupId = extension.groupId.get()
            artifactId = extension.artifactId.get()
        }
    }

    private fun defaultGroupId(project: Project): String =
        project.rootProject.group.toString().ifBlank { project.group.toString() }

    private companion object {
        const val NONE = "none"
        const val PRINT_VERSION = "printVersion"
        const val MISSING_VERSION =
            "PublishData has no version for {}. Set publishData.baseVersion or 'version' in gradle.properties."
    }
}
