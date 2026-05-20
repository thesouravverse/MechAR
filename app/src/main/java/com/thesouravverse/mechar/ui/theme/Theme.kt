package com.thesouravverse.mechar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MechRed = Color(0xFFE63953)
private val MechCyan = Color(0xFF22D6E0)
private val MechNight = Color(0xFF0B0B12)

private val DarkColors = darkColorScheme(
    primary = MechCyan,
    secondary = MechRed,
    background = MechNight,
    surface = MechNight
)

private val LightColors = lightColorScheme(
    primary = MechCyan,
    secondary = MechRed
)

@Composable
fun MechARTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
