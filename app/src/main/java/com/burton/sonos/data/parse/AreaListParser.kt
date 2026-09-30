package com.burton.sonos.data.parse

import com.burton.sonos.data.parse.TinyJson.bool
import com.burton.sonos.data.parse.TinyJson.objList
import com.burton.sonos.data.parse.TinyJson.str
import com.burton.sonos.data.parse.TinyJson.strList
import com.burton.sonos.domain.NamedGroup

object AreaListParser {
    const val ID_PREFIX = "area-"

    fun parse(json: String): List<NamedGroup> {
        if (json.isBlank()) return emptyList()
        val root = runCatching { TinyJson.parseObject(json) }.getOrNull() ?: return emptyList()
        return root.objList("areas").mapNotNull { area ->
            if (area.bool("isReadOnly")) return@mapNotNull null
            val id = area.str("id")
            val name = area.str("name")
            val players = area.strList("playerIds")
            if (id.isBlank() || name.isBlank() || players.isEmpty()) null
            else NamedGroup(
                id = "$ID_PREFIX$id",
                name = name,
                memberUuids = players,
            )
        }
    }
}
