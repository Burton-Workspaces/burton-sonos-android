package com.burton.sonos.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.SleepTimer
import com.burton.sonos.domain.Track
import com.burton.sonos.ui.components.AlbumArt
import com.burton.sonos.ui.components.BurtonModalSheet
import com.burton.sonos.ui.components.SheetActionRow
import com.burton.sonos.ui.components.SheetRowHeader
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonLine
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.track.TrackActionPage

@Composable
fun NowPlayingMoreSheet(
    track: Track?,
    page: TrackActionPage,
    playlists: List<BrowseItem>,
    playlistsLoading: Boolean,
    newPlaylistName: String,
    busy: Boolean,
    notice: String?,
    crossfade: Boolean,
    sleepRemainingSeconds: Int,
    onDismiss: () -> Unit,
    onSaveFavorite: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onBackToActions: () -> Unit,
    onOpenNewPlaylist: () -> Unit,
    onNewPlaylistName: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onSearchArtist: () -> Unit,
    onCrossfade: (Boolean) -> Unit,
    onSleepTimer: (Int) -> Unit,
) {
    val canSave = track?.uri?.isNotBlank() == true
    val artist = track?.artist?.trim().orEmpty()
    BurtonModalSheet(onDismiss = onDismiss) {
        when (page) {
            TrackActionPage.ACTIONS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AlbumArt(url = track?.albumArtUrl, size = 72.dp, corner = 12.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track?.title?.ifBlank { "Nothing playing" } ?: "Nothing playing",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BurtonIvory,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            artist.ifBlank { track?.album?.ifBlank { "Unknown artist" } ?: "Unknown artist" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = BurtonMute,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SheetActionRow(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "Save to Sonos Favorites",
                    enabled = canSave && !busy,
                    onClick = onSaveFavorite,
                )
                SheetActionRow(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    title = "Add to Sonos Playlist",
                    enabled = canSave && !busy,
                    onClick = onOpenPlaylists,
                )
                if (artist.isNotBlank()) {
                    SheetActionRow(
                        icon = Icons.Rounded.Search,
                        title = "Search for $artist",
                        enabled = !busy,
                        onClick = onSearchArtist,
                    )
                }
                SheetActionRow(
                    icon = Icons.Rounded.Tune,
                    title = "Crossfade",
                    enabled = !busy,
                    trailing = {
                        Switch(
                            checked = crossfade,
                            onCheckedChange = onCrossfade,
                            enabled = !busy,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BurtonIvory,
                                checkedTrackColor = BurtonSand,
                                uncheckedThumbColor = BurtonMute,
                                uncheckedTrackColor = BurtonElevated,
                            ),
                        )
                    },
                    onClick = { onCrossfade(!crossfade) },
                )
                SheetActionRow(
                    icon = Icons.Rounded.Timer,
                    title = "Sleep Timer",
                    subtitle = sleepSubtitle(sleepRemainingSeconds),
                )
                SleepTimer.options.forEach { (seconds, label) ->
                    val selected = SleepTimer.selectedSeconds(sleepRemainingSeconds) == seconds
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) BurtonSand else BurtonIvory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) { onSleepTimer(seconds) }
                            .padding(start = 40.dp, top = 10.dp, bottom = 10.dp),
                    )
                }
            }
            TrackActionPage.PLAYLISTS -> {
                SheetRowHeader(title = "Sonos Playlists", onBack = onBackToActions)
                Text(
                    "Save “${track?.title.orEmpty()}” to a playlist on this system.",
                    color = BurtonMute,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                when {
                    playlistsLoading -> CircularProgressIndicator(
                        color = BurtonSand,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    else -> {
                        SheetActionRow(Icons.Rounded.Add, "New playlist", enabled = !busy, onClick = onOpenNewPlaylist)
                        playlists.forEach { playlist ->
                            SheetActionRow(
                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                title = playlist.title,
                                enabled = !busy,
                                onClick = { onAddToPlaylist(playlist.id) },
                            )
                        }
                        if (playlists.isEmpty()) {
                            Text(
                                "No Sonos playlists yet.",
                                color = BurtonMute,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            TrackActionPage.NEW_PLAYLIST -> {
                SheetRowHeader(title = "New playlist", onBack = onOpenPlaylists)
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

private fun sleepSubtitle(remainingSeconds: Int): String? {
    if (remainingSeconds <= 0) return null
    val minutes = (remainingSeconds + 59) / 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val rest = minutes % 60
        if (rest == 0) "$hours hr left" else "$hours hr $rest min left"
    } else {
        "$minutes min left"
    }
}
