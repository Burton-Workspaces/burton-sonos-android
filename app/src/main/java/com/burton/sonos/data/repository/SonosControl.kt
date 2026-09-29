package com.burton.sonos.data.repository

import com.burton.sonos.data.parse.AlarmListParser
import com.burton.sonos.data.parse.DeviceDescriptionParser
import com.burton.sonos.data.parse.DidlLiteParser
import com.burton.sonos.data.parse.ZoneGroupStateParser
import com.burton.sonos.data.soap.SoapClient
import com.burton.sonos.data.soap.SonosServices
import com.burton.sonos.domain.Alarm
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.NowPlaying
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.SystemSource
import com.burton.sonos.domain.Track
import com.burton.sonos.domain.TransportState
import com.burton.sonos.domain.ZoneGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SonosControl @Inject constructor(
    private val soap: SoapClient,
) {
    suspend fun householdFrom(ip: String): Household {
        val description = soap.get("http://$ip:1400/xml/device_description.xml")
        val info = DeviceDescriptionParser.parse(description)
        val base = "http://$ip:1400"
        val topology = soap.action(
            baseUrl = base,
            controlPath = SonosServices.ZONE_GROUP_TOPOLOGY_PATH,
            serviceType = SonosServices.ZONE_GROUP_TOPOLOGY,
            action = "GetZoneGroupState",
        )["ZoneGroupState"].orEmpty()
        val (groups, players) = ZoneGroupStateParser.parse(topology)
        val householdId = soap.action(
            baseUrl = base,
            controlPath = SonosServices.DEVICE_PROPERTIES_PATH,
            serviceType = SonosServices.DEVICE_PROPERTIES,
            action = "GetHouseholdID",
        )["CurrentHouseholdID"].orEmpty().ifBlank { info?.householdId.orEmpty() }
        val enriched = players.map { player ->
            if (player.model.isNotBlank() && player.hasHdmi) player
            else player.copy(
                hasHdmi = player.hasHdmi || looksLikeHomeTheater(player),
                hasLineIn = player.hasLineIn || looksLikeLineIn(player),
            )
        }
        return Household(id = householdId, groups = groups, players = enriched)
    }

    suspend fun nowPlaying(group: ZoneGroup, coordinator: Player): NowPlaying {
        val av = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetTransportInfo",
            args = mapOf("InstanceID" to "0"),
        )
        val position = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetPositionInfo",
            args = mapOf("InstanceID" to "0"),
        )
        val volume = runCatching {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.GROUP_RENDERING_PATH,
                serviceType = SonosServices.GROUP_RENDERING,
                action = "GetGroupVolume",
                args = mapOf("InstanceID" to "0"),
            )["CurrentVolume"]?.toIntOrNull()
        }.getOrNull() ?: soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.RENDERING_CONTROL_PATH,
            serviceType = SonosServices.RENDERING_CONTROL,
            action = "GetVolume",
            args = mapOf("InstanceID" to "0", "Channel" to "Master"),
        )["CurrentVolume"]?.toIntOrNull() ?: 0
        val muted = runCatching {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.RENDERING_CONTROL_PATH,
                serviceType = SonosServices.RENDERING_CONTROL,
                action = "GetMute",
                args = mapOf("InstanceID" to "0", "Channel" to "Master"),
            )["CurrentMute"] == "1"
        }.getOrDefault(false)
        val meta = position["TrackMetaData"].orEmpty()
        val uri = position["TrackURI"].orEmpty()
        val duration = DidlLiteParser.parseHms(position["TrackDuration"].orEmpty())
        val parsed = meta.takeUnless { it.isBlank() || it.equals("NOT_IMPLEMENTED", ignoreCase = true) }
            ?.let { DidlLiteParser.parseTrack(it, coordinator.baseUrl, uri) }
        val track = parsed?.copy(durationSeconds = duration.takeIf { it > 0 } ?: parsed.durationSeconds)
            ?: uri.takeIf { it.isNotBlank() }?.let { fallbackTrack(it, duration) }
        return NowPlaying(
            groupId = group.id,
            coordinatorUuid = coordinator.uuid,
            state = TransportState.from(av["CurrentTransportState"]),
            track = track,
            volume = volume,
            muted = muted,
            positionSeconds = DidlLiteParser.parseHms(position["RelTime"].orEmpty()),
        )
    }

    suspend fun play(coordinator: Player) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "Play",
            args = mapOf("InstanceID" to "0", "Speed" to "1"),
        )
    }

    suspend fun pause(coordinator: Player) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "Pause",
            args = mapOf("InstanceID" to "0"),
        )
    }

    suspend fun next(coordinator: Player) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "Next",
            args = mapOf("InstanceID" to "0"),
        )
    }

    suspend fun previous(coordinator: Player) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "Previous",
            args = mapOf("InstanceID" to "0"),
        )
    }

    suspend fun setVolume(coordinator: Player, volume: Int) {
        val clamped = volume.coerceIn(0, 100).toString()
        runCatching {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.GROUP_RENDERING_PATH,
                serviceType = SonosServices.GROUP_RENDERING,
                action = "SetGroupVolume",
                args = mapOf("InstanceID" to "0", "DesiredVolume" to clamped),
            )
        }.onFailure {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.RENDERING_CONTROL_PATH,
                serviceType = SonosServices.RENDERING_CONTROL,
                action = "SetVolume",
                args = mapOf("InstanceID" to "0", "Channel" to "Master", "DesiredVolume" to clamped),
            )
        }
    }

    suspend fun browse(
        player: Player,
        objectId: String,
        start: Int = 0,
        count: Int = 200,
    ): List<BrowseItem> {
        val result = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
            serviceType = SonosServices.CONTENT_DIRECTORY,
            action = "Browse",
            args = mapOf(
                "ObjectID" to objectId,
                "BrowseFlag" to "BrowseDirectChildren",
                "Filter" to "*",
                "StartingIndex" to start.toString(),
                "RequestedCount" to count.toString(),
                "SortCriteria" to "",
            ),
        )
        return DidlLiteParser.parseItems(result["Result"].orEmpty(), player.baseUrl)
    }

    suspend fun playUri(coordinator: Player, uri: String, metadata: String) {
        val isContainer = uri.contains("x-rincon-cpcontainer") ||
            uri.contains("x-rincon-playlist") ||
            metadata.contains("object.container")
        if (isContainer || uri.startsWith("x-rincon-playlist") || uri.startsWith("x-rincon-cpcontainer")) {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.AV_TRANSPORT_PATH,
                serviceType = SonosServices.AV_TRANSPORT,
                action = "RemoveAllTracksFromQueue",
                args = mapOf("InstanceID" to "0"),
            )
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.AV_TRANSPORT_PATH,
                serviceType = SonosServices.AV_TRANSPORT,
                action = "AddURIToQueue",
                args = mapOf(
                    "InstanceID" to "0",
                    "EnqueuedURI" to uri,
                    "EnqueuedURIMetaData" to metadata,
                    "DesiredFirstTrackNumberEnqueued" to "0",
                    "EnqueueAsNext" to "0",
                ),
            )
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.AV_TRANSPORT_PATH,
                serviceType = SonosServices.AV_TRANSPORT,
                action = "SetAVTransportURI",
                args = mapOf(
                    "InstanceID" to "0",
                    "CurrentURI" to "x-rincon-queue:${coordinator.uuid}#0",
                    "CurrentURIMetaData" to "",
                ),
            )
        } else {
            soap.action(
                baseUrl = coordinator.baseUrl,
                controlPath = SonosServices.AV_TRANSPORT_PATH,
                serviceType = SonosServices.AV_TRANSPORT,
                action = "SetAVTransportURI",
                args = mapOf(
                    "InstanceID" to "0",
                    "CurrentURI" to uri,
                    "CurrentURIMetaData" to metadata,
                ),
            )
        }
        play(coordinator)
    }

    suspend fun playLineIn(coordinator: Player, sourceUuid: String) {
        playUri(coordinator, "x-rincon-stream:$sourceUuid", "")
    }

    suspend fun playTv(coordinator: Player, sourceUuid: String) {
        playUri(coordinator, "x-sonos-htastream:$sourceUuid:spdif", "")
    }

    suspend fun joinGroup(member: Player, coordinatorUuid: String) {
        soap.action(
            baseUrl = member.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "SetAVTransportURI",
            args = mapOf(
                "InstanceID" to "0",
                "CurrentURI" to "x-rincon:$coordinatorUuid",
                "CurrentURIMetaData" to "",
            ),
        )
    }

    suspend fun ungroup(player: Player) {
        soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "BecomeCoordinatorOfStandaloneGroup",
            args = mapOf("InstanceID" to "0"),
        )
    }

    suspend fun listAlarms(player: Player): List<Alarm> {
        val xml = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ALARM_CLOCK_PATH,
            serviceType = SonosServices.ALARM_CLOCK,
            action = "ListAlarms",
        )["CurrentAlarmList"].orEmpty()
        return AlarmListParser.parse(xml)
    }

    suspend fun createAlarm(player: Player, alarm: Alarm): String {
        return soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ALARM_CLOCK_PATH,
            serviceType = SonosServices.ALARM_CLOCK,
            action = "CreateAlarm",
            args = alarmArgs(alarm, includeId = false),
        )["AssignedID"].orEmpty()
    }

    suspend fun updateAlarm(player: Player, alarm: Alarm) {
        soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ALARM_CLOCK_PATH,
            serviceType = SonosServices.ALARM_CLOCK,
            action = "UpdateAlarm",
            args = alarmArgs(alarm, includeId = true),
        )
    }

    suspend fun destroyAlarm(player: Player, alarmId: String) {
        soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ALARM_CLOCK_PATH,
            serviceType = SonosServices.ALARM_CLOCK,
            action = "DestroyAlarm",
            args = mapOf("ID" to alarmId),
        )
    }

    private fun alarmArgs(alarm: Alarm, includeId: Boolean): Map<String, String> {
        val args = linkedMapOf(
            "StartLocalTime" to alarm.startTime,
            "Duration" to alarm.duration.ifBlank { Alarm.DEFAULT_DURATION },
            "Recurrence" to alarm.recurrence,
            "Enabled" to if (alarm.enabled) "1" else "0",
            "RoomUUID" to alarm.roomUuid,
            "ProgramURI" to alarm.programUri.ifBlank { Alarm.BUZZER_URI },
            "ProgramMetaData" to alarm.programMetaData,
            "PlayMode" to alarm.playMode.ifBlank { "NORMAL" },
            "Volume" to alarm.volume.coerceIn(0, 100).toString(),
            "IncludeLinkedZones" to if (alarm.includeLinkedZones) "1" else "0",
        )
        if (includeId) {
            return linkedMapOf("ID" to alarm.id) + args
        }
        return args
    }

    fun localSources(household: Household): List<SystemSource> {
        val sources = mutableListOf(
            SystemSource("queue", "Queue", "What's playing next", SystemSource.Kind.QUEUE, objectId = "Q:0"),
            SystemSource("favorites", "Sonos Favorites", "Saved from any service", SystemSource.Kind.FAVORITES, objectId = "FV:2"),
            SystemSource("playlists", "Sonos Playlists", "Playlists saved on this system", SystemSource.Kind.PLAYLISTS, objectId = "SQ:"),
            SystemSource("artists", "Artists", "Local music library", SystemSource.Kind.LIBRARY, objectId = "A:ALBUMARTIST"),
            SystemSource("albums", "Albums", "Local music library", SystemSource.Kind.LIBRARY, objectId = "A:ALBUM"),
            SystemSource("tracks", "Tracks", "Local music library", SystemSource.Kind.LIBRARY, objectId = "A:TRACKS"),
            SystemSource("library-playlists", "Imported Playlists", "Playlists from shares", SystemSource.Kind.LIBRARY, objectId = "A:PLAYLISTS"),
            SystemSource("shares", "Music Shares", "Network folders indexed by Sonos", SystemSource.Kind.LIBRARY, objectId = "S:"),
            SystemSource("radio", "Radio", "TuneIn on this system", SystemSource.Kind.RADIO, objectId = "R:0/0"),
        )
        household.visiblePlayers.forEach { player ->
            if (player.hasLineIn) {
                sources += SystemSource(
                    id = "linein-${player.uuid}",
                    title = "Line-in · ${player.name}",
                    subtitle = "Analog input on this player",
                    kind = SystemSource.Kind.LINE_IN,
                    playUri = "x-rincon-stream:${player.uuid}",
                    playerUuid = player.uuid,
                )
            }
            if (player.hasHdmi) {
                sources += SystemSource(
                    id = "tv-${player.uuid}",
                    title = "TV · ${player.name}",
                    subtitle = "Home theater input",
                    kind = SystemSource.Kind.TV,
                    playUri = "x-sonos-htastream:${player.uuid}:spdif",
                    playerUuid = player.uuid,
                )
            }
        }
        return sources
    }

    private fun fallbackTrack(uri: String, durationSeconds: Int): Track {
        val title = when {
            uri.startsWith("x-rincon-stream:") -> "Line-in"
            uri.startsWith("x-sonos-htastream:") -> "TV"
            uri.startsWith("x-sonos-vli:") -> "AirPlay"
            uri.startsWith("x-sonosapi-stream:") || uri.startsWith("x-rincon-mp3radio:") -> "Radio"
            uri.startsWith("x-rincon-queue:") -> "Queue"
            else -> "Playing"
        }
        return Track(
            title = title,
            artist = "",
            album = "",
            albumArtUrl = null,
            uri = uri,
            durationSeconds = durationSeconds,
        )
    }

    private fun looksLikeHomeTheater(player: Player): Boolean {
        val blob = "${player.model} ${player.name}".lowercase()
        return listOf("beam", "arc", "ray", "playbar", "playbase", "amp").any { it in blob }
    }

    private fun looksLikeLineIn(player: Player): Boolean {
        val blob = "${player.model} ${player.name}".lowercase()
        return listOf("play:5", "five", "connect", "port", "amp", "era 300").any { it in blob }
    }
}
