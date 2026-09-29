package com.burton.sonos.ui.spotify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.browse.BrowseRow
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun SpotifyBrowseScreen(
    onBack: () -> Unit,
    onOpenFolder: (id: String, title: String) -> Unit,
    viewModel: SpotifyBrowseViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(ui.title, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
        }
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurtonSand)
            }
            ui.error != null -> Text(ui.error ?: "", color = BurtonIvory)
            ui.items.isEmpty() -> Text("Nothing here.", color = BurtonMute)
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
