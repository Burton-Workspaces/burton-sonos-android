package com.burton.sonos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burton.sonos.ui.theme.BurtonIvory

@Composable
fun TransportRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = BurtonIvory)
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                contentDescription = "Play or pause",
                tint = BurtonIvory,
                modifier = Modifier.fillMaxWidth(0.18f),
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = BurtonIvory)
        }
    }
}
