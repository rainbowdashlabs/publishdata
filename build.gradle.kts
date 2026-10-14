import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "de.chojo"
version = releaseVersion("2.0.0")

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
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
            description = "Sets the project version from git tags and branches, adds build data to the artifact " +
                "and publishes to the repository of the matching release channel."
            tags = listOf("versioning", "git", "publishing", "maven", "release")
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
