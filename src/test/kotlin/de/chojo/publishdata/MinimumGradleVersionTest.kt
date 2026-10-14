package de.chojo.publishdata

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Runs a representative build on the oldest supported Gradle version.
 *
 * That Gradle runs on Java 21 at most. The daemon uses the JDK in `MIN_GRADLE_JAVA_HOME`, or the
 * test JVM when that is Java 21 or older; without either the test is skipped.
 */
class MinimumGradleVersionTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `plugin works on the minimum gradle version`() {
        val javaHome = System.getenv("MIN_GRADLE_JAVA_HOME")
            ?: System.getProperty("java.home").takeIf { Runtime.version().feature() <= 21 }
        assumeTrue(javaHome != null, "no JDK 21 for Gradle $MINIMUM_GRADLE_VERSION, set MIN_GRADLE_JAVA_HOME")

        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "sample"""")
        projectDir.resolve("gradle.properties").writeText("version=1.5.0\norg.gradle.java.home=$javaHome\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                `maven-publish`
                id("de.chojo.publishdata")
            }

            group = "de.chojo"

            publishData {
                hashLength = 9
                channels {
                    release(uri("repo/releases").toString())
                    snapshot(uri("repo/snapshots").toString()) {
                        branchSuffix().commitHash()
                    }
                }
                buildData {
                    includeTime = false
                    put("custom", providers.environmentVariable("CUSTOM"))
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withGradleVersion(MINIMUM_GRADLE_VERSION)
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("printVersion", "publish", "--configuration-cache", "--stacktrace")
            .withEnvironment(
                mapOf(
                    "PATH" to System.getenv("PATH"),
                    "PUBLISHDATA_REF" to "feature/x",
                    "PUBLISHDATA_COMMIT" to "0123456789abcdef",
                    "CUSTOM" to "value",
                )
            )
            .build()

        assertThat(result.output).contains("1.5.0-feature-x-SNAPSHOT+012345678")
        assertThat(projectDir.resolve("repo/snapshots/de/chojo/sample/1.5.0-feature-x-SNAPSHOT+012345678")).isDirectory()
    }

    private companion object {
        const val MINIMUM_GRADLE_VERSION = "8.5"
    }
}
