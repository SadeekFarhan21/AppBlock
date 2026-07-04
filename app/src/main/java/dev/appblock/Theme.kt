package dev.appblock

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0A0A0A)
val Graphite = Color(0xFF161616)
val GraphiteHigh = Color(0xFF222222)

private val Scheme = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondaryContainer = GraphiteHigh,
    onSecondaryContainer = Color(0xFFEDEDED),
    background = Ink,
    onBackground = Color(0xFFEDEDED),
    surface = Ink,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Graphite,
    onSurfaceVariant = Color(0xFF9A9A9A),
    surfaceContainer = Graphite,
    surfaceContainerHigh = GraphiteHigh,
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFF2A1517),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF242424),
)

@Composable
fun AppBlockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
