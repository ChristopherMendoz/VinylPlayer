package com.chrismdz.vinylplayer.ui

import android.content.Context

/**
 * Custom track order per album (the tracklist as the artist released it, or however the user
 * likes). The saved order survives switching back to the default order, so it can be re-enabled
 * with one tap; it is only replaced when the user saves a new one.
 */
internal class AlbumTrackOrderStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Saved song URIs in the user's order, or null if this album never got a custom order. */
    fun savedOrder(albumKey: String): List<String>? =
        preferences.getString("$albumKey$ORDER_SUFFIX", null)
            ?.split('\n')
            ?.filter(String::isNotBlank)
            ?.takeIf { it.isNotEmpty() }

    /** True when the album should be shown in its saved custom order. */
    fun isCustomActive(albumKey: String): Boolean =
        preferences.getBoolean("$albumKey$ACTIVE_SUFFIX", false) && savedOrder(albumKey) != null

    /** Saves a new custom order and switches the album to it. */
    fun saveOrder(albumKey: String, songUris: List<String>) {
        preferences.edit()
            .putString("$albumKey$ORDER_SUFFIX", songUris.joinToString("\n"))
            .putBoolean("$albumKey$ACTIVE_SUFFIX", true)
            .apply()
    }

    /** Switches between the default and the saved custom order without losing the latter. */
    fun setCustomActive(albumKey: String, active: Boolean) {
        preferences.edit().putBoolean("$albumKey$ACTIVE_SUFFIX", active).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "vinylplayer_album_track_order"
        const val ORDER_SUFFIX = "|order"
        const val ACTIVE_SUFFIX = "|custom"
    }
}

/**
 * Puts [tracks] in the saved [order]. Songs added to the album after the order was saved keep
 * their default relative order at the end; saved songs no longer in the album are ignored.
 */
internal fun <T> applyCustomTrackOrder(tracks: List<T>, order: List<String>, trackUri: (T) -> String): List<T> {
    val rank = HashMap<String, Int>(order.size).apply {
        order.forEachIndexed { index, uri -> putIfAbsent(uri, index) }
    }
    val (ordered, newTracks) = tracks.partition { trackUri(it) in rank }
    return ordered.sortedBy { rank.getValue(trackUri(it)) } + newTracks
}
