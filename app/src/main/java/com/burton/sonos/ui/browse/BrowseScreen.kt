package com.burton.sonos.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.ui.components.AlbumArt
import com.burton.sonos.ui.components.AlphabetChevron
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.track.TrackActionsHost
import com.burton.sonos.ui.track.TrackActionsViewModel
import kotlinx.coroutines.launch

@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onOpenFolder: (id: String, title: String) -> Unit,
    onOpenGrouping: (() -> Unit)? = null,
    viewModel: BrowseViewModel = hiltViewModel(),
    actionsViewModel: TrackActionsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val alphabet = ui.alphabetLetters
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(
                ui.title,
                style = MaterialTheme.typography.headlineMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (viewModel.isQueue && onOpenGrouping != null) {
                IconButton(onClick = onOpenGrouping) {
                    Icon(Icons.Rounded.SpeakerGroup, contentDescription = "Group speakers", tint = BurtonSand)
                }
            }
        }
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurtonSand)
            }
            ui.error != null -> Text(
                ui.error ?: "",
                color = BurtonIvory,
                modifier = Modifier.padding(16.dp),
            )
            ui.items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                EmptyBrowseCard()
            }
            else -> {
                LaunchedEffect(ui.items.firstOrNull()?.id) {
                    if (ui.remoteAlphabet) listState.scrollToItem(0)
                }
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = if (alphabet.size >= 2) 8.dp else 16.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        items(ui.items, key = { it.id + it.title }) { item ->
                            BrowseRow(
                                item = item,
                                onClick = {
                                    if (item.isContainer) onOpenFolder(item.id, item.title)
                                    else viewModel.play(item)
                                },
                                onMore = if (item.canPlay) {
                                    { actionsViewModel.open(item) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                    if (alphabet.size >= 2) {
                        AlphabetChevron(
                            letters = alphabet,
                            selected = ui.selectedPrefix,
                            onSelect = { letter ->
                                viewModel.selectPrefix(letter)
                                if (!ui.remoteAlphabet) {
                                    val index = viewModel.firstIndexFor(letter)
                                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                                }
                            },
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
    TrackActionsHost(viewModel = actionsViewModel)
}

@Composable
private fun EmptyBrowseCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonElevated, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Text(
            "Nothing in this source yet",
            style = MaterialTheme.typography.titleLarge,
            color = BurtonIvory,
        )
    }
}

@Composable
fun BrowseRow(
    item: BrowseItem,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AlbumArt(url = item.albumArtUrl, size = 48.dp, corner = 8.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonMute, maxLines = 1)
            }
        }
        if (onMore != null) {
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More", tint = BurtonMute)
            }
        } else if (item.canPlay && !item.isContainer) {
            Text("Play", style = MaterialTheme.typography.labelLarge, color = BurtonSand)
        }
    }
}
