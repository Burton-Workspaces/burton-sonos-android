package com.burton.sonos.data.library

import com.burton.sonos.domain.BrowseItem

data class PrefixLocation(
    val prefix: String,
    val index: Int,
)

object PrefixIndex {
    fun parse(csv: String): List<PrefixLocation> {
        val parts = csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return emptyList()
        val delimited = parseDelimited(parts)
        if (delimited.size >= 2) return distinct(delimited)
        return distinct(parseAlternating(parts))
    }

    fun letterFor(title: String): String {
        val ch = title.trim().firstOrNull { it.isLetterOrDigit() } ?: return "#"
        return if (ch.isLetter()) ch.uppercaseChar().toString() else "#"
    }

    fun lettersIn(titles: Iterable<String>): List<String> =
        titles.map { letterFor(it) }.distinct().sortedWith(letterOrder)

    fun supportsLocalJump(objectId: String, items: List<BrowseItem>): Boolean {
        if (items.size < 12) return false
        if (objectId == "Q:0" || objectId.startsWith("Q:")) return false
        if (items.any { it.isContainer }) return true
        return objectId == "A:TRACKS" || objectId.startsWith("A:TRACKS:")
    }

    private fun parseDelimited(parts: List<String>): List<PrefixLocation> {
        val separator = listOf(':', '*', '|').firstOrNull { sep ->
            parts.count { part -> part.contains(sep) && part.substringAfter(sep).toIntOrNull() != null } >= 2
        } ?: return emptyList()
        return parts.mapNotNull { part ->
            val prefix = part.substringBefore(separator).trim()
            val index = part.substringAfter(separator).trim().toIntOrNull() ?: return@mapNotNull null
            locationOrNull(prefix, index)
        }
    }

    private fun parseAlternating(parts: List<String>): List<PrefixLocation> {
        val result = ArrayList<PrefixLocation>(parts.size / 2)
        var index = 0
        while (index + 1 < parts.size) {
            val prefix = parts[index]
            val start = parts[index + 1].toIntOrNull()
            if (start != null && prefix.toIntOrNull() == null) {
                locationOrNull(prefix, start)?.let(result::add)
                index += 2
            } else {
                index += 1
            }
        }
        return result
    }

    private fun locationOrNull(prefix: String, index: Int): PrefixLocation? {
        if (prefix.isEmpty() || index < 0) return null
        val display = if (prefix.length == 1) prefix.uppercase() else prefix
        return PrefixLocation(display, index)
    }

    private fun distinct(locations: List<PrefixLocation>): List<PrefixLocation> =
        locations.distinctBy { it.prefix.uppercase() }.sortedWith(compareBy(letterOrder) { it.prefix })

    private val letterOrder = compareBy<String> { key ->
        when {
            key == "#" -> 0
            key.length == 1 && key[0] in 'A'..'Z' -> 1
            else -> 2
        }
    }.thenBy { it }
}
