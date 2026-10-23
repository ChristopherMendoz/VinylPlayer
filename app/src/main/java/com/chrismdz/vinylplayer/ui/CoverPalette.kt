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

    fun fromBitmap(bitmap: Bitmap): List<Int> {
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
        return fromPixels(pixels.toIntArray())
    }
}
