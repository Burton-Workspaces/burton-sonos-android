package com.burton.sonos.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.browse.BrowseRow
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonLine
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun SearchScreen(
    onOpenFolder: (id: String, title: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Search", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Text(
            "Artists, albums, tracks, playlists, composers, and genres",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search the music library") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (ui.query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BurtonIvory,
                unfocusedTextColor = BurtonIvory,
                focusedBorderColor = BurtonSand,
                unfocusedBorderColor = BurtonLine,
                cursorColor = BurtonIvory,
                focusedPlaceholderColor = BurtonMute,
                unfocusedPlaceholderColor = BurtonMute,
                focusedLeadingIconColor = BurtonSand,
                unfocusedLeadingIconColor = BurtonMute,
                focusedTrailingIconColor = BurtonIvory,
                unfocusedTrailingIconColor = BurtonMute,
            ),
        )
        Spacer(Modifier.height(16.dp))
        when {
            ui.loading -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurtonSand)
            }
            ui.error != null -> Text(ui.error ?: "", color = BurtonIvory)
            ui.query.isBlank() -> Text(
                "Type a name, album, or track to search this system’s library.",
                color = BurtonMute,
            )
            ui.searched && ui.sections.isEmpty() -> Text(
                "No matches in the music library.",
                color = BurtonMute,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ui.sections.forEach { section ->
                    item(key = "header-${section.title}") {
                        Text(
                            section.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BurtonMute,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                    }
                    items(section.items, key = { "${section.title}-${it.id}-${it.title}" }) { item ->
                        BrowseRow(
                            item = item,
                            onClick = {
                                if (item.isContainer) onOpenFolder(item.id, item.title)
                                else viewModel.play(item)
                            },
                            onMore = if (item.canPlay) {
                                { viewModel.openActions(item) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
    ui.actionsItem?.let { item ->
        TrackActionsSheet(
            item = item,
            page = ui.actionPage,
            groupName = ui.groupName,
            playlists = ui.playlists,
            playlistsLoading = ui.playlistsLoading,
            newPlaylistName = ui.newPlaylistName,
            busy = ui.busy,
            notice = ui.notice,
            onDismiss = viewModel::dismissActions,
            onPlayAction = viewModel::runPlayAction,
            onSaveFavorite = viewModel::saveFavorite,
            onOpenPlaylists = viewModel::openPlaylists,
            onBackToActions = viewModel::backToActions,
            onOpenNewPlaylist = viewModel::openNewPlaylist,
            onNewPlaylistName = viewModel::onNewPlaylistName,
            onAddToPlaylist = viewModel::addToPlaylist,
            onCreatePlaylist = viewModel::createPlaylistAndAdd,
        )
    }
}
