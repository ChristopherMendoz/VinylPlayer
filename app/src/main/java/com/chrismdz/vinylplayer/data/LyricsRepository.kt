package com.chrismdz.vinylplayer.data

import com.chrismdz.vinylplayer.data.MusicFile
import java.util.concurrent.TimeUnit

interface LyricsProvider {
    suspend fun searchLyrics(artist: String, title: String): LyricsResult
}

interface LyricsRepository {
    suspend fun getLyrics(songId: String): Lyrics?
    suspend fun fetchAndSaveLyrics(song: MusicFile): Lyrics
    suspend fun translateAndSaveLyrics(song: MusicFile): Lyrics
}

class RoomLyricsRepository(
    private val dao: LyricsDao,
    private val provider: LyricsProvider = LrcLibLyricsProvider(),
    private val translator: LyricsTranslationProvider = LyricsTranslationProvider()
) : LyricsRepository {
    override suspend fun getLyrics(songId: String): Lyrics? = dao.find(songId)?.toModel()

    override suspend fun fetchAndSaveLyrics(song: MusicFile): Lyrics {
        val songId = song.uri.toString()
        val now = System.currentTimeMillis()
        val cached = getLyrics(songId)
        val cacheAge = cached?.let { now - it.fetchedAt } ?: Long.MAX_VALUE
        if (cached != null && cacheAge in 0..CACHE_DURATION_MS) return cached

        if (!hasUsableMetadata(song)) {
            return save(
                Lyrics(
                    songId = songId,
                    text = null,
                    syncedText = null,
                    source = "metadata",
                    fetchedAt = now,
                    found = false
                )
            )
        }

        val result = provider.searchLyrics(
            artist = normalizeArtist(song.artist),
            title = normalizeTitle(song.title)
        )
        return save(
            Lyrics(
                songId = songId,
                text = result.text?.trim()?.takeIf { it.isNotEmpty() },
                syncedText = result.syncedText?.trim()?.takeIf { it.isNotEmpty() },
                source = result.source,
                fetchedAt = now,
                found = !result.text.isNullOrBlank() || !result.syncedText.isNullOrBlank()
            )
        )
    }

    override suspend fun translateAndSaveLyrics(song: MusicFile): Lyrics {
        val original = getLyrics(song.uri.toString())
            ?: fetchAndSaveLyrics(song)
        if (!original.found) return original
        if (!original.translatedText.isNullOrBlank() || !original.translatedSyncedText.isNullOrBlank()) return original
        return save(translator.translate(original))
    }

    private suspend fun save(lyrics: Lyrics): Lyrics {
        dao.save(
            LyricsEntity(
                songId = lyrics.songId,
                lyrics = lyrics.text,
                syncedLyrics = lyrics.syncedText,
                translatedLyrics = lyrics.translatedText,
                translatedSyncedLyrics = lyrics.translatedSyncedText,
                source = lyrics.source,
                fetchedAt = lyrics.fetchedAt
            )
        )
        return lyrics
    }

    private fun hasUsableMetadata(song: MusicFile): Boolean {
        val artist = song.artist.trim()
        val title = song.title.trim()
        val invalid = setOf("", "unknown", "artista desconocido", "track", "canción")
        return artist.length >= 2 && title.length >= 2 &&
            artist.lowercase() !in invalid && title.lowercase() !in invalid
    }

    private fun normalizeArtist(value: String): String = value
        .replace(Regex("\\s*\\[(feat\\.?|ft\\.?|featuring).*?]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\((feat\\.?|ft\\.?|featuring).*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun normalizeTitle(value: String): String = value
        .replace(Regex("\\s*\\((feat\\.?|ft\\.?|featuring).*?\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun LyricsEntity.toModel(): Lyrics = Lyrics(
        songId = songId,
        text = lyrics,
        syncedText = syncedLyrics,
        translatedText = translatedLyrics,
        translatedSyncedText = translatedSyncedLyrics,
        source = source,
        fetchedAt = fetchedAt,
        found = !lyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()
    )

    companion object {
        private val CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(7)
    }
}
