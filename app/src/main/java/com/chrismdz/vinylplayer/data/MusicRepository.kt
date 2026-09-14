package com.chrismdz.vinylplayer.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns

class MusicRepository {
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
                artwork = artworkData?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                },
                artworkData = artworkData,
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
            )
        } finally {
            retriever.release()
        }
    }

    private fun fileName(context: Context, uri: android.net.Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "Canción seleccionada"
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.substringBeforeLast('.') ?: fallback
            } else {
                fallback
            }
        } ?: fallback
    }
}
