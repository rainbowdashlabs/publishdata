package de.chojo.publishdata

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Writes the build data file into [outputDirectory]: `time` and `unix` when [includeTime] is set,
 * followed by [entries]. Only cached when the time is left out.
 */
@CacheableTask
abstract class GenerateBuildData : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val fileName: Property<String>

    @get:Input
    abstract val includeTime: Property<Boolean>

    @get:Input
    abstract val entries: MapProperty<String, String>

    init {
        outputs.cacheIf("the build data contains no build time") { !includeTime.get() }
    }

    @TaskAction
    fun generate() {
        val directory = outputDirectory.get().asFile
        directory.listFiles()?.forEach { it.deleteRecursively() }
        val data = linkedMapOf<String, String>()
        if (includeTime.get()) {
            val now = Instant.now()
            data["time"] = DateTimeFormatter.ISO_INSTANT.format(now)
            data["unix"] = now.epochSecond.toString()
        }
        data += entries.get()
        Files.writeString(directory.resolve(fileName.get()).toPath(), PropertiesFormat.format(data))
    }
}
