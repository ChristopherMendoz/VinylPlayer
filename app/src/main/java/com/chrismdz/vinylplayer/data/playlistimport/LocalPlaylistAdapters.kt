package com.chrismdz.vinylplayer.data.playlistimport

import java.util.Locale

/**
 * M3U and M3U8 (extended or plain). Recognizes #EXTM3U, #EXTINF (duration and "Artist - Title"),
 * #PLAYLIST, #EXTART and #EXTALB. Every non-comment line is an entry location.
 */
internal class M3uPlaylistAdapter : PlaylistImportAdapter {
    override val id: String = "m3u"

    override fun inspect(document: PlaylistImportDocument): AdapterVerdict? {
        val first = document.firstMeaningfulLine
        val format = if (document.extension == "m3u8") PlaylistImportFormat.M3U8 else PlaylistImportFormat.M3U
        if (Regex("(?im)^#EXT-X-(TARGETDURATION|STREAM-INF|MEDIA-SEQUENCE)").containsMatchIn(document.head)) {
            return AdapterVerdict.Rejected("Es una lista de transmisión HLS (#EXT-X-…), no una playlist de canciones.")
        }
        if (first.startsWith("#EXTM3U", ignoreCase = true)) {
            return AdapterVerdict.Supported(92, format, PlaylistImportSource.LOCAL_FILE)
        }
        if (document.extension !in setOf("m3u", "m3u8") && document.mimeType?.contains("mpegurl", true) != true) return null
        val lines = document.head.lineSequence().map(String::trim).filter(String::isNotEmpty).take(50).toList()
        val looksLikeLocations = lines.isNotEmpty() && lines.all { line ->
            line.startsWith("#") || line.contains('/') || line.contains('\\') || line.contains('.')
        }
        return if (looksLikeLocations) AdapterVerdict.Supported(60, format, PlaylistImportSource.LOCAL_FILE)
        else AdapterVerdict.Rejected("El archivo tiene extensión M3U, pero sus líneas no son rutas ni directivas #EXT.")
    }

    override fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile {
        var playlistName: String? = null
        var pendingDuration: Long? = null
        var pendingDisplay: String? = null
        var pendingArtist: String? = null
        var pendingAlbum: String? = null
        val entries = mutableListOf<PlaylistImportEntry>()

        document.text.lineSequence().map(String::trim).forEach { line ->
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val body = line.substring(8)
                    val comma = findInfoComma(body)
                    val durationPart = (if (comma >= 0) body.substring(0, comma) else body).trim().substringBefore(' ')
                    pendingDuration = durationPart.toDoubleOrNull()?.takeIf { it > 0 }?.let { (it * 1000).toLong() }
                    pendingDisplay = if (comma >= 0) ImportText.clean(body.substring(comma + 1)) else null
                }
                line.startsWith("#PLAYLIST:", ignoreCase = true) -> playlistName = ImportText.clean(line.substring(10))
                line.startsWith("#EXTART:", ignoreCase = true) -> pendingArtist = ImportText.clean(line.substring(8))
                line.startsWith("#EXTALB:", ignoreCase = true) -> pendingAlbum = ImportText.clean(line.substring(8))
                line.startsWith("#") -> Unit
                else -> {
                    val (displayArtist, displayTitle) = ImportText.splitArtistTitle(pendingDisplay)
                    val fallbackTitle = if (displayTitle == null) ImportText.titleFromLocation(line) else null
                    val artist = pendingArtist ?: displayArtist
                    entries += PlaylistImportEntry(
                        position = entries.size,
                        source = PlaylistImportSource.LOCAL_FILE,
                        title = displayTitle ?: fallbackTitle,
                        artists = ImportText.splitArtists(artist),
                        album = pendingAlbum,
                        durationMs = pendingDuration,
                        originalLocation = line,
                        titleFromFileName = displayTitle == null,
                        extras = if (line.startsWith("http", ignoreCase = true)) mapOf("url" to line) else emptyMap()
                    )
                    pendingDuration = null
                    pendingDisplay = null
                    pendingArtist = null
                    pendingAlbum = null
                }
            }
        }
        val playlist = ParsedPlaylist(playlistName ?: document.baseName, entries)
        return ParsedPlaylistFile(id, verdict.format, verdict.source, listOf(playlist).filter { it.entries.isNotEmpty() })
    }

    /** The title separator is the first comma outside quoted attribute values (`tvg-name="a, b"`). */
    private fun findInfoComma(body: String): Int {
        var quoted = false
        body.forEachIndexed { index, character ->
            if (character == '"') quoted = !quoted
            if (character == ',' && !quoted) return index
        }
        return -1
    }
}

/** PLS: `[playlist]` with FileN / TitleN / LengthN keys. Entries are ordered by N. */
internal class PlsPlaylistAdapter : PlaylistImportAdapter {
    override val id: String = "pls"

    override fun inspect(document: PlaylistImportDocument): AdapterVerdict? {
        if (document.firstMeaningfulLine.equals("[playlist]", ignoreCase = true)) {
            return AdapterVerdict.Supported(95, PlaylistImportFormat.PLS, PlaylistImportSource.LOCAL_FILE)
        }
        if (document.extension != "pls") return null
        return if (Regex("(?im)^\\s*File\\d+\\s*=").containsMatchIn(document.head)) {
            AdapterVerdict.Supported(70, PlaylistImportFormat.PLS, PlaylistImportSource.LOCAL_FILE)
        } else AdapterVerdict.Rejected("El archivo tiene extensión PLS, pero no contiene la sección [playlist] ni entradas FileN=.")
    }

