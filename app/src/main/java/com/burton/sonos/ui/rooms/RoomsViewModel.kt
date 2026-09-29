package com.burton.sonos.ui.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val state = repository.state

    init {
        repository.start()
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh(scan = true) }
    }

    fun select(groupId: String) {
        viewModelScope.launch { repository.selectGroup(groupId) }
    }

    fun toggle() {
        viewModelScope.launch { repository.togglePlay() }
    }
}
