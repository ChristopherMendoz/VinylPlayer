package com.chrismdz.vinylplayer.data

import java.text.Normalizer
import java.util.Locale

enum class ArtistParticipation {
    PRIMARY,
    FEATURED
}

data class ArtistCredit(
    val artistId: String,
    val name: String,
    val position: Int,
    val participation: ArtistParticipation
)

/** Parses comma-separated metadata without changing the value stored in the music file. */
fun parseArtistCredits(metadata: String?): List<ArtistCredit> {
    val seen = linkedSetOf<String>()
    return metadata.orEmpty()
        .split(',')
        .mapIndexedNotNull { position, rawName ->
            val name = cleanArtistName(rawName)
            if (name.isEmpty()) return@mapIndexedNotNull null

            val artistId = artistIdentity(name)
            if (artistId == "artista desconocido" || !seen.add(artistId)) return@mapIndexedNotNull null

            ArtistCredit(
                artistId = artistId,
                name = name,
                position = position,
                participation = if (seen.size == 1) ArtistParticipation.PRIMARY else ArtistParticipation.FEATURED
            )
        }
}

/** Stable artist identity: compatibility-normalized Unicode, folded whitespace and case. */
fun artistIdentity(name: String): String = Normalizer.normalize(
    cleanArtistName(Normalizer.normalize(name, Normalizer.Form.NFKC)).lowercase(Locale.ROOT),
    Normalizer.Form.NFKC
)

fun filterArtistCatalog(artists: List<ArtistCatalogItem>, query: String): List<ArtistCatalogItem> {
    val cleanQuery = artistIdentity(query)
    return if (cleanQuery.isBlank()) artists else artists.filter { artistIdentity(it.name).contains(cleanQuery) }
}

fun sortArtistCatalog(
    artists: List<ArtistCatalogItem>,
    locale: Locale = Locale.getDefault()
): List<ArtistCatalogItem> {
    val collator = java.text.Collator.getInstance(locale).apply {
        strength = java.text.Collator.PRIMARY
        decomposition = java.text.Collator.CANONICAL_DECOMPOSITION
    }
    return artists.sortedWith { first, second ->
        collator.compare(first.name, second.name).takeIf { it != 0 }
            ?: first.artistId.compareTo(second.artistId)
    }
}

private fun cleanArtistName(value: String): String = buildString(value.length) {
    var previousWasSpace = true
    value.forEach { character ->
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
