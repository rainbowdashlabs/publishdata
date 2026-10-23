/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.Properties

class PropertiesFormatTest {
    @Test
    fun `round trips through Properties`() {
        val entries =
            linkedMapOf(
                "plain" to "value",
                "key with space" to " leading space",
                "special" to "a=b:c#d!e\\f",
                "multi" to "line\nbreak",
                "unicode" to "Grüße ✓",
                "a=b:c" to "#value",
            )
        val properties =
            Properties().apply {
                load(ByteArrayInputStream(PropertiesFormat.format(entries).toByteArray(Charsets.ISO_8859_1)))
            }
        assertThat(properties).containsExactlyInAnyOrderEntriesOf(entries)
    }

    @Test
    fun `leaves separators in values unescaped`() {
        assertThat(PropertiesFormat.format(mapOf("time" to "2026-10-14T18:34:26Z", "url" to "a=b")))
            .isEqualTo("time=2026-10-14T18:34:26Z\nurl=a=b")
    }

    @Test
    fun `keeps order`() {
        assertThat(PropertiesFormat.format(linkedMapOf("b" to "1", "a" to "2"))).isEqualTo("b=1\na=2")
    }
}
