package com.chrismdz.vinylplayer.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.concurrent.TimeUnit

data class ArtistImageCandidate(
    val imageUrl: String,
    val source: String,
    val license: String? = null,
    val attribution: String? = null,
    val pageUrl: String? = null
)

sealed interface ArtistImageLookup {
    data class Found(val candidate: ArtistImageCandidate) : ArtistImageLookup
    data object NotFound : ArtistImageLookup
    data object Ambiguous : ArtistImageLookup
    data object InvalidImage : ArtistImageLookup
}

interface ArtistImageProvider {
    suspend fun findArtistImage(name: String): ArtistImageLookup
}

object ArtistImageStatus {
    const val NOT_DOWNLOADED = "NOT_DOWNLOADED"
    const val PENDING = "PENDING"
    const val DOWNLOADING = "DOWNLOADING"
    const val DOWNLOADED = "DOWNLOADED"
    const val NOT_FOUND = "NOT_FOUND"
    const val ERROR = "ERROR"
}

/**
 * Deezer often lists several artists with the exact same name (homonyms and empty duplicate
 * profiles), so among exact-name matches with a real picture it picks the one with most fans.
 */
class DeezerArtistImageProvider : ArtistImageProvider {
    override suspend fun findArtistImage(name: String): ArtistImageLookup {
        val endpoint = Uri.parse("https://api.deezer.com/search/artist")
            .buildUpon()
            .appendQueryParameter("q", name)
            .build()
        DeezerRequestGate.waitForSlot()
        val json = getJson(endpoint.toString(), setOf("api.deezer.com"))
        // Deezer answers over-quota requests with HTTP 200 and an error object instead of data.
        json.optJSONObject("error")?.let { error ->
            throw ArtistImageRequestException(if (error.optInt("code") == DEEZER_QUOTA_ERROR) "HTTP_429" else "INVALID_RESPONSE")
        }
        val results = json.optJSONArray("data") ?: throw ArtistImageRequestException("INVALID_RESPONSE")
        if (results.length() == 0) return ArtistImageLookup.NotFound

        val wanted = normalizedArtistName(name)
        val matches = (0 until results.length())
            .mapNotNull(results::optJSONObject)
            .filter { normalizedArtistName(it.optString("name")) == wanted }
        if (matches.isEmpty()) return ArtistImageLookup.Ambiguous

        val withPicture = matches.mapNotNull { candidate ->
            listOf("picture_xl", "picture_big", "picture_medium", "picture")
                .firstNotNullOfOrNull { field ->
                    candidate.optString(field).takeIf { it.isNotBlank() && !it.isDeezerPlaceholder() }
                }
                ?.let { candidate to it }
        }
        val (artist, imageUrl) = withPicture.maxByOrNull { (candidate, _) -> candidate.optLong("nb_fan") }
            ?: return ArtistImageLookup.NotFound
        if (!isSecureUrl(imageUrl, setOf("dzcdn.net"))) return ArtistImageLookup.InvalidImage

        val pageUrl = artist.optString("link")
            .takeIf { isSecureUrl(it, setOf("deezer.com")) }
            ?: "https://www.deezer.com/"
        return ArtistImageLookup.Found(
            ArtistImageCandidate(
                imageUrl = imageUrl,
                source = "Deezer",
                attribution = "Deezer",
                pageUrl = pageUrl
            )
        )
    }
}

/** Verifies MusicBrainz ID against Wikidata P434 before resolving a Wikidata P18 file on Commons. */
class MusicBrainzWikidataArtistImageProvider(context: Context) : ArtistImageProvider {
    private val appContext = context.applicationContext

