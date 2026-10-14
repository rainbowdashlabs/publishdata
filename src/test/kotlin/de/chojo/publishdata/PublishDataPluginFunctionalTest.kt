package de.chojo.publishdata

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

class PublishDataPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "Sample"""")
        projectDir.resolve("gradle.properties").writeText("version=1.5.0\n")
        projectDir.resolve(".gitignore").writeText("build/\n.gradle/\n")
        git("init", "-q", "-b", "main")
        writeBuild("")
        commit()
    }

    @Test
    fun `local build resolves to a snapshot even when tagged`() {
        git("tag", "v1.4.0")
        assertThat(version(run("testVersion"))).isEqualTo("1.5.0-SNAPSHOT")
    }

    @Test
    fun `public local build on a tag takes the version from the tag`() {
        git("tag", "v1.4.0")
        val result = run("testVersion", "jar", env = mapOf("PUBLIC_BUILD" to "true"))
        assertThat(version(result)).isEqualTo("1.4.0")
        assertThat(projectDir.resolve("build/libs/Sample-1.4.0.jar")).exists()
    }

    @Test
    fun `github tag build publishes a release`() {
        writeBuild(
            """
            publishData {
                channels {
                    release(uri("repo/releases").toString())
                    snapshot(uri("repo/snapshots").toString())
                }
            }
            """
        )
        commit()
        val env = mapOf(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF_TYPE" to "tag",
            "GITHUB_REF_NAME" to "v2.1.0",
            "GITHUB_SHA" to "0123456789abcdef",
        )
        run("publish", env = env)
        assertThat(projectDir.resolve("repo/releases/de/chojo/sample/2.1.0/sample-2.1.0.jar")).exists()
        assertThat(projectDir.resolve("repo/snapshots")).doesNotExist()
    }

    @Test
    fun `gitlab branch build publishes a snapshot`() {
        writeBuild(
            """
            publishData {
                channels {
                    release(uri("repo/releases").toString())
                    snapshot(uri("repo/snapshots").toString())
                }
            }
            """
        )
        commit()
        run("publish", env = mapOf("GITLAB_CI" to "true", "CI_COMMIT_BRANCH" to "feature/x"))
        assertThat(projectDir.resolve("repo/snapshots/de/chojo/sample/1.5.0-SNAPSHOT")).isDirectory()
        assertThat(projectDir.resolve("repo/releases")).doesNotExist()
    }

    @Test
    fun `build data is added to the resources`() {
        writeBuild(
            """
            publishData {
                buildData {
                    put("custom", "value")
                }
            }
            """
        )
        commit()
        git("checkout", "-q", "--detach")
        run("processResources", env = mapOf("BUILD_TYPE" to "KOFI"))
        val data = projectDir.resolve("build/resources/main/build.data")
        val properties = Properties().apply { data.inputStream().use(::load) }
        assertThat(data.readLines().map { it.substringBefore('=') }).containsExactly(
            "time", "unix", "type", "branch", "commit", "artifactVersion", "artifact", "runtime", "tag", "channel", "custom",
        )
        assertThat(properties)
            .containsEntry("type", "KOFI")
            .containsEntry("branch", "none")
            .containsEntry("commit", git("rev-parse", "--short=7", "HEAD"))
            .containsEntry("artifactVersion", "1.5.0-SNAPSHOT")
            .containsEntry("artifact", "sample")
            .containsEntry("channel", "snapshot")
            .containsEntry("custom", "value")
    }

    @Test
    fun `build data without time is reproducible and cacheable`() {
        writeBuild(
            """
            publishData {
                buildData {
                    includeTime = false
                    fileName = "sample.data"
                }
            }
            """
        )
        commit()
        run("processResources", "--build-cache")
        assertThat(projectDir.resolve("build/resources/main/sample.data").readLines()).noneMatch { it.startsWith("time=") }
        projectDir.resolve("build").deleteRecursively()
        val result = run("processResources", "--build-cache")
        assertThat(result.task(":generateBuildData")?.outcome?.name).isEqualTo("FROM_CACHE")
    }

    @Test
    fun `configuration cache is reused until the git state changes`() {
        assertThat(version(run("testVersion", "--configuration-cache"))).isEqualTo("1.5.0-SNAPSHOT")
        val reused = run("testVersion", "--configuration-cache")
        assertThat(reused.output).contains("Reusing configuration cache.")

        git("tag", "v3.0.0")
        val tagged = run("testVersion", "--configuration-cache", env = mapOf("PUBLIC_BUILD" to "true"))
        assertThat(tagged.output).doesNotContain("Reusing configuration cache.")
        assertThat(version(tagged)).isEqualTo("3.0.0")
    }

    @Test
    fun `publishing works with the configuration cache`() {
        writeBuild(
            """
            publishData {
                channels {
                    snapshot(uri("repo/snapshots").toString())
                }
                buildData()
            }
            """
        )
        commit()
        run("publish", "--configuration-cache")
        val result = run("publish", "--configuration-cache")
        assertThat(result.output).contains("Reusing configuration cache.")
    }

    @Test
    fun `eldonexus preset registers the EldoNexus repository`() {
        writeBuild(
            """
            publishData {
                eldoNexus()
                repository {
                    credentials {
                        username = "user"
                        password = "secret"
                    }
                }
            }
            """
        )
        commit()
        val result = run("help", "--task", "publishAllPublicationsToEldoNexusRepository", env = mapOf("PUBLISHDATA_REF" to "dev"))
        assertThat(result.output).contains("publishAllPublicationsToEldoNexusRepository")
    }

    @Test
    fun `multi module build shares the version and honours coordinate overrides`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "Multi"
            include("core", "api")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("de.chojo.publishdata")
            }

            group = "de.chojo"

            allprojects {
                apply(plugin = "de.chojo.publishdata")
                tasks.register("testVersion") {
                    val line = project.path + "=" + project.version
                    doLast { println("VERSION " + line) }
                }
            }

            subprojects {
                apply(plugin = "java-library")
                apply(plugin = "maven-publish")
                publishData {
                    channels {
                        release(rootProject.uri("repo/releases").toString())
                        snapshot(rootProject.uri("repo/snapshots").toString())
                    }
                    buildData()
                }
            }
            """.trimIndent()
        )
        projectDir.resolve("core").mkdirs()
        projectDir.resolve("api").mkdirs()
        projectDir.resolve("api/build.gradle.kts").writeText(
            """
            publishData {
                artifactId = "multi-api"
                groupId = "de.chojo.multi"
            }
            """.trimIndent()
        )
        commit()
        git("tag", "v2.0.0")

        val result = run("testVersion", "publish", env = mapOf("PUBLIC_BUILD" to "true"))

        val versions = result.output.lines().filter { it.startsWith("VERSION ") }.map { it.removePrefix("VERSION ") }
        assertThat(versions).containsExactlyInAnyOrder(":=2.0.0", ":core=2.0.0", ":api=2.0.0")
        assertThat(projectDir.resolve("repo/releases/de/chojo/core/2.0.0/core-2.0.0.jar")).exists()
        assertThat(projectDir.resolve("repo/releases/de/chojo/multi/multi-api/2.0.0/multi-api-2.0.0.jar")).exists()
        assertThat(projectDir.resolve("api/build/resources/main/build.data").readLines()).contains("artifact=multi-api")
        assertThat(projectDir.resolve("core/build/resources/main/build.data").readLines()).contains("artifact=core")
    }

    @Test
    fun `missing version only fails publishing`() {
        projectDir.resolve("gradle.properties").delete()
        writeBuild(
            """
            publishData {
                channels {
                    snapshot(uri("repo/snapshots").toString())
                }
            }
            """
        )
        commit()
        val build = run("testVersion", "jar")
        assertThat(version(build)).isEqualTo("unspecified")
        assertThat(build.output).contains("PublishData has no version for :")

        val publish = runner("publish").buildAndFail()
        assertThat(publish.output).contains("PublishData has no version for :")
        assertThat(projectDir.resolve("repo")).doesNotExist()
    }

    @Test
    fun `printVersion prints only the version when quiet`() {
        val result = run("printVersion", "-q", "--configuration-cache")
        assertThat(result.output.trim()).isEqualTo("1.5.0-SNAPSHOT")
    }

    @Test
    fun `printVersion yields to a task of the build script`() {
        writeBuild("""tasks.register("printVersion") { doLast { println("own task") } }""")
        commit()
        assertThat(run("printVersion", "-q").output.trim()).isEqualTo("own task")
    }

    @Test
    fun `channel suffix in the base version is removed with a warning`() {
        projectDir.resolve("gradle.properties").writeText("version=1.5.0-SNAPSHOT\n")
        val result = run("testVersion")
        assertThat(version(result)).isEqualTo("1.5.0-SNAPSHOT")
        assertThat(result.output).contains("The base version 1.5.0-SNAPSHOT of : ends with a channel suffix")
    }

    @Test
    fun `info log explains the resolution`() {
        val result = run("testVersion", "--info", env = mapOf("PUBLISHDATA_REF" to "refs/tags/v1.0.0"))
        assertThat(result.output).contains(
            "PublishData resolved version 1.0.0 of : in channel 'release' on GENERIC: tag 'v1.0.0' matches",
        )
    }

    @Test
    fun `resolution is frozen after evaluation`() {
        writeBuild(
            """
            afterEvaluate {
                publishData.channels { channel("late") { onAnyRef() } }
            }
            """
        )
        commit()
        assertThat(version(run("testVersion"))).isEqualTo("1.5.0-SNAPSHOT")
    }

    @Test
    fun `eldonexus preset reads credentials from gradle properties`() {
        writeBuild("publishData { eldoNexus() }")
        commit()
        val env = mapOf("PUBLISHDATA_REF" to "feature/x")

        val missing = runner("publish", "--dry-run", env = env).buildAndFail()
        assertThat(missing.output).contains("EldoNexusUsername")

        val provided = run(
            "publish", "--dry-run",
            env = env + mapOf(
                "ORG_GRADLE_PROJECT_EldoNexusUsername" to "user",
                "ORG_GRADLE_PROJECT_EldoNexusPassword" to "secret",
            ),
        )
        assertThat(provided.output).contains(":publishMavenPublicationToEldoNexusRepository SKIPPED")
    }

    @Test
    fun `groovy dsl`() {
        projectDir.resolve("build.gradle.kts").delete()
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'java-library'
                id 'maven-publish'
                id 'de.chojo.publishdata'
            }

            group = 'de.chojo'

            publishData {
                baseVersion = '2.0.0'
                artifactId = 'groovy-sample'
                channels {
                    release(uri('repo/releases').toString())
                    channel('dev', uri('repo/dev').toString()) {
                        onBranch(~/dev.*/).suffix('DEV').branchSuffix()
                    }
                    snapshot(uri('repo/snapshots').toString()) {
                        branchSuffix()
                    }
                }
                buildData {
                    includeTime = false
                    put('custom', 'value')
                }
            }
            """.trimIndent()
        )
        commit()
        val result = run("printVersion", "publish", env = mapOf("PUBLISHDATA_REF" to "feature/x"))
        assertThat(result.output).contains("2.0.0-feature-x-SNAPSHOT")
        assertThat(projectDir.resolve("repo/snapshots/de/chojo/groovy-sample/2.0.0-feature-x-SNAPSHOT")).isDirectory()
    }

    @Test
    fun `overriding the version in the build script is reported`() {
        writeBuild("version = \"9.9.9\"")
        commit()
        val result = run("testVersion")
        assertThat(version(result)).isEqualTo("9.9.9")
        assertThat(result.output).contains("replaces the version resolved by PublishData")
    }

    private fun writeBuild(configuration: String) {
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                `maven-publish`
                id("de.chojo.publishdata")
            }

            group = "de.chojo"

            $configuration

            tasks.register("testVersion") {
                val resolvedVersion = project.version.toString()
                doLast { println("VERSION=" + resolvedVersion) }
            }
            """.trimIndent()
        )
    }

    private fun commit() {
        git("add", "-A")
        git("commit", "-q", "--allow-empty", "-m", "change")
    }

    private fun version(result: BuildResult): String =
        result.output.lineSequence().first { it.startsWith("VERSION=") }.removePrefix("VERSION=")

    private fun run(vararg args: String, env: Map<String, String> = emptyMap()): BuildResult =
        runner(*args, env = env).build()

    private fun runner(vararg args: String, env: Map<String, String> = emptyMap()): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
            .withEnvironment(cleanEnvironment() + env)

    private fun cleanEnvironment(): Map<String, String> = System.getenv().filterKeys { key ->
        CI_VARIABLES.none { key == it || key.startsWith("${it}_") }
    }

    private fun git(vararg args: String): String {
        val process = ProcessBuilder(
            listOf(
                "git",
                "-c", "user.name=Test",
                "-c", "user.email=test@example.com",
                "-c", "commit.gpgsign=false",
                "-c", "tag.gpgsign=false",
                *args,
            )
        ).directory(projectDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed: $output" }
        return output
    }

    private companion object {
        val CI_VARIABLES = listOf("GITHUB", "GITLAB_CI", "CI", "PUBLISHDATA", "PUBLIC_BUILD", "BUILD_TYPE")
    }
}
