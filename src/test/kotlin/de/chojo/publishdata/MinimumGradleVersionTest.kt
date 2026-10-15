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
        val javaHome = javaHome()
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

        val result = run("printVersion", "publish")

        assertThat(result.output).contains("1.5.0-feature-x-SNAPSHOT+012345678")
        assertThat(projectDir.resolve("repo/snapshots/de/chojo/sample/1.5.0-feature-x-SNAPSHOT+012345678")).isDirectory()
    }

    /** Older Gradle versions generate no accessor for settings extensions, hence the explicit `configure`. */
    @Test
    fun `settings plugin works on the minimum gradle version`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            plugins {
                id("de.chojo.publishdata.settings")
            }

            rootProject.name = "sample"

            configure<de.chojo.publishdata.PublishDataSettings> {
                defaults {
                    channels {
                        snapshot(rootDir.resolve("repo/snapshots").toURI().toString())
                    }
                }
            }
            """.trimIndent()
        )
        projectDir.resolve("gradle.properties").writeText("version=1.5.0\norg.gradle.java.home=${javaHome()}\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                `maven-publish`
            }

            group = "de.chojo"
            """.trimIndent()
        )

        run("publish")

        assertThat(projectDir.resolve("repo/snapshots/de/chojo/sample/1.5.0-SNAPSHOT")).isDirectory()
    }

    /** Before Gradle 8.8 vanniktech creates its publication late, after PublishData has looked. */
    @Test
    fun `vanniktech integration works on the minimum gradle version`() {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "sample"""")
        projectDir.resolve("gradle.properties").writeText("version=1.5.0\norg.gradle.java.home=${javaHome()}\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("com.vanniktech.maven.publish") version "0.34.0"
                id("de.chojo.publishdata")
            }

            group = "de.chojo"

            mavenPublishing {
                publishToMavenCentral()
            }

            publishData {
                channels {
                    release { publishToMavenCentral() }
                    snapshot(uri("repo/snapshots").toString())
                }
            }
            """.trimIndent()
        )

        run("publishChannel")

        assertThat(projectDir.resolve("repo/snapshots/de/chojo/sample/1.5.0-SNAPSHOT")).isDirectory()
    }

    private fun javaHome(): String {
        val javaHome = System.getenv("MIN_GRADLE_JAVA_HOME")
            ?: System.getProperty("java.home").takeIf { Runtime.version().feature() <= 21 }
        assumeTrue(javaHome != null, "no JDK 21 for Gradle $MINIMUM_GRADLE_VERSION, set MIN_GRADLE_JAVA_HOME")
        return javaHome!!
    }

    private fun run(vararg tasks: String) = GradleRunner.create()
        .withGradleVersion(MINIMUM_GRADLE_VERSION)
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*tasks, "--configuration-cache", "--stacktrace")
        .withEnvironment(
            mapOf(
                "PATH" to System.getenv("PATH"),
                "PUBLISHDATA_REF" to "feature/x",
                "PUBLISHDATA_COMMIT" to "0123456789abcdef",
                "CUSTOM" to "value",
            )
        )
        .build()

    private companion object {
        const val MINIMUM_GRADLE_VERSION = "8.5"
    }
}
