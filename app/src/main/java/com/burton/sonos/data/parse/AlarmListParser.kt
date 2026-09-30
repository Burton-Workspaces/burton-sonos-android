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
            xml.contains("<Alarms") || xml.contains("<CurrentAlarmList") || xml.contains("<Alarm") ->
                if (xml.contains("<Alarms") || xml.contains("<CurrentAlarmList")) xml else "<Alarms>$xml</Alarms>"
            else -> return emptyList()
        }
        val root = runCatching { Xml.parse(wrapped).rootElement() }.getOrNull() ?: return emptyList()
        return root.descendants("Alarm").mapNotNull { el ->
            val id = el.attr("ID", "id").ifBlank { return@mapNotNull null }
            val enabledRaw = el.attr("Enabled", "enabled")
            Alarm(
                id = id,
                startTime = el.attr("StartTime", "StartLocalTime").ifBlank { "07:00:00" },
                duration = el.attr("Duration").ifBlank { Alarm.DEFAULT_DURATION },
                recurrence = el.attr("Recurrence").ifBlank { "DAILY" },
                enabled = enabledRaw == "1" || enabledRaw.equals("true", ignoreCase = true),
                roomUuid = el.attr("RoomUUID", "RoomUuid"),
                programUri = el.attr("ProgramURI", "ProgramUri").ifBlank { Alarm.BUZZER_URI },
                programMetaData = el.attr("ProgramMetaData"),
                playMode = el.attr("PlayMode").ifBlank { "NORMAL" },
                volume = el.attr("Volume").toIntOrNull()?.coerceIn(0, 100) ?: 25,
                includeLinkedZones = el.attr("IncludeLinkedZones").let {
                    it == "1" || it.equals("true", ignoreCase = true)
                },
            )
        }.sortedBy { it.startTime }
    }
}
