package com.chrismdz.vinylplayer.data.playlistimport

import com.chrismdz.vinylplayer.data.playlistimport.ImportText.DurationUnit

/**
 * One adapter for every header-based CSV/TSV export. Columns are found by header name, never by position.
 * Profiles only describe what identifies a known exporter (and therefore its platform and units); the
 * parsing itself is shared.
 */
internal class TabularPlaylistAdapter(private val profile: TabularProfile) : PlaylistImportAdapter {
    override val id: String = "tabular:${profile.id}"

    override fun inspect(document: PlaylistImportDocument): AdapterVerdict? {
        if (!looksTabular(document)) return null
        val table = TabularTable.read(document) ?: return null
        val headers = table.headerKeys.toSet()
        if (!profile.matches(headers, table)) {
            if (profile.id != TabularProfile.GENERIC.id) return null
            if (table.columnFor(TabularField.TITLE) == null && table.columnFor(TabularField.EXTERNAL_ID) == null) {
                val shown = table.headers.filter(String::isNotBlank).take(12).joinToString(", ")
                return AdapterVerdict.Rejected(
                    "Se reconoció una tabla, pero no tiene una columna de título o identificador reconocible. " +
                        "Encabezados encontrados: $shown."
                )
            }
            return null
        }
        val format = if (table.delimiter == '\t') PlaylistImportFormat.TSV else PlaylistImportFormat.CSV
        val source = profile.source ?: inferSource(table)
        return AdapterVerdict.Supported(profile.score, format, source)
    }

    override fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile {
        val table = TabularTable.read(document) ?: error("Tabla no legible")
        val source = verdict.source
        val playlistColumn = table.columnFor(TabularField.PLAYLIST_NAME)
        val groups = linkedMapOf<String?, MutableList<List<String>>>()
        table.rows.forEach { row ->
            val key = playlistColumn?.let { ImportText.clean(row.getOrNull(it)) }
            groups.getOrPut(key) { mutableListOf() } += row
        }
        val warnings = mutableListOf<String>()
        val playlists = groups.map { (groupName, rows) ->
            var position = 0
            val entries = rows.mapNotNull { row -> toEntry(table, row, source)?.copy(position = position++) }
            ParsedPlaylist(name = groupName ?: table.preambleTitle ?: document.baseName, entries = entries)
        }.filter { it.entries.isNotEmpty() }

        val allEntries = playlists.flatMap { it.entries }
        if (allEntries.isNotEmpty() && allEntries.none { it.title != null }) {
            warnings += "El archivo solo contiene identificadores externos (por ejemplo, IDs de video), " +
                "sin título ni artista. No es posible reconocer canciones locales automáticamente; " +
                "puedes vincularlas manualmente."
        }
        if (profile.source == null && source != PlaylistImportSource.UNKNOWN) {
            warnings += "La plataforma (${source.displayName}) se dedujo de los enlaces o columnas del archivo."
        }
        return ParsedPlaylistFile(id, verdict.format, source, playlists, warnings)
    }

    private fun toEntry(table: TabularTable, row: List<String>, source: PlaylistImportSource): PlaylistImportEntry? {
        fun value(field: TabularField): String? = table.columnFor(field)?.let { ImportText.clean(row.getOrNull(it)) }

        val title = value(TabularField.TITLE)
        val externalId = table.idColumns.firstNotNullOfOrNull { (column, namespace) ->
            ImportText.clean(row.getOrNull(column))?.let { raw -> namespacedId(raw, namespace) }
        } ?: value(TabularField.URL)?.let(::idFromUrl)
        val location = value(TabularField.LOCATION)
        if (title == null && externalId == null && location == null) return null

        val durationMs = value(TabularField.DURATION_MS)?.let { ImportText.parseDurationMs(it, DurationUnit.MILLISECONDS) }
            ?: value(TabularField.DURATION_SECONDS)?.let { ImportText.parseDurationMs(it, DurationUnit.SECONDS) }
            ?: value(TabularField.DURATION)?.let { ImportText.parseDurationMs(it, profile.durationUnit) }

        val mappedColumns = table.mappedColumns
        val extras = buildMap {
            value(TabularField.URL)?.let { put("url", it) }
            table.headers.forEachIndexed { index, header ->
                if (size >= 10 || index in mappedColumns || header.isBlank()) return@forEachIndexed
                ImportText.clean(row.getOrNull(index))?.let { put(header, it.take(200)) }
            }
        }
        return PlaylistImportEntry(
            position = 0,
            source = source,
            title = title,
            artists = ImportText.splitArtists(value(TabularField.ARTIST)),
            album = value(TabularField.ALBUM),
            durationMs = durationMs,
            externalId = externalId,
            isrc = value(TabularField.ISRC),
            originalLocation = location,
            extras = extras
        )
    }

