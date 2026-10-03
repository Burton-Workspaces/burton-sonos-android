package com.burton.sonos.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeakerGroupingUi(
    val rooms: List<Player> = emptyList(),
    val selectedUuids: Set<String> = emptySet(),
    val presets: List<NamedGroup> = emptyList(),
    val selectedPresetId: String? = null,
    val busy: Boolean = false,
    val canApply: Boolean = false,
)

@HiltViewModel
class SpeakerGroupingViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    private val draft = MutableStateFlow<Set<String>?>(null)
    private val _busy = MutableStateFlow(false)

    val ui = combine(
        repository.state,
        repository.namedGroups,
        draft,
        _busy,
    ) { snapshot, named, override, busy ->
        val rooms = snapshot.household?.visiblePlayers.orEmpty()
        val visible = rooms.map { it.uuid }.toSet()
        val live = snapshot.selectedGroup?.memberUuids.orEmpty().filter { it in visible }.toSet()
        val selected = override ?: live
        val presets = SpeakerGroupingPresets.build(
            visibleUuids = rooms.map { it.uuid },
            areas = snapshot.areas,
            named = named,
        )
        SpeakerGroupingUi(
            rooms = rooms,
            selectedUuids = selected,
            presets = presets,
            selectedPresetId = SpeakerGroupingPresets.matchingId(presets, selected, visible),
            busy = busy,
            canApply = selected.isNotEmpty() && !busy,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SpeakerGroupingUi())

    fun setChecked(uuid: String, checked: Boolean) {
        val current = selectedMembers()
        draft.value = if (checked) current + uuid else current - uuid
    }

    fun selectPreset(group: NamedGroup) {
        val visible = visibleUuids()
        draft.value = group.memberUuids.filter { it in visible }.toSet()
    }

    fun apply(onDone: () -> Unit) {
        val selected = selectedMembers()
        val members = repository.state.value.household?.visiblePlayers
            ?.map { it.uuid }
            ?.filter { it in selected }
            .orEmpty()
        if (members.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                repository.applyNamedGroup(NamedGroup(id = "draft", name = "", memberUuids = members))
            }
            _busy.value = false
            if (result.isSuccess) onDone()
        }
    }

    private fun visibleUuids(): Set<String> =
        repository.state.value.household?.visiblePlayers?.map { it.uuid }?.toSet().orEmpty()

    private fun selectedMembers(): Set<String> {
        val visible = visibleUuids()
        val live = repository.state.value.selectedGroup?.memberUuids.orEmpty().filter { it in visible }.toSet()
        return draft.value ?: live
    }
}
