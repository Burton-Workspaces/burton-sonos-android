package com.burton.sonos.data.smapi

import com.burton.sonos.data.parse.Xml
import com.burton.sonos.data.parse.childText
import com.burton.sonos.data.parse.descendants
import com.burton.sonos.data.parse.rootElement
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.SpotifyAuthTokens
import com.burton.sonos.domain.SpotifyLinkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmapiClient @Inject constructor(
    private val http: OkHttpClient,
) {
    suspend fun beginLink(
        secureUri: String,
        householdId: String,
        deviceId: String,
        serviceType: String,
    ): SpotifyLinkSession = withContext(Dispatchers.IO) {
        runCatching {
            parseAppLink(
                call(
                    endpoint = secureUri,
                    method = "getAppLink",
                    deviceId = deviceId,
                    body = "<householdId>${Xml.escapeXml(householdId)}</householdId>",
                ),
                householdId,
                secureUri,
                serviceType,
                deviceId,
            )
        }.getOrElse {
            parseDeviceLink(
                call(
                    endpoint = secureUri,
                    method = "getDeviceLinkCode",
                    deviceId = deviceId,
                    body = "<householdId>${Xml.escapeXml(householdId)}</householdId>",
                ),
                householdId,
                secureUri,
                serviceType,
                deviceId,
            )
        }
    }

    suspend fun pollAuthToken(
        session: SpotifyLinkSession,
        deviceId: String,
    ): SpotifyAuthTokens? = withContext(Dispatchers.IO) {
        val xml = try {
            call(
                endpoint = session.secureUri,
                method = "getDeviceAuthToken",
                deviceId = deviceId,
                body = buildString {
                    append("<householdId>${Xml.escapeXml(session.householdId)}</householdId>")
                    append("<linkCode>${Xml.escapeXml(session.linkCode)}</linkCode>")
                    append("<linkDeviceId>${Xml.escapeXml(session.linkDeviceId.ifBlank { deviceId })}</linkDeviceId>")
                },
            )
        } catch (_: IOException) {
            return@withContext null
        }
        val root = runCatching { Xml.parse(xml).rootElement() }.getOrNull() ?: return@withContext null
        val result = root.descendants("getDeviceAuthTokenResult").firstOrNull()
            ?: root.descendants("getDeviceAuthTokenResponse").firstOrNull()
            ?: return@withContext null
        val token = result.childText("authToken").ifBlank { result.childText("authToken") }
        val key = result.childText("privateKey")
        if (token.isBlank() || key.isBlank()) return@withContext null
        SpotifyAuthTokens(authToken = token, privateKey = key)
    }

    suspend fun getMetadata(
        secureUri: String,
        deviceId: String,
        householdId: String,
        token: String?,
        key: String?,
        itemId: String,
        index: Int = 0,
        count: Int = 100,
        speakerBaseUrl: String,
        serviceId: String,
        serial: String,
    ): List<BrowseItem> = withContext(Dispatchers.IO) {
        val xml = call(
            endpoint = secureUri,
            method = "getMetadata",
            deviceId = deviceId,
            body = """
                <id>${Xml.escapeXml(itemId)}</id>
                <index>$index</index>
                <count>$count</count>
            """.trimIndent(),
            loginToken = token,
            loginKey = key,
            householdId = householdId,
        )
        parseMetadata(xml, speakerBaseUrl, serviceId, serial)
    }

    private fun parseAppLink(
        xml: String,
        householdId: String,
        secureUri: String,
        serviceType: String,
        deviceId: String,
    ): SpotifyLinkSession {
        val root = Xml.parse(xml).rootElement()
        val deviceLink = root.descendants("deviceLink").firstOrNull()
            ?: throw IOException("Spotify getAppLink did not return a device link")
        return SpotifyLinkSession(
            regUrl = deviceLink.childText("regUrl"),
            linkCode = deviceLink.childText("linkCode"),
            linkDeviceId = deviceLink.childText("linkDeviceId").ifBlank { deviceId },
            householdId = householdId,
            secureUri = secureUri,
            serviceType = serviceType,
        )
    }

    private fun parseDeviceLink(
        xml: String,
        householdId: String,
        secureUri: String,
        serviceType: String,
        deviceId: String,
    ): SpotifyLinkSession {
        val root = Xml.parse(xml).rootElement()
        val result = root.descendants("getDeviceLinkCodeResult").firstOrNull()
            ?: throw IOException("Spotify getDeviceLinkCode failed")
        return SpotifyLinkSession(
            regUrl = result.childText("regUrl"),
            linkCode = result.childText("linkCode"),
            linkDeviceId = result.childText("linkDeviceId").ifBlank { deviceId },
            householdId = householdId,
            secureUri = secureUri,
            serviceType = serviceType,
        )
    }

    private fun parseMetadata(
        xml: String,
        speakerBaseUrl: String,
        serviceId: String,
        serial: String,
    ): List<BrowseItem> {
        val root = runCatching { Xml.parse(xml).rootElement() }.getOrNull() ?: return emptyList()
        val items = root.descendants("mediaCollection") + root.descendants("mediaMetadata")
        return items.map { el ->
            val id = el.childText("id")
            val title = el.childText("title").ifBlank { "Untitled" }
            val itemType = el.childText("itemType")
            val isContainer = el.matchesName("mediaCollection") ||
                itemType !in setOf("track", "stream", "program", "show")
            val art = el.descendants("albumArtURI").firstOrNull()?.textContent
                ?: el.childText("albumArtURI").ifBlank { null }
            val artist = el.descendants("artist").firstOrNull()?.textContent
                ?: el.childText("author")
            val canPlay = el.childText("canPlay").equals("true", ignoreCase = true) || !isContainer
            val uri = if (canPlay) spotifyUri(id, serviceId, serial) else null
            val subtitle = sequenceOf(artist, itemType).firstOrNull { it.isNotBlank() }
            BrowseItem(
                id = id,
                parentId = "",
                title = title,
                subtitle = subtitle,
                albumArtUrl = art,
                uri = uri,
                metadata = spotifyMetadata(id, title, itemType, artist, serviceId, serial),
                upnpClass = if (isContainer) "object.container" else "object.item.audioItem.musicTrack",
                isContainer = isContainer,
            )
        }
    }

    private fun spotifyUri(id: String, serviceId: String, serial: String): String {
        val encoded = java.net.URLEncoder.encode(id, Charsets.UTF_8.name())
        return "x-sonos-spotify:$encoded?sid=$serviceId&flags=8224&sn=$serial"
    }

    private fun spotifyMetadata(
        id: String,
        title: String,
        itemType: String,
        artist: String,
        serviceId: String,
        serial: String,
    ): String {
        val clazz = when (itemType) {
            "album" -> "object.container.album.musicAlbum"
            "playlist", "playlistUserCreated" -> "object.container.playlistContainer"
            "artist" -> "object.container.person.musicArtist"
            else -> "object.item.audioItem.musicTrack"
        }
        return """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/"
              xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"
              xmlns:r="urn:schemas-rinconnetworks-com:metadata-1-0/"
              xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
              <item id="${Xml.escapeXml(id)}" parentID="-1" restricted="true">
                <dc:title>${Xml.escapeXml(title)}</dc:title>
                <upnp:class>$clazz</upnp:class>
                <desc id="cdudn" nameSpace="urn:schemas-rinconnetworks-com:metadata-1-0/">SA_RINCON${serviceId}_$serial</desc>
              </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    private fun call(
        endpoint: String,
        method: String,
        deviceId: String,
        body: String,
        loginToken: String? = null,
        loginKey: String? = null,
        householdId: String? = null,
    ): String {
        val login = if (!loginToken.isNullOrBlank() && !loginKey.isNullOrBlank() && householdId != null) {
            """
              <loginToken>
                <token>${Xml.escapeXml(loginToken)}</token>
                <key>${Xml.escapeXml(loginKey)}</key>
                <householdId>${Xml.escapeXml(householdId)}</householdId>
              </loginToken>
            """.trimIndent()
        } else {
            ""
        }
        val envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
              <s:Header>
                <credentials xmlns="http://www.sonos.com/Services/1.1">
                  <deviceId>${Xml.escapeXml(deviceId)}</deviceId>
                  <deviceProvider>Sonos</deviceProvider>
                  <context/>
                  $login
                </credentials>
              </s:Header>
              <s:Body>
                <$method xmlns="http://www.sonos.com/Services/1.1">
                  $body
                </$method>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        val request = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "\"http://www.sonos.com/Services/1.1#$method\"")
            .header(
                "User-Agent",
                "Linux UPnP/1.0 Sonos/36.11-41270 (ICRU_iPhone14,2); iOS/Version 16.0",
            )
            .post(envelope.toRequestBody(XML_MEDIA))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (text.contains("NOT_LINKED_RETRY", ignoreCase = true) ||
                    text.contains("NOT_LINKED", ignoreCase = true)
                ) {
                    throw IOException("NOT_LINKED_RETRY")
                }
                throw IOException("SMAPI $method failed: HTTP ${response.code}")
            }
            return text
        }
    }

    private fun org.w3c.dom.Element.matchesName(localName: String): Boolean {
        val local = this.localName ?: nodeName.substringAfter(':')
        return local.equals(localName, ignoreCase = true)
    }

    private companion object {
        val XML_MEDIA = "text/xml; charset=utf-8".toMediaType()
    }
}
