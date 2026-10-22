package de.chojo.publishdata.manifest

/**
 * Writes maps, lists, strings, numbers and booleans as JSON. Null values in a map are left out, so
 * an absent field and an unknown one read the same.
 */
internal object ManifestJson {
    fun write(value: Any?, indent: String = ""): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> obj(value, indent)
        is Iterable<*> -> array(value.toList(), indent)
        else -> quote(value.toString())
    }

    private fun obj(map: Map<*, *>, indent: String): String {
        val entries = map.filterValues { it != null }
        if (entries.isEmpty()) return "{}"
        val inner = "$indent  "
        return entries.entries.joinToString(",\n", "{\n", "\n$indent}") { (key, value) ->
            "$inner${quote(key.toString())}: ${write(value, inner)}"
        }
    }

    private fun array(items: List<*>, indent: String): String {
        if (items.isEmpty()) return "[]"
        val inner = "$indent  "
        return items.joinToString(",\n", "[\n", "\n$indent]") { "$inner${write(it, inner)}" }
    }

    private fun quote(text: String): String = buildString {
        append('"')
        for (char in text) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
            }
        }
        append('"')
    }
}
