/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.publication

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Fails when a generated POM lacks an element Maven Central requires, naming every publication and
 * what it misses, so a release stops before anything is uploaded.
 *
 * Maven publish writes each POM to `publications/<publication>/pom-default.xml`, so the publication
 * is named after the POM's directory.
 */
@DisableCachingByDefault(because = "Reading a few small POMs is cheaper than caching the result")
abstract class ValidatePoms : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val poms: ConfigurableFileCollection

    @get:Input
    abstract val projectPath: Property<String>

    @TaskAction
    fun validate() {
        val problems =
            poms.files
                .sortedBy { it.path }
                .mapNotNull { pom ->
                    val missing = CentralPomRequirements.missing(pom)
                    if (missing.isEmpty()) null else "'${pom.parentFile.name}' lacks ${missing.joinToString(", ")}"
                }
        if (problems.isEmpty()) return
        throw GradleException(
            "The POMs of ${projectPath.get()} lack elements Maven Central requires: " +
                problems.joinToString("; ") +
                ". Set them in the pom block of each publication.",
        )
    }
}
