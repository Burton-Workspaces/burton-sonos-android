package com.burton.sonos.data.repository

import com.burton.sonos.data.discovery.SpeakerDiscovery
import com.burton.sonos.data.library.LibrarySearch
import com.burton.sonos.domain.Alarm
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.LibrarySearchSection
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.NowPlaying
import com.burton.sonos.domain.PlayAction
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.SystemSource
import com.burton.sonos.domain.ZoneGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SonosSnapshot(
    val household: Household? = null,
    val nowPlaying: Map<String, NowPlaying> = emptyMap(),
    val alarms: List<Alarm> = emptyList(),
    val selectedGroupId: String? = null,
    val scanning: Boolean = true,
    val error: String? = null,
) {
    val selectedGroup: ZoneGroup?
        get() = household?.groups?.firstOrNull { it.id == selectedGroupId }
            ?: household?.groups?.firstOrNull()

    val selectedCoordinator: Player?
        get() = selectedGroup?.let { household?.coordinator(it) }

    val selectedPlayback: NowPlaying?
        get() = selectedGroup?.id?.let { nowPlaying[it] }
}

@Singleton
class SonosRepository @Inject constructor(
    private val discovery: SpeakerDiscovery,
    private val control: SonosControl,
    private val prefs: LocalPrefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(SonosSnapshot())
    val state: StateFlow<SonosSnapshot> = _state.asStateFlow()
    val namedGroups = prefs.namedGroups
    private var pollJob: Job? = null
    private val volumeJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    fun start() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            hydrateCache()
            val lastIp = prefs.lastSpeakerIp()
            if (lastIp != null) {
                val revived = runCatching { refreshFromIp(lastIp, showScanning = _state.value.household == null) }.isSuccess
                if (!revived) refresh(scan = true)
            } else {
                refresh(scan = true)
            }
            while (isActive) {
                delay(2_000)
                refresh(scan = false)
            }
        }
    }

    private suspend fun hydrateCache() {
        val cached = prefs.cachedHousehold() ?: return
        val savedGroup = prefs.selectedGroup()
        val groupId = when {
            cached.groups.any { it.id == savedGroup } -> savedGroup
            else -> cached.groups.firstOrNull()?.id
        }
        _state.update {
            it.copy(
                household = cached,
                selectedGroupId = groupId,
                scanning = false,
                error = null,
            )
        }
    }

    suspend fun refresh(scan: Boolean = true) {
        mutex.withLock {
            val current = _state.value.household
            if (scan) {
                _state.update { it.copy(scanning = current == null, error = null) }
            }
            try {
                val household = if (scan || current == null) {
                    val speakers = discovery.discover()
                    if (speakers.isEmpty()) {
                        _state.update {
                            it.copy(
                                scanning = false,
                                household = if (scan) it.household else it.household,
                                error = if (scan && it.household == null) {
                                    "No Sonos speakers found on this network."
                                } else {
                                    it.error
                                },
                            )
                        }
                        return
                    }
                    control.householdFrom(speakers.first().ip)
                } else {
                    val ip = current.players.firstOrNull()?.ip ?: return
                    control.householdFrom(ip)
                }
                applyHousehold(household, fastPlayback = current == null || scan)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        scanning = false,
                        error = t.message ?: "Couldn't reach your Sonos system.",
                    )
                }
            }
        }
    }

    private suspend fun refreshFromIp(ip: String, showScanning: Boolean) {
        mutex.withLock {
            if (showScanning && _state.value.household == null) {
                _state.update { it.copy(scanning = true, error = null) }
            }
            val household = control.householdFrom(ip)
            applyHousehold(household, fastPlayback = true)
        }
    }

    private suspend fun applyHousehold(household: Household, fastPlayback: Boolean) {
        val savedGroup = prefs.selectedGroup()
        val previousCoordinator = _state.value.selectedGroup?.coordinatorUuid
        val groupId = when {
            household.groups.any { it.id == _state.value.selectedGroupId } -> _state.value.selectedGroupId
            previousCoordinator != null && household.groups.any { it.coordinatorUuid == previousCoordinator } ->
                household.groups.first { it.coordinatorUuid == previousCoordinator }.id
            household.groups.any { it.id == savedGroup } -> savedGroup
            else -> household.groups.firstOrNull()?.id
        }
        val selected = household.groups.firstOrNull { it.id == groupId }
        val selectedPlayback = if (selected != null) {
            val coordinator = household.coordinator(selected)
            if (coordinator != null) {
                runCatching { control.nowPlaying(selected, coordinator) }.getOrNull()
            } else {
                null
            }
        } else {
            null
        }
        val initialPlayback = if (selected != null && selectedPlayback != null) {
            mapOf(selected.id to selectedPlayback)
        } else {
            emptyMap()
        }
        _state.update {
            it.copy(
                household = household,
                nowPlaying = keepLocalVolume(
                    if (fastPlayback) initialPlayback + it.nowPlaying.filterKeys { key ->
                        household.groups.any { group -> group.id == key }
                    } else it.nowPlaying,
                ),
                selectedGroupId = groupId,
                scanning = false,
                error = null,
            )
        }
        prefs.setCachedHousehold(household)
        household.players.firstOrNull()?.ip?.let { prefs.setLastSpeakerIp(it) }

        val remaining = household.groups.filterNot { it.id == selected?.id }
        val rest = remaining.associate { group ->
            val coordinator = household.coordinator(group) ?: return@associate group.id to null
            group.id to runCatching { control.nowPlaying(group, coordinator) }.getOrNull()
        }.filterValues { it != null }.mapValues { it.value as NowPlaying }
        val alarms = household.players.firstOrNull()?.let { player ->
            runCatching { control.listAlarms(player) }.getOrNull()
        }.orEmpty()
        _state.update { snapshot ->
            snapshot.copy(
                nowPlaying = keepLocalVolume(
                    (selected?.id?.let { id ->
                        selectedPlayback?.let { mapOf(id to it) }
                    } ?: emptyMap()) + rest,
                ),
                alarms = alarms,
            )
        }
    }

    fun selectGroup(groupId: String) {
        _state.update { it.copy(selectedGroupId = groupId) }
        scope.launch { prefs.setSelectedGroupId(groupId) }
    }

    suspend fun togglePlay() {
        val snapshot = _state.value
        val coordinator = snapshot.selectedCoordinator ?: return
        val playing = snapshot.selectedPlayback?.state?.isPlaying == true
        if (playing) control.pause(coordinator) else control.play(coordinator)
        refresh(scan = false)
    }

    suspend fun next() {
        val coordinator = _state.value.selectedCoordinator ?: return
        control.next(coordinator)
        refresh(scan = false)
    }

    suspend fun previous() {
        val coordinator = _state.value.selectedCoordinator ?: return
        control.previous(coordinator)
        refresh(scan = false)
    }

    suspend fun setVolume(volume: Int) {
        val groupId = _state.value.selectedGroup?.id ?: _state.value.selectedGroupId ?: return
        applyVolume(groupId, volume)
    }

    fun setGroupVolume(groupId: String, volume: Int) {
        applyVolume(groupId, volume)
    }

    fun adjustVolume(delta: Int) {
        val groupId = _state.value.selectedGroup?.id ?: _state.value.selectedGroupId ?: return
        applyVolume(groupId, (_state.value.nowPlaying[groupId]?.volume ?: 0) + delta)
    }

    private fun applyVolume(groupId: String, volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        _state.update { snapshot ->
            val current = snapshot.nowPlaying[groupId] ?: return@update snapshot
            snapshot.copy(nowPlaying = snapshot.nowPlaying + (groupId to current.copy(volume = clamped)))
        }
        volumeJobs.remove(groupId)?.cancel()
        volumeJobs[groupId] = scope.launch {
            delay(90)
            val household = _state.value.household ?: return@launch
            val group = household.groups.firstOrNull { it.id == groupId } ?: return@launch
            val coordinator = household.coordinator(group) ?: return@launch
            val target = _state.value.nowPlaying[groupId]?.volume ?: clamped
            runCatching { control.setVolume(coordinator, target) }
        }
    }

    private fun keepLocalVolume(incoming: Map<String, NowPlaying>): Map<String, NowPlaying> {
        val active = volumeJobs.filterValues { it.isActive }.keys
        if (active.isEmpty()) return incoming
        var result = incoming
        for (groupId in active) {
            val local = _state.value.nowPlaying[groupId] ?: continue
            val remote = result[groupId] ?: continue
            result = result + (groupId to remote.copy(volume = local.volume))
        }
        return result
    }

    fun localSources(): List<SystemSource> {
        val household = _state.value.household ?: return emptyList()
        return control.localSources(household)
    }

    suspend fun browse(objectId: String): List<BrowseItem> {
        val player = _state.value.selectedCoordinator
            ?: _state.value.household?.players?.firstOrNull()
            ?: return emptyList()
        return control.browse(player, objectId)
    }

    suspend fun searchLibrary(query: String): List<LibrarySearchSection> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        val player = _state.value.selectedCoordinator
            ?: _state.value.household?.players?.firstOrNull()
            ?: return emptyList()
        return coroutineScope {
            LibrarySearch.categories.map { (title, prefix) ->
                async {
                    val items = runCatching {
                        control.browse(player, LibrarySearch.objectId(prefix, term), count = 40)
                    }.getOrDefault(emptyList())
                    LibrarySearchSection(title = title, items = items)
                }
            }.awaitAll().filter { it.items.isNotEmpty() }
        }
    }

    suspend fun playItem(item: BrowseItem, action: PlayAction = PlayAction.PLAY_NOW) {
        val coordinator = _state.value.selectedCoordinator ?: return
        val uri = item.uri ?: return
        control.enqueueAndPlay(coordinator, uri, item.metadata.orEmpty(), action)
        refresh(scan = false)
    }

    suspend fun saveFavorite(item: BrowseItem) {
        val player = contentPlayer() ?: return
        control.saveFavorite(player, item)
    }

    suspend fun sonosPlaylists(): List<BrowseItem> {
        val player = contentPlayer() ?: return emptyList()
        return control.sonosPlaylists(player)
    }

    suspend fun addToSonosPlaylist(playlistId: String, item: BrowseItem) {
        val player = contentPlayer() ?: return
        control.addToSonosPlaylist(player, playlistId, item)
    }

    suspend fun createSonosPlaylist(title: String): String {
        val player = contentPlayer() ?: return ""
        return control.createSonosPlaylist(player, title)
    }

    suspend fun playSource(source: SystemSource) {
        val coordinator = _state.value.selectedCoordinator ?: return
        when (source.kind) {
            SystemSource.Kind.LINE_IN -> {
                val uuid = source.playerUuid ?: return
                control.playLineIn(coordinator, uuid)
            }
            SystemSource.Kind.TV -> {
                val uuid = source.playerUuid ?: return
                control.playTv(coordinator, uuid)
            }
            else -> Unit
        }
        refresh(scan = false)
    }

    suspend fun setGrouped(memberUuid: String, coordinatorUuid: String, grouped: Boolean) {
        val player = _state.value.household?.player(memberUuid) ?: return
        if (grouped) {
            control.joinGroup(player, coordinatorUuid)
        } else {
            control.ungroup(player)
        }
        delay(700)
        refresh(scan = false)
    }

    suspend fun ungroupAll(coordinatorUuid: String) {
        val household = _state.value.household ?: return
        val group = household.groupFor(coordinatorUuid) ?: return
        household.visibleMembers(group)
            .filter { it.uuid != coordinatorUuid }
            .forEach { control.ungroup(it) }
        delay(700)
        refresh(scan = false)
    }

    suspend fun applyNamedGroup(group: NamedGroup) {
        val household = _state.value.household ?: return
        val members = group.memberUuids.mapNotNull { household.player(it)?.takeUnless { player -> player.invisible } }
        if (members.isEmpty()) return
        val preferred = members.firstOrNull { player ->
            household.groupFor(player.uuid)?.coordinatorUuid == player.uuid &&
                player.uuid == _state.value.selectedGroup?.coordinatorUuid
        } ?: members.firstOrNull { player ->
            household.groupFor(player.uuid)?.coordinatorUuid == player.uuid
        } ?: members.first()
        val currentGroup = household.groupFor(preferred.uuid)
        if (currentGroup != null && currentGroup.coordinatorUuid != preferred.uuid) {
            control.ungroup(preferred)
            delay(700)
        }
        members.filter { it.uuid != preferred.uuid }.forEach { member ->
            control.joinGroup(member, preferred.uuid)
        }
        delay(800)
        refresh(scan = false)
        val live = _state.value.household ?: return
        val formed = live.groupFor(preferred.uuid) ?: return
        live.visibleMembers(formed)
            .filter { it.uuid != preferred.uuid && it.uuid !in group.memberUuids }
            .forEach { control.ungroup(it) }
        delay(700)
        refresh(scan = false)
        _state.value.household?.groupFor(preferred.uuid)?.id?.let { selectGroup(it) }
    }

    suspend fun saveNamedGroups(groups: List<NamedGroup>) {
        prefs.saveNamedGroups(groups)
    }

    suspend fun updateNamedGroups(transform: (List<NamedGroup>) -> List<NamedGroup>) {
        prefs.updateNamedGroups(transform)
    }

    private fun contentPlayer(): Player? =
        _state.value.selectedCoordinator ?: _state.value.household?.players?.firstOrNull()

    private fun alarmSpeaker(): Player? = _state.value.household?.players?.firstOrNull()

    suspend fun saveAlarm(alarm: Alarm) {
        val player = alarmSpeaker() ?: return
        if (alarm.id.isBlank()) {
            control.createAlarm(player, alarm)
        } else {
            control.updateAlarm(player, alarm)
        }
        refresh(scan = false)
    }

    suspend fun setAlarmEnabled(alarm: Alarm, enabled: Boolean) {
        val player = alarmSpeaker() ?: return
        control.updateAlarm(player, alarm.copy(enabled = enabled))
        _state.update { snapshot ->
            snapshot.copy(
                alarms = snapshot.alarms.map { if (it.id == alarm.id) it.copy(enabled = enabled) else it },
            )
        }
    }

    suspend fun deleteAlarm(alarmId: String) {
        val player = alarmSpeaker() ?: return
        control.destroyAlarm(player, alarmId)
        refresh(scan = false)
    }
}
