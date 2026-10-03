package com.burton.sonos.ui.group

import com.burton.sonos.domain.NamedGroup

object SpeakerGroupingPresets {
    const val EVERYWHERE_ID = "everywhere"
    const val EVERYWHERE_NAME = "Everywhere"

    fun build(
        visibleUuids: List<String>,
        areas: List<NamedGroup>,
        named: List<NamedGroup>,
    ): List<NamedGroup> {
        val everywhere = NamedGroup(
            id = EVERYWHERE_ID,
            name = EVERYWHERE_NAME,
            memberUuids = visibleUuids,
        )
        val areaSets = areas.map { it.memberUuids.toSet() }.toSet()
        val areaPills = areas.filterNot { it.name.equals(EVERYWHERE_NAME, ignoreCase = true) }
        val namedPills = named.filterNot { group ->
            group.id.startsWith("live-") ||
                group.name.equals(EVERYWHERE_NAME, ignoreCase = true) ||
                group.memberUuids.isEmpty() ||
                group.memberUuids.toSet() in areaSets
        }
        return listOf(everywhere) + areaPills + namedPills
    }

    fun matchingId(
        presets: List<NamedGroup>,
        selected: Set<String>,
        visible: Set<String>,
    ): String? = presets.firstOrNull { preset ->
        preset.memberUuids.filter { it in visible }.toSet() == selected
    }?.id
}
