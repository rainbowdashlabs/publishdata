package de.chojo

import org.gradle.api.Plugin
import org.gradle.api.Project

class PublishData : Plugin<Project> {
    override fun apply(target: Project) {
        val publishData = target.extensions.create("publishData", PublishDataExtension::class.java, target)



        target.run {
            tasks.register<GenerateBuildData>("createBuildData"){
                publishDataE
            }
        }
    }
}
