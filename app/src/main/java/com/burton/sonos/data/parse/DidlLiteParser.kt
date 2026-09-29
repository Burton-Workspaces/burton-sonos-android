package com.burton.sonos.data.parse

import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Track
import org.w3c.dom.Element

object DidlLiteParser {
    fun parseItems(didl: String, speakerBaseUrl: String): List<BrowseItem> {
        val root = parseRoot(didl) ?: return emptyList()
        val nodes = root.descendants("item") + root.descendants("container")
        return nodes.map { el ->
            val upnpClass = el.childText("class")
            val isContainer = el.matchesName("container") || upnpClass.contains("container", ignoreCase = true)
            val res = el.child("res")
            BrowseItem(
                id = el.getAttribute("id"),
                parentId = el.getAttribute("parentID"),
                title = el.childText("title").ifBlank { "Untitled" },
                subtitle = listOf(el.childText("creator"), el.childText("artist"), el.childText("album"))
                    .firstOrNull { it.isNotBlank() },
                albumArtUrl = resolveAlbumArt(speakerBaseUrl, el.childText("albumArtURI").ifBlank { null }),
                uri = res?.textContent?.trim()?.ifBlank { null },
                metadata = serializeItem(el),
                upnpClass = upnpClass,
                isContainer = isContainer,
            )
        }
    }

    fun parseTrack(didl: String, speakerBaseUrl: String, fallbackUri: String): Track? {
        val item = parseItems(didl, speakerBaseUrl).firstOrNull() ?: return null
        val duration = durationSecondsFromRes(didl)
        return Track(
            title = item.title,
            artist = item.subtitle.orEmpty(),
            album = albumFrom(didl),
            albumArtUrl = item.albumArtUrl,
            uri = item.uri ?: fallbackUri,
            durationSeconds = duration,
        )
    }

    fun resolveAlbumArt(speakerBaseUrl: String, uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        val trimmed = Xml.unescapeXml(uri)
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "$speakerBaseUrl$trimmed"
            else -> "$speakerBaseUrl/$trimmed"
        }
    }

    private fun albumFrom(didl: String): String =
        parseRoot(didl)?.descendants("album")?.firstOrNull()?.textContent?.trim().orEmpty()

    private fun durationSecondsFromRes(didl: String): Int {
        val duration = parseRoot(didl)?.descendants("res")?.firstOrNull()?.getAttribute("duration").orEmpty()
        return parseHms(duration)
    }

    private fun parseRoot(didl: String): Element? {
        if (didl.isBlank() || didl.equals("NOT_IMPLEMENTED", ignoreCase = true)) return null
        val unescaped = Xml.unescapeXml(didl)
        val candidates = listOf(didl, unescaped)
            .map { it.trim() }
            .filter { it.startsWith("<") }
            .distinct()
        for (candidate in candidates) {
            val root = runCatching { Xml.parse(ensureDidl(candidate)).rootElement() }.getOrNull()
            if (root != null) return root
        }
        return null
    }

    fun parseHms(value: String): Int {
        if (value.isBlank() || value == "NOT_IMPLEMENTED" || value == "0:00:00") return 0
        val parts = value.split(":").mapNotNull { it.toDoubleOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]).toInt()
            2 -> (parts[0] * 60 + parts[1]).toInt()
            1 -> parts[0].toInt()
            else -> 0
        }
    }

    private fun ensureDidl(xml: String): String {
        val trimmed = xml.trim()
        return if (trimmed.startsWith("<")) trimmed else "<DIDL-Lite>$trimmed</DIDL-Lite>"
    }

    private fun serializeItem(el: Element): String {
        val res = el.child("res")?.textContent?.trim().orEmpty()
        val resXml = if (res.isBlank()) {
            ""
        } else {
            "<res>${Xml.escapeXml(res)}</res>"
        }
        return "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" " +
            "xmlns:r=\"urn:schemas-rinconnetworks-com:metadata-1-0/\" " +
            "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\">" +
            "<item id=\"${Xml.escapeXml(el.getAttribute("id"))}\" " +
            "parentID=\"${Xml.escapeXml(el.getAttribute("parentID"))}\" restricted=\"true\">" +
            resXml +
            "<dc:title>${Xml.escapeXml(el.childText("title"))}</dc:title>" +
            "<upnp:class>${Xml.escapeXml(el.childText("class"))}</upnp:class>" +
            "</item></DIDL-Lite>"
    }
}
