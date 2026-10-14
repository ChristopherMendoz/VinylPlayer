package com.chrismdz.vinylplayer.data

import java.util.UUID

/** A stable identity for one occurrence of a song in the playback queue. */
data class QueueEntry(
    val id: String,
    val file: MusicFile
) {
    companion object {
        fun create(file: MusicFile): QueueEntry = QueueEntry(UUID.randomUUID().toString(), file)
    }
}
