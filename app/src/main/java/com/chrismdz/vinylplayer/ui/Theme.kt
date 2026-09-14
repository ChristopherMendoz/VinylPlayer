package com.chrismdz.vinylplayer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VinylColors = darkColorScheme(
    primary = Color(0xFFE4B45B),
    onPrimary = Color(0xFF17120A),
    background = Color(0xFF090909),
    surface = Color(0xFF151515),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VinylPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VinylColors,
        content = content
    )
}
