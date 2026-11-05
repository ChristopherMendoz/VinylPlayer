package com.chrismdz.vinylplayer.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ArtistImageRepository(
    private val artistDao: ArtistDao,
    private val provider: ArtistImageProvider?,
    private val imageDirectory: File
) {
    suspend fun loadImage(artist: ArtistCatalogItem): Bitmap? = withContext(Dispatchers.IO) {
        val cachedFile = imageFile(artist.artistId)
        decodeImage(cachedFile)?.let { return@withContext it }

        val stored = artistDao.findArtist(artist.artistId) ?: return@withContext null
        var imageUrl = stored.imageUrl
        val now = System.currentTimeMillis()
        val canRefresh = stored.imageCheckedAt == null || now - stored.imageCheckedAt >= IMAGE_LOOKUP_RETRY_MS
        if (imageUrl.isNullOrBlank() && canRefresh && provider != null) {
            try {
                imageUrl = provider.findArtistImage(stored.name)
                artistDao.updateImageLookup(stored.artistId, imageUrl, now)
            } catch (_: Exception) {
                return@withContext null
            }
        }
        if (imageUrl.isNullOrBlank()) return@withContext null
        if (downloadImage(imageUrl, cachedFile)) return@withContext decodeImage(cachedFile)
        null
    }

    private fun downloadImage(imageUrl: String, target: File): Boolean {
        val url = runCatching { URL(imageUrl) }.getOrNull() ?: return false
        if (url.protocol != "https" || !(url.host == "theaudiodb.com" || url.host.endsWith(".theaudiodb.com"))) return false
        val temporaryFile = File(target.parentFile, "${target.name}.part")
        val connection = runCatching { url.openConnection() as HttpURLConnection }.getOrNull() ?: return false
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "VinylPlayer/0.1")
            if (connection.responseCode !in 200..299 || !connection.contentType.orEmpty().startsWith("image/")) return false
            if (connection.contentLengthLong > MAX_IMAGE_BYTES) return false
            temporaryFile.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output ->
                    val buffer = ByteArray(8_192)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_IMAGE_BYTES) return false
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (decodeImage(temporaryFile) == null) return false
            if (target.exists()) target.delete()
            temporaryFile.renameTo(target)
        } catch (_: Exception) {
            false
        } finally {
            temporaryFile.delete()
            connection.disconnect()
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
            .digest(artistId.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(imageDirectory, "$digest.img")
    }

    private companion object {
        val IMAGE_LOOKUP_RETRY_MS = TimeUnit.DAYS.toMillis(7)
        const val MAX_IMAGE_BYTES = 12L * 1024L * 1024L
        const val MAX_BITMAP_SIDE = 512
    }
}
