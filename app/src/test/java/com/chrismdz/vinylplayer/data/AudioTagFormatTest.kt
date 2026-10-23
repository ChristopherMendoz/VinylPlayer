package com.chrismdz.vinylplayer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTagFormatTest {
    @Test
    fun supportsWritableAudioExtensionsButNotOpusOrVideo() {
        listOf("song.mp3", "song.flac", "song.ogg", "song.m4a", "song.mp4", "song.wav", "song.aiff", "song.wma", "song.dsf")
            .forEach { assertTrue("$it should be supported", AudioTagWriter.supportsFileName(it)) }
        listOf("song.opus", "movie.mkv", "cover.jpg", "song.unknown")
            .forEach { assertFalse("$it should not be supported", AudioTagWriter.supportsFileName(it)) }
    }
}
