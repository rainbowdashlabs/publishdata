package de.chojo.publishdata.manifest

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PluginManifestFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "sample"""")
        projectDir.resolve("src/main/resources").mkdirs()
        projectDir.resolve("src/main/resources/plugin.yml").writeText(
            """
            name: Sample
            version: '${'$'}{version}'
            api-version: '1.20'
            depend: [Vault]
            softdepend: [PlaceholderAPI]
            """.trimIndent()
        )
    }

    private fun writeBuild(plugins: String, body: String) {
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                `maven-publish`
                $plugins
            }

            group = "de.chojo"

            java {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            tasks.processResources {
                val version = project.version.toString()
                inputs.property("version", version)
                filesMatching("plugin.yml") { expand("version" to version) }
            }

            $body
            """.trimIndent()
        )
    }

    private val manifestBlock = """
        pluginManifest {
            minecraft("1.20.4", "1.21.4")
            optional("PlaceholderAPI", min = "2.11.0") {
                modrinth("placeholderapi")
                spigot(6245)
                hangar("HelpChat/PlaceholderAPI")
            }
            requires("ProtocolLib") {
                link("https://github.com/dmulloy2/ProtocolLib")
            }
            put("discord", "https://discord.gg/x")
        }
    """.trimIndent()

    @Test
    fun `the manifest is published beside the jar with the manifest classifier`() {
        writeBuild(
            """id("de.chojo.publishdata.manifest")""",
            """
            version = "1.4.0"
            $manifestBlock
            publishing {
                publications { create<MavenPublication>("maven") { from(components["java"]) } }
                repositories { maven { url = uri("repo") } }
            }
            """,
        )

        run("publish")

        val published = projectDir.resolve("repo/de/chojo/sample/1.4.0/sample-1.4.0-manifest.json")
        assertThat(projectDir.resolve("repo/de/chojo/sample/1.4.0/sample-1.4.0.jar")).exists()
        assertThat(published.readText()).isEqualTo(
            """
            {
              "schema": 1,
              "name": "Sample",
              "version": "1.4.0",
              "platforms": [
                "bukkit"
              ],
              "minecraft": {
                "min": "1.20.4",
                "max": "1.21.4"
              },
              "java": 17,
              "dependencies": [
                {
                  "name": "Vault",
                  "required": true,
                  "links": []
                },
                {
                  "name": "PlaceholderAPI",
                  "required": false,
                  "min": "2.11.0",
                  "links": [
                    {
                      "type": "modrinth",
                      "id": "placeholderapi",
                      "url": "https://modrinth.com/plugin/placeholderapi"
                    },
                    {
                      "type": "spigot",
                      "id": "6245",
                      "url": "https://www.spigotmc.org/resources/6245/"
                    },
                    {
                      "type": "hangar",
                      "id": "HelpChat/PlaceholderAPI",
                      "url": "https://hangar.papermc.io/HelpChat/PlaceholderAPI"
                    }
                  ]
                },
                {
                  "name": "ProtocolLib",
                  "required": true,
                  "links": [
                    {
                      "type": "url",
                      "url": "https://github.com/dmulloy2/ProtocolLib"
                    }
                  ]
                }
              ],
              "extra": {
                "discord": "https://discord.gg/x"
              }
            }

            """.trimIndent()
        )
        assertThat(projectDir.resolve("repo/de/chojo/sample/1.4.0/sample-1.4.0.pom").readText())
            .doesNotContain("manifest")
    }

    @Test
    fun `the manifest follows the version publishdata resolves and rides its publication`() {
        writeBuild(
            """id("de.chojo.publishdata")
                id("de.chojo.publishdata.manifest")""",
            """
            publishData {
                baseVersion = "2.0.0"
                channels { snapshot(uri("repo/snapshots").toString()) }
            }
            """,
        )

        run("publishChannel")

        val directory = projectDir.resolve("repo/snapshots/de/chojo/sample/2.0.0-SNAPSHOT")
        val manifest = directory.listFiles()!!.single { it.name.endsWith("-manifest.json") }
        assertThat(manifest.readText()).contains("\"version\": \"2.0.0-SNAPSHOT\"", "\"min\": \"1.20\"")
    }

    @Test
    fun `the task is cached, works with the configuration cache and reruns when plugin yml changes`() {
        writeBuild("""id("de.chojo.publishdata.manifest")""", """version = "1.0.0"""")

        assertThat(run("generatePluginManifest", "--configuration-cache").task(":generatePluginManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        val again = run("generatePluginManifest", "--configuration-cache")
        assertThat(again.task(":generatePluginManifest")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        assertThat(again.output).contains("Reusing configuration cache")

        projectDir.resolve("src/main/resources/plugin.yml").writeText("name: Renamed\napi-version: '1.21'")
        assertThat(run("generatePluginManifest", "--configuration-cache").task(":generatePluginManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/pluginManifest/manifest.json").readText())
            .contains("\"name\": \"Renamed\"", "\"min\": \"1.21\"")
    }

    @Test
    fun `a project without a descriptor warns about the missing name`() {
        projectDir.resolve("src/main/resources/plugin.yml").delete()
        writeBuild("""id("de.chojo.publishdata.manifest")""", """version = "1.0.0"""")

        val result = run("generatePluginManifest")

        assertThat(result.output).contains("has no name")
        assertThat(projectDir.resolve("build/pluginManifest/manifest.json").readText())
            .doesNotContain("\"name\"")
            .contains("\"platforms\": []")
    }

    @Test
    fun `a plugin yml generated by eldoria's plugin-yml is read like a written one`() {
        projectDir.resolve("src/main/resources/plugin.yml").delete()
        writeBuild(
            """id("de.eldoria.plugin-yml.bukkit") version "$PLUGIN_YML"
                id("de.chojo.publishdata.manifest")""",
            """
            version = "1.0.0"
            bukkit {
                main = "de.chojo.sample.Sample"
                apiVersion = "1.20"
                foliaSupported = true
                depend = listOf("Vault")
                softDepend = listOf("PlaceholderAPI")
            }
            pluginManifest {
                optional("PlaceholderAPI") { modrinth("placeholderapi") }
            }
            """,
        )

        run("generatePluginManifest")

        val manifest = projectDir.resolve("build/pluginManifest/manifest.json").readText()
        assertThat(manifest).contains(
            "\"name\": \"sample\"",
            "\"bukkit\",\n    \"folia\"",
            "\"min\": \"1.20\"",
            "\"name\": \"Vault\",\n      \"required\": true",
            "\"name\": \"PlaceholderAPI\",\n      \"required\": false",
            "https://modrinth.com/plugin/placeholderapi",
        )
    }

    @Test
    fun `a paper plugin yml generated by eldoria's plugin-yml gives its server dependencies`() {
        projectDir.resolve("src/main/resources/plugin.yml").delete()
        writeBuild(
            """id("de.eldoria.plugin-yml.paper") version "$PLUGIN_YML"
                id("de.chojo.publishdata.manifest")""",
            """
            version = "1.0.0"
            paper {
                main = "de.chojo.sample.Sample"
                apiVersion = "1.21"
                serverDependencies {
                    register("Vault") { required = true }
                    register("LuckPerms") { required = false }
                }
            }
            """,
        )

        run("generatePluginManifest")

        val manifest = projectDir.resolve("build/pluginManifest/manifest.json").readText()
        assertThat(manifest).contains(
            "\"platforms\": [\n    \"paper\"\n  ]",
            "\"min\": \"1.21\"",
            "\"name\": \"Vault\",\n      \"required\": true",
            "\"name\": \"LuckPerms\",\n      \"required\": false",
        )
    }

    @Test
    fun `a malformed link fails the build and says why`() {
        writeBuild(
            """id("de.chojo.publishdata.manifest")""",
            """
            version = "1.0.0"
            pluginManifest { requires("X") { hangar("no-slash") } }
            """,
        )

        val result = runner("generatePluginManifest").buildAndFail()

        assertThat(result.output).contains("is not a Hangar project")
    }

    private companion object {
        /** The release of eldoria's plugin-yml the stories generate descriptors with. */
        const val PLUGIN_YML = "0.9.0"
    }

    private fun run(vararg args: String): BuildResult = runner(*args).build()

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
            .withEnvironment(System.getenv().filterKeys { !it.startsWith("GITHUB_") && !it.startsWith("CI") && it != "PUBLIC_BUILD" })
}
