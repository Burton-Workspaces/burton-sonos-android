package com.burton.sonos.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import com.burton.sonos.BuildConfig
import com.burton.sonos.report.BurtonIssues
import com.burton.sonos.ui.alarms.AlarmsScreen
import com.burton.sonos.ui.alarms.AlarmsViewModel
import com.burton.sonos.ui.components.FullScreenModal
import com.burton.sonos.ui.groups.NamedGroupsScreen
import com.burton.sonos.ui.groups.NamedGroupsViewModel
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

private enum class SettingsPage { Root, Groups, Alarms }

@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    groupsViewModel: NamedGroupsViewModel = hiltViewModel(),
    alarmsViewModel: AlarmsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var page by remember { mutableStateOf(SettingsPage.Root) }
    val snapshot by alarmsViewModel.state.collectAsStateWithLifecycle()
    val namedGroups by groupsViewModel.groups.collectAsStateWithLifecycle()
    val grayscaleAlbumArt by settingsViewModel.grayscaleAlbumArt.collectAsStateWithLifecycle()
    val household = snapshot.household
    val groupCount = snapshot.areas.size + namedGroups.count { named ->
        !named.id.startsWith("live-") && named.id !in snapshot.areas.map { it.id }
    }
    FullScreenModal(
        onDismiss = { if (page == SettingsPage.Root) onDismiss() else page = SettingsPage.Root },
        title = when (page) {
            SettingsPage.Root -> "Settings"
            SettingsPage.Groups -> "Groups"
            SettingsPage.Alarms -> "Alarms"
        },
        scrollContent = page == SettingsPage.Root,
        trailing = {
            if (page == SettingsPage.Groups && household != null) {
                IconButton(onClick = groupsViewModel::create) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add group", tint = BurtonIvory)
                }
            } else if (page == SettingsPage.Alarms && household != null) {
                IconButton(onClick = alarmsViewModel::create) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add alarm", tint = BurtonIvory)
                }
            }
        },
    ) {
        when (page) {
            SettingsPage.Root -> {
                Spacer(Modifier.height(20.dp))
                SettingsRow(
                    title = "Groups",
                    subtitle = when {
                        household == null -> "Waiting for speakers"
                        groupCount == 0 -> "Named sets you can form later"
                        else -> "$groupCount on this household"
                    },
                    onClick = { page = SettingsPage.Groups },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Alarms",
                    subtitle = when {
                        household == null -> "Waiting for speakers"
                        snapshot.alarms.isEmpty() -> "Wake this system on a schedule"
                        else -> "${snapshot.alarms.size} on this household"
                    },
                    onClick = { page = SettingsPage.Alarms },
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggle(
                    title = "Grayscale album art",
                    subtitle = "Show covers in black and white",
                    checked = grayscaleAlbumArt,
                    onCheckedChange = settingsViewModel::setGrayscaleAlbumArt,
                )
                Spacer(Modifier.height(10.dp))
                val context = LocalContext.current
                SettingsRow(
                    title = "Burton Sonos",
                    subtitle = "About",
                    trailing = BuildConfig.VERSION_NAME,
                    onLongClick = { BurtonIssues.openNewIssue(context) },
                )
            }
            SettingsPage.Groups -> NamedGroupsScreen(viewModel = groupsViewModel)
            SettingsPage.Alarms -> AlarmsScreen(viewModel = alarmsViewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .then(
                when {
                    onClick != null && onLongClick != null -> {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    onLongClick != null -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                    else -> Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyLarge, color = BurtonMute)
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = BurtonMute,
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
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
