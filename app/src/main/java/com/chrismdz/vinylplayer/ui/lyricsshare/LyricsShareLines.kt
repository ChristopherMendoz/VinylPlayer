package com.chrismdz.vinylplayer.ui.lyricsshare

import com.chrismdz.vinylplayer.data.Lyrics

/** Most lines one card can hold before it gets crowded. */
internal const val MAX_SHARED_LYRIC_LINES = 6

private val LRC_TAGS = Regex("^(\\[[^\\]]*])+")

/**
 * The song's lines for the selection screen, in order. `null` entries mark the original breaks
 * between verses (blank lines). Plain lyrics are preferred because they keep that structure;
 * synced (LRC) lyrics are used otherwise, without their timestamps or metadata tags.
 */
internal fun shareableLyricLines(lyrics: Lyrics): List<String?> {
    val plain = lyrics.text?.takeIf(String::isNotBlank)
    val raw = plain?.lines()
        ?: lyrics.syncedText?.lines()?.map { it.trim().replace(LRC_TAGS, "") }
        ?: emptyList()
    val result = ArrayList<String?>(raw.size)
    raw.forEach { line ->
        val text = line.trim()
        if (text.isEmpty()) {
            // One break per gap, never at the start.
            if (result.isNotEmpty() && result.last() != null) result += null
        } else {
            result += text
        }
    }
    while (result.isNotEmpty() && result.last() == null) result.removeAt(result.lastIndex)
    return result
}
