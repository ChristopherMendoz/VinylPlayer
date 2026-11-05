package com.chrismdz.vinylplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun interface ArtistImageProvider {
    suspend fun findArtistImage(name: String): String?
}

/** TheAudioDB provider; artist names are accepted only when the returned name is an exact identity match. */
class TheAudioDbArtistImageProvider(private val apiKey: String) : ArtistImageProvider {
    override suspend fun findArtistImage(name: String): String? = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "TheAudioDB API key is not configured" }
        val query = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
        val key = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name())
        val connection = URL("https://www.theaudiodb.com/api/v1/json/$key/search.php?s=$query")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VinylPlayer/0.1")
            if (connection.responseCode !in 200..299) throw IllegalStateException("Image service returned ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val artists = json.optJSONArray("artists") ?: return@withContext null
            val matches = (0 until artists.length()).mapNotNull(artists::optJSONObject)
                .filter { artistIdentity(it.optString("strArtist")) == artistIdentity(name) }
            if (matches.size != 1) return@withContext null
            val imageUrl = matches.single().optString("strArtistThumb").takeIf(String::isNotBlank)
                ?: return@withContext null
            val parsed = URL(imageUrl)
            imageUrl.takeIf { parsed.protocol == "https" && isTheAudioDbHost(parsed.host) }
        } finally {
            connection.disconnect()
        }
    }

    private fun isTheAudioDbHost(host: String): Boolean =
        host == "theaudiodb.com" || host.endsWith(".theaudiodb.com")
}
