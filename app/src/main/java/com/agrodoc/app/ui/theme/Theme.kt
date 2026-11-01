package com.agrodoc.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand palette (Section 7 of ARCHITECTURE.md) ──────────────────────────────
val Green900 = Color(0xFF1B5E20)   // primary
val Green600 = Color(0xFF43A047)   // secondary
val GreenContainer = Color(0xFFE8F5E9)
val GreenContainerLight = Color(0xFFC8E6C9)
val Amber = Color(0xFFFFB300)      // tertiary / trust-medium
val ErrorRed = Color(0xFFC62828)
val TextDark = Color(0xFF1B1B1B)
val BackgroundLight = Color(0xFFF9FBF9)

private val LightColors = lightColorScheme(
    primary = Green900,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF002106),
    secondary = Green600,
    onSecondary = Color.White,
    secondaryContainer = GreenContainerLight,
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Amber,
    onTertiary = TextDark,
    error = ErrorRed,
    background = BackgroundLight,
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
)

/**
 * AgroDoc Material 3 theme.
 * Only a light theme is defined for Phase 0; dark theme support will be added in Phase 6.
 */
@Composable
fun AgroDocTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AgroDocTypography,
        content = content,
    )
}
