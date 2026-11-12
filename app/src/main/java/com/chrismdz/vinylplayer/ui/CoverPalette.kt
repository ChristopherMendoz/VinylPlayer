package com.chrismdz.vinylplayer.ui

import android.graphics.Bitmap

object CoverPalette {
    private data class ColorBucket(
        var count: Int = 0,
        var red: Int = 0,
        var green: Int = 0,
        var blue: Int = 0
    ) {
        fun add(color: Int) {
            count++
            red += color shr 16 and 0xFF
            green += color shr 8 and 0xFF
            blue += color and 0xFF
        }

        fun color(): Int = (0xFF shl 24) or
            ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
    }

    fun fromPixels(pixels: IntArray): List<Int> {
        if (pixels.isEmpty()) return emptyList()
        val buckets = linkedMapOf<Int, ColorBucket>()
        var opaquePixels = 0
        pixels.forEach { color ->
            if (color ushr 24 < 0x80) return@forEach
            opaquePixels++
            val red = color shr 16 and 0xFF
            val green = color shr 8 and 0xFF
            val blue = color and 0xFF
            val key = ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
            buckets.getOrPut(key) { ColorBucket() }.add(color)
        }
        if (opaquePixels == 0) return emptyList()
        val ranked = buckets.values.sortedByDescending { it.count }
        if (ranked.first().count.toFloat() / opaquePixels >= 0.36f) return listOf(ranked.first().color())
        return ranked.take(3).map(ColorBucket::color)
    }

    fun fromBitmap(bitmap: Bitmap): List<Int> = fromPixels(samplePixels(bitmap))

    /**
     * Picks the cover's most *characteristic* color plus a second, different-hue one, for the album
     * backdrop. Unlike [fromPixels] (most frequent color), it favours saturated, mid-lightness
     * colors so a red logo on a black sleeve yields red, not black. Grey covers still yield their
     * grey. Returns null when there is nothing opaque to read.
     */
    fun ambienceFromPixels(pixels: IntArray): Pair<Int, Int>? {
        val buckets = linkedMapOf<Int, ColorBucket>()
        pixels.forEach { color ->
            if (color ushr 24 < 0x80) return@forEach
            val key = ((color shr 20 and 0xF) shl 8) or ((color shr 12 and 0xF) shl 4) or (color shr 4 and 0xF)
            buckets.getOrPut(key) { ColorBucket() }.add(color)
        }
        if (buckets.isEmpty()) return null
        data class Scored(val argb: Int, val hue: Float, val score: Float)
        val scored = buckets.values.map { bucket ->
            val argb = bucket.color()
            val hsl = com.chrismdz.vinylplayer.settings.AccentColorValue.toHsl(argb)
            // Vividness: saturated and away from pure black/white. The floor keeps grey covers usable.
            val vividness = 0.12f + hsl.saturation * (1f - kotlin.math.abs(hsl.lightness - 0.5f) * 1.6f).coerceAtLeast(0f)
            Scored(argb, hsl.hue, bucket.count * vividness)
        }.sortedByDescending { it.score }
        val primary = scored.first()
        val secondary = scored.drop(1).firstOrNull { candidate ->
            val distance = kotlin.math.abs(candidate.hue - primary.hue).let { minOf(it, 360f - it) }
            distance >= 28f && candidate.score >= primary.score * 0.18f
        } ?: primary
        return primary.argb to secondary.argb
    }

    fun ambienceFromBitmap(bitmap: Bitmap): Pair<Int, Int>? = ambienceFromPixels(samplePixels(bitmap))

    /** About 32×32 evenly spaced pixels: enough for color statistics, cheap on any cover size. */
    private fun samplePixels(bitmap: Bitmap): IntArray {
        val stepX = ((bitmap.width + 31) / 32).coerceAtLeast(1)
        val stepY = ((bitmap.height + 31) / 32).coerceAtLeast(1)
        val pixels = ArrayList<Int>(1_024)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                pixels += bitmap.getPixel(x, y)
                x += stepX
            }
            y += stepY
        }
        return pixels.toIntArray()
    }
}
