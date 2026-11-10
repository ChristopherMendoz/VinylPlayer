package com.chrismdz.vinylplayer.data

/**
 * A user playlist. [songUris] keeps the playlist order and may contain the same URI several times.
 * [origin] is present only for playlists created by the importer.
 */
data class Playlist(
    val name: String,
    val songUris: List<String>,
    val origin: PlaylistOrigin? = null
)

/** Provenance of an imported playlist. */
data class PlaylistOrigin(
    /** [com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportSource] name. */
    val source: String,
    /** [com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportFormat] name. */
    val format: String,
    val fileName: String?,
    val importedAt: Long,
    val totalEntries: Int,
    /** External reference (Spotify URI, ISRC, persistent ID…) → local content URI chosen at import time. */
    val links: Map<String, String> = emptyMap()
)
