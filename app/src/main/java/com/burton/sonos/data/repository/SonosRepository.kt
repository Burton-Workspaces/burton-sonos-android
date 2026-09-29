package com.burton.sonos.data.repository

import com.burton.sonos.data.discovery.SpeakerDiscovery
import com.burton.sonos.data.smapi.SmapiClient
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.LinkedAccount
import com.burton.sonos.domain.MusicServiceDescriptor
import com.burton.sonos.domain.NowPlaying
import com.burton.sonos.domain.Player
import com.burton.sonos.domain.SpotifyLinkSession
import com.burton.sonos.domain.SystemSource
import com.burton.sonos.domain.ZoneGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    val services: List<MusicServiceDescriptor> = emptyList(),
    val accounts: List<LinkedAccount> = emptyList(),
    val selectedGroupId: String? = null,
    val scanning: Boolean = false,
    val error: String? = null,
) {
    val selectedGroup: ZoneGroup?
        get() = household?.groups?.firstOrNull { it.id == selectedGroupId }
            ?: household?.groups?.firstOrNull()

    val selectedCoordinator: Player?
        get() = selectedGroup?.let { household?.coordinator(it) }

    val selectedPlayback: NowPlaying?
        get() = selectedGroup?.id?.let { nowPlaying[it] }

    val spotifyService: MusicServiceDescriptor?
        get() = services.firstOrNull { it.isSpotify }

    val spotifyAccount: LinkedAccount?
        get() = accounts.firstOrNull {
            it.serviceType == MusicServiceDescriptor.SPOTIFY_SERVICE_TYPE ||
                it.serviceType == spotifyService?.serviceType
        }
}

@Singleton
class SonosRepository @Inject constructor(
    private val discovery: SpeakerDiscovery,
    private val control: SonosControl,
    private val smapi: SmapiClient,
    private val prefs: LocalPrefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(SonosSnapshot())
    val state: StateFlow<SonosSnapshot> = _state.asStateFlow()
    private var pollJob: Job? = null

    fun start() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            refresh(scan = true)
            while (isActive) {
                delay(2_000)
                refresh(scan = false)
            }
        }
    }

    suspend fun refresh(scan: Boolean = true) {
        mutex.withLock {
            if (scan) _state.update { it.copy(scanning = true, error = null) }
            try {
                val household = if (scan || _state.value.household == null) {
                    val speakers = discovery.discover()
                    if (speakers.isEmpty()) {
                        _state.update {
                            it.copy(
                                scanning = false,
                                household = if (scan) null else it.household,
                                error = if (scan) "No Sonos speakers found on this network." else it.error,
                            )
                        }
                        return
                    }
                    control.householdFrom(speakers.first().ip)
                } else {
                    val ip = _state.value.household?.players?.firstOrNull()?.ip ?: return
                    control.householdFrom(ip)
                }
                val savedGroup = prefs.selectedGroup()
                val groupId = when {
                    household.groups.any { it.id == _state.value.selectedGroupId } -> _state.value.selectedGroupId
                    household.groups.any { it.id == savedGroup } -> savedGroup
                    else -> household.groups.firstOrNull()?.id
                }
                val playback = household.groups.associate { group ->
                    val coordinator = household.coordinator(group) ?: return@associate group.id to null
                    group.id to runCatching { control.nowPlaying(group, coordinator) }.getOrNull()
                }.filterValues { it != null }.mapValues { it.value as NowPlaying }
                val anyPlayer = household.players.firstOrNull()
                val services = anyPlayer?.let { runCatching { control.musicServices(it) }.getOrNull() }.orEmpty()
                val accounts = anyPlayer?.let { runCatching { control.accounts(it) }.getOrNull() }.orEmpty()
                _state.update {
                    it.copy(
                        household = household,
                        nowPlaying = playback,
                        services = services,
                        accounts = accounts,
                        selectedGroupId = groupId,
                        scanning = false,
                        error = null,
                    )
                }
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

    suspend fun selectGroup(groupId: String) {
        prefs.setSelectedGroupId(groupId)
        _state.update { it.copy(selectedGroupId = groupId) }
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
        val coordinator = _state.value.selectedCoordinator ?: return
        control.setVolume(coordinator, volume)
        _state.update { snapshot ->
            val groupId = snapshot.selectedGroupId ?: return@update snapshot
            val current = snapshot.nowPlaying[groupId] ?: return@update snapshot
            snapshot.copy(nowPlaying = snapshot.nowPlaying + (groupId to current.copy(volume = volume)))
        }
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

    suspend fun playItem(item: BrowseItem) {
        val coordinator = _state.value.selectedCoordinator ?: return
        val uri = item.uri ?: return
        control.playUri(coordinator, uri, item.metadata.orEmpty())
        refresh(scan = false)
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

    suspend fun beginSpotifyLink(): SpotifyLinkSession {
        val snapshot = _state.value
        val player = snapshot.selectedCoordinator ?: snapshot.household?.players?.firstOrNull()
            ?: error("No Sonos player on the network")
        val spotify = snapshot.spotifyService ?: error("This system does not list Spotify as an available service")
        val householdId = snapshot.household?.id.orEmpty()
        val deviceId = control.serialNumber(player).ifBlank { player.uuid }
        return smapi.beginLink(
            secureUri = spotify.secureUri,
            householdId = householdId,
            deviceId = deviceId,
            serviceType = spotify.serviceType,
        )
    }

    suspend fun completeSpotifyLink(session: SpotifyLinkSession): Boolean {
        val snapshot = _state.value
        val player = snapshot.selectedCoordinator ?: snapshot.household?.players?.firstOrNull()
            ?: return false
        val deviceId = control.serialNumber(player).ifBlank { player.uuid }
        val tokens = smapi.pollAuthToken(session, deviceId) ?: return false
        control.addOAuthAccount(player, session.serviceType, tokens, session.householdId)
        prefs.saveSpotifyTokens(tokens)
        refresh(scan = false)
        return true
    }

    suspend fun browseSpotify(itemId: String = "root"): List<BrowseItem> {
        val snapshot = _state.value
        val player = snapshot.selectedCoordinator ?: snapshot.household?.players?.firstOrNull()
            ?: return emptyList()
        val spotify = snapshot.spotifyService ?: return emptyList()
        val account = snapshot.spotifyAccount
        val stored = prefs.spotifyTokens()
        val deviceId = control.serialNumber(player).ifBlank { player.uuid }
        return smapi.getMetadata(
            secureUri = spotify.secureUri,
            deviceId = deviceId,
            householdId = snapshot.household?.id.orEmpty(),
            token = stored?.authToken ?: account?.username?.ifBlank { null },
            key = stored?.privateKey ?: account?.key?.ifBlank { null },
            itemId = itemId,
            speakerBaseUrl = player.baseUrl,
            serviceId = spotify.id,
            serial = account?.serialNumber ?: "1",
        )
    }
}
