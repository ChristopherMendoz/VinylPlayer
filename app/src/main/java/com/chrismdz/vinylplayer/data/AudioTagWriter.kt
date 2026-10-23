package com.chrismdz.vinylplayer.data

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.app.PendingIntent
import android.os.Build
import android.os.Build.VERSION_CODES.Q
import android.provider.OpenableColumns
import android.app.RecoverableSecurityException
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID

object AudioTagWriter {
    private val supportedExtensions = setOf("mp3", "flac", "ogg", "oga", "mp4", "m4a", "aiff", "aif", "wav", "wma", "dsf")

    fun supports(context: Context, uri: Uri): Boolean = supportsFileName(fileName(context, uri))

    fun supportsFileName(name: String?): Boolean {
        val extension = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return extension in supportedExtensions
    }

    fun write(context: Context, uri: Uri, title: String, artist: String, album: String) {
        if (Build.VERSION.SDK_INT < Q) {
            throw UnsupportedOperationException("La edición segura de etiquetas requiere Android 10 o posterior")
        }
        val name = fileName(context, uri) ?: throw IllegalArgumentException("No se pudo identificar el formato del archivo")
        if (!supportsFileName(name)) throw UnsupportedOperationException("Este formato no permite editar etiquetas")

        val extension = name.substringAfterLast('.').lowercase()
        val tempFile = File(context.cacheDir, "vinyl-tags-${UUID.randomUUID()}.$extension")
        val backupFile = File(context.cacheDir, "vinyl-tags-${UUID.randomUUID()}.backup")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Android no permitió leer el archivo")
            input.use { source ->
                backupFile.outputStream().use { destination -> source.copyTo(destination) }
            }
            backupFile.copyTo(tempFile, overwrite = true)

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, title.trim())
            tag.setField(FieldKey.ARTIST, artist.trim())
            tag.setField(FieldKey.ALBUM, album.trim())
            AudioFileIO.write(audioFile)

            try {
                val output = context.contentResolver.openOutputStream(uri, "rwt")
                    ?: throw IllegalStateException("Android no permitió guardar el archivo")
                output.use { destination -> tempFile.inputStream().use { it.copyTo(destination) } }
            } catch (permissionError: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Q) {
                    throw NeedsWriteApprovalException(permissionError.userAction.actionIntent)
                }
                throw permissionError
            } catch (writeError: Exception) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "rwt")?.use { destination ->
                        backupFile.inputStream().use { it.copyTo(destination) }
                    }
                }
                throw writeError
            }

            val absolutePath = MusicRepository().readFilePath(context, uri)
            absolutePath?.takeIf { it.startsWith('/') }?.let { path ->
                MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
            }
        } finally {
            tempFile.delete()
            backupFile.delete()
        }
    }

    private fun fileName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}

class NeedsWriteApprovalException(val pendingIntent: PendingIntent) : Exception("Android requiere permiso para modificar este archivo")
