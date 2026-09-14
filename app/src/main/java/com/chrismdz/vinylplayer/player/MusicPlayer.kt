package com.chrismdz.vinylplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.chrismdz.vinylplayer.data.MusicFile

class MusicPlayer(
    context: Context,
    private val onStateChanged: (Player) -> Unit
) {
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()
    private var controller: MediaController? = null
    private var pendingFile: MusicFile? = null
    private var released = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            onStateChanged(player)
        }
    }

    init {
        controllerFuture.addListener({
            if (released) return@addListener
            runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                controller = mediaController
                mediaController.addListener(listener)
                pendingFile?.let { load(it) }
                onStateChanged(mediaController)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun load(file: MusicFile) {
        pendingFile = file
        controller?.let { player ->
            val metadata = MediaMetadata.Builder()
                .setTitle(file.title)
                .setArtist(file.artist)
                .setAlbumTitle(file.album)
                .apply {
                    file.artworkData?.let {
                        setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }
                }
                .build()
            val mediaItem = MediaItem.Builder()
                .setMediaId(file.uri.toString())
                .setUri(file.uri)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            onStateChanged(player)
        }
    }

    fun toggle() {
        controller?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    fun release() {
        released = true
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
