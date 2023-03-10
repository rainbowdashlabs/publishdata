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
    abstract val publishDataExtension: Property<PublishDataExtension>

    @TaskAction
    fun generate() {
        val file = outputDirectory.file("build.data").get().asFile

        var data = """
            time=${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}
            unix=${Instant.now().epochSecond}
            type=${publishDataExtension.get().getBuildType()}
            branch=${publishDataExtension.get().getBranch()}
            commit=${publishDataExtension.get().getCommitHash()}
            artifactVersion=${publishDataExtension.get().getVersion(false)}
        """.trimIndent()
        Files.writeString(file.toPath(), data)
    }
}
