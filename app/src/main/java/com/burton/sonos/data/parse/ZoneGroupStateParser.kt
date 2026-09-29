package com.burton.sonos.data.parse

import com.burton.sonos.domain.Player
import com.burton.sonos.domain.ZoneGroup

object ZoneGroupStateParser {
    fun parse(escapedOrRawXml: String): Pair<List<ZoneGroup>, List<Player>> {
        val xml = if (escapedOrRawXml.contains("<ZoneGroup")) {
            Xml.unescapeXml(escapedOrRawXml).let { unescaped ->
                if (unescaped.contains("<ZoneGroup")) unescaped else escapedOrRawXml
            }
        } else {
            Xml.unescapeXml(escapedOrRawXml)
        }
        val wrapped = if (xml.contains("<ZoneGroupState")) xml else "<ZoneGroupState>$xml</ZoneGroupState>"
        val root = Xml.parse(wrapped).rootElement()
        val groups = root.descendants("ZoneGroup").map { groupEl ->
            val members = groupEl.descendants("ZoneGroupMember")
            ZoneGroup(
                id = groupEl.getAttribute("ID"),
                coordinatorUuid = groupEl.getAttribute("Coordinator"),
                memberUuids = members.map { it.getAttribute("UUID") },
            )
        }
        val players = root.descendants("ZoneGroupMember").map { member ->
            val location = member.getAttribute("Location")
            val (ip, port) = hostFromLocation(location)
            Player(
                uuid = member.getAttribute("UUID"),
                name = member.getAttribute("ZoneName").ifBlank { "Sonos" },
                ip = ip,
                port = port,
                location = location,
                model = member.getAttribute("MoreInfo").substringAfter("mdl=", "").substringBefore(";"),
                softwareVersion = member.getAttribute("SoftwareVersion"),
                invisible = member.getAttribute("Invisible") == "1",
                hasLineIn = member.getAttribute("LineOutLevel") != "" ||
                    member.getAttribute("MoreInfo").contains("linn=1") ||
                    looksLikeLineInModel(member.getAttribute("ZoneName"), member.getAttribute("MoreInfo")),
                hasHdmi = member.getAttribute("HTSatChanMapSet").isNotBlank() ||
                    member.getAttribute("MoreInfo").contains("ht=1"),
            )
        }.distinctBy { it.uuid }
        return groups to players
    }

    fun hostFromLocation(location: String): Pair<String, Int> {
        val withoutScheme = location.substringAfter("://", location)
        val hostPort = withoutScheme.substringBefore("/")
        val ip = hostPort.substringBefore(":")
        val port = hostPort.substringAfter(":", "1400").toIntOrNull() ?: 1400
        return ip to port
    }

    private fun looksLikeLineInModel(zoneName: String, moreInfo: String): Boolean {
        val blob = "$zoneName $moreInfo".lowercase()
        return listOf("play:5", "play5", "connect", "port", "amp", "five", "era 300").any { it in blob }
    }
}
