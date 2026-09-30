package com.burton.sonos.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.PlayAction
import com.burton.sonos.ui.components.BurtonModalSheet
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonLine
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun TrackActionsSheet(
    item: BrowseItem,
    page: TrackActionPage,
    groupName: String,
    playlists: List<BrowseItem>,
    playlistsLoading: Boolean,
    newPlaylistName: String,
    busy: Boolean,
    notice: String?,
    onDismiss: () -> Unit,
    onPlayAction: (PlayAction) -> Unit,
    onSaveFavorite: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onBackToActions: () -> Unit,
    onOpenNewPlaylist: () -> Unit,
    onNewPlaylistName: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    BurtonModalSheet(onDismiss = onDismiss) {
        when (page) {
            TrackActionPage.ACTIONS -> {
                Text(item.title, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
                item.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
                }
                Spacer(Modifier.height(16.dp))
                ActionRow(Icons.Rounded.FavoriteBorder, "Save to favorites", enabled = !busy, onClick = onSaveFavorite)
                ActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Sonos Playlist", enabled = !busy, onClick = onOpenPlaylists)
                ActionRow(Icons.Rounded.PlayArrow, "Play Now", enabled = !busy, onClick = { onPlayAction(PlayAction.PLAY_NOW) })
                ActionRow(Icons.Rounded.QueuePlayNext, "Play Next", enabled = !busy, onClick = { onPlayAction(PlayAction.PLAY_NEXT) })
                ActionRow(
                    icon = Icons.Rounded.Queue,
                    title = "Add to End of Queue",
                    subtitle = groupName,
                    enabled = !busy,
                    onClick = { onPlayAction(PlayAction.ADD_TO_QUEUE) },
                )
                ActionRow(
                    icon = Icons.Rounded.Replay,
                    title = "Replace Queue",
                    subtitle = groupName,
                    enabled = !busy,
                    onClick = { onPlayAction(PlayAction.REPLACE_QUEUE) },
                )
            }
            TrackActionPage.PLAYLISTS -> {
                RowHeader(title = "Sonos Playlists", onBack = onBackToActions)
                Text("Save “${item.title}” to a playlist on this system.", color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                when {
                    playlistsLoading -> CircularProgressIndicator(color = BurtonSand, modifier = Modifier.align(Alignment.CenterHorizontally))
                    else -> {
                        ActionRow(Icons.Rounded.Add, "New playlist", enabled = !busy, onClick = onOpenNewPlaylist)
                        playlists.forEach { playlist ->
                            ActionRow(
                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                title = playlist.title,
                                enabled = !busy,
                                onClick = { onAddToPlaylist(playlist.id) },
                            )
                        }
                        if (playlists.isEmpty()) {
                            Text("No Sonos playlists yet.", color = BurtonMute, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            TrackActionPage.NEW_PLAYLIST -> {
                RowHeader(title = "New playlist", onBack = onOpenPlaylists)
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = onNewPlaylistName,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BurtonIvory,
                        unfocusedTextColor = BurtonIvory,
                        focusedBorderColor = BurtonSand,
                        unfocusedBorderColor = BurtonLine,
                        cursorColor = BurtonIvory,
                        focusedPlaceholderColor = BurtonMute,
                        unfocusedPlaceholderColor = BurtonMute,
                    ),
                )
                TextButton(
                    onClick = onCreatePlaylist,
                    enabled = !busy && newPlaylistName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Create and add", color = BurtonSand)
                }
            }
        }
        notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = BurtonSand, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RowHeader(title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BurtonSand, modifier = Modifier.padding(end = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
            }
        }
    }
}
