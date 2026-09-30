package com.burton.sonos.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeakerGroupingViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val state = repository.state
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun select(groupId: String) {
        repository.selectGroup(groupId)
    }

    fun setGrouped(memberUuid: String, grouped: Boolean) {
        val coordinator = repository.state.value.selectedGroup?.coordinatorUuid ?: return
        if (memberUuid == coordinator) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.setGrouped(memberUuid, coordinator, grouped) }
            _busy.value = false
        }
    }

    fun ungroupAll() {
        val coordinator = repository.state.value.selectedGroup?.coordinatorUuid ?: return
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.ungroupAll(coordinator) }
            _busy.value = false
        }
    }
}
