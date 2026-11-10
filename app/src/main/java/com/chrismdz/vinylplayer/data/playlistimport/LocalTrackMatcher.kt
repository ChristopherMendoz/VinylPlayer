package com.chrismdz.vinylplayer.data.playlistimport

import com.chrismdz.vinylplayer.data.MusicFile
import kotlin.math.abs

/**
 * Format-agnostic recognition of imported entries against the existing MediaStore catalog. It never
 * reads files: it only compares metadata, catalog paths/URIs and previous manual links.
 */
class LocalTrackMatcher(
    library: List<MusicFile>,
    /** Reference key (external id / ISRC) → content URI linked by the user in an earlier import. */
    private val knownLinks: Map<String, String> = emptyMap()
) {
    private val tracks = library.map(::LocalTrack)
    private val byUri = tracks.associateBy { it.uri }
    private val byCoreTitle = tracks.groupBy { it.title.core }
    private val byLooseTitle = tracks.groupBy { it.title.loose }
    private val byToken: Map<String, List<LocalTrack>> = buildMap<String, MutableList<LocalTrack>> {
        tracks.forEach { track ->
            track.title.tokens.filter { it.length >= 3 }.toSet().forEach { getOrPut(it) { mutableListOf() } += track }
        }
    }
    private val byFileName: Map<String, List<LocalTrack>> = tracks
        .filter { it.pathSegments.isNotEmpty() }
        .groupBy { it.pathSegments.last() }

    fun match(entry: PlaylistImportEntry): TrackMatch {
        entry.referenceKey?.let { key ->
            knownLinks[key]?.takeIf { it in byUri }?.let { uri ->
                return TrackMatch(
                    MatchStatus.MATCH_HIGH_CONFIDENCE,
                    listOf(MatchCandidate(uri, 100, "Vinculada en una importación anterior")),
                    uri,
                    "Vinculada en una importación anterior"
                )
            }
        }
        matchByLocation(entry)?.let { return it }
        return matchByMetadata(entry)
    }

    private fun matchByLocation(entry: PlaylistImportEntry): TrackMatch? {
        val location = entry.originalLocation ?: return null
        if (location.startsWith("content://", ignoreCase = true)) {
            return byUri[location]?.let { exact(it.uri, "Mismo URI de la biblioteca") }
        }
        val segments = locationSegments(location) ?: return null
        val sameName = byFileName[segments.last()].orEmpty()
        if (sameName.isEmpty()) return null
        val scored = sameName.map { track -> track to trailingMatches(track.pathSegments, segments) }
        val best = scored.maxOf { it.second }
        val top = scored.filter { it.second == best }
        val fullPath = segments.size >= 2 && best == segments.size
        if (top.size == 1 && (fullPath || best >= 2)) {
            return exact(top.single().first.uri, if (fullPath) "Misma ruta de archivo" else "Misma carpeta y nombre de archivo")
        }
        if (top.size == 1) {
            val track = top.single().first
            // Titles guessed from a file name say nothing about the recording; real tags must not contradict it.
            val contradicts = entry.title != null && !entry.titleFromFileName &&
                score(entry, track).let { it == null || it.contradicts }
            if (!contradicts) {
                return TrackMatch(
                    MatchStatus.MATCH_HIGH_CONFIDENCE,
                    listOf(MatchCandidate(track.uri, 80, "Mismo nombre de archivo")),
                    track.uri,
                    "Mismo nombre de archivo en otra carpeta"
                )
            }
        }
        val candidates = top.take(MAX_CANDIDATES).map { MatchCandidate(it.first.uri, 60, "Mismo nombre de archivo") }
        return TrackMatch(MatchStatus.MATCH_AMBIGUOUS, candidates, null, "Varios archivos con el mismo nombre")
    }

    private fun matchByMetadata(entry: PlaylistImportEntry): TrackMatch {
        val title = entry.title ?: return notFound("La entrada no incluye título")
        val parts = TitleParts.parse(title)
        if (parts.core.isEmpty()) return notFound("El título no tiene texto comparable")

        val pool = LinkedHashSet<LocalTrack>()
        byCoreTitle[parts.core]?.let(pool::addAll)
        byLooseTitle[parts.loose]?.let(pool::addAll)
        if (pool.size < 3) {
            parts.tokens.filter { it.length >= 3 }
                .mapNotNull { byToken[it] }
                .sortedBy { it.size }
                .take(3)
                .forEach { tracksWithToken -> if (tracksWithToken.size <= 400) pool.addAll(tracksWithToken) }
        }

        val scored = pool.mapNotNull { track -> score(entry, track, parts)?.let { track to it } }
            .filter { it.second.points >= CANDIDATE_THRESHOLD }
            .sortedByDescending { it.second.points }
        if (scored.isEmpty()) return notFound("No hay canciones locales con título y artista compatibles")

        val candidates = scored.take(MAX_CANDIDATES).map { (track, result) -> MatchCandidate(track.uri, result.points, result.reason) }
        val (topTrack, top) = scored.first()
        val runnerUp = scored.getOrNull(1)?.second
        val margin = top.points - (runnerUp?.points ?: 0)

        val isExact = top.titleScore >= 1.0 && top.artistScore == 1.0 && !top.hardConflict && !top.softConflict &&
            (top.albumEqual || top.durationClose) && (runnerUp == null || margin >= 10)
        if (isExact) return TrackMatch(MatchStatus.MATCH_EXACT, candidates, topTrack.uri, top.reason)

        val artistTrusted = top.artistScore != null && top.artistScore >= 0.8 ||
            (top.artistScore == null && top.durationClose && top.albumEqual)
        val isHigh = top.points >= HIGH_THRESHOLD && !top.hardConflict && artistTrusted &&
            (runnerUp == null || margin >= 12) && !entry.titleFromFileName
        if (isHigh) return TrackMatch(MatchStatus.MATCH_HIGH_CONFIDENCE, candidates, topTrack.uri, top.reason)

        val why = when {
            top.hardConflict -> "La versión local parece distinta (remix, en vivo, etc.)"
            runnerUp != null && margin < 12 -> "Varias canciones locales son igual de probables"
            else -> "Coincidencia parcial; requiere confirmación"
        }
        return TrackMatch(MatchStatus.MATCH_AMBIGUOUS, candidates, null, why)
    }

    private class Score(
        val points: Int,
        val titleScore: Double,
        val artistScore: Double?,
        val albumEqual: Boolean,
        val durationClose: Boolean,
        val hardConflict: Boolean,
        val softConflict: Boolean,
        val reason: String
    ) {
        /** Clear evidence that the file is a different recording than the entry describes. */
        val contradicts: Boolean get() = hardConflict || artistScore == 0.0
    }

    private fun score(entry: PlaylistImportEntry, track: LocalTrack, entryTitle: TitleParts = TitleParts.parse(entry.title.orEmpty())): Score? {
        val titleScore = titleSimilarity(entryTitle, track.title)
        if (titleScore < 0.6) return null

        val entryArtists = (entry.artists.map(ImportText::normalize) + entryTitle.featured).filter(String::isNotEmpty)
        val artistScore = if (entryArtists.isEmpty() || track.artists.isEmpty()) null
        else artistSimilarity(entryArtists, entry.artists.joinToString(" ").let(ImportText::normalize), track)
        // A known, different artist means a different song even when the title is identical.
        if (artistScore == 0.0) return null

        val entryAlbum = TitleParts.parse(entry.album.orEmpty()).loose
        val albumKnown = entryAlbum.isNotEmpty() && track.album.isNotEmpty()
        val albumEqual = albumKnown && entryAlbum == track.album

        val durationDiff = entry.durationMs?.takeIf { it > 0 }?.let { expected ->
            track.durationMs.takeIf { it > 0 }?.let { abs(expected - it) }
        }
        val durationClose = durationDiff != null && durationDiff <= 2_000

        val variantDiff = (entryTitle.variants - track.title.variants) + (track.title.variants - entryTitle.variants)
        val hardConflict = variantDiff.any { it.hard } || entryTitle.otherVersions != track.title.otherVersions
        val softConflict = variantDiff.any { !it.hard }

        var points = (titleScore * 50).toInt()
        points += when (artistScore) {
            null -> 12
            else -> (artistScore * 30).toInt()
        }
        points += when {
            !albumKnown -> 4
            albumEqual -> 10
            else -> 0
        }
        points += when {
            durationDiff == null -> 4
            durationDiff <= 2_000 -> 10
            durationDiff <= 5_000 -> 7
            durationDiff <= 10_000 -> 3
            durationDiff <= 30_000 -> -5
            else -> -15
        }
        if (hardConflict) points -= 35
        if (softConflict) points -= 8
        if (entry.titleFromFileName) points -= 10

        val reason = buildList {
            add(if (titleScore >= 1.0) "título igual" else "título similar")
            when {
                artistScore == null -> add("artista sin confirmar")
                artistScore >= 1.0 -> add("artista igual")
                else -> add("artista parcial")
            }
            if (albumEqual) add("mismo álbum")
            if (durationDiff != null) add(if (durationClose) "misma duración" else "duración distinta (${durationDiff / 1000}s)")
            if (hardConflict) add("otra versión")
            else if (softConflict) add("variante (remaster/edición)")
        }.joinToString(", ").replaceFirstChar(Char::uppercase)

        return Score(points.coerceIn(0, 100), titleScore, artistScore, albumEqual, durationClose, hardConflict, softConflict, reason)
    }

    private fun titleSimilarity(entry: TitleParts, local: TitleParts): Double {
        if (entry.core == local.core) return 1.0
        if (entry.loose.isNotEmpty() && entry.loose == local.loose) return 0.85
        val a = entry.tokens.toSet()
        val b = local.tokens.toSet()
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val smaller = minOf(a.size, b.size)
        val larger = maxOf(a.size, b.size)
        if (intersection == smaller.toDouble() && smaller.toDouble() / larger >= 0.6) return 0.7
        val jaccard = intersection / a.union(b).size
        return if (jaccard >= 0.7) 0.6 else 0.0
    }

    private fun artistSimilarity(entryArtists: List<String>, entryJoined: String, track: LocalTrack): Double {
        if (entryJoined == track.artistJoined || entryArtists.first() == track.artists.first()) return 1.0
        if (entryArtists.any { it in track.artists }) return 0.8
        val containment = entryArtists.any { artist ->
            artist.length >= 3 && track.artists.any { local -> local.length >= 3 && (local.contains(artist) || artist.contains(local)) }
        }
        return if (containment) 0.6 else 0.0
    }

    private fun exact(uri: String, reason: String) =
        TrackMatch(MatchStatus.MATCH_EXACT, listOf(MatchCandidate(uri, 100, reason)), uri, reason)

    private fun notFound(reason: String) = TrackMatch(MatchStatus.NOT_FOUND, emptyList(), null, reason)

    private class LocalTrack(file: MusicFile) {
        val uri: String = file.uri.toString()
        val title: TitleParts = TitleParts.parse(file.title)
        val artists: List<String> = if (file.artist == UNKNOWN_ARTIST) emptyList()
        else (ImportText.splitArtists(file.artist).map(ImportText::normalize) + title.featured).filter(String::isNotEmpty).distinct()
        val artistJoined: String = ImportText.normalize(file.artist)
        val album: String = if (file.album == UNKNOWN_ALBUM) "" else TitleParts.parse(file.album).loose
        val durationMs: Long = file.durationMs
        val pathSegments: List<String> = file.filePath?.let { locationSegments(it) }.orEmpty()
    }

    companion object {
        private const val CANDIDATE_THRESHOLD = 40
        private const val HIGH_THRESHOLD = 72
        private const val MAX_CANDIDATES = 5
        private const val UNKNOWN_ARTIST = "Artista desconocido"
        private const val UNKNOWN_ALBUM = "Álbum desconocido"

        /**
         * Splits a local path, file URL or Windows/classic-Mac path into lowercase segments for suffix
         * comparison. Returns null for remote URLs and platform URIs, which are never treated as files.
         */
        internal fun locationSegments(location: String): List<String>? {
            var value = location.trim()
            val scheme = Regex("^([a-zA-Z][a-zA-Z0-9+.-]+):").find(value)?.groupValues?.get(1)?.lowercase()
            when {
                scheme == "file" -> value = ImportText.decodePercent(value.substring(5).removePrefix("//").removePrefix("localhost"))
                scheme != null && scheme.length > 1 -> return null
            }
            val segments = value.split('/', '\\', ':')
                .map(String::trim)
                .filter { it.isNotEmpty() && it != "." && it != ".." }
                .map { it.lowercase() }
            return segments.takeIf { it.isNotEmpty() && it.last().contains('.') }
        }

        private fun trailingMatches(local: List<String>, imported: List<String>): Int {
            var count = 0
            while (count < local.size && count < imported.size &&
                local[local.size - 1 - count] == imported[imported.size - 1 - count]
            ) count++
            return count
        }
    }
}

