package com.chrismdz.vinylplayer.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArtistImageRepository(
    private val artistDao: ArtistDao,
    private val providers: List<ArtistImageProvider>,
    private val imageDirectory: File
) {
    suspend fun loadCachedImage(artistId: String): Bitmap? = withContext(Dispatchers.IO) {
        decodeImage(imageFile(artistId))
    }

    suspend fun processArtistImage(artistId: String, attemptCount: Int): ArtistImageWorkResult =
        withContext(Dispatchers.IO) {
            val artist = artistDao.findArtist(artistId) ?: return@withContext ArtistImageWorkResult.COMPLETE
            val target = imageFile(artistId)
            if (decodeImage(target) != null) {
                if (artist.imageStatus != ArtistImageStatus.DOWNLOADED) {
                    artistDao.updateImageResult(
                        artistId,
                        ArtistImageStatus.DOWNLOADED,
                        artist.imageCheckedAt ?: System.currentTimeMillis(),
                        null,
                        null
                    )
                }
                return@withContext ArtistImageWorkResult.COMPLETE
            }

            artistDao.updateImageStatus(artistId, ArtistImageStatus.DOWNLOADING, null, null)
            var temporaryFailure: String? = null
            var ambiguous = false
            var invalidImage = false

            for (provider in providers) {
                val lookup = try {
                    provider.findArtistImage(artist.name)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    temporaryFailure = temporaryFailure ?: classifyRequestError(error)
                    continue
                }
                when (lookup) {
                    is ArtistImageLookup.Found -> {
                        val candidate = lookup.candidate
                        if (!isAllowedCandidate(candidate)) {
                            invalidImage = true
                            continue
                        }
                        when (val download = downloadImage(candidate, target)) {
                            ImageDownloadResult.Invalid -> {
                                invalidImage = true
                                continue
                            }
                            is ImageDownloadResult.Failed -> {
                                temporaryFailure = temporaryFailure ?: download.code
                                continue
                            }
                            ImageDownloadResult.Downloaded -> Unit
                        }
                        artistDao.updateDownloadedImage(
                            artistId = artistId,
                            imageUrl = candidate.imageUrl,
                            checkedAt = System.currentTimeMillis(),
                            source = candidate.source,
                            license = candidate.license,
                            attribution = candidate.attribution,
                            pageUrl = candidate.pageUrl
                        )
                        return@withContext ArtistImageWorkResult.COMPLETE
                    }
                    ArtistImageLookup.Ambiguous -> ambiguous = true
                    ArtistImageLookup.InvalidImage -> invalidImage = true
                    ArtistImageLookup.NotFound -> Unit
                }
            }

            val now = System.currentTimeMillis()
            if (temporaryFailure != null || invalidImage) {
                val retryMinutes = minOf(MAX_RETRY_MINUTES, INITIAL_RETRY_MINUTES shl attemptCount.coerceAtMost(10))
                val error = temporaryFailure ?: "INVALID_IMAGE"
                artistDao.updateImageResult(
                    artistId = artistId,
                    status = ArtistImageStatus.ERROR,
                    checkedAt = now,
                    error = error,
                    retryAfter = now + TimeUnit.MINUTES.toMillis(retryMinutes.toLong())
                )
                ArtistImageWorkResult.RETRY
            } else {
                val reason = if (ambiguous) "AMBIGUOUS_ARTIST" else "NO_IMAGE_FOUND"
                artistDao.updateImageResult(
                    artistId = artistId,
                    status = ArtistImageStatus.NOT_FOUND,
                    checkedAt = now,
                    error = reason,
                    retryAfter = now + NOT_FOUND_RETRY_MS
                )
                ArtistImageWorkResult.COMPLETE
            }
        }

    private fun downloadImage(candidate: ArtistImageCandidate, target: File): ImageDownloadResult {
        val allowedDomains = candidate.imageDomains()
        var currentUrl = runCatching { URL(candidate.imageUrl) }.getOrNull()
            ?: return ImageDownloadResult.Invalid
        val temporaryFile = File(target.parentFile, "${target.name}.part")
        return try {
            repeat(MAX_REDIRECTS + 1) { redirectCount ->
                if (!isSecureImageUrl(currentUrl, allowedDomains)) return ImageDownloadResult.Invalid
                val connection = currentUrl.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = IMAGE_TIMEOUT_MS
                    connection.readTimeout = IMAGE_TIMEOUT_MS
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("User-Agent", "VinylPlayer/0.1 (com.chrismdz.vinylplayer)")
                    val status = connection.responseCode
                    if (status in 300..399) {
                        val location = connection.getHeaderField("Location") ?: return ImageDownloadResult.Invalid
                        if (redirectCount == MAX_REDIRECTS) return ImageDownloadResult.Invalid
                        currentUrl = runCatching { URL(currentUrl, location) }.getOrNull()
                            ?: return ImageDownloadResult.Invalid
                        return@repeat
                    }
                    if (status == 429 || status in 500..599) return ImageDownloadResult.Failed("SERVER_ERROR")
                    if (status !in 200..299 || !connection.contentType.orEmpty().startsWith("image/")) {
                        return ImageDownloadResult.Invalid
                    }
                    if (connection.contentLengthLong > MAX_IMAGE_BYTES) return ImageDownloadResult.Invalid
                    target.parentFile?.mkdirs()
                    temporaryFile.outputStream().use { output ->
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var totalBytes = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_IMAGE_BYTES) return ImageDownloadResult.Invalid
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    if (decodeImage(temporaryFile) == null) return ImageDownloadResult.Invalid
                    try {
                        Files.move(
                            temporaryFile.toPath(),
                            target.toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporaryFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                    return ImageDownloadResult.Downloaded
                } finally {
                    connection.disconnect()
                }
            }
            ImageDownloadResult.Invalid
        } catch (_: java.io.IOException) {
            ImageDownloadResult.Failed("CONNECTION_ERROR")
        } catch (_: Exception) {
            ImageDownloadResult.Failed("DOWNLOAD_ERROR")
        } finally {
            temporaryFile.delete()
        }
    }

    private fun decodeImage(file: File): Bitmap? {
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_BITMAP_SIDE || bounds.outHeight / sample > MAX_BITMAP_SIDE) sample *= 2
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }

    private fun imageFile(artistId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(artistId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(imageDirectory, "$digest.img")
    }

    private fun isAllowedCandidate(candidate: ArtistImageCandidate): Boolean = runCatching {
        val pageUrlIsValid = candidate.pageUrl?.let { pageUrl ->
            val url = URL(pageUrl)
            val allowedPageDomains = when (candidate.source) {
                "Deezer" -> setOf("deezer.com")
                "Wikimedia Commons" -> setOf("commons.wikimedia.org")
                else -> emptySet()
            }
            url.protocol == "https" && url.userInfo == null && allowedPageDomains.any { domain ->
                url.host == domain || url.host.endsWith(".$domain")
            }
        } ?: true
        isSecureImageUrl(URL(candidate.imageUrl), candidate.imageDomains()) && pageUrlIsValid
    }.getOrDefault(false)

    private fun ArtistImageCandidate.imageDomains(): Set<String> = when (source) {
        "Deezer" -> setOf("dzcdn.net")
        "Wikimedia Commons" -> setOf("upload.wikimedia.org", "commons.wikimedia.org")
        else -> emptySet()
    }

    private fun isSecureImageUrl(url: URL, allowedDomains: Set<String>): Boolean {
        val host = url.host.lowercase()
        return url.protocol == "https" && url.userInfo == null && allowedDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
    }

    private fun classifyRequestError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.startsWith("HTTP_5") || message == "HTTP_429" -> "SERVER_ERROR"
            message.startsWith("HTTP_") -> "HTTP_ERROR"
            message.startsWith("INVALID_") || message == "RESPONSE_TOO_LARGE" -> message
            else -> "CONNECTION_ERROR"
        }
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 12L * 1024L * 1024L
        const val MAX_BITMAP_SIDE = 512
        const val MAX_REDIRECTS = 3
        const val IMAGE_TIMEOUT_MS = 10_000
        const val BUFFER_SIZE = 8_192
        const val INITIAL_RETRY_MINUTES = 15
        const val MAX_RETRY_MINUTES = 24 * 60
        val NOT_FOUND_RETRY_MS = TimeUnit.DAYS.toMillis(30)
    }
}

enum class ArtistImageWorkResult {
    COMPLETE,
    RETRY
}

private sealed interface ImageDownloadResult {
    data object Downloaded : ImageDownloadResult
    data object Invalid : ImageDownloadResult
    data class Failed(val code: String) : ImageDownloadResult
}
