package de.chojo

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

abstract class GenerateBuildData : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val type: Property<String>

    @get:Input
    abstract val branch: Property<String>

    @get:Input
    abstract val commit: Property<String>

    @get:Input
    abstract val artifactVersion: Property<String>

    @get:Input
    abstract val active: Property<Boolean>
    @get:Input
    abstract val additionalData:  MapProperty<String, String>

    @TaskAction
    fun generate() {
        if (!active.get()) return
        val data = hashMapOf<String, Any>(
            "time" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "unix" to Instant.now().epochSecond,
            "type" to type.get(),
            "branch" to branch.get(),
            "commit" to commit.get(),
            "artifactVersion" to artifactVersion.get(),
            "runtime" to Runtime.version()
        )
        additionalData.get().entries.stream().forEach { e -> data[e.key] = e.value }

        val file = outputDirectory.file("build.data").get().asFile
        val dataString = data.entries.stream().map { e -> "${e.key}=${e.value}" }.collect(Collectors.joining("\n"))
        Files.writeString(file.toPath(), dataString)
    }
}
