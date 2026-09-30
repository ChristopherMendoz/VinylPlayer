package com.chrismdz.vinylplayer.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class LrcLibLyricsProvider : LyricsProvider {
    override suspend fun searchLyrics(artist: String, title: String): LyricsResult = withContext(Dispatchers.IO) {
        val endpoint = Uri.parse("https://lrclib.net/api/get").buildUpon()
            .appendQueryParameter("artist_name", artist)
            .appendQueryParameter("track_name", title)
            .build().toString()
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VinylPlayer/0.1 (https://github.com/chrismdz/VinylPlayer)")
            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> LyricsResult(null, null, "LRCLIB")
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    LyricsResult(
                        text = json.optString("plainLyrics").takeIf { it.isNotBlank() },
                        syncedText = json.optString("syncedLyrics").takeIf { it.isNotBlank() },
                        source = "LRCLIB"
                    )
                }
                else -> throw IOException("LRCLIB HTTP $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
}
