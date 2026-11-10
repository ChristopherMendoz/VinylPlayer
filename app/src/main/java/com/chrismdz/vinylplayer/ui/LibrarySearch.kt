package com.chrismdz.vinylplayer.ui

import com.chrismdz.vinylplayer.data.Playlist
import java.util.Locale

internal fun <T> filterLibrary(
    library: List<T>,
    query: String,
    title: (T) -> String,
    artist: (T) -> String,
    album: (T) -> String
): List<T> {
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) return library
    return library.filter { item ->
        title(item).contains(cleanQuery, ignoreCase = true) ||
            artist(item).contains(cleanQuery, ignoreCase = true) ||
            album(item).contains(cleanQuery, ignoreCase = true)
    }
}

internal fun <T, K> groupMatchingLibraryItems(
    library: List<T>,
    query: String,
    key: (T) -> K,
    title: (T) -> String,
    artist: (T) -> String,
    album: (T) -> String
): Map<K, List<T>> {
    val matchingKeys = filterLibrary(library, query, title, artist, album)
        .mapTo(mutableSetOf(), key)
    return library.filter { key(it) in matchingKeys }.groupBy(key)
}

internal fun <T> chooseAlbumCoverTrack(
    tracks: List<T>,
    selectedTrackUri: String?,
    trackUri: (T) -> String
): T? = selectedTrackUri?.let { uri -> tracks.firstOrNull { trackUri(it) == uri } } ?: tracks.firstOrNull()

internal fun <T> sortNamedGroups(groups: Map<String, List<T>>): Map<String, List<T>> =
    groups.entries
        .sortedWith(
            compareBy<Map.Entry<String, List<T>>> { it.key.lowercase(Locale.ROOT) }
                .thenBy { it.key }
        )
        .associateTo(linkedMapOf()) { it.key to it.value }

internal fun filterPlaylists(playlists: List<Playlist>, query: String): List<Playlist> {
    val cleanQuery = query.trim()
    return if (cleanQuery.isEmpty()) playlists
    else playlists.filter { it.name.contains(cleanQuery, ignoreCase = true) }
}

/**
 * Songs of a playlist in stored order, duplicates included. Each value keeps its position in
 * [Playlist.songUris] so actions on a repeated song target that exact occurrence. URIs that are no
 * longer in the library are skipped without changing the order of the rest.
 */
internal fun <T> playlistSongEntries(
    library: List<T>,
    playlist: Playlist?,
    query: String,
    uri: (T) -> String,
    title: (T) -> String,
    artist: (T) -> String,
    album: (T) -> String
): List<IndexedValue<T>> {
    if (playlist == null) return emptyList()
    val byUri = library.associateBy(uri)
    val members = playlist.songUris.mapIndexedNotNull { index, songUri -> byUri[songUri]?.let { IndexedValue(index, it) } }
    if (query.isBlank() || playlist.name.contains(query.trim(), ignoreCase = true)) return members
    val matching = filterLibrary(members.map { it.value }.distinctBy(uri), query, title, artist, album)
        .mapTo(hashSetOf(), uri)
    return members.filter { uri(it.value) in matching }
}

internal fun <T> playlistSongs(
    library: List<T>,
    playlist: Playlist?,
    query: String,
    uri: (T) -> String,
    title: (T) -> String,
    artist: (T) -> String,
    album: (T) -> String
): List<T> = playlistSongEntries(library, playlist, query, uri, title, artist, album).map { it.value }
