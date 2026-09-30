package com.burton.sonos.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.domain.ZoneGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NamedGroupsUi(
    val editing: NamedGroup? = null,
    val applying: Boolean = false,
    val notice: String? = null,
)

@HiltViewModel
class NamedGroupsViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    val snapshot = repository.state
    val groups = repository.namedGroups.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    private val _ui = MutableStateFlow(NamedGroupsUi())
    val ui = _ui.asStateFlow()

    init {
        repository.start()
    }

    fun selectLive(groupId: String) {
        repository.selectGroup(groupId)
    }

    fun saveLive(group: ZoneGroup) {
        val household = repository.state.value.household ?: return
        val members = household.visibleMembers(group)
        if (members.isEmpty()) return
        val saved = NamedGroup(
            id = UUID.randomUUID().toString(),
            name = household.groupName(group),
            memberUuids = members.map { it.uuid },
        )
        viewModelScope.launch {
            repository.updateNamedGroups { current ->
                if (current.any { it.memberUuids.toSet() == saved.memberUuids.toSet() }) current
                else current + saved
            }
            _ui.update { it.copy(notice = "Saved ${saved.name}") }
        }
    }

    fun ungroupLive(group: ZoneGroup) {
        viewModelScope.launch {
            _ui.update { it.copy(applying = true, notice = null) }
            runCatching { repository.ungroupAll(group.coordinatorUuid) }
                .onSuccess { _ui.update { it.copy(applying = false, notice = "Ungrouped") } }
                .onFailure { error ->
                    _ui.update { it.copy(applying = false, notice = error.message ?: "Couldn't ungroup.") }
                }
        }
    }

    fun create() {
        val household = repository.state.value.household
        val selected = repository.state.value.selectedGroup
        val members = selected?.memberUuids ?: household?.visiblePlayers?.map { it.uuid }.orEmpty()
        _ui.update {
            it.copy(
                editing = NamedGroup(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    memberUuids = members,
                ),
                notice = null,
            )
        }
    }

    fun edit(group: NamedGroup) {
        _ui.update { it.copy(editing = group, notice = null) }
    }

    fun cancel() {
        _ui.update { it.copy(editing = null) }
    }

    fun setName(name: String) {
        _ui.update { state ->
            state.copy(editing = state.editing?.copy(name = name))
        }
    }

    fun toggleMember(uuid: String, selected: Boolean) {
        _ui.update { state ->
            val editing = state.editing ?: return@update state
            val members = if (selected) {
                (editing.memberUuids + uuid).distinct()
            } else {
                editing.memberUuids.filterNot { it == uuid }
            }
            state.copy(editing = editing.copy(memberUuids = members))
        }
    }

    fun save() {
        val editing = _ui.value.editing ?: return
        val name = editing.name.trim()
        if (name.isBlank() || editing.memberUuids.isEmpty()) return
        val saved = editing.copy(name = name)
        viewModelScope.launch {
            repository.updateNamedGroups { current ->
                if (current.any { it.id == saved.id }) {
                    current.map { if (it.id == saved.id) saved else it }
                } else {
                    current + saved
                }
            }
            _ui.update { it.copy(editing = null) }
        }
    }

    fun delete() {
        val editing = _ui.value.editing ?: return
        viewModelScope.launch {
            repository.updateNamedGroups { current -> current.filterNot { it.id == editing.id } }
            _ui.update { it.copy(editing = null) }
        }
    }

    fun apply(group: NamedGroup) {
        viewModelScope.launch {
            _ui.update { it.copy(applying = true, notice = null) }
            runCatching { repository.applyNamedGroup(group) }
                .onSuccess { _ui.update { it.copy(applying = false, notice = "Grouped as ${group.name}") } }
                .onFailure { error ->
                    _ui.update { it.copy(applying = false, notice = error.message ?: "Couldn't form that group.") }
                }
        }
    }
}
