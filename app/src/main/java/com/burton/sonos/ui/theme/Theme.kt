package com.burton.sonos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BurtonScheme = darkColorScheme(
    primary = BurtonIvory,
    onPrimary = BurtonBlack,
    primaryContainer = BurtonElevated,
    onPrimaryContainer = BurtonIvory,
    secondary = BurtonSand,
    onSecondary = BurtonBlack,
    secondaryContainer = BurtonGraphite,
    onSecondaryContainer = BurtonSand,
    tertiary = BurtonSandDim,
    background = BurtonBlack,
    onBackground = BurtonIvory,
    surface = BurtonVoid,
    onSurface = BurtonIvory,
    surfaceVariant = BurtonCharcoal,
    onSurfaceVariant = BurtonMute,
    outline = BurtonLine,
    outlineVariant = BurtonGraphite,
    error = BurtonDanger,
    onError = BurtonIvory,
    inverseSurface = BurtonIvory,
    inverseOnSurface = BurtonBlack,
    scrim = Color.Black,
)

@Composable
fun BurtonSonosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BurtonScheme,
        typography = BurtonTypography,
        content = content,
    )
}
