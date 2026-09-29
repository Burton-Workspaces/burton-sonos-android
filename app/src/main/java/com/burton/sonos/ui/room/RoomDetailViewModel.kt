package com.burton.sonos.ui.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    private val repository: SonosRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val groupId: String = savedStateHandle["groupId"] ?: ""
    val state = repository.state
    private val _grouping = MutableStateFlow(false)
    val grouping = _grouping.asStateFlow()

    init {
        if (groupId.isNotBlank()) {
            viewModelScope.launch { repository.selectGroup(groupId) }
        }
    }

    fun toggle() = viewModelScope.launch { repository.togglePlay() }
    fun next() = viewModelScope.launch { repository.next() }
    fun previous() = viewModelScope.launch { repository.previous() }
    fun setVolume(volume: Int) = viewModelScope.launch { repository.setVolume(volume) }

    fun setGrouped(memberUuid: String, grouped: Boolean) {
        val coordinator = repository.state.value.selectedGroup?.coordinatorUuid ?: return
        if (memberUuid == coordinator) return
        viewModelScope.launch {
            _grouping.value = true
            runCatching { repository.setGrouped(memberUuid, coordinator, grouped) }
            _grouping.value = false
        }
    }

    fun ungroupAll() {
        val coordinator = repository.state.value.selectedGroup?.coordinatorUuid ?: return
        viewModelScope.launch {
            _grouping.value = true
            runCatching { repository.ungroupAll(coordinator) }
            _grouping.value = false
        }
    }
}
