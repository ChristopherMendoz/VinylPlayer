package com.chrismdz.vinylplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumTrackOrderTest {
    private val id: (String) -> String = { it }

    @Test
    fun ordersTracksBySavedOrder() {
        assertEquals(
            listOf("c", "a", "b"),
            applyCustomTrackOrder(listOf("a", "b", "c"), listOf("c", "a", "b"), id)
        )
    }

    @Test
    fun appendsTracksAddedAfterSavingInDefaultOrder() {
        assertEquals(
            listOf("b", "a", "d", "e"),
            applyCustomTrackOrder(listOf("a", "d", "b", "e"), listOf("b", "a"), id)
        )
    }

    @Test
    fun ignoresSavedTracksThatLeftTheAlbum() {
        assertEquals(
            listOf("c", "a"),
            applyCustomTrackOrder(listOf("a", "c"), listOf("c", "gone", "a"), id)
        )
    }
}
