package com.burton.sonos.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val title: String,
    val items: List<BrowseItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: SonosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val objectId: String = savedStateHandle["objectId"] ?: ""
    private val title: String = savedStateHandle["title"] ?: "Browse"
    private val _ui = MutableStateFlow(BrowseUiState(title = title))
    val ui = _ui.asStateFlow()
    val isQueue = objectId == "Q:0"

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            runCatching { repository.browse(objectId) }
                .onSuccess { _ui.value = BrowseUiState(title = title, items = it, loading = false) }
                .onFailure {
                    _ui.value = BrowseUiState(
                        title = title,
                        loading = false,
                        error = it.message ?: "Couldn't open this source.",
                    )
                }
        }
    }

    fun play(item: BrowseItem) {
        viewModelScope.launch { repository.playItem(item) }
    }
}
