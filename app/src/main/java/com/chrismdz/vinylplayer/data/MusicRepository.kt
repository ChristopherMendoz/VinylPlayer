package com.chrismdz.vinylplayer.data

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.provider.OpenableColumns

class MusicRepository {
    fun readLibraryMetadata(context: Context): List<MusicFile> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (" +
            "${MediaStore.Audio.Media.MIME_TYPE} = ? OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? )"
        val selectionArgs = arrayOf("audio/mpeg", "%.mp3")
        val result = mutableListOf<MusicFile>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idColumn)
                )
                result += MusicFile(
                    uri = uri,
                    title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() }
                        ?: cursor.getString(nameColumn)?.substringBeforeLast('.')
                        ?: "Canción",
                    artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() }
                        ?: "Artista desconocido",
                    album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() }
                        ?: "Álbum desconocido",
                    artwork = null,
                    artworkData = null,
                    durationMs = cursor.getLong(durationColumn)
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
                    ?.toLongOrNull() ?: 0L
            )
        } finally {
            retriever.release()
        }
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
