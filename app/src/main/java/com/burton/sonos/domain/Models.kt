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

    fun visibleMembers(group: ZoneGroup): List<Player> =
        group.memberUuids.mapNotNull { uuid -> player(uuid)?.takeUnless { it.invisible } }

    fun isGrouped(group: ZoneGroup): Boolean = visibleMembers(group).size > 1

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
    val metadata: String? = null,
    val objectId: String = "",
) {
    fun toBrowseItem(): BrowseItem = BrowseItem(
        id = objectId.ifBlank { uri },
        parentId = "Q:0",
        title = title,
        subtitle = artist.ifBlank { null },
        albumArtUrl = albumArtUrl,
        uri = uri.ifBlank { null },
        metadata = metadata,
        upnpClass = "object.item.audioItem.musicTrack",
        isContainer = false,
    )
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

data class QueuePlayMode(
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
) {
    fun toggleShuffle(): QueuePlayMode = copy(shuffle = !shuffle)

    fun cycleRepeat(): QueuePlayMode = copy(
        repeat = when (repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        },
    )

    fun toSonos(): String = when {
        shuffle && repeat == RepeatMode.ONE -> "SHUFFLE_REPEAT_ONE"
        shuffle && repeat == RepeatMode.ALL -> "SHUFFLE"
        shuffle -> "SHUFFLE_NOREPEAT"
        repeat == RepeatMode.ONE -> "REPEAT_ONE"
        repeat == RepeatMode.ALL -> "REPEAT_ALL"
        else -> "NORMAL"
    }

    companion object {
        fun fromSonos(raw: String?): QueuePlayMode = when (raw?.uppercase()) {
            "REPEAT_ALL" -> QueuePlayMode(false, RepeatMode.ALL)
            "REPEAT_ONE" -> QueuePlayMode(false, RepeatMode.ONE)
            "SHUFFLE_NOREPEAT" -> QueuePlayMode(true, RepeatMode.OFF)
            "SHUFFLE" -> QueuePlayMode(true, RepeatMode.ALL)
            "SHUFFLE_REPEAT_ONE" -> QueuePlayMode(true, RepeatMode.ONE)
            else -> QueuePlayMode()
        }
    }
}

object SleepTimer {
    val options = listOf(
        0 to "Off",
        15 * 60 to "15 minutes",
        30 * 60 to "30 minutes",
        45 * 60 to "45 minutes",
        60 * 60 to "1 hour",
        2 * 60 * 60 to "2 hours",
    )

    fun selectedSeconds(remaining: Int): Int {
        if (remaining <= 0) return 0
        return options.map { it.first }.filter { it > 0 }.minBy { kotlin.math.abs(it - remaining) }
    }

    fun sonosDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }
}

data class NowPlaying(
    val groupId: String,
    val coordinatorUuid: String,
    val state: TransportState,
    val track: Track?,
    val volume: Int,
    val muted: Boolean,
    val positionSeconds: Int,
    val playMode: QueuePlayMode = QueuePlayMode(),
    val crossfade: Boolean = false,
    val sleepRemainingSeconds: Int = 0,
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

data class LibrarySearchSection(
    val title: String,
    val items: List<BrowseItem>,
)

data class NamedGroup(
    val id: String,
    val name: String,
    val memberUuids: List<String>,
)

enum class PlayAction {
    PLAY_NOW,
    PLAY_NEXT,
    ADD_TO_QUEUE,
    REPLACE_QUEUE,
}


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
    }
}

data class Alarm(
    val id: String,
    val startTime: String,
    val duration: String,
    val recurrence: String,
    val enabled: Boolean,
    val roomUuid: String,
    val programUri: String,
    val programMetaData: String,
    val playMode: String,
    val volume: Int,
    val includeLinkedZones: Boolean,
) {
    val hour: Int get() = startTime.substringBefore(":").toIntOrNull() ?: 7
    val minute: Int get() = startTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    fun displayTime(is24Hour: Boolean): String {
        if (is24Hour) return "%02d:%02d".format(hour, minute)
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$hour12:%02d $amPm".format(minute)
    }

    fun displayRecurrence(): String = when (recurrence) {
        "ONCE" -> "Once"
        "WEEKDAYS" -> "Weekdays"
        "WEEKENDS" -> "Weekends"
        "DAILY" -> "Daily"
        else -> customDaysLabel(recurrence)
    }

    val isBuzzer: Boolean get() = programUri.startsWith("x-rincon-buzzer")

    companion object {
        const val BUZZER_URI = "x-rincon-buzzer:0"
        const val DEFAULT_DURATION = "02:00:00"

        fun timeString(hour: Int, minute: Int): String =
            "%02d:%02d:00".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

        fun draft(
            roomUuid: String,
            hour: Int = 7,
            minute: Int = 0,
        ): Alarm = Alarm(
            id = "",
            startTime = timeString(hour, minute),
            duration = DEFAULT_DURATION,
            recurrence = "WEEKDAYS",
            enabled = true,
            roomUuid = roomUuid,
            programUri = BUZZER_URI,
            programMetaData = "",
            playMode = "NORMAL",
            volume = 25,
            includeLinkedZones = false,
        )
    }
}

fun customDaysLabel(raw: String): String {
    if (!raw.startsWith("ON_") || raw.length <= 3) return raw
    val days = raw.removePrefix("ON_").mapNotNull { it.digitToIntOrNull() }.toSet()
    if (days == (1..7).toSet()) return "Daily"
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.sorted().joinToString(" ") { names.getOrElse(it - 1) { "?" } }
}

fun recurrenceFromDays(days: Set<Int>): String {
    val normalized = days.filter { it in 1..7 }.toSet()
    return when (normalized) {
        setOf(1, 2, 3, 4, 5, 6, 7) -> "DAILY"
        setOf(1, 2, 3, 4, 5) -> "WEEKDAYS"
        setOf(6, 7) -> "WEEKENDS"
        emptySet<Int>() -> "ONCE"
        else -> "ON_" + (1..7).filter { it in normalized }.joinToString("")
    }
}

fun daysFromRecurrence(raw: String): Set<Int> = when (raw) {
    "ONCE" -> emptySet()
    "WEEKDAYS" -> setOf(1, 2, 3, 4, 5)
    "WEEKENDS" -> setOf(6, 7)
    "DAILY" -> (1..7).toSet()
    else -> if (raw.startsWith("ON_")) {
        raw.removePrefix("ON_").mapNotNull { it.digitToIntOrNull() }.filter { it in 1..7 }.toSet()
    } else {
        (1..7).toSet()
    }
}

