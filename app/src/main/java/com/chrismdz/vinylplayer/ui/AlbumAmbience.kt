package com.chrismdz.vinylplayer.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.chrismdz.vinylplayer.settings.AccentColorValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The atmosphere of one album: two tones taken from its cover, already darkened and tamed so
 * white text and the gold controls stay readable on top of them.
 */
@Immutable
internal data class CoverAmbience(val primary: Color, val secondary: Color) {
    companion object {
        /** Elegant neutral used when an album has no cover or no readable color. */
        val Neutral = CoverAmbience(Color(0xFF2A2622), Color(0xFF16181A))
    }
}

/** The two characteristic colors read from a cover, before any toning; null colors = unreadable cover. */
private data class CoverColors(val primary: Int?, val secondary: Int?)

/**
 * Results per cover bitmap, so returning to an album, scrolling the grid or skipping songs never
 * re-analyses a cover. Shared by the album backdrop and the mini player. Keyed by bitmap identity +
 * generation instead of holding the bitmap, so covers can still be freed.
 */
internal object CoverAmbienceCache {
    private val cache = LruCache<String, CoverColors>(160)

    private fun key(bitmap: Bitmap) =
        "${System.identityHashCode(bitmap)}:${bitmap.generationId}:${bitmap.width}x${bitmap.height}"

    private fun peekColors(bitmap: Bitmap): CoverColors? = cache.get(key(bitmap))

    /** Analyses off the main thread; cheap when cached. */
    private suspend fun colors(bitmap: Bitmap): CoverColors {
        peekColors(bitmap)?.let { return it }
        val colors = withContext(Dispatchers.Default) {
            val pair = runCatching { CoverPalette.ambienceFromBitmap(bitmap) }.getOrNull()
            CoverColors(pair?.first, pair?.second)
        }
        cache.put(key(bitmap), colors)
        return colors
    }

    fun peek(bitmap: Bitmap): CoverAmbience? = peekColors(bitmap)?.let(::toAmbience)

    suspend fun get(bitmap: Bitmap): CoverAmbience = toAmbience(colors(bitmap))

    fun peekMiniPlayer(bitmap: Bitmap): Color? = peekColors(bitmap)?.let(::toMiniPlayer)

    suspend fun miniPlayer(bitmap: Bitmap): Color = toMiniPlayer(colors(bitmap))

    private fun toAmbience(colors: CoverColors): CoverAmbience {
        val primary = colors.primary ?: return CoverAmbience.Neutral
        val primaryHsl = AccentColorValue.toHsl(primary)
        val secondaryHsl = AccentColorValue.toHsl(colors.secondary ?: primary)
        // A near-black/grey second color would turn the middle of the screen black on colorful
        // covers; use a neighbouring shade of the main color instead.
        val secondary = if (primaryHsl.saturation >= 0.15f &&
            (secondaryHsl.saturation < 0.15f || secondaryHsl.lightness < 0.12f)
        ) {
            AccentColorValue.fromHsl(primaryHsl.hue + 18f, primaryHsl.saturation, primaryHsl.lightness)
        } else colors.secondary ?: primary
        return CoverAmbience(backdropTone(primary, 0.36f, minLightness = 0.20f), backdropTone(secondary, 0.26f, minLightness = 0.15f))
    }

    /** Richer and a bit lighter than the backdrop: the bar is small and should read as the album's color. */
    private fun toMiniPlayer(colors: CoverColors): Color {
        val primary = colors.primary ?: return MiniPlayerNeutral
        val hsl = AccentColorValue.toHsl(primary)
        val saturation = if (hsl.saturation < 0.08f) 0f else hsl.saturation.coerceIn(0.38f, 0.92f)
        return Color(AccentColorValue.fromHsl(hsl.hue, saturation, hsl.lightness.coerceIn(0.23f, 0.37f)))
    }

