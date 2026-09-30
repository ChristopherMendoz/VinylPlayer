package com.chrismdz.vinylplayer.data

data class Lyrics(
    val songId: String,
    val text: String?,
    val syncedText: String?,
    val translatedText: String? = null,
    val translatedSyncedText: String? = null,
    val source: String?,
    val fetchedAt: Long,
    val found: Boolean
)

data class LyricsResult(
    val text: String?,
    val syncedText: String?,
    val source: String
)

enum class LyricsStatus {
    IDLE,
    LOADING,
    AVAILABLE,
    NOT_FOUND,
    OFFLINE,
    TEMPORARY_ERROR,
    INSUFFICIENT_METADATA
}

data class LyricLine(
    val startMs: Long,
    val text: String
)

fun parseSyncedLyrics(value: String): List<LyricLine> {
    val pattern = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]\\s*(.*)")
    return value.lineSequence().flatMap { line ->
        val match = pattern.matchEntire(line.trim()) ?: return@flatMap emptySequence()
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        sequenceOf(LyricLine(minutes * 60_000L + seconds * 1_000L + fraction, match.groupValues[4]))
    }.filter { it.text.isNotBlank() }.sortedBy { it.startMs }.toList()
}
