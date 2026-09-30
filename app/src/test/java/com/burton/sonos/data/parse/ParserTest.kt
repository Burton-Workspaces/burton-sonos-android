package com.burton.sonos.data.parse

import com.burton.sonos.data.library.LibrarySearch
import com.burton.sonos.data.parse.DidlLiteParser
import com.burton.sonos.data.parse.TinyJson
import com.burton.sonos.data.parse.ZoneGroupStateParser
import com.burton.sonos.data.repository.HouseholdCache
import com.burton.sonos.data.repository.NamedGroupCache
import com.burton.sonos.domain.Alarm
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.ZoneGroup
import com.burton.sonos.domain.daysFromRecurrence
import com.burton.sonos.domain.recurrenceFromDays
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneGroupStateParserTest {
    @Test
    fun parsesCoordinatorAndMembers() {
        val xml = """
            <ZoneGroupState>
              <ZoneGroups>
                <ZoneGroup Coordinator="RINCON_AAA" ID="RINCON_AAA:1">
                  <ZoneGroupMember UUID="RINCON_AAA" Location="http://192.168.1.20:1400/xml/device_description.xml" ZoneName="Kitchen" SoftwareVersion="80.0" Invisible="0"/>
                  <ZoneGroupMember UUID="RINCON_BBB" Location="http://192.168.1.21:1400/xml/device_description.xml" ZoneName="Dining" SoftwareVersion="80.0" Invisible="0"/>
                </ZoneGroup>
              </ZoneGroups>
            </ZoneGroupState>
        """.trimIndent()
        val (groups, players) = ZoneGroupStateParser.parse(xml)
        assertEquals(1, groups.size)
        assertEquals("RINCON_AAA", groups[0].coordinatorUuid)
        assertEquals(listOf("RINCON_AAA", "RINCON_BBB"), groups[0].memberUuids)
        assertEquals(2, players.size)
        assertEquals("Kitchen", players.first { it.uuid == "RINCON_AAA" }.name)
        assertEquals("192.168.1.20", players.first { it.uuid == "RINCON_AAA" }.ip)
    }
}

class DidlLiteParserTest {
    @Test
    fun parsesTrackMetadata() {
        val didl = """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
              <item id="1" parentID="0" restricted="true">
                <res protocolInfo="http-get:*:audio/mpeg:*" duration="0:03:21">http://example/track.mp3</res>
                <dc:title>Night Drive</dc:title>
                <dc:creator>Analog Heart</dc:creator>
                <upnp:class>object.item.audioItem.musicTrack</upnp:class>
                <upnp:album>After Hours</upnp:album>
                <upnp:albumArtURI>/getaa?u=foo</upnp:albumArtURI>
              </item>
            </DIDL-Lite>
        """.trimIndent()
        val track = DidlLiteParser.parseTrack(didl, "http://192.168.1.20:1400", "x-sonos-http:foo")
        requireNotNull(track)
        assertEquals("Night Drive", track.title)
        assertEquals("Analog Heart", track.artist)
        assertEquals("After Hours", track.album)
        assertEquals("http://192.168.1.20:1400/getaa?u=foo", track.albumArtUrl)
        assertEquals(201, track.durationSeconds)
    }

    @Test
    fun parsesAlreadyUnescapedDidlWithAmpersands() {
        val didl = """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns:r="urn:schemas-rinconnetworks-com:metadata-1-0/" xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
              <item id="-1" parentID="-1" restricted="true">
                <res protocolInfo="x-file-cifs:*:audio/flac:*" duration="0:05:05">x-file-cifs://192.168.0.98/media/music/Stan%20Getz%20%26%20Joao%20Gilberto/track.flac</res>
                <upnp:albumArtURI>/getaa?u=x-file-cifs%3a%2f%2fhost%2ftrack.flac&amp;v=920</upnp:albumArtURI>
                <dc:title>Para machuchar meu coracao</dc:title>
                <upnp:class>object.item.audioItem.musicTrack</upnp:class>
                <dc:creator>Stan Getz &amp; Joao Gilberto</dc:creator>
                <upnp:album>Getz/Gilberto</upnp:album>
              </item>
            </DIDL-Lite>
        """.trimIndent()
        val track = DidlLiteParser.parseTrack(didl, "http://192.168.0.101:1400", "")
        requireNotNull(track)
        assertEquals("Para machuchar meu coracao", track.title)
        assertEquals("Stan Getz & Joao Gilberto", track.artist)
        assertEquals("Getz/Gilberto", track.album)
        assertEquals(
            "http://192.168.0.101:1400/getaa?u=x-file-cifs%3a%2f%2fhost%2ftrack.flac&v=920",
            track.albumArtUrl,
        )
    }

