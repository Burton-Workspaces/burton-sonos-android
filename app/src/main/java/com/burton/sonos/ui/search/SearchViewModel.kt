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

data class SearchUiState(
    val query: String = "",
    val sections: List<LibrarySearchSection> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
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
            repository.incomingSearch.collect { pending ->
                if (!pending.isNullOrBlank()) {
                    onQueryChange(pending)
                    repository.consumeIncomingSearch()
                }
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
}