    private fun inferSource(table: TabularTable): PlaylistImportSource {
        val headerText = table.headerKeys.joinToString(" ")
        headerSources.firstOrNull { (marker, _) -> headerText.contains(marker) }?.let { return it.second }
        val hits = mutableMapOf<PlaylistImportSource, Int>()
        table.rows.take(300).forEach { row ->
            row.forEach { cell ->
                val lower = cell.lowercase()
                valueSources.firstOrNull { (marker, _) -> lower.contains(marker) }?.let { (_, source) ->
                    hits[source] = (hits[source] ?: 0) + 1
                }
            }
        }
        val total = hits.values.sum()
        val dominant = hits.maxByOrNull { it.value } ?: return PlaylistImportSource.UNKNOWN
        return if (dominant.value * 10 >= total * 8) dominant.key else PlaylistImportSource.UNKNOWN
    }

    companion object {
        fun all(): List<PlaylistImportAdapter> = TabularProfile.ALL.map(::TabularPlaylistAdapter)

        private val headerSources = listOf(
            "spotify" to PlaylistImportSource.SPOTIFY,
            "deezer" to PlaylistImportSource.DEEZER,
            "tidal" to PlaylistImportSource.TIDAL,
            "asin" to PlaylistImportSource.AMAZON_MUSIC,
            "amazon" to PlaylistImportSource.AMAZON_MUSIC,
            "apple music" to PlaylistImportSource.APPLE_MUSIC,
            "itunes" to PlaylistImportSource.APPLE_MUSIC,
            "youtube" to PlaylistImportSource.YOUTUBE_MUSIC
        )
        private val valueSources = listOf(
            "open.spotify.com" to PlaylistImportSource.SPOTIFY,
            "spotify:track:" to PlaylistImportSource.SPOTIFY,
            "deezer.com" to PlaylistImportSource.DEEZER,
            "tidal.com" to PlaylistImportSource.TIDAL,
            "music.amazon." to PlaylistImportSource.AMAZON_MUSIC,
            "music.apple.com" to PlaylistImportSource.APPLE_MUSIC,
            "itunes.apple.com" to PlaylistImportSource.APPLE_MUSIC,
            "music.youtube.com" to PlaylistImportSource.YOUTUBE_MUSIC,
            "youtube.com/watch" to PlaylistImportSource.YOUTUBE_MUSIC,
            "youtu.be/" to PlaylistImportSource.YOUTUBE_MUSIC
        )

        private fun looksTabular(document: PlaylistImportDocument): Boolean {
            val first = document.firstMeaningfulLine
            if (first.startsWith("<") || first.startsWith("#EXTM3U", ignoreCase = true) ||
                first.startsWith("[playlist]", ignoreCase = true)
            ) return false
            return first.any { it == ',' || it == ';' || it == '\t' || it == '|' }
        }

        private fun namespacedId(raw: String, namespace: String?): String {
            if (raw.contains(':') || raw.startsWith("http", ignoreCase = true)) {
                return idFromUrl(raw) ?: raw
            }
            return if (namespace == null) raw else "$namespace$raw"
        }

        private val urlIdPatterns = listOf(
            Regex("open\\.spotify\\.com/(?:intl-[a-z]+/)?track/([A-Za-z0-9]+)") to "spotify:track:",
            Regex("deezer\\.com/(?:[a-z]{2}/)?track/(\\d+)") to "deezer:track:",
            Regex("tidal\\.com/(?:browse/)?track/(\\d+)") to "tidal:track:",
            Regex("(?:youtube\\.com/watch\\?(?:.*&)?v=|youtu\\.be/)([A-Za-z0-9_-]{6,})") to "youtube:video:",
            Regex("music\\.apple\\.com/.*[?&]i=(\\d+)") to "applemusic:song:"
        )

        internal fun idFromUrl(value: String): String? {
            if (value.startsWith("spotify:", ignoreCase = true)) return value
            return urlIdPatterns.firstNotNullOfOrNull { (pattern, namespace) ->
                pattern.find(value)?.groupValues?.getOrNull(1)?.let { "$namespace$it" }
            }
        }
    }
}

