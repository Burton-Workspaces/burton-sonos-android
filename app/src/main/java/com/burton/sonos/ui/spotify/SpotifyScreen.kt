package com.burton.sonos.ui.spotify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.browse.BrowseRow
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonVoid

@Composable
fun SpotifyScreen(
    onOpenFolder: (id: String, title: String) -> Unit,
    viewModel: SpotifyViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("BURTON SONOS", style = MaterialTheme.typography.labelSmall, color = BurtonSand)
        Text("Spotify", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BurtonCharcoal, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (ui.connected) "Connected to this household" else "Not on this household yet",
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
            )
            Text(
                text = if (ui.connected) {
                    ui.nickname.ifBlank { "Spotify is available as a source on these speakers." }
                } else {
                    "Add Spotify so this Sonos system can play your library, playlists, and radio."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
            ui.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSand) }
            Button(
                onClick = { viewModel.startLink(context) },
                enabled = !ui.linking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurtonIvory,
                    contentColor = BurtonVoid,
                    disabledContainerColor = BurtonMute,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.connected) "Reconnect Spotify" else "Add Spotify")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Browse", style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
        Spacer(Modifier.height(8.dp))
        when {
            ui.loadingBrowse -> CircularProgressIndicator(color = BurtonSand, modifier = Modifier.padding(24.dp))
            ui.items.isEmpty() -> Text(
                if (ui.connected) {
                    "Connected, but the catalog isn't readable yet. Reconnect if this stays empty."
                } else {
                    "Add Spotify to browse this account from Burton Sonos."
                },
                color = BurtonMute,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(ui.items, key = { it.id }) { item ->
                    BrowseRow(
                        item = item,
                        onClick = {
                            if (item.isContainer) onOpenFolder(item.id, item.title) else viewModel.play(item)
                        },
                    )
                }
            }
        }
    }
}
