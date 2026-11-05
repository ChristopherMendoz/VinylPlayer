package com.chrismdz.vinylplayer.data

data class AlbumArtistCredits(
    val primaryArtist: String?,
    val participants: List<String>,
    val collaborators: List<String>
)

/** Consolidates comma-separated per-track artist metadata without changing the source values. */
fun consolidateAlbumArtists(trackArtists: List<String?>): AlbumArtistCredits {
    val participants = linkedMapOf<String, String>()
    val collaboratorKeys = linkedSetOf<String>()
    val primaryCounts = linkedMapOf<String, Int>()

    trackArtists.forEach { metadata ->
        val artists = parseArtists(metadata)
        artists.forEach { artist ->
            participants.putIfAbsent(artistKey(artist), artist)
        }

        val trackPrimary = artists.firstOrNull() ?: return@forEach
        val trackPrimaryKey = artistKey(trackPrimary)
        primaryCounts[trackPrimaryKey] = (primaryCounts[trackPrimaryKey] ?: 0) + 1
        artists.drop(1).forEach { collaboratorKeys += artistKey(it) }
    }

    var albumPrimaryKey: String? = null
    var highestPrimaryCount = 0
    primaryCounts.forEach { (key, count) ->
        if (count > highestPrimaryCount) {
            albumPrimaryKey = key
            highestPrimaryCount = count
        }
    }

    return AlbumArtistCredits(
        primaryArtist = albumPrimaryKey?.let(participants::get),
        participants = participants.values.toList(),
        collaborators = collaboratorKeys
            .asSequence()
            .filterNot { it == albumPrimaryKey }
            .mapNotNull(participants::get)
            .toList()
    )
}

private fun parseArtists(metadata: String?): List<String> {
    return parseArtistCredits(metadata).map(ArtistCredit::name)
}

private fun artistKey(name: String): String = artistIdentity(name)
