package de.chojo.publishdata.manifest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PluginManifestTest {
    private val descriptor = PluginDescriptor(
        name = "BloodNight",
        apiVersion = "1.20",
        platforms = listOf("bukkit"),
        dependencies = listOf(
            ManifestDependency("Vault", required = true),
            ManifestDependency("PlaceholderAPI", required = false),
        ),
    )

    @Test
    fun `the descriptor fills what the build script leaves out`() {
        val manifest = PluginManifest.of(descriptor, "1.4.0", PluginManifest.Declared())

        assertThat(manifest).isEqualTo(
            PluginManifest("BloodNight", "1.4.0", listOf("bukkit"), "1.20", null, null, descriptor.dependencies, emptyMap())
        )
    }

    @Test
    fun `the build script overrides and refines, and adds dependencies after the descriptor's`() {
        val papi = DependencyLink.modrinth("placeholderapi")
        val manifest = PluginManifest.of(
            descriptor,
            "1.4.0",
            PluginManifest.Declared(
                name = "Blood Night",
                platforms = listOf("paper", "folia"),
                minecraftMin = "1.20.4",
                minecraftMax = "1.21.4",
                java = 21,
                dependencies = listOf(
                    ManifestDependency("placeholderapi", required = true, min = "2.11.0", links = listOf(papi)),
                    ManifestDependency("LuckPerms", required = false),
                ),
                extra = mapOf("discord" to "https://discord.gg/x"),
            ),
        )

        assertThat(manifest.name).isEqualTo("Blood Night")
        assertThat(manifest.platforms).containsExactly("paper", "folia")
        assertThat(manifest.minecraftMin).isEqualTo("1.20.4")
        assertThat(manifest.minecraftMax).isEqualTo("1.21.4")
        assertThat(manifest.java).isEqualTo(21)
        assertThat(manifest.dependencies).containsExactly(
            ManifestDependency("Vault", required = true),
            ManifestDependency("PlaceholderAPI", required = true, min = "2.11.0", links = listOf(papi)),
            ManifestDependency("LuckPerms", required = false),
        )
    }

    @Test
    fun `refining keeps what the refinement leaves out and does not repeat a link`() {
        val link = DependencyLink.spigot(1)
        val original = ManifestDependency("X", required = false, min = "1", max = "2", links = listOf(link))

        assertThat(original.refinedBy(ManifestDependency("X", required = true, max = "3", links = listOf(link))))
            .isEqualTo(ManifestDependency("X", required = true, min = "1", max = "3", links = listOf(link)))
    }

    @Test
    fun `json carries the schema, leaves out what is unknown and escapes text`() {
        val manifest = PluginManifest(
            name = "Quote \" and \\ and\nline\u0001",
            version = "1.0.0",
            platforms = emptyList(),
            minecraftMin = "1.20",
            minecraftMax = null,
            java = 21,
            dependencies = listOf(
                ManifestDependency("Vault", required = true, links = listOf(DependencyLink.url("https://example.com/v")))
            ),
            extra = emptyMap(),
        )

        assertThat(manifest.toJson()).isEqualTo(
            """
            {
              "schema": 1,
              "name": "Quote \" and \\ and\nline\u0001",
              "version": "1.0.0",
              "platforms": [],
              "minecraft": {
                "min": "1.20"
              },
              "java": 21,
              "dependencies": [
                {
                  "name": "Vault",
                  "required": true,
                  "links": [
                    {
                      "type": "url",
                      "url": "https://example.com/v"
                    }
                  ]
                }
              ],
              "extra": {}
            }

            """.trimIndent()
        )
    }
}
