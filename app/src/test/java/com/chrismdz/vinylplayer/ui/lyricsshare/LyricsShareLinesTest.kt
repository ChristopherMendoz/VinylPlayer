package com.chrismdz.vinylplayer.ui.lyricsshare

import com.chrismdz.vinylplayer.data.Lyrics
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsShareLinesTest {
    private fun lyrics(text: String? = null, synced: String? = null) = Lyrics(
        songId = "song", text = text, syncedText = synced, source = null, fetchedAt = 0L, found = true
    )

    @Test
    fun keepsVerseBreaksFromPlainLyrics() {
        assertEquals(
            listOf("Line one", "Line two", null, "Line three"),
            shareableLyricLines(lyrics(text = "\nLine one\nLine two\n\n\nLine three\n\n"))
        )
    }

    @Test
    fun stripsTimestampsAndTagsFromSyncedLyrics() {
        assertEquals(
            listOf("Hello", null, "World"),
            shareableLyricLines(lyrics(synced = "[ar:Someone]\n[00:01.00]Hello\n[00:05.00]\n[00:06.50][00:20.00]World"))
        )
    }

    @Test
    fun prefersPlainLyricsWhenBothExist() {
        assertEquals(listOf("Plain"), shareableLyricLines(lyrics(text = "Plain", synced = "[00:01.00]Synced")))
    }
}
