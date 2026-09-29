package com.burton.sonos.domain

data class Player(
    val uuid: String,
    val name: String,
    val ip: String,
    val port: Int = 1400,
    val location: String,
    val model: String,
    val softwareVersion: String,
    val invisible: Boolean,
    val hasLineIn: Boolean,
    val hasHdmi: Boolean,
) {
    val baseUrl: String get() = "http://$ip:$port"
}

data class ZoneGroup(
    val id: String,
    val coordinatorUuid: String,
    val memberUuids: List<String>,
)

data class Household(
    val id: String,
    val groups: List<ZoneGroup>,
    val players: List<Player>,
) {
    val visiblePlayers: List<Player>
        get() = players.filterNot { it.invisible }

    fun player(uuid: String): Player? = players.firstOrNull { it.uuid == uuid }

    fun groupFor(uuid: String): ZoneGroup? =
        groups.firstOrNull { uuid in it.memberUuids }

    fun coordinator(group: ZoneGroup): Player? = player(group.coordinatorUuid)

    fun groupName(group: ZoneGroup): String {
        val names = group.memberUuids.mapNotNull { uuid ->
            player(uuid)?.takeUnless { it.invisible }?.name
        }
        return when {
            names.isEmpty() -> "Room"
            names.size == 1 -> names.first()
            else -> names.joinToString(" + ")
        }
    }
}

enum class TransportState {
    PLAYING,
    PAUSED,
    STOPPED,
    TRANSITIONING,
    UNKNOWN,
    ;

    val isPlaying: Boolean get() = this == PLAYING || this == TRANSITIONING

    companion object {
        fun from(raw: String?): TransportState = when (raw?.uppercase()) {
            "PLAYING" -> PLAYING
            "PAUSED_PLAYBACK", "PAUSED" -> PAUSED
            "STOPPED" -> STOPPED
            "TRANSITIONING" -> TRANSITIONING
            else -> UNKNOWN
        }
    }
}

data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUrl: String?,
    val uri: String,
    val durationSeconds: Int,
)

data class NowPlaying(
    val groupId: String,
    val coordinatorUuid: String,
    val state: TransportState,
    val track: Track?,
    val volume: Int,
    val muted: Boolean,
    val positionSeconds: Int,
) {
    val displayTitle: String
        get() = track?.title?.takeIf { it.isNotBlank() }
            ?: if (state.isPlaying) "Playing" else "Not playing"
}

data class BrowseItem(
    val id: String,
    val parentId: String,
    val title: String,
    val subtitle: String?,
    val albumArtUrl: String?,
    val uri: String?,
    val metadata: String?,
    val upnpClass: String,
    val isContainer: Boolean,
) {
    val canPlay: Boolean get() = !uri.isNullOrBlank()
}

data class MusicServiceDescriptor(
    val id: String,
    val name: String,
    val serviceType: String,
    val auth: String,
    val uri: String,
    val secureUri: String,
    val capabilities: String,
) {
    val isSpotify: Boolean
        get() = name.equals("Spotify", ignoreCase = true) || id == SPOTIFY_SERVICE_ID

    companion object {
        const val SPOTIFY_SERVICE_ID = "9"
        const val SPOTIFY_SERVICE_TYPE = "2311"
    }
}

data class LinkedAccount(
    val serialNumber: String,
    val serviceType: String,
    val nickname: String,
    val username: String,
    val key: String,
    val oaDeviceId: String,
)

data class SystemSource(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: Kind,
    val objectId: String? = null,
    val playUri: String? = null,
    val playerUuid: String? = null,
) {
    enum class Kind {
        QUEUE,
        FAVORITES,
        PLAYLISTS,
        LIBRARY,
        RADIO,
        LINE_IN,
        TV,
        SERVICE,
    }
}

data class SpotifyLinkSession(
    val regUrl: String,
    val linkCode: String,
    val linkDeviceId: String,
    val householdId: String,
    val secureUri: String,
    val serviceType: String,
)

data class SpotifyAuthTokens(
    val authToken: String,
    val privateKey: String,
)
