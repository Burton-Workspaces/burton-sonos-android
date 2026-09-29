package com.burton.sonos.ui.spotify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.ui.browse.BrowseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotifyBrowseViewModel @Inject constructor(
    private val repository: SonosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val itemId: String = savedStateHandle["itemId"] ?: "root"
    private val title: String = savedStateHandle["title"] ?: "Spotify"
    private val _ui = MutableStateFlow(BrowseUiState(title = title))
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.browseSpotify(itemId) }
                .onSuccess { _ui.value = BrowseUiState(title, it, loading = false) }
                .onFailure { _ui.value = BrowseUiState(title, loading = false, error = it.message) }
        }
    }

    fun play(item: BrowseItem) {
        viewModelScope.launch { repository.playItem(item) }
    }
}
