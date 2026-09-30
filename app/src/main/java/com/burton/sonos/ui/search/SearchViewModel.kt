package com.burton.sonos.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.LibrarySearchSection
import com.burton.sonos.domain.PlayAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrackActionPage {
    ACTIONS,
    PLAYLISTS,
    NEW_PLAYLIST,
}

data class SearchUiState(
    val query: String = "",
    val sections: List<LibrarySearchSection> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val actionsItem: BrowseItem? = null,
    val actionPage: TrackActionPage = TrackActionPage.ACTIONS,
    val playlists: List<BrowseItem> = emptyList(),
    val playlistsLoading: Boolean = false,
    val newPlaylistName: String = "",
    val groupName: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val _ui = MutableStateFlow(SearchUiState())
    val ui = _ui.asStateFlow()

    init {
        repository.start()
        viewModelScope.launch {
            repository.state.collect { snapshot ->
                val household = snapshot.household
                val group = snapshot.selectedGroup
                val name = if (household != null && group != null) household.groupName(group) else "this group"
                _ui.update { it.copy(groupName = name) }
            }
        }
        viewModelScope.launch {
            queryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { term ->
                    if (term.isBlank()) {
                        _ui.update {
                            it.copy(sections = emptyList(), loading = false, error = null, searched = false)
                        }
                        return@collectLatest
                    }
                    _ui.update { it.copy(loading = true, error = null) }
                    runCatching { repository.searchLibrary(term) }
                        .onSuccess { sections ->
                            _ui.update {
                                it.copy(sections = sections, loading = false, searched = true, error = null)
                            }
                        }
                        .onFailure { error ->
                            _ui.update {
                                it.copy(
                                    loading = false,
                                    searched = true,
                                    error = error.message ?: "Couldn't search the music library.",
                                )
                            }
                        }
                }
        }
    }

    fun onQueryChange(value: String) {
        queryFlow.value = value
        _ui.update { it.copy(query = value) }
    }

    fun clear() {
        onQueryChange("")
    }

    fun play(item: BrowseItem) {
        viewModelScope.launch { repository.playItem(item, PlayAction.PLAY_NOW) }
    }

    fun openActions(item: BrowseItem) {
        _ui.update {
            it.copy(
                actionsItem = item,
                actionPage = TrackActionPage.ACTIONS,
                playlists = emptyList(),
                newPlaylistName = "",
                notice = null,
            )
        }
    }

    fun dismissActions() {
        _ui.update {
            it.copy(
                actionsItem = null,
                actionPage = TrackActionPage.ACTIONS,
                busy = false,
                notice = null,
            )
        }
    }

    fun runPlayAction(action: PlayAction) {
        val item = _ui.value.actionsItem ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.playItem(item, action) }
                .onSuccess { dismissActions() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't update the queue.") }
                }
        }
    }

    fun saveFavorite() {
        val item = _ui.value.actionsItem ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.saveFavorite(item) }
                .onSuccess { dismissActions() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't save that favorite.") }
                }
        }
    }

    fun openPlaylists() {
        viewModelScope.launch {
            _ui.update { it.copy(actionPage = TrackActionPage.PLAYLISTS, playlistsLoading = true, notice = null) }
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
        _ui.update { it.copy(actionPage = TrackActionPage.ACTIONS, notice = null) }
    }

    fun openNewPlaylist() {
        _ui.update { it.copy(actionPage = TrackActionPage.NEW_PLAYLIST, newPlaylistName = "", notice = null) }
    }

    fun onNewPlaylistName(value: String) {
        _ui.update { it.copy(newPlaylistName = value) }
    }

    fun addToPlaylist(playlistId: String) {
        val item = _ui.value.actionsItem ?: return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching { repository.addToSonosPlaylist(playlistId, item) }
                .onSuccess { dismissActions() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't add to that playlist.") }
                }
        }
    }

    fun createPlaylistAndAdd() {
        val item = _ui.value.actionsItem ?: return
        val name = _ui.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, notice = null) }
            runCatching {
                val id = repository.createSonosPlaylist(name)
                if (id.isNotBlank()) repository.addToSonosPlaylist(id, item)
            }
                .onSuccess { dismissActions() }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, notice = error.message ?: "Couldn't create that playlist.") }
                }
        }
    }
}