    @Test
    fun stillParsesSoapEscapedDidl() {
        val didl = "&lt;DIDL-Lite xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; xmlns:upnp=&quot;urn:schemas-upnp-org:metadata-1-0/upnp/&quot; xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/&quot;&gt;&lt;item id=&quot;1&quot; parentID=&quot;0&quot; restricted=&quot;true&quot;&gt;&lt;dc:title&gt;Night Drive&lt;/dc:title&gt;&lt;dc:creator&gt;Analog Heart&lt;/dc:creator&gt;&lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;"
        val track = DidlLiteParser.parseTrack(didl, "http://192.168.1.20:1400", "")
        requireNotNull(track)
        assertEquals("Night Drive", track.title)
        assertEquals("Analog Heart", track.artist)
    }

    @Test
    fun parseHms() {
        assertEquals(0, DidlLiteParser.parseHms("NOT_IMPLEMENTED"))
        assertEquals(125, DidlLiteParser.parseHms("2:05"))
        assertEquals(3723, DidlLiteParser.parseHms("1:02:03"))
    }
}

class AlarmListParserTest {
    @Test
    fun parsesHouseholdAlarms() {
        val xml = """
            <Alarms>
              <Alarm ID="8" StartTime="07:15:00" Duration="02:00:00" Recurrence="WEEKDAYS" Enabled="1"
                RoomUUID="RINCON_AAA" ProgramURI="x-rincon-buzzer:0" ProgramMetaData=""
                PlayMode="NORMAL" Volume="25" IncludeLinkedZones="0"/>
              <Alarm ID="9" StartTime="09:00:00" Duration="01:00:00" Recurrence="ON_135" Enabled="0"
                RoomUUID="RINCON_BBB" ProgramURI="x-rincon-queue:RINCON_BBB#0" ProgramMetaData=""
                PlayMode="SHUFFLE" Volume="40" IncludeLinkedZones="1"/>
            </Alarms>
        """.trimIndent()
        val alarms = AlarmListParser.parse(xml)
        assertEquals(2, alarms.size)
        assertEquals("8", alarms[0].id)
        assertEquals("07:15:00", alarms[0].startTime)
        assertEquals("WEEKDAYS", alarms[0].recurrence)
        assertEquals(true, alarms[0].enabled)
        assertEquals("RINCON_AAA", alarms[0].roomUuid)
        assertEquals(25, alarms[0].volume)
        assertEquals(false, alarms[0].includeLinkedZones)
        assertEquals(false, alarms[1].enabled)
        assertEquals(true, alarms[1].includeLinkedZones)
        assertEquals("Weekdays", alarms[0].displayRecurrence())
        assertEquals("Mon Wed Fri", alarms[1].displayRecurrence())
    }

    @Test
    fun parsesSoapEscapedAlarmList() {
        val xml = "&lt;Alarms&gt;&lt;Alarm ID=&quot;1&quot; StartTime=&quot;06:30:00&quot; Recurrence=&quot;DAILY&quot; Enabled=&quot;1&quot; RoomUUID=&quot;RINCON_X&quot; ProgramURI=&quot;x-rincon-buzzer:0&quot; Volume=&quot;10&quot; IncludeLinkedZones=&quot;0&quot;/&gt;&lt;/Alarms&gt;"
        val alarms = AlarmListParser.parse(xml)
        assertEquals(1, alarms.size)
        assertEquals("06:30:00", alarms[0].startTime)
        assertEquals("Daily", alarms[0].displayRecurrence())
    }
}

class AlarmRecurrenceTest {
    @Test
    fun mapsCustomDays() {
        assertEquals("WEEKDAYS", recurrenceFromDays(setOf(1, 2, 3, 4, 5)))
        assertEquals("ON_16", recurrenceFromDays(setOf(1, 6)))
        assertEquals(setOf(1, 2, 3, 4, 5), daysFromRecurrence("WEEKDAYS"))
        assertEquals(setOf(1, 6), daysFromRecurrence("ON_16"))
        assertEquals("07:05:00", Alarm.timeString(7, 5))
    }
}

