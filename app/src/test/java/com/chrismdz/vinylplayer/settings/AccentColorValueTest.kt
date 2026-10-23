package com.chrismdz.vinylplayer.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccentColorValueTest {
    @Test
    fun parsesRgbAsOpaqueArgb() {
        assertEquals(0xFF12AB34.toInt(), AccentColorValue.parseHex("#12AB34"))
    }

    @Test
    fun parsesArgbAndPreservesAlphaWhenFormatting() {
        val argb = 0x7F12AB34
        assertEquals(argb, AccentColorValue.parseHex("#7F12AB34"))
        assertEquals("#7F12AB34", AccentColorValue.toHex(argb))
    }

    @Test
    fun rejectsInvalidHex() {
        assertNull(AccentColorValue.parseHex("red"))
        assertNull(AccentColorValue.parseHex("#12345"))
    }

    @Test
    fun choosesForegroundUsingLinearRgbContrast() {
        assertEquals(0xFF000000.toInt(), AccentColorValue.highContrastForeground(0xFF00B200.toInt(), 0xFF151515.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), AccentColorValue.highContrastForeground(0xFF102040.toInt(), 0xFF151515.toInt()))
        assertEquals(0xFF000000.toInt(), AccentColorValue.highContrastForeground(0xFFFFFF00.toInt(), 0xFFFFFFFF.toInt()))
    }

    @Test
    fun compositesTranslucentAccentOverCurrentThemeSurface() {
        assertEquals(0xFFFFFFFF.toInt(), AccentColorValue.highContrastForeground(0x01FFFFFF, 0xFF151515.toInt()))
        assertEquals(0xFF000000.toInt(), AccentColorValue.highContrastForeground(0x01FFFFFF, 0xFFFFFFFF.toInt()))
    }
}
