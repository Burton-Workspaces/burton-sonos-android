package com.burton.sonos.data.repository

import com.burton.sonos.data.library.PrefixIndex
import com.burton.sonos.data.library.PrefixLocation
import com.burton.sonos.data.parse.AlarmListParser
import com.burton.sonos.data.parse.DeviceDescriptionParser
import com.burton.sonos.data.parse.DidlLiteParser
import com.burton.sonos.data.parse.ZoneGroupStateParser
import com.burton.sonos.data.soap.MuseClient
import com.burton.sonos.data.soap.SoapClient
import com.burton.sonos.data.soap.SonosServices
import com.burton.sonos.domain.Alarm
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.NowPlaying
import com.burton.sonos.domain.PlayAction
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.QueuePlayMode
import com.burton.sonos.domain.SleepTimer
import com.burton.sonos.domain.SystemSource
import com.burton.sonos.domain.Track
import com.burton.sonos.domain.TransportState
import com.burton.sonos.domain.ZoneGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SonosControl @Inject constructor(
    private val soap: SoapClient,
    private val muse: MuseClient,
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

    suspend fun nowPlaying(group: ZoneGroup, coordinator: Player, extras: Boolean = false): NowPlaying {
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
        val playMode = if (extras) runCatching { playMode(coordinator) }.getOrDefault(QueuePlayMode()) else QueuePlayMode()
        val crossfade = if (extras) runCatching { crossfade(coordinator) }.getOrDefault(false) else false
        val sleep = if (extras) runCatching { remainingSleepSeconds(coordinator) }.getOrDefault(0) else 0
        return NowPlaying(
            groupId = group.id,
            coordinatorUuid = coordinator.uuid,
            state = TransportState.from(av["CurrentTransportState"]),
            track = track,
            volume = volume,
            muted = muted,
            positionSeconds = DidlLiteParser.parseHms(position["RelTime"].orEmpty()),
            playMode = playMode,
            crossfade = crossfade,
            sleepRemainingSeconds = sleep,
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

    suspend fun playMode(coordinator: Player): QueuePlayMode {
        val result = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetTransportSettings",
            args = mapOf("InstanceID" to "0"),
        )
        return QueuePlayMode.fromSonos(result["PlayMode"] ?: result["CurrentPlayMode"])
    }

    suspend fun setPlayMode(coordinator: Player, mode: QueuePlayMode) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "SetPlayMode",
            args = mapOf("InstanceID" to "0", "NewPlayMode" to mode.toSonos()),
        )
    }

    suspend fun crossfade(coordinator: Player): Boolean {
        val result = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetCrossfadeMode",
            args = mapOf("InstanceID" to "0"),
        )
        val raw = result["CrossfadeMode"].orEmpty()
        return raw == "1" || raw.equals("true", ignoreCase = true)
    }

    suspend fun setCrossfade(coordinator: Player, enabled: Boolean) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "SetCrossfadeMode",
            args = mapOf("InstanceID" to "0", "CrossfadeMode" to if (enabled) "1" else "0"),
        )
    }

    suspend fun remainingSleepSeconds(coordinator: Player): Int {
        val result = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetRemainingSleepTimerDuration",
            args = mapOf("InstanceID" to "0"),
        )
        return DidlLiteParser.parseHms(result["RemainingSleepTimerDuration"].orEmpty())
    }

    suspend fun setSleepTimer(coordinator: Player, seconds: Int) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "ConfigureSleepTimer",
            args = mapOf(
                "InstanceID" to "0",
                "NewSleepTimerDuration" to SleepTimer.sonosDuration(seconds),
            ),
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

    suspend fun prefixLocations(player: Player, objectId: String): List<PrefixLocation> {
        val result = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
            serviceType = SonosServices.CONTENT_DIRECTORY,
            action = "GetAllPrefixLocations",
            args = mapOf("ObjectID" to objectId),
        )
        return PrefixIndex.parse(result["PrefixAndIndexCSV"].orEmpty())
    }

    suspend fun findPrefix(player: Player, objectId: String, prefix: String): Int? {
        val result = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
            serviceType = SonosServices.CONTENT_DIRECTORY,
            action = "FindPrefix",
            args = mapOf("ObjectID" to objectId, "Prefix" to prefix),
        )
        return result["StartingIndex"]?.toIntOrNull()?.takeIf { it >= 0 }
    }

    suspend fun refreshShareIndex(player: Player) {
        val option = runCatching {
            soap.action(
                baseUrl = player.baseUrl,
                controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
                serviceType = SonosServices.CONTENT_DIRECTORY,
                action = "GetAlbumArtistDisplayOption",
            )["AlbumArtistDisplayOption"]
        }.getOrNull().orEmpty()
        soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
            serviceType = SonosServices.CONTENT_DIRECTORY,
            action = "RefreshShareIndex",
            args = mapOf("AlbumArtistDisplayOption" to option),
        )
    }

    suspend fun shareIndexInProgress(player: Player): Boolean {
        val result = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
            serviceType = SonosServices.CONTENT_DIRECTORY,
            action = "GetShareIndexInProgress",
        )
        val raw = result["IsIndexing"] ?: result["ShareIndexInProgress"].orEmpty()
        return raw == "1" || raw.equals("true", ignoreCase = true)
    }

    suspend fun playUri(coordinator: Player, uri: String, metadata: String) {
        enqueueAndPlay(coordinator, uri, metadata, PlayAction.PLAY_NOW)
    }

    suspend fun enqueueAndPlay(
        coordinator: Player,
        uri: String,
        metadata: String,
        action: PlayAction,
    ) {
        when (action) {
            PlayAction.PLAY_NOW -> {
                if (shouldUseQueue(uri, metadata)) {
                    replaceQueue(coordinator, uri, metadata)
                } else {
                    setTransportUri(coordinator, uri, metadata)
                    play(coordinator)
                }
            }
            PlayAction.REPLACE_QUEUE -> replaceQueue(coordinator, uri, metadata)
            PlayAction.PLAY_NEXT -> {
                addUriToQueue(coordinator, uri, metadata, asNext = true)
                if (!isPlayingQueue(coordinator)) {
                    setQueue(coordinator)
                    play(coordinator)
                }
            }
            PlayAction.ADD_TO_QUEUE -> {
                addUriToQueue(coordinator, uri, metadata, asNext = false)
            }
        }
    }

    suspend fun addUriToQueue(
        coordinator: Player,
        uri: String,
        metadata: String,
        asNext: Boolean,
    ) {
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
                "EnqueueAsNext" to if (asNext) "1" else "0",
            ),
        )
    }

    suspend fun replaceQueue(coordinator: Player, uri: String, metadata: String) {
        soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "RemoveAllTracksFromQueue",
            args = mapOf("InstanceID" to "0"),
        )
        addUriToQueue(coordinator, uri, metadata, asNext = false)
        setQueue(coordinator)
        play(coordinator)
    }

    suspend fun setQueue(coordinator: Player) {
        setTransportUri(coordinator, "x-rincon-queue:${coordinator.uuid}#0", "")
    }

    suspend fun isPlayingQueue(coordinator: Player): Boolean {
        val uri = soap.action(
            baseUrl = coordinator.baseUrl,
            controlPath = SonosServices.AV_TRANSPORT_PATH,
            serviceType = SonosServices.AV_TRANSPORT,
            action = "GetMediaInfo",
            args = mapOf("InstanceID" to "0"),
        )["CurrentURI"].orEmpty()
        return uri.startsWith("x-rincon-queue:")
    }

    suspend fun saveFavorite(player: Player, item: BrowseItem) {
        createObject(player, "FV:2", DidlLiteParser.existingOrSimpleDidl(item))
    }

    suspend fun sonosPlaylists(player: Player): List<BrowseItem> = browse(player, "SQ:")

    suspend fun addToSonosPlaylist(player: Player, playlistId: String, item: BrowseItem) {
        createObject(player, playlistId, DidlLiteParser.existingOrSimpleDidl(item))
    }

    suspend fun createSonosPlaylist(player: Player, title: String): String {
        val result = createObject(
            player,
            "SQ:",
            DidlLiteParser.playlistContainerDidl(title),
        )
        return result["ObjectID"].orEmpty().ifBlank { result["AssignedObjectID"].orEmpty() }
    }

    private suspend fun createObject(
        player: Player,
        parentId: String,
        elements: String,
    ): Map<String, String> {
        return runCatching {
            soap.action(
                baseUrl = player.baseUrl,
                controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
                serviceType = SonosServices.CONTENT_DIRECTORY,
                action = "CreateObject",
                args = mapOf("ContainerID" to parentId, "Elements" to elements),
            )
        }.recoverCatching {
            soap.action(
                baseUrl = player.baseUrl,
                controlPath = SonosServices.CONTENT_DIRECTORY_PATH,
                serviceType = SonosServices.CONTENT_DIRECTORY,
                action = "CreateObject",
                args = mapOf("ObjectID" to parentId, "Elements" to elements),
            )
        }.getOrThrow()
    }

    private suspend fun setTransportUri(coordinator: Player, uri: String, metadata: String) {
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

    private fun shouldUseQueue(uri: String, metadata: String): Boolean {
        return uri.contains("x-rincon-cpcontainer") ||
            uri.contains("x-rincon-playlist") ||
            uri.startsWith("x-rincon-playlist") ||
            uri.startsWith("x-rincon-cpcontainer") ||
            metadata.contains("object.container")
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

    suspend fun museHouseholdId(player: Player): String =
        soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ZONE_GROUP_TOPOLOGY_PATH,
            serviceType = SonosServices.ZONE_GROUP_TOPOLOGY,
            action = "GetZoneGroupAttributes",
        )["CurrentMuseHouseholdId"].orEmpty()

    suspend fun listAreas(player: Player): List<NamedGroup> {
        val museId = museHouseholdId(player)
        if (museId.isBlank()) return emptyList()
        return muse.listAreas(player.ip, museId)
    }

    suspend fun listAlarms(player: Player): List<Alarm> {
        val result = soap.action(
            baseUrl = player.baseUrl,
            controlPath = SonosServices.ALARM_CLOCK_PATH,
            serviceType = SonosServices.ALARM_CLOCK,
            action = "ListAlarms",
        )
        val xml = result["CurrentAlarmList"].orEmpty().ifBlank {
            result.values.firstOrNull { it.contains("<Alarm", ignoreCase = true) }.orEmpty()
        }
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
            SystemSource("composers", "Composers", "Local music library", SystemSource.Kind.LIBRARY, objectId = "A:COMPOSER"),
            SystemSource("genres", "Genres", "Local music library", SystemSource.Kind.LIBRARY, objectId = "A:GENRE"),
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