    /**
     * Keeps the hue, caps saturation so neon covers don't glare, and pins lightness low enough
     * for white text. Near-grey covers stay grey instead of getting an invented tint.
     */
    private fun backdropTone(argb: Int, maxLightness: Float, minLightness: Float): Color {
        val hsl = AccentColorValue.toHsl(argb)
        val saturation = if (hsl.saturation < 0.12f) hsl.saturation else hsl.saturation.coerceIn(0.38f, 0.74f)
        val lightness = hsl.lightness.coerceIn(minLightness, maxLightness)
        return Color(AccentColorValue.fromHsl(hsl.hue, saturation, lightness))
    }
}

internal val MiniPlayerNeutral = Color(0xFF202020)

/**
 * Mini player background for the playing song's album cover, cross-faded between songs. Like the
 * album backdrop, a cover that is still loading keeps the previous color instead of flashing grey.
 */
@Composable
internal fun rememberAnimatedMiniPlayerColor(artwork: Bitmap?): Color {
    var target by remember { mutableStateOf(artwork?.let(CoverAmbienceCache::peekMiniPlayer) ?: MiniPlayerNeutral) }
    LaunchedEffect(artwork) {
        if (artwork != null) {
            target = CoverAmbienceCache.miniPlayer(artwork)
        } else {
            delay(700)
            target = MiniPlayerNeutral
        }
    }
    val color by animateColorAsState(target, tween(550), label = "mini-player-color")
    return color
}

/**
 * Ambience for [artwork], animated between albums. While a new cover is still loading (null) the
 * previous colors are kept, so the screen never flashes to the fallback and back.
 */
@Composable
internal fun rememberAnimatedCoverAmbience(artwork: Bitmap?, durationMillis: Int = 650): CoverAmbience {
    var target by remember { mutableStateOf(artwork?.let(CoverAmbienceCache::peek) ?: CoverAmbience.Neutral) }
    LaunchedEffect(artwork) {
        if (artwork != null) {
            target = CoverAmbienceCache.get(artwork)
        } else {
            // Give a lazily loaded cover a moment before concluding there is none.
            delay(700)
            target = CoverAmbience.Neutral
        }
    }
    val primary by animateColorAsState(target.primary, tween(durationMillis), label = "ambience-primary")
    val secondary by animateColorAsState(target.secondary, tween(durationMillis), label = "ambience-secondary")
    return CoverAmbience(primary, secondary)
}

/**
 * Immersive backdrop: the cover's main color glows from the top, blends through its second color
 * and fades smoothly into the app's black, so the track list sits on a dark, readable base.
 * [intensity] (0..1) lets the glow recede as the user scrolls into the track list.
 */
internal fun Modifier.coverAmbienceBackground(
    ambience: CoverAmbience,
    base: Color = VinylPalette.Background,
    intensity: () -> Float = { 1f }
): Modifier = drawBehind {
    drawRect(base)
    val strength = intensity().coerceIn(0f, 1f)
    if (strength <= 0f) return@drawBehind
    val height = size.height
    // Vertical wash: color at the top, eased through several stops so there is no visible band.
    // Color holds through the upper and middle screen and only dissolves into pure black at the
    // very bottom, so black never takes over the middle of the screen.
    drawRect(
        Brush.verticalGradient(
            0f to ambience.primary.copy(alpha = strength),
            0.28f to ambience.primary.copy(alpha = 0.90f * strength),
            0.50f to ambience.secondary.copy(alpha = 0.68f * strength),
            0.70f to ambience.secondary.copy(alpha = 0.30f * strength),
            0.88f to ambience.secondary.copy(alpha = 0f),
            startY = 0f,
            endY = height
        )
    )
    // Soft radial glow behind the cover adds depth instead of a flat band of color.
    val radius = size.width * 0.95f
    drawRect(
        Brush.radialGradient(
            0f to ambience.primary.copy(alpha = 0.6f * strength),
            1f to Color.Transparent,
            center = Offset(size.width / 2f, height * 0.16f),
            radius = radius
        )
    )
}