/** Version markers that distinguish recordings. Hard ones mean a different recording. */
internal enum class TitleVariant(val hard: Boolean) {
    REMIX(true), LIVE(true), ACOUSTIC(true), INSTRUMENTAL(true), DEMO(true), KARAOKE(true),
    SPED_UP(true), SLOWED(true), EXTENDED(true), ACAPELLA(true), REMASTER(false), EDIT(false), MONO(false)
}

/**
 * Title split into comparable pieces: [core] keeps everything except featured artists and version
 * markers; [loose] also drops any bracketed or dash suffix; [variants] keeps the version markers so a
 * remix or a live take is never silently treated as the original.
 */
internal class TitleParts private constructor(
    val core: String,
    val loose: String,
    val variants: Set<TitleVariant>,
    /** Normalized text of "... Version" markers that are not a known variant (e.g. "spanish version"). */
    val otherVersions: Set<String>,
    val featured: List<String>
) {
    val tokens: List<String> get() = ImportText.tokens(core)

    companion object {
        // Brackets are escaped everywhere: Android's ICU regex treats {…} inside a [...] set as a string literal.
        private val segmentPattern = Regex("\\(([^\\(\\)]*)\\)|\\[([^\\[\\]]*)\\]|\\{([^\\{\\}]*)\\}")
        private val dashPattern = Regex("\\s+[-–—]\\s+")
        private val featPattern = Regex("^(feat|ft|featuring|with|con)\\b\\s*(.*)$")
        private val neutralSegments = setOf("original mix", "original version", "album version", "original", "version original")
        private val keywordVariants: List<Pair<Regex, TitleVariant>> = listOf(
            Regex("\\b(remix|rmx|mix|bootleg|rework|vip)\\b") to TitleVariant.REMIX,
            Regex("\\b(live|en vivo|en directo|ao vivo|unplugged|concert|concierto)\\b") to TitleVariant.LIVE,
            Regex("\\b(acoustic|acustico|acustica|acoustique)\\b") to TitleVariant.ACOUSTIC,
            Regex("\\binstrumental\\b") to TitleVariant.INSTRUMENTAL,
            Regex("\\b(demo|maqueta)\\b") to TitleVariant.DEMO,
            Regex("\\bkaraoke\\b") to TitleVariant.KARAOKE,
            Regex("\\b(sped up|speed up|nightcore)\\b") to TitleVariant.SPED_UP,
            Regex("\\b(slowed|reverb)\\b") to TitleVariant.SLOWED,
            Regex("\\bextended\\b") to TitleVariant.EXTENDED,
            Regex("\\b(acapella|a cappella|a capella)\\b") to TitleVariant.ACAPELLA,
            Regex("\\b(remaster|remastered|remasterizado|remasterizada|remasterizacion|remasterisé|remasterise)\\b") to TitleVariant.REMASTER,
            Regex("\\b(radio edit|edit|single version|radio version)\\b") to TitleVariant.EDIT,
            Regex("\\bmono\\b") to TitleVariant.MONO
        )

        fun parse(raw: String): TitleParts {
            val variants = mutableSetOf<TitleVariant>()
            val otherVersions = mutableSetOf<String>()
            val featured = mutableListOf<String>()
            val coreBuilder = StringBuilder()
            val looseBuilder = StringBuilder()

            /** Returns true when the segment is metadata (variant/feat) and must leave the core title. */
            fun classify(segment: String): Boolean {
                val normalized = ImportText.normalize(segment)
                if (normalized.isEmpty()) return true
                if (normalized in neutralSegments) return true
                featPattern.matchEntire(normalized)?.let { match ->
                    featured += ImportText.splitArtists(match.groupValues[2]).map(ImportText::normalize).filter(String::isNotEmpty)
                    return true
                }
                val found = keywordVariants.filter { it.first.containsMatchIn(normalized) }.map { it.second }
                if (found.isNotEmpty()) {
                    variants += found
                    return true
                }
                if (Regex("\\b(version|versión|versao|take)\\b").containsMatchIn(normalized)) {
                    otherVersions += normalized
                    return true
                }
                return false
            }

            // Bracketed segments.
            var lastEnd = 0
            val withoutBrackets = StringBuilder()
            segmentPattern.findAll(raw).forEach { match ->
                withoutBrackets.append(raw, lastEnd, match.range.first)
                val content = match.groupValues.drop(1).firstOrNull(String::isNotEmpty).orEmpty()
                if (!classify(content)) coreBuilder.append(' ').append(content)
                lastEnd = match.range.last + 1
            }
            withoutBrackets.append(raw.substring(lastEnd))

            // Dash suffixes ("Song - 2011 Remaster", "Song - Live at X").
            val pieces = withoutBrackets.split(dashPattern)
            val head = pieces.first()
            val keptTail = pieces.drop(1).filterNot(::classify)
            looseBuilder.append(head)
            val core = ImportText.normalize(listOf(head).plus(keptTail).joinToString(" ") + coreBuilder.toString())
            val loose = ImportText.normalize(looseBuilder.toString())
            return TitleParts(core, loose.ifEmpty { core }, variants, otherVersions, featured.distinct())
        }
    }
}
