package com.burton.sonos.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun parseHms() {
        assertEquals(0, DidlLiteParser.parseHms("NOT_IMPLEMENTED"))
        assertEquals(125, DidlLiteParser.parseHms("2:05"))
        assertEquals(3723, DidlLiteParser.parseHms("1:02:03"))
    }
}

class MusicServicesParserTest {
    @Test
    fun findsSpotify() {
        val xml = """
            <Services>
              <Service Id="9" Name="Spotify" Uri="http://spotify.com/smapi" SecureUri="https://spotify.com/smapi" Capabilities="0">
                <Policy Auth="AppLink"/>
              </Service>
            </Services>
        """.trimIndent()
        val services = MusicServicesParser.parse(xml)
        assertEquals(1, services.size)
        assertTrue(services[0].isSpotify)
        assertEquals("2311", services[0].serviceType)
        assertEquals("AppLink", services[0].auth)
    }
}

class AccountsParserTest {
    @Test
    fun skipsDeletedAccounts() {
        val xml = """
            <ZPSupportInfo>
              <Accounts>
                <Account Type="2311" SerialNum="8" Deleted="0">
                  <UN>user</UN>
                  <NN>Home</NN>
                  <Key>abc</Key>
                  <OADevID>dev</OADevID>
                </Account>
                <Account Type="2311" SerialNum="9" Deleted="1">
                  <UN>old</UN>
                </Account>
              </Accounts>
            </ZPSupportInfo>
        """.trimIndent()
        val accounts = AccountsParser.parse(xml)
        assertEquals(1, accounts.size)
        assertEquals("Home", accounts[0].nickname)
        assertEquals("2311", accounts[0].serviceType)
    }
}
