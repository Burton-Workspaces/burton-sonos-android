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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonDanger
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonLine
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonVoid

@Composable
fun NamedGroupsScreen(
    viewModel: NamedGroupsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val household = snapshot.household
    val editing = ui.editing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        if (editing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::cancel) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
                }
                Text(
                    if (groups.any { it.id == editing.id }) "Edit group" else "New group",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BurtonIvory,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                Button(
                    onClick = viewModel::save,
                    enabled = editing.name.isNotBlank() && editing.memberUuids.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save group")
                }
                if (groups.any { it.id == editing.id }) {
                    TextButton(onClick = viewModel::delete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Delete group", color = BurtonDanger)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            Text("BURTON SONOS", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Groups", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::create) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create group", tint = BurtonIvory)
                }
            }
            Text(
                "Current rooms on this system, plus named sets you can form later.",
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
            ui.notice?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = BurtonSand, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            val liveGroups = household?.groups.orEmpty()
            val liveMemberSets = liveGroups.map { group ->
                household?.visibleMembers(group)?.map { it.uuid }?.toSet().orEmpty()
            }.toSet()
            val saved = groups.filterNot { named ->
                named.id.startsWith("live-") && named.memberUuids.toSet() in liveMemberSets
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (household == null) {
                    item {
                        Text("Waiting for speakers on this network.", color = BurtonMute)
                    }
                } else {
                    item {
                        Text("ON THIS SYSTEM", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
                    }
                    items(liveGroups, key = { it.id }) { group ->
                        val members = household.visibleMembers(group)
                        val savedAlready = groups.any { it.memberUuids.toSet() == members.map { player -> player.uuid }.toSet() }
                        LiveGroupCard(
                            name = household.groupName(group),
                            subtitle = members.joinToString { it.name }.ifBlank { "Room" },
                            grouped = household.isGrouped(group),
                            selected = group.id == snapshot.selectedGroupId,
                            applying = ui.applying,
                            onSelect = { viewModel.selectLive(group.id) },
                            onSave = { viewModel.saveLive(group) }.takeIf { !savedAlready && members.size > 1 },
                            onUngroup = { viewModel.ungroupLive(group) }.takeIf { household.isGrouped(group) },
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("SAVED GROUPS", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
                }
                if (saved.isEmpty()) {
                    item {
                        Text(
                            "Save a current group, or create a named set to form later.",
                            color = BurtonMute,
                        )
                    }
                }
                items(saved, key = { it.id }) { group ->
                    NamedGroupCard(
                        group = group,
                        subtitle = household?.visiblePlayers
                            ?.filter { it.uuid in group.memberUuids }
                            ?.joinToString { it.name }
                            ?.ifBlank { "${group.memberUuids.size} rooms" }
                            ?: "${group.memberUuids.size} rooms",
                        applying = ui.applying,
                        onOpen = { viewModel.edit(group) },
                        onForm = { viewModel.apply(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveGroupCard(
    name: String,
    subtitle: String,
    grouped: Boolean,
    selected: Boolean,
    applying: Boolean,
    onSelect: () -> Unit,
    onSave: (() -> Unit)?,
    onUngroup: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .padding(16.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleLarge, color = BurtonIvory, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(
                subtitle,
                if (grouped) "Grouped" else null,
                if (selected) "Selected" else null,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Row {
            if (onSave != null) {
                TextButton(onClick = onSave, enabled = !applying) {
                    Text("Save group", color = BurtonSand)
                }
            }
            if (onUngroup != null) {
                TextButton(onClick = onUngroup, enabled = !applying) {
                    Text("Ungroup", color = BurtonSand)
                }
            }
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
