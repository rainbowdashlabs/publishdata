import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "de.chojo"
version = releaseVersion("2.1.0")

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

dependencies {
    implementation(libs.snakeyaml)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website = "https://github.com/rainbowdashlabs/publishdata"
    vcsUrl = "https://github.com/rainbowdashlabs/publishdata"
    plugins {
        create("publishData") {
            id = "de.chojo.publishdata"
            implementationClass = "de.chojo.publishdata.PublishDataPlugin"
            displayName = "PublishData"
            description = "Derives the project version from git tags and branches and publishes each build to " +
                "the right Maven repository: tags as releases, branches as snapshots. Works on GitHub Actions, " +
                "GitLab CI, Bitbucket Pipelines, Jenkins and locally, and can embed build information in the artifact."
            tags = listOf("versioning", "git", "release", "publishing", "maven", "ci")
        }
        create("publishDataSettings") {
            id = "de.chojo.publishdata.settings"
            implementationClass = "de.chojo.publishdata.PublishDataSettingsPlugin"
            displayName = "PublishData for settings"
            description = "Applies de.chojo.publishdata to every project of a build and configures it once, " +
                "in settings.gradle.kts."
            tags = listOf("versioning", "git", "release", "publishing", "maven", "ci")
        }
        create("pluginManifest") {
            id = "de.chojo.publishdata.manifest"
            implementationClass = "de.chojo.publishdata.manifest.PluginManifestPlugin"
            displayName = "PublishData plugin manifest"
            description = "Publishes a JSON manifest beside a Minecraft plugin's jar: supported Minecraft versions, " +
                "platforms, Java version and dependencies with links to Modrinth, SpigotMC and Hangar. Read from " +
                "plugin.yml or paper-plugin.yml and refined in the build script."
            tags = listOf("minecraft", "paper", "spigot", "plugin", "manifest", "publishing")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "EldoNexus"
            url = uri(releaseRepository())
            credentials(PasswordCredentials::class) {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }
}

/**
 * Derives the version of this plugin from the GitHub Actions ref: a tag is the release version,
 * branches get a channel suffix on top of [baseVersion]. Mirrors the plugin's `eldoNexus()` preset,
 * which the plugin cannot apply to itself.
 */
fun releaseVersion(baseVersion: String): String {
    val refName = System.getenv("GITHUB_REF_NAME") ?: return "$baseVersion-SNAPSHOT"
    return when {
        System.getenv("GITHUB_REF_TYPE") == "tag" -> refName.removePrefix("v")
        refName.startsWith("dev") -> "$baseVersion-DEV"
        else -> "$baseVersion-SNAPSHOT"
    }
}

/** Selects the EldoNexus repository matching [releaseVersion]. */
fun releaseRepository(): String {
    val version = project.version.toString()
    return when {
        version.endsWith("-SNAPSHOT") -> "https://eldonexus.de/repository/maven-snapshots/"
        version.endsWith("-DEV") -> "https://eldonexus.de/repository/maven-dev/"
        else -> "https://eldonexus.de/repository/maven-releases/"
    }
}
