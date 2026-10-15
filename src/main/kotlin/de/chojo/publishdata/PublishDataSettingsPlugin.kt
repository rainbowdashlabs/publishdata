package de.chojo.publishdata

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Applies [PublishDataPlugin] to every project of the build and configures it with the
 * [PublishDataSettings.defaults] declared in the settings script.
 *
 * Being loaded by the settings, the plugin classes are shared with all build scripts: a project can
 * still apply `de.chojo.publishdata` itself, without a version, and gets the same plugin.
 */
class PublishDataSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val publishData = settings.extensions.create("publishData", PublishDataSettings::class.java)
        settings.gradle.beforeProject {
            pluginManager.apply(PublishDataPlugin::class.java)
            val extension = extensions.getByType(PublishDataExtension::class.java)
            publishData.defaults.forEach { it.execute(extension) }
        }
    }
}
