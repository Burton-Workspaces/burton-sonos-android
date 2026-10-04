package com.burton.sonos.data.library

import com.burton.sonos.domain.BrowseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefixIndexTest {
    @Test
    fun parsesAlternatingCsv() {
        val parsed = PrefixIndex.parse("A,0,B,12,C,40,#,2")
        assertEquals(listOf("#", "A", "B", "C"), parsed.map { it.prefix })
        assertEquals(0, parsed.first { it.prefix == "A" }.index)
        assertEquals(12, parsed.first { it.prefix == "B" }.index)
        assertEquals(2, parsed.first { it.prefix == "#" }.index)
    }

    @Test
    fun parsesDelimitedCsv() {
        val parsed = PrefixIndex.parse("A:0,B:18,Z:900")
        assertEquals(listOf("A", "B", "Z"), parsed.map { it.prefix })
        assertEquals(18, parsed[1].index)
        assertEquals(900, parsed[2].index)
    }

    @Test
    fun ignoresEmptyAndOddJunk() {
        assertEquals(emptyList<PrefixLocation>(), PrefixIndex.parse(""))
        assertEquals(emptyList<PrefixLocation>(), PrefixIndex.parse("A"))
        val parsed = PrefixIndex.parse("A,0,B")
        assertEquals(listOf("A"), parsed.map { it.prefix })
    }

    @Test
    fun letterForUsesFirstAlphanumeric() {
        assertEquals("N", PrefixIndex.letterFor("Night Drive"))
        assertEquals("B", PrefixIndex.letterFor("  beatles"))
        assertEquals("#", PrefixIndex.letterFor("99 Problems"))
        assertEquals("#", PrefixIndex.letterFor("..."))
    }

    @Test
    fun localJumpOnCatalogsNotQueue() {
        val artists = (1..12).map { container("Artist $it") }
        assertTrue(PrefixIndex.supportsLocalJump("A:ALBUMARTIST", artists))
        assertTrue(PrefixIndex.supportsLocalJump("A:TRACKS", artists.map { it.copy(isContainer = false) }))
        assertFalse(PrefixIndex.supportsLocalJump("Q:0", artists))
        assertFalse(PrefixIndex.supportsLocalJump("A:ALBUM/Dark Side", artists.map { it.copy(isContainer = false) }))
        assertFalse(PrefixIndex.supportsLocalJump("A:ALBUMARTIST", artists.take(3)))
    }

    private fun container(title: String) = BrowseItem(
        id = title,
        parentId = "A:ALBUMARTIST",
        title = title,
        subtitle = null,
        albumArtUrl = null,
        uri = null,
        metadata = null,
        upnpClass = "object.container",
        isContainer = true,
    )
}
