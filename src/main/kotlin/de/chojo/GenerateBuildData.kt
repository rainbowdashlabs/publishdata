package de.chojo

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.time.Instant
import java.time.format.DateTimeFormatter

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

    @TaskAction
    fun generate() {
        val file = outputDirectory.file("build.data").get().asFile

        var data = """
            time=${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}
            unix=${Instant.now().epochSecond}
            type=${type.get()}
            branch=${branch.get()}
            commit=${commit.get()}
            artifactVersion=${artifactVersion.get()}
        """.trimIndent()
        Files.writeString(file.toPath(), data)
    }
}
