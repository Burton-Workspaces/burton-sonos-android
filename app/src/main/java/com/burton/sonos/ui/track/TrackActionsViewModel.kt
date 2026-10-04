package com.burton.sonos.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.PlayAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrackActionPage {
    ACTIONS,
    PLAYLISTS,
    NEW_PLAYLIST,
}

data class TrackActionsUi(
    val item: BrowseItem? = null,
    val page: TrackActionPage = TrackActionPage.ACTIONS,
    val playlists: List<BrowseItem> = emptyList(),
    val playlistsLoading: Boolean = false,
    val newPlaylistName: String = "",
    val groupName: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
)

@HiltViewModel
class TrackActionsViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(TrackActionsUi())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { snapshot ->
                val household = snapshot.household
                val group = snapshot.selectedGroup
                val name = if (household != null && group != null) household.groupName(group) else "this group"
                _ui.update { it.copy(groupName = name) }
            }
        }
    }

    fun open(item: BrowseItem) {
        _ui.update {
            it.copy(
                item = item,
                page = TrackActionPage.ACTIONS,
                playlists = emptyList(),
                newPlaylistName = "",
                notice = null,
            )
        }
    }

    fun dismiss() {
        _ui.update {
            it.copy(
                item = null,
                page = TrackActionPage.ACTIONS,
                busy = false,
                notice = null,
            )
        }
    }

    fun runPlayAction(action: PlayAction) {
        val item = _ui.value.item ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.playItem(item, action) }
                .onSuccess { dismiss() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't update the queue.") }
                }
        }
    }

    fun saveFavorite() {
        val item = _ui.value.item ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.saveFavorite(item) }
                .onSuccess { dismiss() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't save that favorite.") }
                }
        }
    }

    fun openPlaylists() {
        viewModelScope.launch {
            _ui.update { it.copy(page = TrackActionPage.PLAYLISTS, playlistsLoading = true, notice = null) }
            runCatching { repository.sonosPlaylists() }
                .onSuccess { playlists ->
                    _ui.update { it.copy(playlists = playlists, playlistsLoading = false) }
                }
                .onFailure { error ->
                    _ui.update {
                        it.copy(
                            playlistsLoading = false,
                            notice = error.message ?: "Couldn't load Sonos playlists.",
                        )
                    }
                }
        }
    }

    fun backToActions() {
        _ui.update { it.copy(page = TrackActionPage.ACTIONS, notice = null) }
    }

    fun openNewPlaylist() {
        _ui.update { it.copy(page = TrackActionPage.NEW_PLAYLIST, newPlaylistName = "", notice = null) }
    }

    fun onNewPlaylistName(value: String) {
        _ui.update { it.copy(newPlaylistName = value) }
    }

    fun addToPlaylist(playlistId: String) {
        val item = _ui.value.item ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.addToSonosPlaylist(playlistId, item) }
                .onSuccess { dismiss() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't add to that playlist.") }
                }
        }
    }

    fun createPlaylistAndAdd() {
        val item = _ui.value.item ?: return
        val name = _ui.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching {
                val id = repository.createSonosPlaylist(name)
                if (id.isNotBlank()) repository.addToSonosPlaylist(id, item)
            }
                .onSuccess { dismiss() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't create that playlist.") }
                }
        }
    }
}
