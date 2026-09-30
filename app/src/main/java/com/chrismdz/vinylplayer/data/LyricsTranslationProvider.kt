package com.chrismdz.vinylplayer.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class LyricsTranslationProvider {
    suspend fun translate(lyrics: Lyrics): Lyrics = withContext(Dispatchers.IO) {
        val translatedSynced = lyrics.syncedText?.let { translateSynced(it) }
        val translatedPlain = if (lyrics.syncedText.isNullOrBlank()) {
            lyrics.text?.let { translateText(it) }
        } else {
            translatedSynced?.lines()?.joinToString("\n") { it.substringAfter(']') }
        }
        lyrics.copy(
            translatedText = translatedPlain?.takeIf { it.isNotBlank() },
            translatedSyncedText = translatedSynced?.takeIf { it.isNotBlank() }
        )
    }

    private suspend fun translateSynced(value: String): String {
        val lines = value.lines()
        val result = lines.toMutableList()
        val entries = lines.mapIndexedNotNull { index, line ->
            val match = TIMESTAMP_LINE.matchEntire(line.trim()) ?: return@mapIndexedNotNull null
            SyncedEntry(index, match.groupValues[1], match.groupValues[2])
        }
        entries.chunked(8).forEach { batch ->
            val source = batch.joinToString("\n") { it.text }
            val translated = translateText(source).lines()
            batch.forEachIndexed { index, entry ->
                val text = translated.getOrNull(index)?.trim().orEmpty()
                if (text.isNotEmpty()) result[entry.index] = "${entry.timestamp} $text"
            }
        }
        return result.joinToString("\n")
    }

    private suspend fun translateText(value: String): String = withContext(Dispatchers.IO) {
        if (value.isBlank()) return@withContext value
        val endpoint = Uri.parse("https://translate.googleapis.com/translate_a/single").buildUpon()
            .appendQueryParameter("client", "gtx")
            .appendQueryParameter("sl", "en")
            .appendQueryParameter("tl", "es")
            .appendQueryParameter("dt", "t")
            .appendQueryParameter("q", value)
            .build().toString()
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VinylPlayer/0.1")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Translation HTTP ${connection.responseCode}")
            }
            val root = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            val segments = root.optJSONArray(0) ?: throw IOException("Invalid translation response")
            buildString {
                for (index in 0 until segments.length()) {
                    segments.optJSONArray(index)?.optString(0)?.let(::append)
                }
            }.ifBlank { throw IOException("Empty translation response") }
        } finally {
            connection.disconnect()
        }
    }

    private data class SyncedEntry(val index: Int, val timestamp: String, val text: String)

    private companion object {
        val TIMESTAMP_LINE = Regex("^(\\[(?:\\d{1,3}:\\d{2}(?:\\.\\d{1,3})?)])\\s*(.*)$")
    }
}
