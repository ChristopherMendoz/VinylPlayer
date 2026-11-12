package com.chrismdz.vinylplayer.settings

enum class ThemeMode(val title: String) {
    DARK("Oscuro"),
    LIGHT("Claro"),
    SYSTEM("Según el sistema")
}

enum class AccentColor(val title: String) {
    GOLD("Dorado"),
    RED("Rojo"),
    CUSTOM("Personalizado")
}

enum class PlayerBackground(val title: String) {
    BLURRED_COVER("Portada desenfocada"),
    SOLID("Color sólido"),
    COVER_COLORS("Colores de portada")
}

enum class RepeatMode(val title: String) {
    OFF("Desactivado"),
    ALL("Toda la fila"),
    ONE("Una canción")
}

enum class QueueAddBehavior(val title: String) {
    ADD_TO_END("Agregar al final"),
    PLAY_NEXT("Reproducir a continuación")
}

enum class LibrarySortOrder(val title: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum")
}

/** How the album and artist collections are laid out in the library. */
enum class CollectionViewMode(val title: String) {
    GRID("Cuadrícula"),
    LIST("Lista")
}

enum class TranslationLanguage(val title: String, val code: String) {
    SPANISH("Español", "es"),
    ENGLISH("Inglés", "en"),
    FRENCH("Francés", "fr"),
    GERMAN("Alemán", "de"),
    ITALIAN("Italiano", "it"),
    PORTUGUESE("Portugués", "pt")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.GOLD,
    val customAccentArgb: Int? = null,
    val playerBackground: PlayerBackground = PlayerBackground.BLURRED_COVER,
    val tonearmAnimationEnabled: Boolean = true,
    val resumeLastSession: Boolean = true,
    val autoAdvance: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE,
    val albumViewMode: CollectionViewMode = CollectionViewMode.GRID,
    val artistViewMode: CollectionViewMode = CollectionViewMode.GRID,
    val keepQueueBetweenSessions: Boolean = true,
    val queueAddBehavior: QueueAddBehavior = QueueAddBehavior.ADD_TO_END,
    val allowQueueDuplicates: Boolean = true,
    val lyricsEnabled: Boolean = true,
    val lyricsTranslationEnabled: Boolean = true,
    val translationLanguage: TranslationLanguage = TranslationLanguage.SPANISH,
    val lyricsFontSizeSp: Int = 22,
    val lyricsBoldText: Boolean = true,
    val lyricsPreviousLines: Int = 2,
    val lyricsNextLines: Int = 4,
    val globalLyricsOffsetMs: Long = 0L,
    val equalizerEnabled: Boolean = true
)
