package de.chojo.publishdata.manifest

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DependencyLinkTest {
    @Test
    fun `shortcuts expand to the page on their site`() {
        assertThat(DependencyLink.modrinth("placeholderapi"))
            .isEqualTo(DependencyLink("modrinth", "placeholderapi", "https://modrinth.com/plugin/placeholderapi"))
        assertThat(DependencyLink.spigot(6245))
            .isEqualTo(DependencyLink("spigot", "6245", "https://www.spigotmc.org/resources/6245/"))
        assertThat(DependencyLink.hangar("HelpChat/PlaceholderAPI"))
            .isEqualTo(DependencyLink("hangar", "HelpChat/PlaceholderAPI", "https://hangar.papermc.io/HelpChat/PlaceholderAPI"))
        assertThat(DependencyLink.url("https://github.com/MilkBowl/Vault"))
            .isEqualTo(DependencyLink("url", null, "https://github.com/MilkBowl/Vault"))
    }

    @Test
    fun `malformed ids and addresses fail`() {
        assertThatThrownBy { DependencyLink.modrinth("place holder") }.hasMessageContaining("Modrinth")
        assertThatThrownBy { DependencyLink.modrinth("../evil") }.hasMessageContaining("Modrinth")
        assertThatThrownBy { DependencyLink.spigot(0) }.hasMessageContaining("SpigotMC")
        assertThatThrownBy { DependencyLink.hangar("PlaceholderAPI") }.hasMessageContaining("owner/slug")
        assertThatThrownBy { DependencyLink.url("javascript:alert(1)") }.hasMessageContaining("http")
        assertThatThrownBy { DependencyLink.url("ftp://example.com/x") }.hasMessageContaining("http")
        assertThatThrownBy { DependencyLink.url("https:///nohost") }.hasMessageContaining("http")
        assertThatThrownBy { DependencyLink.url("not a url") }.hasMessageContaining("address")
    }
}
