package com.burton.sonos.ui.room

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.sonos.ui.components.AlbumArt
import com.burton.sonos.ui.components.TransportRow
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand
import com.burton.sonos.ui.theme.BurtonSandDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    onBack: () -> Unit,
    viewModel: RoomDetailViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val household = snapshot.household
    val group = snapshot.selectedGroup
    val playback = snapshot.selectedPlayback
    val name = if (household != null && group != null) household.groupName(group) else "Room"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BurtonIvory)
        }
        Text(text = name, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
        Text(
            text = if (playback?.state?.isPlaying == true) "Playing" else "Idle",
            style = MaterialTheme.typography.labelLarge,
            color = BurtonSand,
        )
        Spacer(Modifier.height(28.dp))
        AlbumArt(url = playback?.track?.albumArtUrl, size = 280.dp, corner = 24.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = playback?.track?.title ?: playback?.displayTitle ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            color = BurtonIvory,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = playback?.track?.artist?.ifBlank { playback.track?.album } ?: "Choose a source to start",
            style = MaterialTheme.typography.bodyLarge,
            color = BurtonMute,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        TransportRow(
            isPlaying = playback?.state?.isPlaying == true,
            onPrevious = viewModel::previous,
            onToggle = viewModel::toggle,
            onNext = viewModel::next,
        )
        Spacer(Modifier.height(8.dp))
        Text("Volume", style = MaterialTheme.typography.labelSmall, color = BurtonSandDim)
        Slider(
            value = (playback?.volume ?: 0).toFloat(),
            onValueChange = { viewModel.setVolume(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = BurtonIvory,
                activeTrackColor = BurtonSand,
                inactiveTrackColor = BurtonMute.copy(alpha = 0.3f),
            ),
        )
        Spacer(Modifier.height(32.dp))
    }
}
