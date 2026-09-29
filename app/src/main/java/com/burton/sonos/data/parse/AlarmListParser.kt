package com.burton.sonos.data.parse

import com.burton.sonos.domain.Alarm

object AlarmListParser {
    fun parse(escapedOrRawXml: String): List<Alarm> {
        if (escapedOrRawXml.isBlank()) return emptyList()
        val xml = if (escapedOrRawXml.contains("<Alarm")) {
            Xml.unescapeXml(escapedOrRawXml).let { unescaped ->
                if (unescaped.contains("<Alarm")) unescaped else escapedOrRawXml
            }
        } else {
            Xml.unescapeXml(escapedOrRawXml)
        }
        val wrapped = when {
            xml.contains("<Alarms") -> xml
            xml.contains("<Alarm") -> "<Alarms>$xml</Alarms>"
            else -> return emptyList()
        }
        val root = runCatching { Xml.parse(wrapped).rootElement() }.getOrNull() ?: return emptyList()
        return root.descendants("Alarm").mapNotNull { el ->
            val id = el.getAttribute("ID").ifBlank { return@mapNotNull null }
            Alarm(
                id = id,
                startTime = el.getAttribute("StartTime").ifBlank { "07:00:00" },
                duration = el.getAttribute("Duration").ifBlank { Alarm.DEFAULT_DURATION },
                recurrence = el.getAttribute("Recurrence").ifBlank { "DAILY" },
                enabled = el.getAttribute("Enabled") == "1",
                roomUuid = el.getAttribute("RoomUUID"),
                programUri = el.getAttribute("ProgramURI").ifBlank { Alarm.BUZZER_URI },
                programMetaData = el.getAttribute("ProgramMetaData"),
                playMode = el.getAttribute("PlayMode").ifBlank { "NORMAL" },
                volume = el.getAttribute("Volume").toIntOrNull()?.coerceIn(0, 100) ?: 25,
                includeLinkedZones = el.getAttribute("IncludeLinkedZones") == "1",
            )
        }.sortedBy { it.startTime }
    }
}
