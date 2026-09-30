package com.burton.sonos.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.components.BurtonModalSheet
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonSandDim

@Composable
fun SpeakerGroupingSheet(
    onDismiss: () -> Unit,
    viewModel: SpeakerGroupingViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val household = snapshot.household
    val group = snapshot.selectedGroup
    val rooms = household?.visiblePlayers.orEmpty()
    val around = if (household != null && group != null) household.groupName(group) else "this room"
    val otherGroups = household?.let { house ->
        house.groups.filter { it.id != group?.id && house.isGrouped(it) }
    }.orEmpty()
    BurtonModalSheet(onDismiss = onDismiss) {
        Text("Speaker grouping", style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
        Text(
            "Form a group around $around. Other rooms join this coordinator.",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        if (household == null || group == null) {
            Text("Waiting for speakers.", color = BurtonMute)
            return@BurtonModalSheet
        }
        if (household.isGrouped(group)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Playing together", color = BurtonSand, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::ungroupAll, enabled = !busy) {
                    Text("Ungroup", color = BurtonSand)
                }
            }
        }
        rooms.forEach { player ->
            val inGroup = player.uuid in group.memberUuids
            val coordinator = player.uuid == group.coordinatorUuid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(player.name, color = BurtonIvory, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (coordinator) "Coordinator" else if (inGroup) "In this group" else "Not grouped",
                        color = BurtonSandDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = inGroup,
                    enabled = !coordinator && !busy,
                    onCheckedChange = { viewModel.setGrouped(player.uuid, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BurtonIvory,
                        checkedTrackColor = BurtonSand,
                        uncheckedThumbColor = BurtonMute,
                        uncheckedTrackColor = BurtonElevated,
                        disabledCheckedTrackColor = BurtonSand.copy(alpha = 0.4f),
                    ),
                )
            }
        }
        if (otherGroups.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("OTHER GROUPS", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
            Spacer(Modifier.height(8.dp))
            otherGroups.forEach { other ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                        .clickable { viewModel.select(other.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(household.groupName(other), color = BurtonIvory, style = MaterialTheme.typography.titleMedium)
                    Text("Already grouped · tap to form around this set", color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
