package de.chojo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class PublishData : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val extension = PublishDataExtension(this)
            extensions.add("publishData", extension)

            val generated = layout.buildDirectory.dir("generated/publishdata")

            val generateTask = tasks.register<GenerateBuildData>("generateBuildData") {
                outputDirectory.set(generated)
                type.set(extension.getBuildType())
                branch.set(extension.getBranch())
                commit.set(extension.getCommitHash())
                artifactVersion.set(extension.getVersion(false))
            }

            plugins.withType<JavaPlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    resources.srcDir(generateTask)
                }
            }
        }
    }
}
