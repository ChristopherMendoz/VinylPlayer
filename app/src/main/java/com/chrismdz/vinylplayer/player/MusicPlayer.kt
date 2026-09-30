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
    private var pendingQueue: List<MusicFile> = emptyList()
    private var pendingShuffleEnabled = false
    private var pendingPlayWhenReady = false
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
                pendingFile?.let { load(it, pendingQueue, pendingPlayWhenReady, pendingShuffleEnabled) }
                onStateChanged(mediaController)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun load(file: MusicFile, playWhenReady: Boolean = false) {
        load(file, listOf(file), playWhenReady, false)
    }

    fun load(file: MusicFile, queue: List<MusicFile>, playWhenReady: Boolean = false, shuffleEnabled: Boolean = false) {
        pendingFile = file
        pendingQueue = queue.ifEmpty { listOf(file) }
        pendingPlayWhenReady = playWhenReady
        pendingShuffleEnabled = shuffleEnabled
        controller?.let { player ->
            val mediaItems = pendingQueue.distinctBy { it.uri }.map { item ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .apply {
                        item.artworkData?.let {
                            setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        }
                    }
                    .build()
                MediaItem.Builder()
                    .setMediaId(item.uri.toString())
                    .setUri(item.uri)
                    .setMediaMetadata(metadata)
                    .build()
            }
            val selectedIndex = mediaItems.indexOfFirst { it.mediaId == file.uri.toString() }.coerceAtLeast(0)
            player.setMediaItems(mediaItems, selectedIndex, 0L)
            player.shuffleModeEnabled = shuffleEnabled
            player.prepare()
            if (playWhenReady) player.play() else player.pause()
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

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    fun release() {
        released = true
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
