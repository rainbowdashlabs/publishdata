/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.publication

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider

/** What PublishData adds to the Maven publications of a project. */
internal object PublicationDefaults {
    /**
     * The system property Gradle reads each time it publishes a file to decide whether to add the
     * `.sha256` and `.sha512` checksums. Gradle replaces the system properties for every build and
     * restores them afterwards, so setting it while publishing does not outlive the build.
     */
    const val ONLY_REQUIRED_CHECKSUMS_PROPERTY = "org.gradle.internal.publish.checksums.insecure"

    const val VALIDATE_POMS = "validatePoms"

    /**
     * Makes every Maven publication default its POM name and description to the project's, read
     * when the POM is written so a description set late in the build script counts. Values set by
     * the build take precedence.
     */
    fun addPomConventions(project: Project) {
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val name = project.name
        val description = project.provider { project.description }
        publishing.publications.withType(MavenPublication::class.java).configureEach {
            pom.name.convention(name)
            pom.description.convention(description)
        }
    }

    /**
     * Lets the repository publications of the project write only `.md5` and `.sha1` checksums
     * while [onlyRequiredChecksums] is true. The property is set when a publish task starts rather
     * than when the build is configured, so it also applies when the configuration cache is reused.
     */
    fun limitChecksums(
        project: Project,
        onlyRequiredChecksums: Provider<Boolean>,
    ) {
        project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
            doFirst {
                if (onlyRequiredChecksums.get()) System.setProperty(ONLY_REQUIRED_CHECKSUMS_PROPERTY, "true")
            }
        }
    }

    /** Registers [VALIDATE_POMS], which checks the POM of every Maven publication of the project. */
    fun registerValidatePoms(project: Project): TaskProvider<ValidatePoms> =
        project.tasks.register(VALIDATE_POMS, ValidatePoms::class.java) {
            group = "publishing"
            description = "Checks that every POM has the elements Maven Central requires."
            poms.from(project.tasks.withType(GenerateMavenPom::class.java))
            projectPath.set(project.path)
        }

    /** Makes every repository publish task of the project wait for [validation]. */
    fun requireValidPoms(
        project: Project,
        validation: TaskProvider<ValidatePoms>,
    ) {
        project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
            dependsOn(validation)
        }
    }
}
