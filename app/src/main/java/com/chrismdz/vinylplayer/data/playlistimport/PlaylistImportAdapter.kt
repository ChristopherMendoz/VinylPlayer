package com.chrismdz.vinylplayer.data.playlistimport

/** Verdict of an adapter about a document. */
sealed interface AdapterVerdict {
    /** The adapter can interpret the document. Higher [score] means a more specific structural match. */
    data class Supported(
        val score: Int,
        val format: PlaylistImportFormat,
        val source: PlaylistImportSource
    ) : AdapterVerdict

    /** The structure was recognized but cannot be interpreted; [reason] explains what is missing. */
    data class Rejected(val reason: String) : AdapterVerdict
}

/**
 * Converts one file structure into [ParsedPlaylistFile]. Adapters only parse; matching against the local
 * library and persistence are shared by every adapter, so adding a format never touches that logic.
 */
interface PlaylistImportAdapter {
    val id: String

    /** Returns null when the document is clearly not this adapter's structure. */
    fun inspect(document: PlaylistImportDocument): AdapterVerdict?

    fun parse(document: PlaylistImportDocument, verdict: AdapterVerdict.Supported): ParsedPlaylistFile
}

sealed interface DetectionOutcome {
    data class Detected(
        val adapter: PlaylistImportAdapter,
        val verdict: AdapterVerdict.Supported
    ) : DetectionOutcome

    data class Unsupported(val reason: String) : DetectionOutcome
}

/**
 * Chooses the adapter whose structural check is most specific. Extension alone never makes a file
 * supported: every adapter inspects the content.
 */
class PlaylistImportFormatDetector(private val adapters: List<PlaylistImportAdapter>) {
    fun detect(document: PlaylistImportDocument): DetectionOutcome {
        if (document.text.isBlank()) return DetectionOutcome.Unsupported("El archivo está vacío.")
        val verdicts = adapters.mapNotNull { adapter ->
            runCatching { adapter.inspect(document) }.getOrNull()?.let { adapter to it }
        }
        val best = verdicts
            .mapNotNull { (adapter, verdict) -> (verdict as? AdapterVerdict.Supported)?.let { adapter to it } }
            .maxByOrNull { it.second.score }
        if (best != null) return DetectionOutcome.Detected(best.first, best.second)
        val rejection = verdicts.firstNotNullOfOrNull { (it.second as? AdapterVerdict.Rejected)?.reason }
        return DetectionOutcome.Unsupported(rejection ?: describeUnknown(document))
    }

    private fun describeUnknown(document: PlaylistImportDocument): String {
        val line = document.firstMeaningfulLine.take(80)
        val extension = document.extension.takeIf(String::isNotEmpty)?.let { ".$it" } ?: "sin extensión"
        return "No hay un adaptador para esta estructura ($extension). Primera línea: “$line”. " +
            "Se admiten CSV/TSV con encabezados, M3U, M3U8, PLS, XSPF y XML de iTunes."
    }
}
