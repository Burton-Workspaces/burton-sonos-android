package com.burton.sonos.ui.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val repository: SonosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val groupId: String = savedStateHandle["groupId"] ?: ""
    val state = repository.state

    init {
        if (groupId.isNotBlank()) {
            viewModelScope.launch { repository.selectGroup(groupId) }
        }
    }

    fun toggle() = viewModelScope.launch { repository.togglePlay() }
    fun next() = viewModelScope.launch { repository.next() }
    fun previous() = viewModelScope.launch { repository.previous() }
    fun setVolume(volume: Int) = viewModelScope.launch { repository.setVolume(volume) }
}
