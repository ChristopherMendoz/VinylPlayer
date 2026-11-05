package com.chrismdz.vinylplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ArtistCreditsTest {
    @Test
    fun splitsOnlyOnCommasAndKeepsCreditOrderAndRoles() {
        val credits = parseArtistCredits("Kanye West, Jay-Z, Rihanna; A/B & C")

        assertEquals(listOf("Kanye West", "Jay-Z", "Rihanna; A/B & C"), credits.map { it.name })
        assertEquals(listOf(0, 1, 2), credits.map { it.position })
        assertEquals(
            listOf(ArtistParticipation.PRIMARY, ArtistParticipation.FEATURED, ArtistParticipation.FEATURED),
            credits.map { it.participation }
        )
    }

    @Test
    fun removesCaseWhitespaceAndUnicodeDuplicatesWithoutReordering() {
        val credits = parseArtistCredits("Beyoncé,  beyonce\u0301, JAY-Z,\u00a0jay-z")

        assertEquals(listOf("Beyoncé", "JAY-Z"), credits.map { it.name })
        assertEquals(listOf(0, 2), credits.map { it.position })
    }

    @Test
    fun ignoresEmptyAndUnknownCredits() {
        assertEquals(emptyList<ArtistCredit>(), parseArtistCredits(" , , Artista desconocido "))
        assertEquals(emptyList<ArtistCredit>(), parseArtistCredits(null))
    }

    @Test
    fun sortsNamesAlphabeticallyWithUnicodeAccents() {
        val catalog = listOf(
            ArtistCatalogItem("z", "Zoé", 1, null, null),
            ArtistCatalogItem("a", "Álvaro", 1, null, null),
            ArtistCatalogItem("b", "Björk", 1, null, null)
        )

        assertEquals(listOf("Álvaro", "Björk", "Zoé"), sortArtistCatalog(catalog, Locale("es")).map { it.name })
    }

    @Test
    fun buildsUniqueArtistSongRelationsWithPrimaryAndFeaturedRoles() {
        val snapshot = buildArtistLibrarySnapshot(
            listOf(
                ArtistSongMetadata("song-a", "Kanye West"),
                ArtistSongMetadata("song-b", "Kanye West, Jay-Z"),
                ArtistSongMetadata("song-c", "Jay-Z, Kanye West"),
                ArtistSongMetadata("song-b", "Kanye West, Kanye West, Jay-Z")
            )
        )

        assertEquals(listOf("Kanye West", "Jay-Z"), snapshot.artists.map { it.name })
        assertEquals(5, snapshot.links.size)
        assertEquals(
            listOf("PRIMARY", "PRIMARY", "FEATURED"),
            snapshot.links.filter { it.artistId == "kanye west" }.map { it.role }
        )
        assertEquals(2, snapshot.links.count { it.artistId == "jay-z" })
    }

    @Test
    fun similarArtistNamesRemainSeparateAndSongsWithoutArtistsAddNoRelations() {
        val snapshot = buildArtistLibrarySnapshot(
            listOf(
                ArtistSongMetadata("song-a", "The Weeknd"),
                ArtistSongMetadata("song-b", "Weeknd"),
                ArtistSongMetadata("song-c", "Artista desconocido")
            )
        )

        assertEquals(listOf("The Weeknd", "Weeknd"), snapshot.artists.map { it.name })
        assertEquals(2, snapshot.links.size)
    }
}
