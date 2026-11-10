package com.chrismdz.vinylplayer.data.playlistimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.chrismdz.vinylplayer.data.MusicFile
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

class PlaylistImportException(message: String) : Exception(message)

/**
 * Entry point of the importer: reads the user-picked document (Storage Access Framework URI, read once,
 * no persisted permission needed), detects the structure, delegates parsing to the adapter and runs the
 * shared matcher. New formats only need a new [PlaylistImportAdapter] in [adapters].
 */
class PlaylistImportManager(
    private val adapters: List<PlaylistImportAdapter> = defaultAdapters()
) {
    private val detector = PlaylistImportFormatDetector(adapters)

    fun readDocument(context: Context, uri: Uri): PlaylistImportDocument {
        val resolver = context.applicationContext.contentResolver
        val (name, size) = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) null to null
                else {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    (if (nameIndex >= 0) cursor.getString(nameIndex) else null) to
                        (if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null)
                }
            } ?: (null to null)
        if (size != null && size > MAX_FILE_BYTES) {
            throw PlaylistImportException("El archivo supera el tamaño máximo admitido (${MAX_FILE_BYTES / 1_048_576} MB).")
        }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_FILE_BYTES) {
                    throw PlaylistImportException("El archivo supera el tamaño máximo admitido (${MAX_FILE_BYTES / 1_048_576} MB).")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw PlaylistImportException("No se pudo abrir el archivo seleccionado.")
        return PlaylistImportDocument(name, resolver.getType(uri), decode(bytes))
    }

    fun detectAndParse(document: PlaylistImportDocument): ParsedPlaylistFile {
        return when (val outcome = detector.detect(document)) {
            is DetectionOutcome.Unsupported -> throw PlaylistImportException(outcome.reason)
            is DetectionOutcome.Detected -> {
                val parsed = runCatching { outcome.adapter.parse(document, outcome.verdict) }.getOrElse { error ->
                    throw PlaylistImportException(
                        "Se detectó ${outcome.verdict.format.displayName}, pero el contenido no se pudo interpretar: " +
                            (error.message ?: error.javaClass.simpleName)
                    )
                }
                if (parsed.playlists.isEmpty() || parsed.playlists.all { it.entries.isEmpty() }) {
                    throw PlaylistImportException(
                        "Se detectó ${parsed.format.displayName}, pero no contiene canciones que se puedan leer."
                    )
                }
                parsed
            }
        }
    }

    fun buildReview(
        file: ParsedPlaylistFile,
        playlistIndex: Int,
        fileName: String?,
        library: List<MusicFile>,
        knownLinks: Map<String, String>
    ): PlaylistImportReview {
        val index = playlistIndex.coerceIn(0, file.playlists.lastIndex)
        val playlist = file.playlists[index]
        val matcher = LocalTrackMatcher(library, knownLinks)
        val items = playlist.entries.map { entry -> ImportReviewItem(entry, matcher.match(entry)) }
        val warnings = buildList {
            addAll(file.warnings)
            addAll(playlist.warnings)
            if (library.isEmpty()) add("La biblioteca local está vacía o sin permiso de lectura; no se pudo reconocer ninguna canción.")
        }
        return PlaylistImportReview(
            fileName = fileName,
            adapterId = file.adapterId,
            format = file.format,
            source = file.source,
            availablePlaylists = file.playlists.map { "${it.name ?: "Sin nombre"} (${it.entries.size})" },
            selectedPlaylistIndex = index,
            playlistName = playlist.name?.take(MAX_NAME_LENGTH) ?: "Playlist importada",
            items = items,
            warnings = warnings
        )
    }

    companion object {
        const val MAX_FILE_BYTES = 32L * 1024 * 1024
        const val MAX_NAME_LENGTH = 120

        /** MIME types offered by the picker; detection still inspects the content of whatever is chosen. */
        val PICKER_MIME_TYPES = arrayOf(
            "text/*",
            "audio/x-mpegurl",
            "audio/mpegurl",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
            "audio/x-scpls",
            "application/xspf+xml",
            "application/xml",
            "application/csv",
            "application/vnd.ms-excel",
            "application/octet-stream"
        )

        fun defaultAdapters(): List<PlaylistImportAdapter> = TabularPlaylistAdapter.all() + listOf(
            M3uPlaylistAdapter(),
            PlsPlaylistAdapter(),
            XspfPlaylistAdapter(),
            ItunesXmlPlaylistAdapter()
        )

        /** BOM-aware decoding (UTF-8/UTF-16), UTF-16 without BOM by zero-byte pattern, then UTF-8 or Windows-1252. */
        internal fun decode(bytes: ByteArray): String {
            fun has(vararg prefix: Int) = bytes.size >= prefix.size && prefix.indices.all { bytes[it] == prefix[it].toByte() }
            return when {
                has(0xEF, 0xBB, 0xBF) -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
                has(0xFF, 0xFE) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
                has(0xFE, 0xFF) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
                looksUtf16(bytes, littleEndian = true) -> String(bytes, Charsets.UTF_16LE)
                looksUtf16(bytes, littleEndian = false) -> String(bytes, Charsets.UTF_16BE)
                else -> try {
                    Charsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString()
                } catch (_: CharacterCodingException) {
                    String(bytes, runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1))
                }
            }.removePrefix("﻿")
        }

        private fun looksUtf16(bytes: ByteArray, littleEndian: Boolean): Boolean {
            val sample = minOf(bytes.size, 512) / 2 * 2
            if (sample < 8) return false
            var zeros = 0
            for (index in 0 until sample step 2) {
                val zeroByte = if (littleEndian) bytes[index + 1] else bytes[index]
                if (zeroByte == 0.toByte()) zeros++
            }
            return zeros * 10 >= (sample / 2) * 7
        }
    }
}
