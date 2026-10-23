/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata.publication

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CentralPomRequirementsTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `complete pom misses nothing`() {
        assertThat(CentralPomRequirements.missing(pom(COMPLETE))).isEmpty()
    }

    @Test
    fun `bare pom misses every element`() {
        assertThat(CentralPomRequirements.missing(pom("")))
            .containsExactly("name", "description", "url", "licenses", "developers", "scm")
    }

    @Test
    fun `blank and empty elements count as missing`() {
        val content =
            """
            <name>Sample</name>
            <description> </description>
            <url>https://example.com</url>
            <licenses/>
            <developers><developer><id>dev</id></developer></developers>
            <scm><connection>scm:git:x</connection></scm>
            """
        assertThat(CentralPomRequirements.missing(pom(content))).containsExactly("description", "licenses", "scm")
    }

    @Test
    fun `elements of nested sections do not count for the project`() {
        val content =
            """
            <parent><name>Parent</name><description>Parent</description><url>https://example.com</url></parent>
            """
        assertThat(CentralPomRequirements.missing(pom(content))).contains("name", "description", "url")
    }

    private fun pom(content: String): File =
        dir.resolve("pom.xml").apply {
            writeText(
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  $content
                </project>
                """.trimIndent(),
            )
        }

    private companion object {
        const val COMPLETE =
            """
            <name>Sample</name>
            <description>A sample</description>
            <url>https://example.com</url>
            <licenses><license><name>MIT</name></license></licenses>
            <developers><developer><id>dev</id></developer></developers>
            <scm><url>https://example.com/scm</url></scm>
            """
    }
}
