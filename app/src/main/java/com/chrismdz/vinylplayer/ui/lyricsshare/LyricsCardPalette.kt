package com.chrismdz.vinylplayer.ui.lyricsshare

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import com.chrismdz.vinylplayer.settings.AccentColorValue
import com.chrismdz.vinylplayer.ui.CoverAmbience
import com.chrismdz.vinylplayer.ui.CoverAmbienceCache
import com.chrismdz.vinylplayer.ui.CoverPalette
import com.chrismdz.vinylplayer.ui.VinylPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How a multi-color background is laid out on the card. */
internal enum class CardGradient { VERTICAL, DIAGONAL }

/**
 * One color combination for a lyrics card. Colors are ARGB ints because the card is drawn with
 * android.graphics (the same drawing is used for the preview and for the exported image).
 */
internal data class LyricsCardColors(
    val label: String,
    /** One color = solid background; more = gradient through them. */
    val background: List<Int>,
    val gradient: CardGradient,
    /** Slightly different surface for inner panels (Classic card). */
    val surface: Int,
    val text: Int,
    val secondaryText: Int,
    val accent: Int
)

/**
 * Adapter between VinylPlayer's existing cover color system and the share cards. It does not
 * analyse covers itself: it reads the same cached analysis that drives the album backdrop
 * ([CoverAmbienceCache]) plus the existing frequent-color palette ([CoverPalette.fromBitmap]),
 * and derives card variants from them.
 */
internal object LyricsCardPalette {

    /** Card schemes for [cover]; falls back to a dark neutral + gold set when there is no cover. */
    suspend fun schemesFor(cover: Bitmap?): List<LyricsCardColors> {
        if (cover == null) return fallback()
        val (rawPrimary, rawSecondary) = CoverAmbienceCache.characteristicColors(cover)
        val primary = rawPrimary ?: return fallback()
        val secondary = rawSecondary ?: primary
        // Exactly the album screen's backdrop tones (same cache, same toning).
        val ambience = CoverAmbienceCache.get(cover)
        val extras = withContext(Dispatchers.Default) {
            runCatching { CoverPalette.fromBitmap(cover) }.getOrDefault(emptyList())
        }
        return build(primary, secondary, ambience, extras)
    }

    /** Neutral set used until the cover is analysed, or when it has no readable colors. */
    fun fallback(): List<LyricsCardColors> {
        val neutral = CoverAmbience.Neutral
        val gold = VinylPalette.Gold.toArgb()
        return build(
            primary = neutral.primary.toArgb(),
            secondary = neutral.secondary.toArgb(),
            ambience = neutral,
            extras = listOf(neutral.primary.toArgb(), gold, neutral.secondary.toArgb())
        )
    }

    private fun build(primary: Int, secondary: Int, ambience: CoverAmbience, extras: List<Int>): List<LyricsCardColors> {
        val p = hsl(primary)
        // Distinct third color for richer mixes: a frequent cover color whose hue differs from the
        // main one, else a neighbour of the main hue (never a random color).
        val third = extras.firstOrNull { hueDistance(hsl(it).hue, p.hue) > 25f && hsl(it).saturation > 0.12f }
            ?: shiftHue(primary, 28f)
        val albumTop = ambience.primary.toArgb()
        val albumMid = ambience.secondary.toArgb()

        return listOf(
            // 1. The album screen's own gradient, fading into near-black like the album view.
            scheme("Álbum", listOf(albumTop, albumMid, tone(secondary, 0.07f, maxSat = 0.6f)), CardGradient.VERTICAL),
            // 2. Alternative gradient led by the secondary colors.
            scheme("Alterno", listOf(tone(secondary, 0.34f), tone(third, 0.22f)), CardGradient.DIAGONAL),
            // 3. Main color, solid.
            scheme("Sólido", listOf(tone(primary, 0.30f, minSat = 0.4f)), CardGradient.VERTICAL),
            // 4. Deep, dark take on the palette.
            scheme("Oscuro", listOf(tone(primary, 0.15f), tone(secondary, 0.07f)), CardGradient.VERTICAL),
            // 5. Light, pastel take (dark text).
            scheme("Claro", listOf(tone(primary, 0.86f, maxSat = 0.55f), tone(secondary, 0.77f, maxSat = 0.5f)), CardGradient.DIAGONAL),
            // 6. The cover's secondary colors combined.
            scheme("Mezcla", listOf(tone(secondary, 0.32f), tone(third, 0.27f), tone(primary, 0.18f)), CardGradient.DIAGONAL)
        )
    }

    private fun scheme(label: String, background: List<Int>, gradient: CardGradient): LyricsCardColors {
        val average = average(background)
        // Dark text wherever it gives more contrast than white (WCAG crossover ≈ 0.18 luminance).
        val light = luminance(average) > 0.18f
        val text = if (light) 0xFF141414.toInt() else 0xFFFFFFFF.toInt()
        val secondaryText = if (light) 0xB3141414.toInt() else 0xBFFFFFFF.toInt()
        val baseHsl = hsl(background.first())
        // Accent: the background's own hue, pushed to a readable lightness.
        val accent = AccentColorValue.fromHsl(
            baseHsl.hue,
            if (baseHsl.saturation < 0.1f) 0f else baseHsl.saturation.coerceIn(0.45f, 0.85f),
            if (light) 0.32f else 0.72f
        )
        val surface = blend(average, if (light) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), if (light) 0.35f else 0.28f)
        return LyricsCardColors(label, background, gradient, surface, text, secondaryText, accent)
    }

    // --- color helpers -------------------------------------------------------------------------

    private fun hsl(argb: Int) = AccentColorValue.toHsl(argb)

    private fun tone(argb: Int, lightness: Float, minSat: Float = 0.3f, maxSat: Float = 0.78f): Int {
        val h = hsl(argb)
        val saturation = if (h.saturation < 0.1f) h.saturation else h.saturation.coerceIn(minSat, maxSat)
        return AccentColorValue.fromHsl(h.hue, saturation, lightness)
    }

    private fun shiftHue(argb: Int, degrees: Float): Int {
        val h = hsl(argb)
        return AccentColorValue.fromHsl(h.hue + degrees, h.saturation, h.lightness)
    }

    private fun hueDistance(a: Float, b: Float): Float = kotlin.math.abs(a - b).let { minOf(it, 360f - it) }

    private fun average(colors: List<Int>): Int {
        var r = 0; var g = 0; var b = 0
        colors.forEach { r += it shr 16 and 0xFF; g += it shr 8 and 0xFF; b += it and 0xFF }
        val n = colors.size.coerceAtLeast(1)
        return (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        fun channel(shift: Int): Int {
            val a = from shr shift and 0xFF
            val b = to shr shift and 0xFF
            return (a + (b - a) * amount).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun luminance(argb: Int): Float {
        fun linear(c: Int): Float {
            val v = c / 255f
            return if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * linear(argb shr 16 and 0xFF) + 0.7152f * linear(argb shr 8 and 0xFF) + 0.0722f * linear(argb and 0xFF)
    }
}
