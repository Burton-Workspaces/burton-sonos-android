package com.burton.sonos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burton.sonos.domain.QueuePlayMode
import com.burton.sonos.domain.RepeatMode
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun TransportRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    playMode: QueuePlayMode = QueuePlayMode(),
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onShuffle != null) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(56.dp)) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = if (playMode.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (playMode.shuffle) BurtonSand else BurtonMute,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = BurtonIvory,
                modifier = Modifier.size(44.dp),
            )
        }
        IconButton(onClick = onToggle, modifier = Modifier.size(96.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                contentDescription = "Play or pause",
                tint = BurtonIvory,
                modifier = Modifier.size(88.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = BurtonIvory,
                modifier = Modifier.size(44.dp),
            )
        }
        if (onRepeat != null) {
            IconButton(onClick = onRepeat, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = if (playMode.repeat == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = when (playMode.repeat) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat on"
                        RepeatMode.ONE -> "Repeat once"
                    },
                    tint = if (playMode.repeat == RepeatMode.OFF) BurtonMute else BurtonSand,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
