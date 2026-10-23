package com.chrismdz.vinylplayer.data

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

class MusicRepository {
    fun readLibraryMetadata(context: Context): List<MusicFile> {
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
        }.toTypedArray()
        val result = mutableListOf<MusicFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn)?.takeIf(String::isNotBlank) else null
                val relativePath = if (relativePathColumn >= 0) cursor.getString(relativePathColumn) else null
                if (displayName?.endsWith(".opus", ignoreCase = true) == true ||
                    mimeType.equals("audio/opus", ignoreCase = true)
                ) {
                    continue
                }

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idColumn)
                )
                result += MusicFile(
                    uri = uri,
                    title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() }
                        ?: displayName?.substringBeforeLast('.')
                        ?: "Canción",
                    artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() }
                        ?: "Artista desconocido",
                    album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() }
                        ?: "Álbum desconocido",
                    artwork = null,
                    artworkData = null,
                    durationMs = cursor.getLong(durationColumn),
                    filePath = dataPath ?: relativePath?.let { relative ->
                        displayName?.let { name -> File(Environment.getExternalStorageDirectory(), "$relative$name").absolutePath }
                    } ?: displayName
                )
            }
        }
        return result
    }

    fun readArtwork(context: Context, uri: android.net.Uri): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val data = retriever.embeddedPicture
            data?.let { decodeArtwork(it, 240) }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun readFullArtwork(context: Context, uri: android.net.Uri): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { data ->
                BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun readMetadata(context: Context, uri: android.net.Uri): MusicFile {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val artworkData = retriever.embeddedPicture
            MusicFile(
                uri = uri,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: fileName(context, uri),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Artista desconocido",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?: "Álbum desconocido",
                artwork = artworkData?.let { decodeArtwork(it, 1000) },
                artworkData = artworkData,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                filePath = readFilePath(context, uri)
            )
        } finally {
            retriever.release()
        }
    }

    @Suppress("DEPRECATION")
    fun readFilePath(context: Context, uri: android.net.Uri): String? {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
        } else arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val queried = runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    .takeIf { it >= 0 }?.let(cursor::getString)?.takeIf(String::isNotBlank)
                if (data != null) return@use data
                val relative = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    .takeIf { it >= 0 }?.let(cursor::getString)
                val displayName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 }?.let(cursor::getString)?.takeIf(String::isNotBlank)
                if (relative != null && displayName != null) {
                    File(Environment.getExternalStorageDirectory(), "$relative$displayName").absolutePath
                } else displayName
            }
        }.getOrNull()
        if (!queried.isNullOrBlank()) return queried
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf(String::isNotBlank) else null
        } ?: uri.toString()
    }

    private fun fileName(context: Context, uri: android.net.Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "Canción seleccionada"
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') ?: fallback else fallback
        } ?: fallback
    }

    private fun decodeArtwork(data: ByteArray, maxSize: Int): android.graphics.Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxSize || bounds.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(
            data,
            0,
            data.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
    }
}
