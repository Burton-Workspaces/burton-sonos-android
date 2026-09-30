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
                id = groupEl.attr("ID", "id"),
                coordinatorUuid = groupEl.attr("Coordinator"),
                memberUuids = members.map { it.attr("UUID") }.filter { it.isNotBlank() },
            )
        }
        val players = root.descendants("ZoneGroupMember").map { member ->
            val location = member.attr("Location")
            val moreInfo = member.attr("MoreInfo")
            val zoneName = member.attr("ZoneName")
            val (ip, port) = hostFromLocation(location)
            Player(
                uuid = member.attr("UUID"),
                name = zoneName.ifBlank { "Sonos" },
                ip = ip,
                port = port,
                location = location,
                model = moreInfo.substringAfter("mdl=", "").substringBefore(";"),
                softwareVersion = member.attr("SoftwareVersion"),
                invisible = member.attr("Invisible").let { it == "1" || it.equals("true", ignoreCase = true) },
                hasLineIn = member.attr("LineOutLevel").isNotBlank() ||
                    moreInfo.contains("linn=1") ||
                    looksLikeLineInModel(zoneName, moreInfo),
                hasHdmi = member.attr("HTSatChanMapSet").isNotBlank() ||
                    moreInfo.contains("ht=1"),
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
