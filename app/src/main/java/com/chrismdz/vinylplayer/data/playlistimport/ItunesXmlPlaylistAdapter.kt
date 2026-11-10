package com.chrismdz.vinylplayer.data.playlistimport

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * XML pull parser configured for untrusted input: DOCTYPE declarations are not processed, so external
 * entities and DTDs are never fetched, and documents that declare entities are rejected outright.
 */
internal object SafeXml {
    fun parser(text: String, namespaceAware: Boolean): XmlPullParser {
        require(!text.contains("<!ENTITY", ignoreCase = true)) { "El XML declara entidades y no se procesará por seguridad." }
        return Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, namespaceAware)
            runCatching { setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
            setInput(StringReader(text))
        }
    }
}

/**
 * iTunes / Music.app XML (property list with a "Tracks" dictionary and a "Playlists" array). Works for a
 * single exported playlist and for a full library export; system playlists (Library, Music, Podcasts…)
 * and folders are skipped and every remaining playlist is offered for selection.
 */
internal class ItunesXmlPlaylistAdapter : PlaylistImportAdapter {
    override val id: String = "itunes-xml"

    override fun inspect(document: PlaylistImportDocument): AdapterVerdict? {
        val head = document.head
        if (!head.contains("<plist", ignoreCase = true)) return null
        val hasTracks = document.text.contains("<key>Tracks</key>")
        val hasPlaylists = document.text.contains("<key>Playlists</key>")
        return when {
            hasTracks && hasPlaylists -> AdapterVerdict.Supported(95, PlaylistImportFormat.ITUNES_XML, PlaylistImportSource.APPLE_MUSIC)
            hasTracks -> AdapterVerdict.Supported(80, PlaylistImportFormat.ITUNES_XML, PlaylistImportSource.APPLE_MUSIC)
            else -> AdapterVerdict.Rejected("Es una lista de propiedades XML, pero no contiene las claves Tracks/Playlists de iTunes.")
        }
    }

    override fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile {
        val parser = SafeXml.parser(document.text, namespaceAware = false)
        var root: Any? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "dict") {
                root = readValue(parser)
                break
            }
        }
        val plist = root as? Map<*, *> ?: error("La lista de propiedades no tiene un diccionario raíz.")
        val tracks = (plist["Tracks"] as? Map<*, *>).orEmpty()
        val warnings = mutableListOf<String>()

        fun entryFor(trackId: Any?, position: Int): PlaylistImportEntry? {
            val track = tracks[trackId.toString()] as? Map<*, *> ?: return null
            val location = (track["Location"] as? String)?.let(::fileUrlToPath)
            val title = ImportText.clean(track["Name"] as? String)
            return PlaylistImportEntry(
                position = position,
                source = PlaylistImportSource.APPLE_MUSIC,
                title = title ?: location?.let(ImportText::titleFromLocation),
                artists = ImportText.splitArtists(track["Artist"] as? String),
                album = ImportText.clean(track["Album"] as? String),
                durationMs = (track["Total Time"] as? Long)?.takeIf { it > 0 },
                externalId = (track["Persistent ID"] as? String)?.let { "itunes:persistent:$it" },
                originalLocation = location,
                titleFromFileName = title == null,
                extras = listOfNotNull(
                    (track["Album Artist"] as? String)?.let { "albumArtist" to it },
                    (track["Track Number"] as? Long)?.let { "trackNumber" to it.toString() },
                    (track["Kind"] as? String)?.let { "kind" to it }
                ).toMap()
            )
        }

        val playlists = (plist["Playlists"] as? List<*>).orEmpty().mapNotNull { item ->
            val playlist = item as? Map<*, *> ?: return@mapNotNull null
            if (playlist["Master"] == true || playlist["Folder"] == true || playlist["Visible"] == false ||
                playlist.containsKey("Distinguished Kind")
            ) return@mapNotNull null
            val items = (playlist["Playlist Items"] as? List<*>).orEmpty()
            var position = 0
            val entries = items.mapNotNull { (it as? Map<*, *>)?.get("Track ID")?.let { id -> entryFor(id, position)?.also { position++ } } }
            if (entries.size < items.size) warnings += "${items.size - entries.size} referencias de “${playlist["Name"]}” no tenían datos de pista."
            entries.takeIf { it.isNotEmpty() }?.let { ParsedPlaylist(ImportText.clean(playlist["Name"] as? String), it) }
        }.ifEmpty {
            val libraryEntries = tracks.keys.mapIndexedNotNull { index, key -> entryFor(key, index) }
            if (libraryEntries.isEmpty()) emptyList() else {
                warnings += "El archivo no contiene playlists de usuario; se ofrece la biblioteca completa en el orden del archivo."
                listOf(ParsedPlaylist(document.baseName ?: "Biblioteca de iTunes", libraryEntries))
            }
        }
        return ParsedPlaylistFile(id, verdict.format, verdict.source, playlists, warnings)
    }

    /** Reads one plist value; the parser must be positioned on its START_TAG. */
    private fun readValue(parser: XmlPullParser): Any? = when (parser.name) {
        "dict" -> {
            val map = LinkedHashMap<String, Any?>()
            var key: String? = null
            while (parser.next() != XmlPullParser.END_TAG || parser.name != "dict") {
                if (parser.eventType == XmlPullParser.END_DOCUMENT) break
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name == "key") key = parser.nextText()
                else {
                    val value = readValue(parser)
                    key?.let { map[it] = value }
                    key = null
                }
            }
            map
        }
        "array" -> {
            val list = mutableListOf<Any?>()
            while (parser.next() != XmlPullParser.END_TAG || parser.name != "array") {
                if (parser.eventType == XmlPullParser.END_DOCUMENT) break
                if (parser.eventType == XmlPullParser.START_TAG) list += readValue(parser)
            }
            list
        }
        "integer" -> parser.nextText().trim().toLongOrNull()
        "real" -> parser.nextText().trim().toDoubleOrNull()
        "true", "false" -> {
            val value = parser.name == "true"
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType == XmlPullParser.END_DOCUMENT) break
            }
            value
        }
        "string", "date", "data" -> parser.nextText()
        else -> {
            skip(parser)
            null
        }
    }

    private fun skip(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    /** "file://localhost/Users/a/Music/x%20y.mp3" → "/Users/a/Music/x y.mp3". Kept only for comparison. */
    private fun fileUrlToPath(location: String): String {
        if (!location.startsWith("file://", ignoreCase = true)) return location
        val withoutScheme = location.substring(7).removePrefix("localhost")
        return ImportText.decodePercent(withoutScheme)
    }
}