internal enum class TabularField { TITLE, ARTIST, ALBUM, DURATION_MS, DURATION_SECONDS, DURATION, EXTERNAL_ID, ISRC, URL, LOCATION, PLAYLIST_NAME }

internal class TabularProfile(
    val id: String,
    val source: PlaylistImportSource?,
    val score: Int,
    val durationUnit: DurationUnit,
    val matches: (headers: Set<String>, table: TabularTable) -> Boolean
) {
    companion object {
        /** Exportify (Spotify). Old and new exports both carry these header names. */
        val EXPORTIFY = TabularProfile("exportify", PlaylistImportSource.SPOTIFY, 95, DurationUnit.MILLISECONDS) { headers, _ ->
            "track name" in headers &&
                ("artist name s" in headers || "artist names" in headers) &&
                ("track uri" in headers || "spotify id" in headers)
        }

        /** iTunes / Music.app "Export Playlist…" as Unicode text (tab separated, Time in seconds). */
        val ITUNES_TEXT = TabularProfile("itunes-text", PlaylistImportSource.APPLE_MUSIC, 90, DurationUnit.SECONDS) { headers, table ->
            table.delimiter == '\t' &&
                listOf("name", "artist", "album", "time", "kind").all { it in headers }
        }

        /** Google Takeout YouTube/YouTube Music playlists: a "Video ID" column, often without titles. */
        val YOUTUBE_TAKEOUT = TabularProfile("youtube-takeout", PlaylistImportSource.YOUTUBE_MUSIC, 80, DurationUnit.GUESS) { headers, _ ->
            "video id" in headers
        }

        /** Any table with a recognizable title column; platform inferred from links or column names. */
        val GENERIC = TabularProfile("generic", null, 50, DurationUnit.GUESS) { _, table ->
            table.columnFor(TabularField.TITLE) != null
        }

        val ALL = listOf(EXPORTIFY, ITUNES_TEXT, YOUTUBE_TAKEOUT, GENERIC)
    }
}

