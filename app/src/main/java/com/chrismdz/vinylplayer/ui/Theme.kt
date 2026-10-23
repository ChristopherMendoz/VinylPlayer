package com.chrismdz.vinylplayer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.chrismdz.vinylplayer.settings.AccentColor
import com.chrismdz.vinylplayer.settings.AccentColorValue
import com.chrismdz.vinylplayer.settings.ThemeMode

@Composable
fun VinylPlayerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GOLD,
    customAccentArgb: Int? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val primary = customAccentArgb?.takeIf { accentColor == AccentColor.CUSTOM }?.let { Color(it) } ?: when (accentColor) {
        AccentColor.GOLD -> if (darkTheme) Color(0xFFE4B45B) else Color(0xFF805500)
        AccentColor.RED -> if (darkTheme) Color(0xFFFF7070) else Color(0xFFB3261E)
        AccentColor.CUSTOM -> if (darkTheme) Color(0xFFE4B45B) else Color(0xFF805500)
    }
    val accentBackdrop = if (darkTheme) 0xFF151515.toInt() else 0xFFFFFFFF.toInt()
    val onPrimary = Color(AccentColorValue.highContrastForeground(primary.toArgb(), accentBackdrop))
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            background = Color(0xFF090909),
            surface = Color(0xFF151515),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            background = Color(0xFFF6F3ED),
            surface = Color.White,
            onBackground = Color(0xFF201D18),
            onSurface = Color(0xFF201D18)
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
