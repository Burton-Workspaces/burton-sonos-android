package com.burton.sonos.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.library.PrefixIndex
import com.burton.sonos.data.library.PrefixLocation
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class BrowseUiState(
    val title: String,
    val items: List<BrowseItem> = emptyList(),
    val prefixes: List<PrefixLocation> = emptyList(),
    val alphabetLetters: List<String> = emptyList(),
    val selectedPrefix: String? = null,
    val remoteAlphabet: Boolean = false,
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
    private var jumpJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        jumpJob?.cancel()
        viewModelScope.launch {
            _ui.value = BrowseUiState(title = title, loading = true)
            val prefixes = async { repository.prefixLocations(objectId) }
            runCatching { repository.browse(objectId) }
                .onSuccess { items ->
                    val locations = prefixes.await()
                    _ui.value = stateFor(items, locations, initialPrefix(locations, items))
                }
                .onFailure {
                    prefixes.cancel()
                    _ui.value = BrowseUiState(
                        title = title,
                        loading = false,
                        error = it.message ?: "Couldn't open this source.",
                    )
                }
        }
    }

    fun selectPrefix(prefix: String) {
        val state = _ui.value
        if (prefix.equals(state.selectedPrefix, ignoreCase = true)) return
        if (!state.remoteAlphabet) {
            _ui.update { it.copy(selectedPrefix = prefix) }
            return
        }
        val previous = state.selectedPrefix
        _ui.update { it.copy(selectedPrefix = prefix) }
        jumpJob?.cancel()
        jumpJob = viewModelScope.launch {
            delay(90)
            val start = state.prefixes.firstOrNull { it.prefix.equals(prefix, ignoreCase = true) }?.index
                ?: repository.findPrefixIndex(objectId, prefix)
            if (start == null) {
                _ui.update { current ->
                    if (current.selectedPrefix == prefix) current.copy(selectedPrefix = previous) else current
                }
                return@launch
            }
            runCatching { repository.browse(objectId, start = start) }
                .onSuccess { items ->
                    _ui.update { current ->
                        if (current.selectedPrefix != prefix) current
                        else stateFor(items, current.prefixes, prefix)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _ui.update { current ->
                        if (current.selectedPrefix == prefix) current.copy(selectedPrefix = previous) else current
                    }
                }
        }
    }

    fun firstIndexFor(prefix: String): Int =
        _ui.value.items.indexOfFirst { PrefixIndex.letterFor(it.title) == prefix }

    fun play(item: BrowseItem) {
        viewModelScope.launch { repository.playItem(item) }
    }

    private fun stateFor(
        items: List<BrowseItem>,
        locations: List<PrefixLocation>,
        selected: String?,
    ): BrowseUiState {
        val remote = locations.size >= 2
        val letters = if (remote) {
            locations.map { it.prefix }
        } else if (PrefixIndex.supportsLocalJump(objectId, items)) {
            PrefixIndex.lettersIn(items.map { it.title })
        } else {
            emptyList()
        }
        return BrowseUiState(
            title = title,
            items = items,
            prefixes = locations,
            alphabetLetters = letters,
            selectedPrefix = selected,
            remoteAlphabet = remote,
            loading = false,
        )
    }

    private fun initialPrefix(locations: List<PrefixLocation>, items: List<BrowseItem>): String? {
        locations.firstOrNull { it.index == 0 }?.prefix?.let { return it }
        return items.firstOrNull()?.let { PrefixIndex.letterFor(it.title) }
    }
}
