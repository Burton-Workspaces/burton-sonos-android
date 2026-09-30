package com.burton.sonos.ui.alarms

import android.text.format.DateFormat
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.domain.Alarm
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun AlarmsScreen(
    viewModel: AlarmsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val household = snapshot.household
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var editorId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Alarms",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            if (household != null) {
                IconButton(onClick = { editorId = "new" }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add alarm", tint = BurtonIvory)
                }
            }
        }
        Text(
            text = when {
                household == null -> "Waiting for speakers"
                snapshot.alarms.isEmpty() -> "Wake this system on a schedule"
                else -> "${snapshot.alarms.size} on this household"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(snapshot.alarms, key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    roomName = household?.player(alarm.roomUuid)?.name ?: "Room",
                    is24Hour = is24Hour,
                    onToggle = { viewModel.setEnabled(alarm, it) },
                    onClick = { editorId = alarm.id },
                )
            }
        }
    }
    editorId?.let { id ->
        AlarmEditorScreen(alarmId = id, onBack = { editorId = null })
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    roomName: String,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.displayTime(is24Hour),
                style = MaterialTheme.typography.headlineMedium,
                color = if (alarm.enabled) BurtonIvory else BurtonMute,
            )
            Text(
                text = listOf(alarm.displayRecurrence(), roomName).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BurtonIvory,
                checkedTrackColor = BurtonSand,
                uncheckedThumbColor = BurtonMute,
                uncheckedTrackColor = BurtonElevated,
            ),
        )
    }
}
