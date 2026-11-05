package com.chrismdz.vinylplayer.data

import kotlinx.coroutines.flow.Flow

data class ArtistLibrarySnapshot(
    val artists: List<ArtistEntity>,
    val links: List<ArtistTrackCreditEntity>
)

data class ArtistSongMetadata(val songId: String, val artistMetadata: String)

class ArtistLibraryRepository(private val artistDao: ArtistDao) {
    fun observeCatalog(): Flow<List<ArtistCatalogItem>> = artistDao.observeCatalog()

    fun observeTrackLinks(artistId: String): Flow<List<ArtistTrackLink>> = artistDao.observeTrackLinks(artistId)

    suspend fun refresh(files: List<MusicFile>) {
        val snapshot = buildArtistLibrarySnapshot(files.map { ArtistSongMetadata(it.uri.toString(), it.artist) })
        artistDao.refreshLibrary(snapshot.artists, snapshot.links)
    }
}

fun buildArtistLibrarySnapshot(songs: List<ArtistSongMetadata>): ArtistLibrarySnapshot {
    val artists = linkedMapOf<String, ArtistEntity>()
    val links = linkedMapOf<Pair<String, String>, ArtistTrackCreditEntity>()
    songs.forEach { song ->
        parseArtistCredits(song.artistMetadata).forEach { credit ->
            artists.putIfAbsent(credit.artistId, ArtistEntity(credit.artistId, credit.name))
            val songId = song.songId
            links.putIfAbsent(
                credit.artistId to songId,
                ArtistTrackCreditEntity(
                    artistId = credit.artistId,
                    songId = songId,
                    position = credit.position,
                    role = credit.participation.name
                )
            )
        }
    }
    return ArtistLibrarySnapshot(artists.values.toList(), links.values.toList())
}
