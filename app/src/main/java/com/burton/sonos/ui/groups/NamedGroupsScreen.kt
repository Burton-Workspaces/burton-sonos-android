package com.burton.sonos.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.domain.NamedGroup
import com.burton.sonos.ui.components.FullScreenModal
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonDanger
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonLine
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun NamedGroupsScreen(
    viewModel: NamedGroupsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val household = snapshot.household
    val editing = ui.editing
    val liveMemberSets = household?.groups.orEmpty().map { group ->
        household?.visibleMembers(group)?.map { it.uuid }?.toSet().orEmpty()
    }.toSet()
    val saved = groups.filterNot { named ->
        named.id.startsWith("live-") &&
            (named.memberUuids.size < 2 || named.memberUuids.toSet() in liveMemberSets)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Named sets you can form later.",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        ui.notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = BurtonSand, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (household == null) {
                item {
                    Text("Waiting for speakers on this network.", color = BurtonMute)
                }
            } else if (saved.isEmpty()) {
                item {
                    Text(
                        "Create a named set of rooms, then form it when you want those speakers together.",
                        color = BurtonMute,
                    )
                }
            }
            items(saved, key = { it.id }) { group ->
                val members = household?.visiblePlayers
                    ?.filter { it.uuid in group.memberUuids }
                    .orEmpty()
                val formed = group.memberUuids.toSet() in liveMemberSets && group.memberUuids.size > 1
                NamedGroupCard(
                    group = group,
                    subtitle = listOfNotNull(
                        members.joinToString { it.name }
                            .ifBlank { "${group.memberUuids.size} rooms" },
                        "Formed".takeIf { formed },
                    ).joinToString(" · "),
                    applying = ui.applying,
                    onOpen = { viewModel.edit(group) },
                    onForm = { viewModel.apply(group) },
                )
            }
        }
    }
    if (editing != null) {
        val existing = groups.any { it.id == editing.id }
        FullScreenModal(
            onDismiss = viewModel::cancel,
            title = if (existing) "Edit group" else "Add group",
            actionLabel = if (existing) "Save group" else "Add group",
            actionEnabled = editing.name.isNotBlank() && editing.memberUuids.isNotEmpty(),
            onAction = viewModel::save,
            extraFooter = {
                if (existing) {
                    TextButton(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete group", color = BurtonDanger)
                    }
                }
            },
        ) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = editing.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Name this group") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = groupFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select the speakers that belong to this named set. Forming it groups them live around the first selected room.",
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
            Spacer(Modifier.height(12.dp))
            household?.visiblePlayers.orEmpty().forEach { player ->
                val checked = player.uuid in editing.memberUuids
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(player.name, color = BurtonIvory, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = checked,
                        onCheckedChange = { viewModel.toggleMember(player.uuid, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BurtonIvory,
                            checkedTrackColor = BurtonSand,
                            uncheckedThumbColor = BurtonMute,
                            uncheckedTrackColor = BurtonElevated,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NamedGroupCard(
    group: NamedGroup,
    subtitle: String,
    applying: Boolean,
    onOpen: () -> Unit,
    onForm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Text(group.name, style = MaterialTheme.typography.titleLarge, color = BurtonIvory, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        TextButton(onClick = onForm, enabled = !applying) {
            Text("Form this group", color = BurtonSand)
        }
    }
}

@Composable
private fun groupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BurtonIvory,
    unfocusedTextColor = BurtonIvory,
    focusedBorderColor = BurtonSand,
    unfocusedBorderColor = BurtonLine,
    cursorColor = BurtonIvory,
    focusedPlaceholderColor = BurtonMute,
    unfocusedPlaceholderColor = BurtonMute,
)
