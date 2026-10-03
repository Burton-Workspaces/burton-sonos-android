package com.burton.sonos.ui.group

import com.burton.sonos.domain.NamedGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerGroupingPresetsTest {
    @Test
    fun everywhereIsAlwaysFirst() {
        val presets = SpeakerGroupingPresets.build(
            visibleUuids = listOf("A", "B", "C"),
            areas = listOf(NamedGroup("area-1", "Downstairs", listOf("A", "B"))),
            named = listOf(NamedGroup("g1", "Office", listOf("C"))),
        )
        assertEquals(SpeakerGroupingPresets.EVERYWHERE_ID, presets.first().id)
        assertEquals(SpeakerGroupingPresets.EVERYWHERE_NAME, presets.first().name)
        assertEquals(listOf("A", "B", "C"), presets.first().memberUuids)
        assertEquals(listOf("Everywhere", "Downstairs", "Office"), presets.map { it.name })
    }

    @Test
    fun skipsReservedEverywhereAndLiveDuplicates() {
        val presets = SpeakerGroupingPresets.build(
            visibleUuids = listOf("A", "B"),
            areas = listOf(
                NamedGroup("area-everywhere", "Everywhere", listOf("A", "B")),
                NamedGroup("area-1", "Downstairs", listOf("A", "B")),
            ),
            named = listOf(
                NamedGroup("live-A-B", "A + B", listOf("A", "B")),
                NamedGroup("dup", "Downstairs copy", listOf("A", "B")),
                NamedGroup("g1", "Office", listOf("A")),
            ),
        )
        assertEquals(listOf("Everywhere", "Downstairs", "Office"), presets.map { it.name })
    }

    @Test
    fun matchingPrefersEverywhereWhenAllRoomsSelected() {
        val visible = setOf("A", "B")
        val presets = SpeakerGroupingPresets.build(visible.toList(), emptyList(), emptyList())
        assertEquals(
            SpeakerGroupingPresets.EVERYWHERE_ID,
            SpeakerGroupingPresets.matchingId(presets, visible, visible),
        )
        assertTrue(SpeakerGroupingPresets.matchingId(presets, setOf("A"), visible) == null)
    }
}
