/*
 *     SPDX-License-Identifier: LGPL-3.0-or-later
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.publishdata

/**
 * Writes entries in the `.properties` format, in their given order and without a header.
 *
 * Everything outside printable ASCII is written as a unicode escape, so the result reads the same
 * through `Properties.load` from a stream (ISO-8859-1) and from a reader. Separators are only
 * escaped in keys, so values like timestamps stay readable for consumers that split on `=`.
 */
internal object PropertiesFormat {
    /** Formats [entries] as one `key=value` line each. */
    fun format(entries: Map<String, String>): String =
        entries.entries.joinToString("\n") { (key, value) -> "${escape(key, true)}=${escape(value, false)}" }

    private fun escape(
        text: String,
        isKey: Boolean,
    ): String =
        buildString {
            text.forEachIndexed { index, char ->
                when {
                    char == '\\' -> append("\\\\")
                    char == '\n' -> append("\\n")
                    char == '\r' -> append("\\r")
                    char == '\t' -> append("\\t")
                    char == '\u000c' -> append("\\f")
                    isKey && (char in "=:" || (index == 0 && char in "#!")) -> append('\\').append(char)
                    char == ' ' && (isKey || index == 0) -> append("\\ ")
                    char < ' ' || char > '~' -> append("\\u%04x".format(char.code))
                    else -> append(char)
                }
            }
        }
}