/** Header row located (skipping preambles like Takeout's playlist metadata block) plus data rows. */
internal class TabularTable private constructor(
    val delimiter: Char,
    val headers: List<String>,
    val headerKeys: List<String>,
    val rows: List<List<String>>,
    val preambleTitle: String?
) {
    private val fieldColumns: Map<TabularField, Int> = buildMap {
        headerKeys.forEachIndexed { index, key ->
            val field = FIELD_ALIASES[key] ?: return@forEachIndexed
            if (field !in this) put(field, index)
        }
    }

    /** ID columns in priority order with the namespace applied to bare values. */
    val idColumns: List<Pair<Int, String?>> = ID_COLUMNS.mapNotNull { (key, namespace) ->
        headerKeys.indexOf(key).takeIf { it >= 0 }?.let { it to namespace }
    }

    val mappedColumns: Set<Int> = fieldColumns.values.toSet() + idColumns.map { it.first }

    fun columnFor(field: TabularField): Int? =
        if (field == TabularField.EXTERNAL_ID) idColumns.firstOrNull()?.first else fieldColumns[field]

    companion object {
        fun read(document: PlaylistImportDocument): TabularTable? =
            document.memo.getOrPut("tabular") { readUncached(document) ?: Unreadable } as? TabularTable

        private object Unreadable

        private fun readUncached(document: PlaylistImportDocument): TabularTable? {
            val delimiter = CsvReader.detectDelimiter(document.text)
            val all = CsvReader.read(document.text, delimiter)
            if (all.isEmpty()) return null
            var preambleTitle: String? = null
            var headerIndex = -1
            for (index in 0 until minOf(all.size, 30)) {
                val keys = all[index].map(::headerKey)
                if ("playlist id" in keys || "channel id" in keys) {
                    val titleColumn = keys.indexOf("title").takeIf { it >= 0 } ?: keys.indexOf("playlist title")
                    if (titleColumn >= 0) preambleTitle = ImportText.clean(all.getOrNull(index + 1)?.getOrNull(titleColumn))
                    continue
                }
                if (keys.any { FIELD_ALIASES[it] == TabularField.TITLE } || keys.any { key -> ID_COLUMNS.any { it.first == key } }) {
                    headerIndex = index
                    break
                }
            }
            if (headerIndex < 0) headerIndex = 0
            val headers = all[headerIndex].map { it.trim().removePrefix("﻿") }
            return TabularTable(
                delimiter = delimiter,
                headers = headers,
                headerKeys = headers.map(::headerKey),
                rows = all.drop(headerIndex + 1),
                preambleTitle = preambleTitle
            )
        }

        private fun headerKey(header: String): String = ImportText.normalize(header)

        private val FIELD_ALIASES: Map<String, TabularField> = buildMap {
            listOf(
                "track name", "title", "song", "song title", "song name", "track", "track title", "name",
                "titulo", "cancion", "nombre", "nombre de la cancion", "titre", "titel"
            ).forEach { put(it, TabularField.TITLE) }
            listOf(
                "artist name s", "artist names", "artist name", "artist", "artists", "artista", "artistas",
                "creator", "performer", "interprete", "artiste", "kunstler", "artist name 1"
            ).forEach { put(it, TabularField.ARTIST) }
            listOf("album name", "album", "album title", "release", "release title", "album titulo")
                .forEach { put(it, TabularField.ALBUM) }
            listOf("track duration ms", "duration ms", "length ms", "duration in ms", "total time")
                .forEach { put(it, TabularField.DURATION_MS) }
            listOf("duration s", "duration seconds", "duration sec", "length s", "length seconds", "seconds")
                .forEach { put(it, TabularField.DURATION_SECONDS) }
            listOf("duration", "length", "time", "duracion", "tiempo", "duree", "dauer")
                .forEach { put(it, TabularField.DURATION) }
            listOf("isrc", "track isrc").forEach { put(it, TabularField.ISRC) }
            listOf("url", "link", "track url", "track link", "spotify url", "youtube url", "deezer url", "tidal url")
                .forEach { put(it, TabularField.URL) }
            listOf("location", "path", "file", "file path", "filepath", "filename", "file name", "ruta", "archivo")
                .forEach { put(it, TabularField.LOCATION) }
            listOf("playlist name", "playlist", "playlist title", "lista", "nombre de la playlist")
                .forEach { put(it, TabularField.PLAYLIST_NAME) }
        }

        private val ID_COLUMNS: List<Pair<String, String?>> = listOf(
            "track uri" to "spotify:track:",
            "spotify id" to "spotify:track:",
            "spotify uri" to "spotify:track:",
            "spotify track id" to "spotify:track:",
            "video id" to "youtube:video:",
            "youtube id" to "youtube:video:",
            "deezer id" to "deezer:track:",
            "deezer track id" to "deezer:track:",
            "tidal id" to "tidal:track:",
            "tidal track id" to "tidal:track:",
            "asin" to "amazon:asin:",
            "amazon id" to "amazon:asin:",
            "apple music id" to "applemusic:song:",
            "persistent id" to "itunes:persistent:",
            "track id" to null,
            "id" to null,
            "uri" to null
        )
    }
}
