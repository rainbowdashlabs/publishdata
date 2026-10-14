package de.chojo.publishdata.resolve

import de.chojo.publishdata.git.Ci
import java.io.Serializable

/**
 * The outcome of resolving a build against the configured channels.
 *
 * @property channel name of the selected channel, [Resolver.NO_CHANNEL] when none matched
 * @property version the version of the build
 * @property repository the repository to publish to, null when the channel has none
 * @property branch the built branch, if any
 * @property tag the tag the version was taken from, if any
 * @property commit the shortened commit hash, if known
 * @property ci the system the build runs on
 * @property public whether the build is public, see [de.chojo.publishdata.git.GitContext.public]
 * @property reason why the channel was selected, for logging
 */
data class Resolution(
    val channel: String,
    val version: String,
    val repository: String?,
    val branch: String?,
    val tag: String?,
    val commit: String?,
    val ci: Ci,
    val public: Boolean,
    val reason: String,
) : Serializable {
    /** False when the build needs a base version and none is set. */
    val hasVersion: Boolean get() = version != Resolver.UNSPECIFIED
}
