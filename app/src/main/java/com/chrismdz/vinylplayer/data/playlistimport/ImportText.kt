package com.chrismdz.vinylplayer.data.playlistimport

import java.text.Normalizer
import java.util.Locale

/** Text helpers shared by adapters and the matcher. */
internal object ImportText {
    /**
     * Comparison key: Unicode compatibility normalization, case folding, accents removed only from Latin
     * letters (so non-Latin scripts keep their meaningful marks), apostrophes dropped and other punctuation
     * treated as a word boundary.
     */
    fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val builder = StringBuilder(decomposed.length)
        var previousBase: Char? = null
        for (character in decomposed) {
            val type = Character.getType(character)
            if (type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()) {
                val base = previousBase
                val baseIsLatin = base != null && Character.UnicodeScript.of(base.code) == Character.UnicodeScript.LATIN
                if (!baseIsLatin) builder.append(character)
                continue
            }
            previousBase = character
            builder.append(character)
        }
        val folded = Normalizer.normalize(builder.toString(), Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val output = StringBuilder(folded.length)
        var pendingSpace = false
        for (character in folded) {
            when {
                character == '\'' || character == '’' || character == '‘' || character == '`' || character == '´' -> Unit
                Character.isLetterOrDigit(character) ||
                    Character.getType(character) == Character.NON_SPACING_MARK.toInt() ||
                    Character.getType(character) == Character.COMBINING_SPACING_MARK.toInt() -> {
                    if (pendingSpace && output.isNotEmpty()) output.append(' ')
                    pendingSpace = false
                    output.append(character)
                }
                else -> pendingSpace = true
            }
        }
        return output.toString()
    }

    fun tokens(value: String): List<String> = value.split(' ').filter(String::isNotEmpty)

    /** Collapses whitespace and trims; keeps the original spelling for display. */
    fun clean(value: String?): String? =
        value?.replace('\u0000', ' ')?.trim()?.replace(Regex("\\s+"), " ")?.takeIf(String::isNotEmpty)

    /** Splits a multi-artist field on commas, semicolons and "feat." markers ("AC/DC" stays one artist). */
    fun splitArtists(value: String?): List<String> {
        val cleanValue = clean(value) ?: return emptyList()
        return cleanValue
            .split(Regex("\\s*[,;]\\s*|\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE))
            .mapNotNull(::clean)
            .distinct()
    }

    /** Splits "Artist - Title" display strings used by M3U/PLS. Returns (artist, title). */
    fun splitArtistTitle(display: String?): Pair<String?, String?> {
        val cleanDisplay = clean(display) ?: return null to null
        val separator = Regex("\\s+[-–—]\\s+").find(cleanDisplay) ?: return null to cleanDisplay
        val artist = clean(cleanDisplay.substring(0, separator.range.first))
        val title = clean(cleanDisplay.substring(separator.range.last + 1))
        return if (artist == null || title == null) null to cleanDisplay else artist to title
    }

    /** Title guessed from a file name: extension and leading track numbers removed. */
    fun titleFromLocation(location: String): String? {
        val name = location.trimEnd('/', '\\')
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .let { decodePercent(it) }
            .substringBeforeLast('.')
        return clean(name.replace(Regex("^\\d{1,3}\\s*[-._)]?\\s+"), ""))
    }

    /**
     * Parses durations such as "3:25", "1:02:03", "205", "205000" or "205.4".
     * [numberUnit] tells how to read a bare number when the source documents its unit.
     */
    fun parseDurationMs(value: String?, numberUnit: DurationUnit = DurationUnit.GUESS): Long? {
        val text = clean(value) ?: return null
        if (text.contains(':')) {
            val parts = text.split(':').map { it.trim().toDoubleOrNull() ?: return null }
            if (parts.size !in 2..3) return null
            val seconds = parts.fold(0.0) { total, part -> total * 60 + part }
            return (seconds * 1000).toLong().takeIf { it > 0 }
        }
        val number = text.replace(',', '.').toDoubleOrNull() ?: return null
        if (number <= 0) return null
        val millis = when (numberUnit) {
            DurationUnit.MILLISECONDS -> number
            DurationUnit.SECONDS -> number * 1000
            DurationUnit.GUESS -> if (number > 10_000) number else number * 1000
        }
        return millis.toLong()
    }

    fun decodePercent(value: String): String {
        if (!value.contains('%')) return value
        return runCatching {
            java.net.URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
        }.getOrDefault(value)
    }

    enum class DurationUnit { MILLISECONDS, SECONDS, GUESS }
}
