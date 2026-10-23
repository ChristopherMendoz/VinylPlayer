package com.chrismdz.vinylplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverPaletteTest {
    @Test
    fun emptyAndTransparentPixelsHaveNoPalette() {
        assertTrue(CoverPalette.fromPixels(intArrayOf()).isEmpty())
        assertTrue(CoverPalette.fromPixels(intArrayOf(0x00112233, 0x7F445566)).isEmpty())
    }

    @Test
    fun returnsDominantColorWhenItIsClear() {
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()
        assertEquals(listOf(red), CoverPalette.fromPixels(IntArray(8) { if (it < 7) red else blue }))
    }

    @Test
    fun combinesRepresentativeColorsWhenThereIsNoDominantColor() {
        val colors = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
        assertEquals(3, CoverPalette.fromPixels(colors).size)
    }
}
