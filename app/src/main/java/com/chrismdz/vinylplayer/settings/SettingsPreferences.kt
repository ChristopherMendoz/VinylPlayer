package com.chrismdz.vinylplayer.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        themeMode = preferences.enumValue("theme_mode", ThemeMode.DARK),
        accentColor = preferences.enumValue("accent_color", AccentColor.GOLD),
        customAccentArgb = if (preferences.contains("custom_accent_argb")) preferences.getInt("custom_accent_argb", 0xFFE4B45B.toInt()) else null,
        playerBackground = preferences.enumValue("player_background", PlayerBackground.BLURRED_COVER),
        tonearmAnimationEnabled = preferences.getBoolean("tonearm_animation", true),
        resumeLastSession = preferences.getBoolean("resume_last_session", true),
        autoAdvance = preferences.getBoolean("auto_advance", true),
        repeatMode = preferences.enumValue("repeat_mode", RepeatMode.OFF),
        shuffleEnabled = preferences.getBoolean("shuffle_enabled", false),
        librarySortOrder = preferences.enumValue("library_sort_order", LibrarySortOrder.TITLE),
        albumViewMode = preferences.enumValue("album_view_mode", CollectionViewMode.GRID),
        artistViewMode = preferences.enumValue("artist_view_mode", CollectionViewMode.GRID),
        keepQueueBetweenSessions = preferences.getBoolean("keep_queue_between_sessions", true),
        queueAddBehavior = preferences.enumValue("queue_add_behavior", QueueAddBehavior.ADD_TO_END),
        allowQueueDuplicates = preferences.getBoolean("allow_queue_duplicates", true),
        lyricsEnabled = preferences.getBoolean("lyrics_enabled", true),
        lyricsTranslationEnabled = preferences.getBoolean("lyrics_translation_enabled", true),
        translationLanguage = preferences.enumValue("translation_language", TranslationLanguage.SPANISH),
        lyricsFontSizeSp = preferences.getInt("lyrics_font_size_sp", 22).coerceIn(16, 40),
        lyricsBoldText = preferences.getBoolean("lyrics_bold_text", true),
        lyricsPreviousLines = preferences.getInt("lyrics_previous_lines", 2).coerceIn(0, 8),
        lyricsNextLines = preferences.getInt("lyrics_next_lines", 4).coerceIn(0, 12),
        globalLyricsOffsetMs = preferences.getLong("global_lyrics_offset_ms", 0L).coerceIn(-600_000L, 600_000L),
        equalizerEnabled = preferences.getBoolean("eq_enabled", true)
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("accent_color", settings.accentColor.name)
            .apply {
                if (settings.customAccentArgb == null) remove("custom_accent_argb")
                else putInt("custom_accent_argb", settings.customAccentArgb)
            }
            .putString("player_background", settings.playerBackground.name)
            .putBoolean("tonearm_animation", settings.tonearmAnimationEnabled)
            .putBoolean("resume_last_session", settings.resumeLastSession)
            .putBoolean("auto_advance", settings.autoAdvance)
            .putString("repeat_mode", settings.repeatMode.name)
            .putBoolean("shuffle_enabled", settings.shuffleEnabled)
            .putString("library_sort_order", settings.librarySortOrder.name)
            .putString("album_view_mode", settings.albumViewMode.name)
            .putString("artist_view_mode", settings.artistViewMode.name)
            .putBoolean("keep_queue_between_sessions", settings.keepQueueBetweenSessions)
            .putString("queue_add_behavior", settings.queueAddBehavior.name)
            .putBoolean("allow_queue_duplicates", settings.allowQueueDuplicates)
            .putBoolean("lyrics_enabled", settings.lyricsEnabled)
            .putBoolean("lyrics_translation_enabled", settings.lyricsTranslationEnabled)
            .putString("translation_language", settings.translationLanguage.name)
            .putInt("lyrics_font_size_sp", settings.lyricsFontSizeSp.coerceIn(16, 40))
            .putBoolean("lyrics_bold_text", settings.lyricsBoldText)
            .putInt("lyrics_previous_lines", settings.lyricsPreviousLines.coerceIn(0, 8))
            .putInt("lyrics_next_lines", settings.lyricsNextLines.coerceIn(0, 12))
            .putLong("global_lyrics_offset_ms", settings.globalLyricsOffsetMs.coerceIn(-600_000L, 600_000L))
            .putBoolean("eq_enabled", settings.equalizerEnabled)
            .apply()
    }

    fun resetAppearance(): AppSettings {
        val updated = load().copy(
            themeMode = AppSettings().themeMode,
            accentColor = AppSettings().accentColor,
            customAccentArgb = null,
            playerBackground = AppSettings().playerBackground,
            tonearmAnimationEnabled = AppSettings().tonearmAnimationEnabled,
            lyricsFontSizeSp = AppSettings().lyricsFontSizeSp,
            lyricsBoldText = AppSettings().lyricsBoldText
        )
        preferences.edit()
            .putString("theme_mode", updated.themeMode.name)
            .putString("accent_color", updated.accentColor.name)
            .remove("custom_accent_argb")
            .putString("player_background", updated.playerBackground.name)
            .putBoolean("tonearm_animation", updated.tonearmAnimationEnabled)
            .putInt("lyrics_font_size_sp", updated.lyricsFontSizeSp)
            .putBoolean("lyrics_bold_text", updated.lyricsBoldText)
            .apply()
        return updated
    }

    fun resetAll(): AppSettings {
        val defaults = AppSettings()
        preferences.edit().apply {
            RESETTABLE_KEYS.forEach { remove(it) }
            repeat(5) { putInt("eq_band_$it", 0) }
            apply()
        }
        save(defaults)
        return defaults
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(key: String, default: T): T =
        runCatching { enumValueOf<T>(getString(key, default.name) ?: default.name) }.getOrDefault(default)

    companion object {
        const val PREFERENCES_NAME = "vinylplayer_settings"

        private val RESETTABLE_KEYS = listOf(
            "theme_mode", "accent_color", "custom_accent_argb", "player_background", "tonearm_animation",
            "resume_last_session", "auto_advance", "repeat_mode", "shuffle_enabled", "library_sort_order", "album_view_mode", "artist_view_mode",
            "keep_queue_between_sessions", "queue_add_behavior", "allow_queue_duplicates",
            "lyrics_enabled", "lyrics_translation_enabled", "translation_language",
            "lyrics_font_size_sp", "lyrics_bold_text", "lyrics_previous_lines", "lyrics_next_lines",
            "global_lyrics_offset_ms", "eq_enabled",
            "eq_band_0", "eq_band_1", "eq_band_2", "eq_band_3", "eq_band_4"
        )
    }
}
