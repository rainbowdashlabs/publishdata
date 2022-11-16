package de.chojo

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.Properties
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

class GenerateBuildData() : DefaultTask() {
    @Input
    val publishDataExtension: Property<PublishDataExtension> = project.objects.property();

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun generate() {
        var props = Properties()
        props.put("commit", Git.)
    }
}
