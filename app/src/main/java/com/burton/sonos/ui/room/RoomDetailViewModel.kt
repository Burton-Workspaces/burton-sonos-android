package com.burton.sonos.ui.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.ui.track.TrackActionPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingMoreUi(
    val visible: Boolean = false,
    val page: TrackActionPage = TrackActionPage.ACTIONS,
    val playlists: List<BrowseItem> = emptyList(),
    val playlistsLoading: Boolean = false,
    val newPlaylistName: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
)

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val repository: SonosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val groupId: String = savedStateHandle["groupId"] ?: ""
    val state = repository.state
    private val _more = MutableStateFlow(NowPlayingMoreUi())
    val more = _more.asStateFlow()

    init {
        if (groupId.isNotBlank()) {
            repository.selectGroup(groupId)
        }
    }

    fun toggle() = viewModelScope.launch { repository.togglePlay() }
    fun next() = viewModelScope.launch { repository.next() }
    fun previous() = viewModelScope.launch { repository.previous() }
    fun setVolume(volume: Int) = viewModelScope.launch { repository.setVolume(volume) }
    fun toggleShuffle() = viewModelScope.launch { repository.toggleShuffle() }
    fun cycleRepeat() = viewModelScope.launch { repository.cycleRepeat() }
    fun setCrossfade(enabled: Boolean) = viewModelScope.launch { repository.setCrossfade(enabled) }
    fun setSleepTimer(seconds: Int) = viewModelScope.launch { repository.setSleepTimer(seconds) }

    fun openMore() {
        _more.value = NowPlayingMoreUi(visible = true)
    }

    fun dismissMore() {
        _more.value = NowPlayingMoreUi()
    }

    fun saveFavorite() {
        val item = currentTrackItem() ?: return
        viewModelScope.launch {
            _more.update { it.copy(busy = true, notice = null) }
            runCatching { repository.saveFavorite(item) }
                .onSuccess { dismissMore() }
                .onFailure { error ->
                    _more.update { it.copy(busy = false, notice = error.message ?: "Couldn't save that favorite.") }
                }
        }
    }

    fun openPlaylists() {
        viewModelScope.launch {
            _more.update { it.copy(page = TrackActionPage.PLAYLISTS, playlistsLoading = true, notice = null) }
            runCatching { repository.sonosPlaylists() }
                .onSuccess { playlists ->
                    _more.update { it.copy(playlists = playlists, playlistsLoading = false) }
                }
                .onFailure { error ->
                    _more.update {
                        it.copy(
                            playlistsLoading = false,
                            notice = error.message ?: "Couldn't load Sonos playlists.",
                        )
                    }
                }
        }
    }

    fun backToActions() {
        _more.update { it.copy(page = TrackActionPage.ACTIONS, notice = null) }
    }

    fun openNewPlaylist() {
        _more.update { it.copy(page = TrackActionPage.NEW_PLAYLIST, newPlaylistName = "", notice = null) }
    }

    fun onNewPlaylistName(value: String) {
        _more.update { it.copy(newPlaylistName = value) }
    }

    fun addToPlaylist(playlistId: String) {
        val item = currentTrackItem() ?: return
        viewModelScope.launch {
            _more.update { it.copy(busy = true, notice = null) }
            runCatching { repository.addToSonosPlaylist(playlistId, item) }
                .onSuccess { dismissMore() }
                .onFailure { error ->
                    _more.update { it.copy(busy = false, notice = error.message ?: "Couldn't add to that playlist.") }
                }
        }
    }

    fun createPlaylistAndAdd() {
        val item = currentTrackItem() ?: return
        val name = _more.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _more.update { it.copy(busy = true, notice = null) }
            runCatching {
                val id = repository.createSonosPlaylist(name)
                if (id.isNotBlank()) repository.addToSonosPlaylist(id, item)
            }
                .onSuccess { dismissMore() }
                .onFailure { error ->
                    _more.update { it.copy(busy = false, notice = error.message ?: "Couldn't create that playlist.") }
                }
        }
    }

    fun searchArtist(onGoSearch: () -> Unit) {
        val artist = state.value.selectedPlayback?.track?.artist?.trim().orEmpty()
        if (artist.isBlank()) return
        repository.requestSearch(artist)
        dismissMore()
        onGoSearch()
    }

    private fun currentTrackItem(): BrowseItem? =
        state.value.selectedPlayback?.track?.takeIf { it.uri.isNotBlank() }?.toBrowseItem()
}