    override fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile {
        val files = sortedMapOf<Int, String>()
        val titles = mutableMapOf<Int, String>()
        val lengths = mutableMapOf<Int, Long>()
        val keyPattern = Regex("^(file|title|length)(\\d+)$", RegexOption.IGNORE_CASE)
        document.text.lineSequence().map(String::trim).forEach { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@forEach
            val match = keyPattern.matchEntire(line.substring(0, separator).trim()) ?: return@forEach
            val number = match.groupValues[2].toIntOrNull() ?: return@forEach
            val value = line.substring(separator + 1).trim()
            when (match.groupValues[1].lowercase(Locale.ROOT)) {
                "file" -> if (value.isNotEmpty()) files[number] = value
                "title" -> ImportText.clean(value)?.let { titles[number] = it }
                "length" -> value.toLongOrNull()?.takeIf { it > 0 }?.let { lengths[number] = it * 1000 }
            }
        }
        val entries = files.entries.mapIndexed { position, (number, location) ->
            val (artist, title) = ImportText.splitArtistTitle(titles[number])
            PlaylistImportEntry(
                position = position,
                source = PlaylistImportSource.LOCAL_FILE,
                title = title ?: ImportText.titleFromLocation(location),
                artists = ImportText.splitArtists(artist),
                durationMs = lengths[number],
                originalLocation = location,
                titleFromFileName = title == null
            )
        }
        val playlist = ParsedPlaylist(document.baseName, entries)
        return ParsedPlaylistFile(id, verdict.format, verdict.source, listOf(playlist).filter { it.entries.isNotEmpty() })
    }
}

/** XSPF 1.0 (`http://xspf.org/ns/0/`): playlist title and track location/title/creator/album/duration/identifier. */
internal class XspfPlaylistAdapter : PlaylistImportAdapter {
    override val id: String = "xspf"

    override fun inspect(document: PlaylistImportDocument): AdapterVerdict? {
        val head = document.head
        val hasPlaylist = head.contains("<playlist", ignoreCase = true)
        if (hasPlaylist && head.contains("xspf.org/ns/0", ignoreCase = true)) {
            return AdapterVerdict.Supported(95, PlaylistImportFormat.XSPF, PlaylistImportSource.LOCAL_FILE)
        }
        if (document.extension != "xspf") return null
        return if (hasPlaylist && head.contains("<trackList", ignoreCase = true)) {
            AdapterVerdict.Supported(70, PlaylistImportFormat.XSPF, PlaylistImportSource.LOCAL_FILE)
        } else AdapterVerdict.Rejected("El archivo tiene extensión XSPF, pero no contiene <playlist> con <trackList>.")
    }

    override fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile {
        val parser = SafeXml.parser(document.text, namespaceAware = false)
        var playlistTitle: String? = null
        val entries = mutableListOf<PlaylistImportEntry>()
        var inTrack = false
        var trackFields = mutableMapOf<String, String>()
        var depthInTrack = 0
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    val name = parser.name.substringAfter(':').lowercase(Locale.ROOT)
                    when {
                        name == "track" && !inTrack -> {
                            inTrack = true
                            depthInTrack = parser.depth
                            trackFields = mutableMapOf()
                        }
                        inTrack && parser.depth == depthInTrack + 1 &&
                            name in setOf("location", "title", "creator", "album", "duration", "identifier", "tracknum", "annotation") -> {
                            val text = ImportText.clean(parser.nextText())
                            if (text != null && name !in trackFields) trackFields[name] = text
                        }
                        !inTrack && name == "title" && parser.depth == 2 -> playlistTitle = ImportText.clean(parser.nextText())
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val name = parser.name.substringAfter(':').lowercase(Locale.ROOT)
                    if (inTrack && name == "track" && parser.depth == depthInTrack) {
                        inTrack = false
                        val location = trackFields["location"]
                        val title = trackFields["title"]
                        entries += PlaylistImportEntry(
                            position = entries.size,
                            source = PlaylistImportSource.LOCAL_FILE,
                            title = title ?: location?.let(ImportText::titleFromLocation),
                            artists = ImportText.splitArtists(trackFields["creator"]),
                            album = trackFields["album"],
                            durationMs = trackFields["duration"]?.let { ImportText.parseDurationMs(it, ImportText.DurationUnit.MILLISECONDS) },
                            externalId = trackFields["identifier"],
                            originalLocation = location,
                            titleFromFileName = title == null,
                            extras = listOfNotNull(
                                trackFields["tracknum"]?.let { "trackNum" to it },
                                trackFields["annotation"]?.let { "annotation" to it.take(200) }
                            ).toMap()
                        )
                    }
                }
            }
            event = parser.next()
        }
        val playlist = ParsedPlaylist(playlistTitle ?: document.baseName, entries)
        return ParsedPlaylistFile(id, verdict.format, verdict.source, listOf(playlist).filter { it.entries.isNotEmpty() })
    }
}
