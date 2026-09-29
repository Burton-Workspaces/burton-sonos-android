package com.burton.sonos.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.SystemSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val state = repository.state
    fun sources(): List<SystemSource> = repository.localSources()
    fun play(source: SystemSource) {
        viewModelScope.launch { repository.playSource(source) }
    }
}