    override suspend fun findArtistImage(name: String): ArtistImageLookup {
        val escapedName = name.replace("\\", "\\\\").replace("\"", "\\\"")
        val query = "artist:\"$escapedName\""
        val endpoint = Uri.parse("https://musicbrainz.org/ws/2/artist/")
            .buildUpon()
            .appendQueryParameter("query", query)
            .appendQueryParameter("fmt", "json")
            .appendQueryParameter("limit", "25")
            .build()

        MusicBrainzRequestGate.waitForSlot(appContext)
        val musicBrainz = getJson(
            endpoint.toString(),
            setOf("musicbrainz.org"),
            userAgent = MUSICBRAINZ_USER_AGENT
        )
        val artists = musicBrainz.optJSONArray("artists")
            ?: throw ArtistImageRequestException("INVALID_RESPONSE")
        if (artists.length() == 0) return ArtistImageLookup.NotFound

        val wanted = normalizedArtistName(name)
        val exactMatches = (0 until artists.length())
            .mapNotNull(artists::optJSONObject)
            .filter { normalizedArtistName(it.optString("name")) == wanted }
        if (exactMatches.isEmpty()) return ArtistImageLookup.Ambiguous
        if (exactMatches.size != 1 || artists.length() >= MAX_MUSICBRAINZ_RESULTS) {
            return ArtistImageLookup.Ambiguous
        }

        val mbid = exactMatches.single().optString("id")
            .takeIf { MUSICBRAINZ_ID.matches(it) }
            ?: return ArtistImageLookup.InvalidImage
        val sparql = """
            SELECT ?item ?image WHERE {
              ?item wdt:P434 "$mbid";
                    wdt:P18 ?image.
            }
            LIMIT 10
        """.trimIndent()
        val wikidataEndpoint = Uri.parse("https://query.wikidata.org/sparql")
            .buildUpon()
            .appendQueryParameter("query", sparql)
            .appendQueryParameter("format", "json")
            .build()
        val wikidata = getJson(
            wikidataEndpoint.toString(),
            setOf("query.wikidata.org"),
            accept = "application/sparql-results+json"
        )
        val bindings = wikidata.optJSONObject("results")?.optJSONArray("bindings")
            ?: throw ArtistImageRequestException("INVALID_RESPONSE")
        if (bindings.length() == 0) return ArtistImageLookup.NotFound

        val rows = (0 until bindings.length()).mapNotNull(bindings::optJSONObject)
        val itemIds = rows.mapNotNull { it.optJSONObject("item")?.optString("value") }.distinct()
        if (itemIds.size != 1) return ArtistImageLookup.Ambiguous
        val imageFiles = rows.mapNotNull { it.optJSONObject("image")?.optString("value") }
            .distinct().take(MAX_COMMONS_IMAGES)
        if (imageFiles.isEmpty()) return ArtistImageLookup.NotFound

        for (imageFileUrl in imageFiles) {
            val fileName = commonsFileName(imageFileUrl) ?: continue
            val commons = fetchCommonsImage(fileName) ?: continue
            return commons
        }
        return ArtistImageLookup.NotFound
    }

    private suspend fun fetchCommonsImage(fileName: String): ArtistImageLookup.Found? {
        val endpoint = Uri.parse("https://commons.wikimedia.org/w/api.php")
            .buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("titles", "File:$fileName")
            .appendQueryParameter("prop", "imageinfo")
            .appendQueryParameter("iiprop", "url|extmetadata")
            .appendQueryParameter("iiurlwidth", "1000")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .build()
        val json = getJson(
            endpoint.toString(),
            setOf("commons.wikimedia.org"),
            userAgent = WIKIMEDIA_USER_AGENT
        )
        val pages = json.optJSONObject("query")?.optJSONArray("pages")
            ?: throw ArtistImageRequestException("INVALID_RESPONSE")
        val page = (0 until pages.length()).mapNotNull(pages::optJSONObject).firstOrNull()
            ?: return null
        val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        val imageUrl = listOf("thumburl", "url")
            .firstNotNullOfOrNull { field -> imageInfo.optString(field).takeIf(String::isNotBlank) }
            ?: return null
        if (!isSecureUrl(imageUrl, setOf("upload.wikimedia.org", "commons.wikimedia.org"))) return null

        val metadata = imageInfo.optJSONObject("extmetadata")
        val license = metadata?.plainMetadata("LicenseShortName")
            ?: metadata?.plainMetadata("UsageTerms")
        val attribution = metadata?.plainMetadata("Attribution")
            ?: metadata?.plainMetadata("Artist")
        val pageUrl = imageInfo.optString("descriptionurl")
            .takeIf { isSecureUrl(it, setOf("commons.wikimedia.org")) }
            ?: commonsFilePage(page.optString("title").removePrefix("File:"))

        return ArtistImageLookup.Found(
            ArtistImageCandidate(
                imageUrl = imageUrl,
                source = "Wikimedia Commons",
                license = license,
                attribution = attribution,
                pageUrl = pageUrl
            )
        )
    }

    private fun commonsFilePage(fileName: String): String = Uri.Builder()
        .scheme("https")
        .authority("commons.wikimedia.org")
        .appendPath("wiki")
        .appendPath("File:$fileName")
        .build()
        .toString()

