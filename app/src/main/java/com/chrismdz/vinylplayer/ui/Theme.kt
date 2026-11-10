package com.chrismdz.vinylplayer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chrismdz.vinylplayer.settings.AccentColor
import com.chrismdz.vinylplayer.settings.AccentColorValue
import com.chrismdz.vinylplayer.settings.ThemeMode

/**
 * VinylPlayer palette: near-black layered surfaces with a gold accent used sparingly (selection, primary
 * actions, now playing). Every screen reads these through MaterialTheme so dialogs, menus and switches
 * match without hard-coded colors.
 */
internal object VinylPalette {
    val Gold = Color(0xFFE4B45B)
    val GoldDeep = Color(0xFFA8741A)
    val Red = Color(0xFFFF7070)
    val RedDeep = Color(0xFFB3261E)
    val Danger = Color(0xFFFF6B6B)
    val Warning = Color(0xFFFFB74D)

    // Dark surfaces, from the page background up to raised elements.
    val Background = Color(0xFF090909)
    val Surface = Color(0xFF121212)
    val SurfaceContainer = Color(0xFF181818)
    val SurfaceContainerHigh = Color(0xFF202020)
    val SurfaceContainerHighest = Color(0xFF2A2A2A)

    // Light surfaces: clean neutral greys, never pure white, so the theme is bright without glare.
    val LightBackground = Color(0xFFF1F1F3)
    val LightSurface = Color(0xFFF8F8FA)
    val LightContainer = Color(0xFFE8E8EC)
    val LightContainerHigh = Color(0xFFDFDFE4)
    val LightContainerHighest = Color(0xFFD3D3D9)
    val LightText = Color(0xFF16161A)
}

private val VinylTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
)

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
        AccentColor.GOLD -> if (darkTheme) VinylPalette.Gold else VinylPalette.GoldDeep
        AccentColor.RED -> if (darkTheme) VinylPalette.Red else VinylPalette.RedDeep
        AccentColor.CUSTOM -> if (darkTheme) VinylPalette.Gold else VinylPalette.GoldDeep
    }
    val accentBackdrop = if (darkTheme) VinylPalette.Surface.toArgb() else VinylPalette.LightSurface.toArgb()
    val onPrimary = Color(AccentColorValue.highContrastForeground(primary.toArgb(), accentBackdrop))
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = primary,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primary.copy(alpha = 0.18f),
            onSecondaryContainer = Color.White,
            background = VinylPalette.Background,
            onBackground = Color.White,
            surface = VinylPalette.Surface,
            onSurface = Color.White,
            surfaceVariant = VinylPalette.SurfaceContainerHigh,
            onSurfaceVariant = Color.White.copy(alpha = 0.64f),
            surfaceContainerLowest = VinylPalette.Background,
            surfaceContainerLow = VinylPalette.Surface,
            surfaceContainer = VinylPalette.SurfaceContainer,
            surfaceContainerHigh = VinylPalette.SurfaceContainerHigh,
            surfaceContainerHighest = VinylPalette.SurfaceContainerHighest,
            outline = Color.White.copy(alpha = 0.22f),
            outlineVariant = Color.White.copy(alpha = 0.08f),
            error = VinylPalette.Danger,
            onError = Color.Black,
            errorContainer = Color(0xFF5C1A1A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.16f),
            onPrimaryContainer = primary,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primary.copy(alpha = 0.16f),
            onSecondaryContainer = VinylPalette.LightText,
            background = VinylPalette.LightBackground,
            onBackground = VinylPalette.LightText,
            surface = VinylPalette.LightSurface,
            onSurface = VinylPalette.LightText,
            surfaceVariant = VinylPalette.LightContainerHigh,
            onSurfaceVariant = VinylPalette.LightText.copy(alpha = 0.68f),
            surfaceContainerLowest = VinylPalette.LightSurface,
            surfaceContainerLow = VinylPalette.LightBackground,
            surfaceContainer = VinylPalette.LightContainer,
            surfaceContainerHigh = VinylPalette.LightContainerHigh,
            surfaceContainerHighest = VinylPalette.LightContainerHighest,
            outline = VinylPalette.LightText.copy(alpha = 0.24f),
            outlineVariant = VinylPalette.LightText.copy(alpha = 0.10f),
            error = VinylPalette.RedDeep,
            onError = Color.White
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = VinylTypography,
        content = content
    )
}
