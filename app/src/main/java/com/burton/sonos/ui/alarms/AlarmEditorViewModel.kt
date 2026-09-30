package com.burton.sonos.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.Alarm
import com.burton.sonos.domain.daysFromRecurrence
import com.burton.sonos.domain.recurrenceFromDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmEditorState(
    val alarm: Alarm,
    val isNew: Boolean,
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    private var alarmId: String = "new"
    private val isNew get() = alarmId == "new"
    val household = repository.state
    private val _ui = MutableStateFlow(
        AlarmEditorState(
            alarm = Alarm.draft(roomUuid = ""),
            isNew = true,
        ),
    )
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { snapshot ->
                val current = _ui.value.alarm
                if (current.roomUuid.isBlank()) {
                    val room = snapshot.household?.visiblePlayers?.firstOrNull()?.uuid.orEmpty()
                    if (room.isNotBlank()) {
                        _ui.update { it.copy(alarm = it.alarm.copy(roomUuid = room)) }
                    }
                }
                if (!isNew && current.id != alarmId) {
                    snapshot.alarms.firstOrNull { it.id == alarmId }?.let { found ->
                        _ui.update { it.copy(alarm = found) }
                    }
                }
            }
        }
    }

    fun open(id: String) {
        alarmId = id
        val existing = if (isNew) null else repository.state.value.alarms.firstOrNull { it.id == alarmId }
        val room = existing?.roomUuid?.ifBlank { null }
            ?: repository.state.value.household?.visiblePlayers?.firstOrNull()?.uuid.orEmpty()
        _ui.value = AlarmEditorState(
            alarm = existing ?: Alarm.draft(roomUuid = room),
            isNew = isNew,
        )
    }

    fun setTime(hour: Int, minute: Int) {
        _ui.update { it.copy(alarm = it.alarm.copy(startTime = Alarm.timeString(hour, minute))) }
    }

    fun setRecurrence(recurrence: String) {
        _ui.update { it.copy(alarm = it.alarm.copy(recurrence = recurrence)) }
    }

    fun setDays(days: Set<Int>) {
        _ui.update { it.copy(alarm = it.alarm.copy(recurrence = recurrenceFromDays(days))) }
    }

    fun setRoom(uuid: String) {
        _ui.update { it.copy(alarm = it.alarm.copy(roomUuid = uuid)) }
    }

    fun setVolume(volume: Int) {
        _ui.update { it.copy(alarm = it.alarm.copy(volume = volume.coerceIn(0, 100))) }
    }

    fun setIncludeLinked(include: Boolean) {
        _ui.update { it.copy(alarm = it.alarm.copy(includeLinkedZones = include)) }
    }

    fun setBuzzer(buzzer: Boolean) {
        _ui.update {
            val uri = if (buzzer) {
                Alarm.BUZZER_URI
            } else {
                "x-rincon-queue:${it.alarm.roomUuid}#0"
            }
            it.copy(alarm = it.alarm.copy(programUri = uri))
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val alarm = _ui.value.alarm
            if (alarm.roomUuid.isBlank()) {
                _ui.update { it.copy(error = "Pick a room for this alarm.") }
                return@launch
            }
            _ui.update { it.copy(saving = true, error = null) }
            runCatching { repository.saveAlarm(alarm) }
                .onSuccess { onDone() }
                .onFailure { error ->
                    _ui.update {
                        it.copy(saving = false, error = error.message ?: "Couldn't save this alarm.")
                    }
                }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _ui.value.alarm.id
        if (id.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.deleteAlarm(id) }
                .onSuccess { onDone() }
                .onFailure { error ->
                    _ui.update { it.copy(error = error.message ?: "Couldn't delete this alarm.") }
                }
        }
    }

    fun selectedDays(): Set<Int> = daysFromRecurrence(_ui.value.alarm.recurrence)
}
