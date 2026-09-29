package com.burton.sonos.ui.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.burton.sonos.domain.NowPlaying
import com.burton.sonos.ui.components.AlbumArt
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonSandDim

@Composable
fun RoomsScreen(
    onOpenRoom: (String) -> Unit,
    viewModel: RoomsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val household = snapshot.household
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "BURTON SONOS",
            style = MaterialTheme.typography.labelSmall,
            color = BurtonSand,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "System",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Scan again", tint = BurtonIvory)
            }
        }
        Text(
            text = when {
                household == null && snapshot.scanning -> "Looking for speakers on this network"
                household == null -> "Connect to the same Wi-Fi as your Sonos"
                else -> "${household.visiblePlayers.size} rooms on this household"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.scanning && household == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurtonSand)
                }
            }
            household == null -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = snapshot.error ?: "No speakers found.",
                        color = BurtonIvory,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = viewModel::refresh) {
                        Text("Scan network", color = BurtonSand)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(household.groups, key = { it.id }) { group ->
                        RoomCard(
                            name = household.groupName(group),
                            grouped = household.isGrouped(group),
                            playback = snapshot.nowPlaying[group.id],
                            selected = group.id == snapshot.selectedGroupId,
                            onClick = {
                                viewModel.select(group.id)
                                onOpenRoom(group.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    name: String,
    grouped: Boolean,
    playback: NowPlaying?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlbumArt(url = playback?.track?.albumArtUrl, size = 72.dp, corner = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (grouped) {
                Text("Grouped", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = playback?.displayTitle ?: "Not playing",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) BurtonSand else BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            playback?.track?.artist?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSandDim, maxLines = 1)
            }
        }
    }
}
