package com.chrismdz.vinylplayer.ui.lyricsshare

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Turns a card into a real image (not a screenshot) and shares or saves it. */
internal object LyricsCardExporter {
    private const val SHARE_DIRECTORY = "lyrics_cards"
    private const val SHARE_FILE = "VinylPlayer-letra.png"

    /** Renders the card at full 1080×1920 resolution, off the main thread. */
    suspend fun render(content: LyricsCardContent, style: LyricsCardStyle, colors: LyricsCardColors): Bitmap =
        withContext(Dispatchers.Default) {
            // A software canvas can't draw hardware bitmaps; use a software copy of the cover if needed.
            val cover = content.cover?.let { bitmap ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else bitmap
            }
            val image = Bitmap.createBitmap(LyricsCardRenderer.WIDTH, LyricsCardRenderer.HEIGHT, Bitmap.Config.ARGB_8888)
            LyricsCardRenderer.draw(Canvas(image), content.copy(cover = cover), style, colors)
            image
        }

    /**
     * Opens Android's share sheet (Instagram Stories, WhatsApp, Snapchat…). The image goes to one
     * fixed cache file that is overwritten each time, so temporary copies never pile up.
     */
    suspend fun share(context: Context, image: Bitmap, songTitle: String) {
        val file = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
            File(directory, SHARE_FILE).also { target ->
                FileOutputStream(target).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.lyricsshare", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, songTitle)
            clipData = ClipData.newRawUri(songTitle, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir letra").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    /** Whether saving needs the legacy storage permission (Android 9 and older). */
    val needsLegacyWritePermission: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Saves the image to Pictures/VinylPlayer in the gallery. Returns true on success. */
    suspend fun saveToGallery(context: Context, image: Bitmap, songTitle: String): Boolean = withContext(Dispatchers.IO) {
        val safeTitle = songTitle.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(60).ifBlank { "letra" }
        val displayName = "VinylPlayer - $safeTitle - ${System.currentTimeMillis()}.png"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VinylPlayer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false
                try {
                    resolver.openOutputStream(uri)?.use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        ?: error("No output stream")
                    resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                    true
                } catch (error: Exception) {
                    resolver.delete(uri, null, null)
                    throw error
                }
            } else {
                @Suppress("DEPRECATION")
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VinylPlayer")
                    .apply { mkdirs() }
                val file = File(directory, displayName)
                FileOutputStream(file).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                true
            }
        }.getOrDefault(false)
    }
}
