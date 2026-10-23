package com.chrismdz.vinylplayer.data

import java.text.Normalizer
import java.util.Locale

private const val UNKNOWN_ARTIST_LABEL = "artista desconocido"

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
    val seen = linkedSetOf<String>()
    return metadata.orEmpty()
        .split(',')
        .mapNotNull { rawName ->
            val displayName = cleanArtistName(rawName)
            if (displayName.isEmpty()) return@mapNotNull null

            val key = artistKey(displayName)
            if (key == UNKNOWN_ARTIST_LABEL) return@mapNotNull null
            if (seen.add(key)) displayName else null
        }
}

private fun cleanArtistName(name: String): String = buildString(name.length) {
    var previousWasSpace = true
    name.forEach { character ->
        val isSpace = Character.isWhitespace(character) || Character.isSpaceChar(character)
        if (isSpace) {
            if (!previousWasSpace) append(' ')
        } else {
            append(character)
        }
        previousWasSpace = isSpace
    }
    if (isNotEmpty() && last() == ' ') deleteAt(lastIndex)
}

private fun artistKey(name: String): String = Normalizer.normalize(
    Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT),
    Normalizer.Form.NFC
)
