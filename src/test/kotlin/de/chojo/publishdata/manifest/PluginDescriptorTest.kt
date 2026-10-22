package de.chojo.publishdata.manifest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PluginDescriptorTest {
    @TempDir
    lateinit var dir: File

    private fun file(name: String, text: String) = dir.resolve(name).apply { writeText(text.trimIndent()) }

    @Test
    fun `a plugin yml gives name, api version and hard and soft dependencies`() {
        val yml = file(
            "plugin.yml",
            """
            name: BloodNight
            api-version: '1.20'
            folia-supported: true
            depend: [Vault]
            softdepend: [PlaceholderAPI]
            """,
        )

        assertThat(PluginDescriptor.read(listOf(yml))).isEqualTo(
            PluginDescriptor(
                name = "BloodNight",
                apiVersion = "1.20",
                platforms = listOf("bukkit", "folia"),
                dependencies = listOf(
                    ManifestDependency("Vault", required = true),
                    ManifestDependency("PlaceholderAPI", required = false),
                ),
            )
        )
    }

    @Test
    fun `a paper plugin yml wins and reads server dependencies, required by default`() {
        val bukkit = file("plugin.yml", "name: Old\napi-version: '1.19'")
        val paper = file(
            "paper-plugin.yml",
            """
            name: BloodNight
            api-version: '1.21'
            dependencies:
              server:
                Vault:
                  load: BEFORE
                PlaceholderAPI:
                  required: false
            """,
        )

        val descriptor = PluginDescriptor.read(listOf(bukkit, paper))!!

        assertThat(descriptor.name).isEqualTo("BloodNight")
        assertThat(descriptor.apiVersion).isEqualTo("1.21")
        assertThat(descriptor.platforms).containsExactly("bukkit", "paper")
        assertThat(descriptor.dependencies).containsExactly(
            ManifestDependency("Vault", required = true),
            ManifestDependency("PlaceholderAPI", required = false),
        )
    }

    @Test
    fun `no descriptor, an empty one and one without dependencies`() {
        assertThat(PluginDescriptor.read(emptyList())).isNull()
        assertThat(PluginDescriptor.read(listOf(dir.resolve("plugin.yml")))).isNull()
        assertThat(PluginDescriptor.read(listOf(file("plugin.yml", ""))))
            .isEqualTo(PluginDescriptor(null, null, listOf("bukkit"), emptyList()))
        assertThat(PluginDescriptor.read(listOf(file("paper-plugin.yml", "name: P\ndependencies: {}"))))
            .isEqualTo(PluginDescriptor("P", null, listOf("paper"), emptyList()))
    }
}
