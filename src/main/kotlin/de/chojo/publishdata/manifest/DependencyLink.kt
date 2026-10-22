package de.chojo.publishdata.manifest

import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException

/**
 * Where a dependency is found. [url] is always written, so a reader that does not know [type] can
 * still follow it; the shortcut types carry the [id] the address was built from.
 */
data class DependencyLink(val type: String, val id: String?, val url: String) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
        private val SLUG = Regex("[A-Za-z0-9_.-]+")
        private val HANGAR = Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")

        /** A project on Modrinth, by its slug or id. */
        fun modrinth(project: String): DependencyLink {
            require(SLUG.matches(project)) { "'$project' is not a Modrinth project slug or id" }
            return DependencyLink("modrinth", project, "https://modrinth.com/plugin/$project")
        }

        /** A resource on SpigotMC, by its numeric id. */
        fun spigot(resource: Int): DependencyLink {
            require(resource > 0) { "'$resource' is not a SpigotMC resource id" }
            return DependencyLink("spigot", resource.toString(), "https://www.spigotmc.org/resources/$resource/")
        }

        /** A project on Hangar, as `owner/slug`. */
        fun hangar(project: String): DependencyLink {
            require(HANGAR.matches(project)) { "'$project' is not a Hangar project, which is written owner/slug" }
            return DependencyLink("hangar", project, "https://hangar.papermc.io/$project")
        }

        /** Any http or https address. */
        fun url(address: String): DependencyLink {
            val uri = try {
                URI(address)
            } catch (e: URISyntaxException) {
                throw IllegalArgumentException("'$address' is not an address", e)
            }
            require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
                "'$address' is not an http or https address"
            }
            return DependencyLink("url", null, address)
        }
    }
}
