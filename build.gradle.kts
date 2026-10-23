import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.publishdata)
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xjdk-release=21")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
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
    website = "https://docs.chojo.dev/publishdata"
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

publishData {
    eldoNexus(dev = false)
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "PublishData"
            description = "Derives the project version from git tags and branches and publishes each build to " +
                "the right Maven repository."
            url = "https://github.com/rainbowdashlabs/publishdata"
            inceptionYear = "2021"
            licenses {
                license {
                    name = "LGPL-3.0-or-later"
                    url = "https://www.gnu.org/licenses/lgpl-3.0.html"
                }
            }
            developers {
                developer {
                    id = "rainbowdashlabs"
                    name = "Nora Fülling"
                    email = "mail@chojo.dev"
                    url = "https://github.com/rainbowdashlabs"
                }
            }
            scm {
                url = "https://github.com/rainbowdashlabs/publishdata"
                connection = "scm:git:git://github.com/rainbowdashlabs/publishdata.git"
                developerConnection = "scm:git:ssh://github.com/rainbowdashlabs/publishdata.git"
            }
        }
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}
