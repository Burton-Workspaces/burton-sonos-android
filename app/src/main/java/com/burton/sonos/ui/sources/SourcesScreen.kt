package com.burton.sonos.ui.sources

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.domain.SystemSource
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun SourcesScreen(
    onBrowse: (objectId: String, title: String) -> Unit,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val indexing by viewModel.indexing.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val sources = viewModel.sources()
    val household = snapshot.household
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Sources",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = viewModel::scanForNewContent,
                enabled = household != null && !indexing,
            ) {
                if (indexing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BurtonSand,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Sync,
                        contentDescription = "Scan for new content",
                        tint = if (household != null) BurtonIvory else BurtonMute,
                    )
                }
            }
        }
        Text(
            text = when {
                indexing -> "Scanning music library for new content"
                notice != null -> notice.orEmpty()
                household == null -> "Waiting for speakers"
                else -> snapshot.selectedGroup?.let { household.groupName(it) } ?: "This system"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sources, key = { it.id }) { source ->
                SourceRow(
                    source = source,
                    onClick = {
                        when (source.kind) {
                            SystemSource.Kind.LINE_IN, SystemSource.Kind.TV -> viewModel.play(source)
                            else -> source.objectId?.let { onBrowse(it, source.title) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SourceRow(source: SystemSource, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(iconFor(source.kind), contentDescription = null, tint = BurtonSand)
        Column(modifier = Modifier.weight(1f)) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
            Text(source.subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = BurtonMute)
    }
}

private fun iconFor(kind: SystemSource.Kind): ImageVector = when (kind) {
    SystemSource.Kind.QUEUE -> Icons.AutoMirrored.Rounded.QueueMusic
    SystemSource.Kind.FAVORITES -> Icons.Rounded.Favorite
    SystemSource.Kind.PLAYLISTS, SystemSource.Kind.LIBRARY -> Icons.Rounded.LibraryMusic
    SystemSource.Kind.RADIO -> Icons.Rounded.Radio
    SystemSource.Kind.LINE_IN -> Icons.Rounded.Headphones
    SystemSource.Kind.TV -> Icons.Rounded.Tv
}
