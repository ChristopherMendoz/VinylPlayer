package com.chrismdz.vinylplayer.data

import android.graphics.Bitmap
import android.net.Uri

data class MusicFile(
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: Bitmap?,
    val artworkData: ByteArray?,
    val durationMs: Long
)
