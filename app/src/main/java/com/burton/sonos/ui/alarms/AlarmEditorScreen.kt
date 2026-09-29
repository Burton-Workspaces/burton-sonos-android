package com.burton.sonos.ui.alarms

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonDanger
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonVoid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(
    onBack: () -> Unit,
    viewModel: AlarmEditorViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snapshot by viewModel.household.collectAsStateWithLifecycle()
    val players = snapshot.household?.visiblePlayers.orEmpty()
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val alarm = ui.alarm
    var showTimePicker by remember { mutableStateOf(false) }
    val picker = rememberTimePickerState(
        initialHour = alarm.hour,
        initialMinute = alarm.minute,
        is24Hour = is24Hour,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
        }
        Text(
            text = if (ui.isNew) "New alarm" else "Edit alarm",
            style = MaterialTheme.typography.headlineLarge,
            color = BurtonIvory,
        )
        Spacer(Modifier.height(20.dp))
        Text("TIME", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Text(
            text = alarm.displayTime(is24Hour),
            style = MaterialTheme.typography.displayLarge,
            color = BurtonIvory,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { showTimePicker = true }
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("REPEAT", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recurrenceChip("ONCE", "Once", alarm.recurrence, viewModel::setRecurrence)
            recurrenceChip("WEEKDAYS", "Weekdays", alarm.recurrence, viewModel::setRecurrence)
            recurrenceChip("WEEKENDS", "Weekends", alarm.recurrence, viewModel::setRecurrence)
            recurrenceChip("DAILY", "Daily", alarm.recurrence, viewModel::setRecurrence)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
                val day = index + 1
                val selected = day in viewModel.selectedDays()
                DayChip(
                    label = label,
                    selected = selected,
                    onClick = {
                        val days = viewModel.selectedDays().toMutableSet()
                        if (selected) days -= day else days += day
                        viewModel.setDays(days)
                    },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("ROOM", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Spacer(Modifier.height(8.dp))
        players.forEach { player ->
            val selected = player.uuid == alarm.roomUuid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        if (selected) BurtonElevated else BurtonCharcoal,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { viewModel.setRoom(player.uuid) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(player.name, color = BurtonIvory, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("VOLUME  ${alarm.volume}", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Slider(
            value = alarm.volume.toFloat(),
            onValueChange = { viewModel.setVolume(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = BurtonIvory,
                activeTrackColor = BurtonSand,
                inactiveTrackColor = BurtonMute.copy(alpha = 0.3f),
            ),
        )
        SettingToggle(
            title = "Include grouped rooms",
            checked = alarm.includeLinkedZones,
            onCheckedChange = viewModel::setIncludeLinked,
        )
        SettingToggle(
            title = "Chime instead of queue",
            checked = alarm.isBuzzer,
            onCheckedChange = viewModel::setBuzzer,
        )
        ui.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = BurtonDanger, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.save(onBack) },
            enabled = !ui.saving,
            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.isNew) "Create alarm" else "Save alarm")
        }
        if (!ui.isNew) {
            TextButton(
                onClick = { viewModel.delete(onBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete alarm", color = BurtonDanger)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(picker.hour, picker.minute)
                    showTimePicker = false
                }) { Text("Set", color = BurtonSand) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = BurtonMute) }
            },
            text = {
                TimePicker(
                    state = picker,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = BurtonCharcoal,
                        selectorColor = BurtonSand,
                        periodSelectorSelectedContainerColor = BurtonElevated,
                        timeSelectorSelectedContainerColor = BurtonElevated,
                    ),
                )
            },
            containerColor = BurtonVoid,
        )
    }
}

@Composable
private fun recurrenceChip(
    value: String,
    label: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BurtonElevated,
            selectedLabelColor = BurtonIvory,
            containerColor = BurtonCharcoal,
            labelColor = BurtonMute,
        ),
    )
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) BurtonVoid else BurtonIvory,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BurtonSand else BurtonCharcoal)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = BurtonIvory, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BurtonIvory,
                checkedTrackColor = BurtonSand,
                uncheckedThumbColor = BurtonMute,
                uncheckedTrackColor = BurtonElevated,
            ),
        )
    }
}
