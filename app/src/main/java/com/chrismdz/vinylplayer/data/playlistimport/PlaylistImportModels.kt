package com.chrismdz.vinylplayer.data.playlistimport

import java.util.Locale

/** Platform or origin a playlist file was exported from. Detection is evidence-based, never assumed. */
enum class PlaylistImportSource(val displayName: String) {
    SPOTIFY("Spotify"),
    APPLE_MUSIC("Apple Music / iTunes"),
    YOUTUBE_MUSIC("YouTube Music"),
    DEEZER("Deezer"),
    TIDAL("Tidal"),
    AMAZON_MUSIC("Amazon Music"),
    LOCAL_FILE("Playlist local"),
    UNKNOWN("Origen no identificado");

    companion object {
        fun fromName(name: String?): PlaylistImportSource =
            entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}

/** Concrete file structure that an adapter understood. */
enum class PlaylistImportFormat(val displayName: String) {
    CSV("CSV"),
    TSV("Texto tabulado"),
    M3U("M3U"),
    M3U8("M3U8"),
    PLS("PLS"),
    XSPF("XSPF"),
    ITUNES_XML("XML de iTunes / Música");

    companion object {
        fun fromName(name: String?): PlaylistImportFormat? = entries.firstOrNull { it.name == name }
    }
}

/**
 * One entry of the original playlist, normalized to a common shape. Every field is optional except the
 * position and source. [externalId] is a namespaced reference (e.g. `spotify:track:…`) and is never used
 * as a file path; only [originalLocation] can point to a local file, and it is only compared against the
 * authorized MediaStore catalog, never opened.
 */
data class PlaylistImportEntry(
    val position: Int,
    val source: PlaylistImportSource,
    val title: String? = null,
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val durationMs: Long? = null,
    val externalId: String? = null,
    val isrc: String? = null,
    val originalLocation: String? = null,
    val titleFromFileName: Boolean = false,
    val extras: Map<String, String> = emptyMap()
) {
    val displayTitle: String
        get() = title?.takeIf(String::isNotBlank)
            ?: originalLocation?.let(::lastLocationSegment)
            ?: externalId
            ?: "Entrada ${position + 1}"

    val displayArtist: String? get() = artists.takeIf { it.isNotEmpty() }?.joinToString(", ")

    /** Stable key used to remember manual links across imports. */
    val referenceKey: String?
        get() = externalId?.takeIf(String::isNotBlank)
            ?: isrc?.takeIf(String::isNotBlank)?.let { "isrc:${it.uppercase(Locale.ROOT)}" }
}

data class ParsedPlaylist(
    val name: String?,
    val entries: List<PlaylistImportEntry>,
    val warnings: List<String> = emptyList()
)

/** What an adapter extracted from a file; a file can hold several playlists (e.g. an iTunes library). */
data class ParsedPlaylistFile(
    val adapterId: String,
    val format: PlaylistImportFormat,
    val source: PlaylistImportSource,
    val playlists: List<ParsedPlaylist>,
    val warnings: List<String> = emptyList()
)

/** Decoded file content handed to adapters. Adapters never get file system access. */
class PlaylistImportDocument(
    val fileName: String?,
    val mimeType: String?,
    val text: String
) {
    val extension: String = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
    val baseName: String? = fileName?.substringBeforeLast('.')?.trim()?.takeIf(String::isNotEmpty)
    val head: String by lazy { text.take(16_384) }
    val firstMeaningfulLine: String by lazy {
        text.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty()
    }

    /** Per-document cache so several adapters can share one expensive parse (e.g. the CSV table). */
    internal val memo: MutableMap<String, Any> = HashMap()
}

enum class MatchStatus(val label: String) {
    MATCH_EXACT("Coincidencia exacta"),
    MATCH_HIGH_CONFIDENCE("Alta confianza"),
    MATCH_AMBIGUOUS("Ambigua"),
    NOT_FOUND("No encontrada"),
    MANUALLY_LINKED("Vinculada manualmente")
}

data class MatchCandidate(
    val uri: String,
    val score: Int,
    val reason: String
)

data class TrackMatch(
    val status: MatchStatus,
    val candidates: List<MatchCandidate>,
    val selectedUri: String?,
    val reason: String
)

/** One reviewable row: the original entry, the automatic decision and the user's decision. */
data class ImportReviewItem(
    val entry: PlaylistImportEntry,
    val autoMatch: TrackMatch,
    val status: MatchStatus = autoMatch.status,
    val selectedUri: String? = autoMatch.selectedUri,
    val excluded: Boolean = false
) {
    val needsDecision: Boolean get() = !excluded && status == MatchStatus.MATCH_AMBIGUOUS
    val willImport: Boolean
        get() = !excluded && selectedUri != null && status != MatchStatus.MATCH_AMBIGUOUS && status != MatchStatus.NOT_FOUND
}

data class PlaylistImportReview(
    val fileName: String?,
    val adapterId: String,
    val format: PlaylistImportFormat,
    val source: PlaylistImportSource,
    val availablePlaylists: List<String>,
    val selectedPlaylistIndex: Int,
    val playlistName: String,
    val items: List<ImportReviewItem>,
    val warnings: List<String>
) {
    val total: Int get() = items.size
    val automaticCount: Int
        get() = items.count {
            !it.excluded && (it.status == MatchStatus.MATCH_EXACT || it.status == MatchStatus.MATCH_HIGH_CONFIDENCE)
        }
    val exactCount: Int get() = items.count { !it.excluded && it.status == MatchStatus.MATCH_EXACT }
    val highConfidenceCount: Int get() = items.count { !it.excluded && it.status == MatchStatus.MATCH_HIGH_CONFIDENCE }
    val ambiguousCount: Int get() = items.count { it.needsDecision }
    val notFoundCount: Int get() = items.count { !it.excluded && it.status == MatchStatus.NOT_FOUND }
    val manualCount: Int get() = items.count { !it.excluded && it.status == MatchStatus.MANUALLY_LINKED }
    val excludedCount: Int get() = items.count { it.excluded }
    val importCount: Int get() = items.count { it.willImport }
}

/** Final outcome reported to the user after the playlist was stored. */
data class PlaylistImportResult(
    val playlistName: String,
    val source: PlaylistImportSource,
    val format: PlaylistImportFormat,
    val totalEntries: Int,
    val importedCount: Int,
    val manuallyLinkedCount: Int,
    val skippedTitles: List<String>
)

private fun lastLocationSegment(location: String): String =
    location.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { location }
