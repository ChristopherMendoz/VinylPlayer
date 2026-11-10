package com.chrismdz.vinylplayer.data.playlistimport

/**
 * RFC 4180 style reader: quoted fields, escaped quotes (""), delimiters and line breaks inside quotes,
 * CRLF/LF/CR line endings. A quote that appears mid-field is kept literally instead of failing.
 */
internal object CsvReader {
    private val candidates = charArrayOf(',', ';', '\t', '|')

    /** Picks the delimiter that splits the first logical record into the most columns. */
    fun detectDelimiter(text: String): Char {
        val firstRecord = StringBuilder()
        var inQuotes = false
        for (character in text) {
            if (character == '"') inQuotes = !inQuotes
            if (!inQuotes && (character == '\n' || character == '\r')) {
                if (firstRecord.isNotBlank()) break else continue
            }
            firstRecord.append(character)
            if (firstRecord.length > 8_192) break
        }
        var best = ','
        var bestCount = 0
        candidates.forEach { delimiter ->
            var count = 0
            var quoted = false
            firstRecord.forEach { character ->
                if (character == '"') quoted = !quoted
                if (!quoted && character == delimiter) count++
            }
            if (count > bestCount) {
                best = delimiter
                bestCount = count
            }
        }
        return best
    }

    fun read(text: String, delimiter: Char, maxRows: Int = 200_000): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var fieldStarted = false
        var index = 0

        fun endField() {
            row += field.toString()
            field.setLength(0)
            fieldStarted = false
        }

        fun endRow() {
            endField()
            if (row.any { it.isNotBlank() }) rows += row.toList()
            row.clear()
        }

        while (index < text.length && rows.size < maxRows) {
            val character = text[index]
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(character)
                }
            } else {
                when {
                    character == '"' && !fieldStarted -> {
                        inQuotes = true
                        fieldStarted = true
                    }
                    character == delimiter -> endField()
                    character == '\r' -> {
                        if (index + 1 < text.length && text[index + 1] == '\n') index++
                        endRow()
                    }
                    character == '\n' -> endRow()
                    else -> {
                        field.append(character)
                        fieldStarted = true
                    }
                }
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
