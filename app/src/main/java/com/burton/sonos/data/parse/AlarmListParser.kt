package com.burton.sonos.data.parse

import com.burton.sonos.domain.Alarm

object AlarmListParser {
    fun parse(escapedOrRawXml: String): List<Alarm> {
        if (escapedOrRawXml.isBlank()) return emptyList()
        val unescaped = Xml.unescapeXml(escapedOrRawXml)
        // SOAP textContent already unescapes CurrentAlarmList once. Unescaping again
        // turns ProgramURI `&amp;` and DIDL ProgramMetaData into invalid XML and
        // drops the whole household list. Try the raw payload first.
        return listOf(escapedOrRawXml, unescaped)
            .distinct()
            .firstNotNullOfOrNull { candidate -> parseDocument(candidate).takeIf { it.isNotEmpty() } }
            .orEmpty()
    }

    private fun parseDocument(raw: String): List<Alarm> {
        val wrapped = when {
            raw.contains("<Alarms") || raw.contains("<CurrentAlarmList") || raw.contains("<Alarm") ->
                if (raw.contains("<Alarms") || raw.contains("<CurrentAlarmList")) raw else "<Alarms>$raw</Alarms>"
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