class LibrarySearchTest {
    @Test
    fun buildsLibraryObjectIds() {
        assertEquals("A:TRACKS:Night Drive", LibrarySearch.objectId("A:TRACKS", "  Night Drive  "))
        assertEquals("A:ALBUM:Getz/Gilberto", LibrarySearch.objectId("A:ALBUM", "Getz/Gilberto"))
        assertEquals(6, LibrarySearch.categories.size)
    }
}

class TinyJsonTest {
    @Test
    fun roundTripsObjectsAndArrays() {
        val encoded = TinyJson.stringify(
            mapOf(
                "name" to "Kitchen + Dining",
                "count" to 2,
                "on" to true,
                "rooms" to listOf("Kitchen", "Dining"),
            ),
        )
        val parsed = TinyJson.parseObject(encoded)
        assertEquals("Kitchen + Dining", parsed["name"])
        assertEquals(2L, parsed["count"])
        assertEquals(true, parsed["on"])
        assertEquals(listOf("Kitchen", "Dining"), parsed["rooms"])
    }
}

class HouseholdCacheTest {
    @Test
    fun roundTripsHousehold() {
        val household = Household(
            id = "HH",
            groups = listOf(
                ZoneGroup(id = "RINCON_A:1", coordinatorUuid = "RINCON_A", memberUuids = listOf("RINCON_A", "RINCON_B")),
            ),
            players = listOf(
                Player(
                    uuid = "RINCON_A",
                    name = "Kitchen",
                    ip = "192.168.1.20",
                    port = 1400,
                    location = "http://192.168.1.20:1400/xml/device_description.xml",
                    model = "S6",
                    softwareVersion = "80",
                    invisible = false,
                    hasLineIn = true,
                    hasHdmi = false,
                ),
                Player(
                    uuid = "RINCON_B",
                    name = "Dining",
                    ip = "192.168.1.21",
                    port = 1400,
                    location = "http://192.168.1.21:1400/xml/device_description.xml",
                    model = "S5",
                    softwareVersion = "80",
                    invisible = false,
                    hasLineIn = false,
                    hasHdmi = false,
                ),
            ),
        )
        val restored = HouseholdCache.decode(HouseholdCache.encode(household))
        requireNotNull(restored)
        assertEquals("HH", restored.id)
        assertEquals(listOf("RINCON_A", "RINCON_B"), restored.groups[0].memberUuids)
        assertEquals("Kitchen", restored.player("RINCON_A")?.name)
        assertEquals(true, restored.player("RINCON_A")?.hasLineIn)
    }
}

class NamedGroupCacheTest {
    @Test
    fun roundTripsNamedGroups() {
        val groups = listOf(
            NamedGroup(id = "g1", name = "Downstairs", memberUuids = listOf("RINCON_A", "RINCON_B")),
            NamedGroup(id = "g2", name = "Office", memberUuids = listOf("RINCON_C")),
        )
        val restored = NamedGroupCache.decode(NamedGroupCache.encode(groups))
        assertEquals(groups, restored)
    }

    @Test
    fun emptyAndInvalidAreEmpty() {
        assertEquals(emptyList<NamedGroup>(), NamedGroupCache.decode(""))
        assertEquals(emptyList<NamedGroup>(), NamedGroupCache.decode("not-json"))
    }
}

class DidlBuilderTest {
    @Test
    fun buildsPlaylistContainer() {
        val didl = DidlLiteParser.playlistContainerDidl("Late Night")
        assertEquals(true, didl.contains("<dc:title>Late Night</dc:title>"))
        assertEquals(true, didl.contains("playlistContainer"))
    }

    @Test
    fun prefersExistingMetadata() {
        val item = BrowseItem(
            id = "A:TRACKS/1",
            parentId = "A:TRACKS",
            title = "Night Drive",
            subtitle = "Analog Heart",
            albumArtUrl = null,
            uri = "x-file-cifs://host/track.flac",
            metadata = "<DIDL-Lite><item id=\"1\"></item></DIDL-Lite>",
            upnpClass = "object.item.audioItem.musicTrack",
            isContainer = false,
        )
        assertEquals(item.metadata, DidlLiteParser.existingOrSimpleDidl(item))
    }
}