    private fun commonsFileName(imageUrl: String): String? = runCatching {
        val url = URL(imageUrl)
        if (url.host != "commons.wikimedia.org" || url.path.substringBefore("?")
                .contains("/Special:FilePath/").not()
        ) return null
        val encodedName = url.path.substringAfter("/Special:FilePath/")
        URLDecoder.decode(encodedName.replace("+", "%2B"), StandardCharsets.UTF_8.name())
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private companion object {
        const val MAX_MUSICBRAINZ_RESULTS = 25
        const val MAX_COMMONS_IMAGES = 5
        const val MUSICBRAINZ_USER_AGENT = "VinylPlayer/0.1 (com.chrismdz.vinylplayer)"
        const val WIKIMEDIA_USER_AGENT = "VinylPlayer/0.1 (com.chrismdz.vinylplayer)"
        val MUSICBRAINZ_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}

private class ArtistImageRequestException(val code: String) : IOException(code)

private const val DEEZER_QUOTA_ERROR = 4

/** Profiles without a photo point at Deezer's generic artwork, whose path has an empty image hash. */
private fun String.isDeezerPlaceholder(): Boolean = contains("/images/artist//")

/** Deezer allows about 50 requests per 5 seconds; spacing them keeps a full library scan under it. */
private object DeezerRequestGate {
    private val mutex = Mutex()
    private var lastRequestAt = 0L
    private const val MIN_INTERVAL_MS = 150L

    suspend fun waitForSlot() = mutex.withLock {
        val waitMs = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestAt).coerceAtLeast(0L)
        if (waitMs > 0L) delay(waitMs)
        lastRequestAt = System.currentTimeMillis()
    }
}

private object MusicBrainzRequestGate {
    private val mutex = Mutex()
    private const val PREFERENCE_FILE = "artist_image_api_rate_limit"
    private const val LAST_REQUEST_KEY = "musicbrainz_last_request_at"

    suspend fun waitForSlot(context: Context) = mutex.withLock {
        val preferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastRequest = preferences.getLong(LAST_REQUEST_KEY, 0L)
        val waitMs = TimeUnit.SECONDS.toMillis(1) - (now - lastRequest).coerceAtLeast(0L)
        if (waitMs > 0L) delay(waitMs)
        preferences.edit().putLong(LAST_REQUEST_KEY, System.currentTimeMillis()).commit()
    }
}

private suspend fun getJson(
    endpoint: String,
    allowedHosts: Set<String>,
    userAgent: String = "VinylPlayer/0.1 (com.chrismdz.vinylplayer)",
    accept: String = "application/json"
): JSONObject = withContext(Dispatchers.IO) {
    var currentUrl = URL(endpoint)
    repeat(MAX_REDIRECTS + 1) { redirectCount ->
        if (!isAllowedHost(currentUrl.host, allowedHosts) || currentUrl.protocol != "https") {
            throw ArtistImageRequestException("INVALID_URL")
        }
        val connection = currentUrl.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = REQUEST_TIMEOUT_MS
            connection.readTimeout = REQUEST_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", userAgent)
            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw ArtistImageRequestException("INVALID_REDIRECT")
                if (redirectCount == MAX_REDIRECTS) throw ArtistImageRequestException("TOO_MANY_REDIRECTS")
                currentUrl = URL(currentUrl, location)
                return@repeat
            }
            if (status !in 200..299) throw ArtistImageRequestException("HTTP_$status")
            val body = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var totalBytes = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    if (totalBytes > MAX_JSON_BYTES) throw ArtistImageRequestException("RESPONSE_TOO_LARGE")
                    output.write(buffer, 0, count)
                }
                String(output.toByteArray(), StandardCharsets.UTF_8)
            }
            return@withContext try {
                JSONObject(body)
            } catch (_: Exception) {
                throw ArtistImageRequestException("INVALID_RESPONSE")
            }
        } finally {
            connection.disconnect()
        }
    }
    throw ArtistImageRequestException("TOO_MANY_REDIRECTS")
}

private fun isSecureUrl(value: String, allowedDomains: Set<String>): Boolean = runCatching {
    val url = URL(value)
    url.protocol == "https" && url.userInfo == null && isAllowedHost(url.host, allowedDomains)
}.getOrDefault(false)

private fun isAllowedHost(host: String, allowedDomains: Set<String>): Boolean {
    val normalizedHost = host.lowercase()
    return allowedDomains.any { domain ->
        normalizedHost == domain || normalizedHost.endsWith(".$domain")
    }
}

private fun normalizedArtistName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .filterNot { it in '\u0300'..'\u036f' }
    .lowercase()
    .trim()
    .replace(Regex("\\s+"), " ")

private fun JSONObject.plainMetadata(key: String): String? = optJSONObject(key)
    ?.optString("value")
    ?.replace(Regex("<[^>]*>"), " ")
    ?.replace("&amp;", "&")
    ?.replace("&nbsp;", " ")
    ?.replace(Regex("\\s+"), " ")
    ?.trim()
    ?.takeIf(String::isNotBlank)

private const val REQUEST_TIMEOUT_MS = 10_000
private const val MAX_REDIRECTS = 3
private const val MAX_JSON_BYTES = 2 * 1024 * 1024
