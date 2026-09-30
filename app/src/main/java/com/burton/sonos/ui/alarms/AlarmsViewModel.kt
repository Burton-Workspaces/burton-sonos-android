package com.burton.sonos.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val state = repository.state
    private val _editorId = MutableStateFlow<String?>(null)
    val editorId = _editorId.asStateFlow()

    init {
        repository.start()
    }

    fun create() {
        _editorId.value = "new"
    }

    fun edit(id: String) {
        _editorId.value = id
    }

    fun closeEditor() {
        _editorId.value = null
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { repository.setAlarmEnabled(alarm, enabled) }
    }
}
