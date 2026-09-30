package com.burton.sonos.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.SystemSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val state = repository.state
    private val _indexing = MutableStateFlow(false)
    val indexing = _indexing.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    init {
        repository.start()
    }

    fun sources(): List<SystemSource> = repository.localSources()

    fun play(source: SystemSource) {
        viewModelScope.launch { repository.playSource(source) }
    }

    fun scanForNewContent() {
        if (_indexing.value) return
        viewModelScope.launch {
            _indexing.value = true
            _notice.value = null
            try {
                repository.refreshShareIndex()
            } catch (error: TimeoutCancellationException) {
                _notice.value = "Library scan is still running. Try again in a bit."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _notice.value = error.message ?: "Couldn't scan for new content."
            } finally {
                _indexing.value = false
            }
        }
    }
}
