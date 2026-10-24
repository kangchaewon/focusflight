package com.flightfocus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = Color(0xFF0B1424)
private val Cabin = Color(0xFF15233B)
private val CabinHigh = Color(0xFF1F3152)
private val Amber = Color(0xFFFFB547)
private val Sky = Color(0xFF7CC4FF)
private val Cloud = Color(0xFFE8EEF7)
private val Warning = Color(0xFFFF6B6B)

private val ColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Night,
    secondary = Sky,
    onSecondary = Night,
    background = Night,
    onBackground = Cloud,
    surface = Night,
    onSurface = Cloud,
    surfaceContainer = Cabin,
    surfaceContainerHigh = CabinHigh,
    surfaceVariant = Cabin,
    onSurfaceVariant = Color(0xFFA9B6CB),
    error = Warning,
)

@Composable
fun FlightFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, content = content)
}
