package com.burton.sonos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSand

@Composable
fun AlphabetChevron(
    letters: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 2) return
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = if (expanded) Alignment.CenterEnd else Alignment.CenterEnd,
    ) {
        if (!expanded) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(56.dp)
                    .clip(shape)
                    .background(BurtonElevated)
                    .clickable { expanded = true }
                    .semantics { contentDescription = "Browse alphabetically" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = BurtonSand,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .clip(shape)
                    .background(BurtonElevated),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(letters) {
                            detectTapGestures { offset ->
                                letterAt(offset.y, size.height.toFloat(), letters)?.let(onSelect)
                            }
                        }
                        .pointerInput(letters) {
                            detectVerticalDragGestures { change, _ ->
                                letterAt(change.position.y, size.height.toFloat(), letters)?.let(onSelect)
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    letters.forEach { letter ->
                        val active = letter.equals(selected, ignoreCase = true)
                        Text(
                            letter,
                            color = if (active) BurtonSand else BurtonIvory,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                letterSpacing = 0.sp,
                            ),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { expanded = false }
                        .semantics { contentDescription = "Hide alphabet" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BurtonMute,
                    )
                }
            }
        }
    }
}

private fun letterAt(y: Float, height: Float, letters: List<String>): String? {
    if (letters.isEmpty() || height <= 0f) return letters.firstOrNull()
    val index = (y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)
    return letters[index]
}
