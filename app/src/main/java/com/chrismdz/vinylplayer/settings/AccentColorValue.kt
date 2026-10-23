package com.chrismdz.vinylplayer.settings

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class HslColor(val hue: Float, val saturation: Float, val lightness: Float)

object AccentColorValue {
    fun highContrastForeground(argb: Int, backgroundArgb: Int): Int {
        fun linearize(channel: Int): Float {
            val srgb = channel / 255f
            return if (srgb <= 0.04045f) srgb / 12.92f else ((srgb + 0.055f) / 1.055f).pow(2.4f)
        }

        val alpha = (argb ushr 24 and 0xFF) / 255f
        fun composite(foreground: Int, background: Int): Int =
            (foreground * alpha + background * (1f - alpha)).roundToInt()

        val luminance =
            0.2126f * linearize(composite(argb shr 16 and 0xFF, backgroundArgb shr 16 and 0xFF)) +
                0.7152f * linearize(composite(argb shr 8 and 0xFF, backgroundArgb shr 8 and 0xFF)) +
                0.0722f * linearize(composite(argb and 0xFF, backgroundArgb and 0xFF))
        val blackContrast = (luminance + 0.05f) / 0.05f
        val whiteContrast = 1.05f / (luminance + 0.05f)
        return if (blackContrast >= whiteContrast) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }

    fun parseHex(value: String): Int? {
        val digits = value.trim().removePrefix("#")
        if (digits.length != 6 && digits.length != 8) return null
        val parsed = digits.toLongOrNull(16) ?: return null
        return if (digits.length == 6) (0xFF000000L or parsed).toInt() else parsed.toInt()
    }

    fun toHex(argb: Int): String = String.format(Locale.US, "#%08X", argb)

    fun fromHsl(hue: Float, saturation: Float, lightness: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f / 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val chroma = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = chroma * (1f - kotlin.math.abs((h * 6f) % 2f - 1f))
        val m = l - chroma / 2f
        val (r, g, b) = when ((h * 6f).toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        return (0xFF shl 24) or
            ((r + m).coerceIn(0f, 1f) * 255).roundToInt().shl(16) or
            ((g + m).coerceIn(0f, 1f) * 255).roundToInt().shl(8) or
            ((b + m).coerceIn(0f, 1f) * 255).roundToInt()
    }

    fun toHsl(argb: Int): HslColor {
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val lightness = (max + min) / 2f
        val saturation = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * lightness - 1f))
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return HslColor(hue, saturation, lightness)
    }
}
