package com.burton.sonos.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.components.BurtonModalSheet
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonVoid

@Composable
fun SpeakerGroupingSheet(
    onDismiss: () -> Unit,
    viewModel: SpeakerGroupingViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BurtonModalSheet(
        onDismiss = onDismiss,
        footer = {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.apply(onDismiss) },
                enabled = ui.canApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurtonIvory,
                    contentColor = BurtonVoid,
                    disabledContainerColor = BurtonCharcoal,
                    disabledContentColor = BurtonMute,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply")
            }
        },
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ui.presets, key = { it.id }) { preset ->
                GroupPill(
                    name = preset.name,
                    selected = preset.id == ui.selectedPresetId,
                    enabled = !ui.busy,
                    onClick = { viewModel.selectPreset(preset) },
                )
            }
        }
        if (ui.rooms.isEmpty()) {
            Text("Waiting for speakers.", color = BurtonMute)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ui.rooms.forEach { player ->
                    val checked = player.uuid in ui.selectedUuids
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                            .clickable(enabled = !ui.busy) { viewModel.setChecked(player.uuid, !checked) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            player.name,
                            color = BurtonIvory,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = checked,
                            enabled = !ui.busy,
                            onCheckedChange = { viewModel.setChecked(player.uuid, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BurtonSand,
                                uncheckedColor = BurtonMute,
                                checkmarkColor = BurtonVoid,
                                disabledCheckedColor = BurtonSand.copy(alpha = 0.4f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPill(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = name,
        color = if (selected) BurtonVoid else BurtonIvory,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BurtonSand else BurtonCharcoal)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
