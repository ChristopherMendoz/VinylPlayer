package com.chrismdz.vinylplayer.ui

import com.chrismdz.vinylplayer.data.Playlist
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySearchTest {
    private data class Track(val uri: String, val title: String, val artist: String, val album: String)

    private val tracks = listOf(
        Track("one", "Dawn", "Mira", "North"),
        Track("two", "Stay", "Sol", "Dawn"),
        Track("three", "Evening", "Sol", "Dawn"),
        Track("four", "Night", "Sol", "Dawn"),
        Track("five", "Sunrise", "Sol", "DAWN")
    )

    @Test
    fun searchesTitleArtistAndAlbumWithoutCaseSensitivity() {
        assertEquals(tracks, filter(tracks, "DAWN"))
        assertEquals(listOf(tracks[0]), filter(tracks, "mIrA"))
        assertEquals(listOf(tracks[0]), filter(tracks, "nOrTh", album = Track::album))
    }

    @Test
    fun filtersPlaylistsByNameAndSongsByMetadataWhenOpened() {
        val playlist = Playlist("Morning", listOf("one", "two"))
        assertEquals(listOf(playlist), filterPlaylists(listOf(playlist), "MORN"))
        assertEquals(listOf(tracks[1]), playlistSongs(
            library = tracks,
            playlist = playlist,
            query = "sol",
            uri = Track::uri,
            title = Track::title,
            artist = Track::artist,
            album = Track::album
        ))
    }

    @Test
    fun albumSearchShowsTheCompleteAlbumGroupForAccurateCountAndArtwork() {
        val albums = groupMatchingLibraryItems(
            library = tracks,
            query = "stay",
            key = Track::album,
            title = Track::title,
            artist = Track::artist,
            album = Track::album
        )

        assertEquals(listOf(tracks[1], tracks[2], tracks[3]), albums["Dawn"])
    }

    @Test
    fun albumCoverUsesSelectedTrackOrDefaultsToFirstTrack() {
        assertEquals(tracks[2], chooseAlbumCoverTrack(tracks, "three", Track::uri))
        assertEquals(tracks.first(), chooseAlbumCoverTrack(tracks, null, Track::uri))
        assertEquals(tracks.first(), chooseAlbumCoverTrack(tracks, "missing", Track::uri))
    }

    @Test
    fun albumNamesDifferingOnlyByCaseRemainSeparateAndVisible() {
        val albums = sortNamedGroups(
            groupMatchingLibraryItems(
                library = tracks,
                query = "",
                key = Track::album,
                title = Track::title,
                artist = Track::artist,
                album = Track::album
            )
        )

        assertEquals(listOf("DAWN", "Dawn", "North"), albums.keys.toList())
        assertEquals(listOf(tracks[1], tracks[2], tracks[3]), albums["Dawn"])
        assertEquals(listOf(tracks[4]), albums["DAWN"])
    }

    private fun filter(items: List<Track>, query: String, album: (Track) -> String = Track::album) =
        filterLibrary(items, query, Track::title, Track::artist, album)
}
