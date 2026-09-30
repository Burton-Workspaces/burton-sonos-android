package com.burton.sonos.data.repository

import com.burton.sonos.data.parse.TinyJson
import com.burton.sonos.data.parse.TinyJson.bool
import com.burton.sonos.data.parse.TinyJson.int
import com.burton.sonos.data.parse.TinyJson.objList
import com.burton.sonos.data.parse.TinyJson.str
import com.burton.sonos.data.parse.TinyJson.strList
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.ZoneGroup

object HouseholdCache {
    fun encode(household: Household): String = TinyJson.stringify(
        mapOf(
            "id" to household.id,
            "groups" to household.groups.map { group ->
                mapOf(
                    "id" to group.id,
                    "coordinatorUuid" to group.coordinatorUuid,
                    "memberUuids" to group.memberUuids,
                )
            },
            "players" to household.players.map { player ->
                mapOf(
                    "uuid" to player.uuid,
                    "name" to player.name,
                    "ip" to player.ip,
                    "port" to player.port,
                    "location" to player.location,
                    "model" to player.model,
                    "softwareVersion" to player.softwareVersion,
                    "invisible" to player.invisible,
                    "hasLineIn" to player.hasLineIn,
                    "hasHdmi" to player.hasHdmi,
                )
            },
        ),
    )

    fun decode(raw: String): Household? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = TinyJson.parseObject(raw)
            val players = root.objList("players").map { obj ->
                Player(
                    uuid = obj.str("uuid"),
                    name = obj.str("name", "Sonos"),
                    ip = obj.str("ip"),
                    port = obj.int("port", 1400),
                    location = obj.str("location"),
                    model = obj.str("model"),
                    softwareVersion = obj.str("softwareVersion"),
                    invisible = obj.bool("invisible"),
                    hasLineIn = obj.bool("hasLineIn"),
                    hasHdmi = obj.bool("hasHdmi"),
                )
            }
            val groups = root.objList("groups").map { obj ->
                ZoneGroup(
                    id = obj.str("id"),
                    coordinatorUuid = obj.str("coordinatorUuid"),
                    memberUuids = obj.strList("memberUuids"),
                )
            }
            if (players.isEmpty() || groups.isEmpty()) null
            else Household(id = root.str("id"), groups = groups, players = players)
        }.getOrNull()
    }
}

object NamedGroupCache {
    fun encode(groups: List<NamedGroup>): String = TinyJson.stringify(
        groups.map { group ->
            mapOf(
                "id" to group.id,
                "name" to group.name,
                "memberUuids" to group.memberUuids,
            )
        },
    )

    fun decode(raw: String): List<NamedGroup> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            TinyJson.parseArray(raw).mapNotNull { item ->
                val obj = item as? Map<*, *> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val typed = obj as Map<String, Any?>
                val id = typed.str("id")
                val name = typed.str("name")
                if (id.isBlank() || name.isBlank()) null
                else NamedGroup(id = id, name = name, memberUuids = typed.strList("memberUuids"))
            }
        }.getOrDefault(emptyList())
    }
}
