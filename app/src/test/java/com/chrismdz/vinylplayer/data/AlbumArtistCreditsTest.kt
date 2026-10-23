package com.chrismdz.vinylplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumArtistCreditsTest {
    @Test
    fun deduplicatesRepeatedArtistsAndFeaturesInTrackOrder() {
        val credits = consolidateAlbumArtists(
            listOf(
                "Kanye West, Jay-Z",
                "Kanye West, Rihanna",
                "Kanye West, Jay-Z, Kid Cudi",
                "Kanye West"
            )
        )

        assertEquals("Kanye West", credits.primaryArtist)
        assertEquals(listOf("Kanye West", "Jay-Z", "Rihanna", "Kid Cudi"), credits.participants)
        assertEquals(listOf("Jay-Z", "Rihanna", "Kid Cudi"), credits.collaborators)
    }

    @Test
    fun comparesCaseWhitespaceAndCanonicalUnicodeWhileKeepingFirstDisplayName() {
        val credits = consolidateAlbumArtists(
            listOf("Kanye West, Jay-Z, Café", "kanye west,  JAY-Z, Cafe\u0301")
        )

        assertEquals(listOf("Kanye West", "Jay-Z", "Café"), credits.participants)
        assertEquals(listOf("Jay-Z", "Café"), credits.collaborators)
    }

    @Test
    fun countsOnlyTrackLeadsAndKeepsOtherTrackLeadsAsParticipants() {
        val credits = consolidateAlbumArtists(
            listOf("Artist A, Artist B", "Artist C", "Artist A, Artist D")
        )

        assertEquals("Artist A", credits.primaryArtist)
        assertEquals(listOf("Artist A", "Artist B", "Artist C", "Artist D"), credits.participants)
        assertEquals(listOf("Artist B", "Artist D"), credits.collaborators)
    }

    @Test
    fun breaksPrimaryArtistTiesByFirstTrackAppearance() {
        val credits = consolidateAlbumArtists(listOf("Artist B, Artist X", "Artist A, Artist Y"))

        assertEquals("Artist B", credits.primaryArtist)
    }

    @Test
    fun ignoresMissingAndEmptyNamesWithoutLosingValidNames() {
        val credits = consolidateAlbumArtists(
            listOf(null, "", " , ", "Artista desconocido", "Artist A, , Artist B")
        )

        assertEquals("Artist A", credits.primaryArtist)
        assertEquals(listOf("Artist A", "Artist B"), credits.participants)
        assertEquals(listOf("Artist B"), credits.collaborators)

        val missingCredits = consolidateAlbumArtists(listOf(null, " , "))
        assertEquals(AlbumArtistCredits(null, emptyList(), emptyList()), missingCredits)
    }

    @Test
    fun omitsAlbumPrimaryFromCollaboratorsWhenFeaturedOnAnotherTrack() {
        val credits = consolidateAlbumArtists(listOf("Artist A", "Artist B, artist a, Artist C"))

        assertEquals("Artist A", credits.primaryArtist)
        assertEquals(listOf("Artist A", "Artist B", "Artist C"), credits.participants)
        assertEquals(listOf("Artist C"), credits.collaborators)
    }

    @Test
    fun keepsSimilarButDistinctArtistNamesSeparate() {
        val credits = consolidateAlbumArtists(listOf("A.B., AB, A-B"))

        assertEquals(listOf("A.B.", "AB", "A-B"), credits.participants)
    }
}
