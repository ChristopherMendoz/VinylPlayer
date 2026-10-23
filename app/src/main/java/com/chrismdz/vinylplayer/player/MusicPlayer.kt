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
import com.chrismdz.vinylplayer.data.QueueEntry

class MusicPlayer(
    context: Context,
    private val resumeOnConnect: Boolean = true,
    private val onStateChanged: (Player) -> Unit
) {
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()
    private var controller: MediaController? = null
    private var pendingEntry: QueueEntry? = null
    private var pendingQueue: List<QueueEntry> = emptyList()
    private var pendingPositionMs = 0L
    private var pendingShuffleEnabled = false
    private var pendingRepeatOneEnabled = false
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
                mediaController.shuffleModeEnabled = false
                mediaController.repeatMode = if (pendingRepeatOneEnabled) {
                    Player.REPEAT_MODE_ONE
                } else {
                    Player.REPEAT_MODE_OFF
                }
                val queueAlreadyLoaded = pendingQueue.isNotEmpty() &&
                    mediaController.mediaItemCount == pendingQueue.size &&
                    pendingQueue.indices.all { index ->
                        mediaController.getMediaItemAt(index).mediaId == pendingQueue[index].id
                    }
                if (!queueAlreadyLoaded) {
                    val currentItemMatches = pendingEntry?.file?.uri ==
                        mediaController.currentMediaItem?.localConfiguration?.uri
                    val shouldResumeExistingItem = resumeOnConnect && currentItemMatches && mediaController.mediaItemCount > 0
                    pendingEntry?.let {
                        load(
                            it,
                            pendingQueue,
                            if (shouldResumeExistingItem) mediaController.playWhenReady else pendingPlayWhenReady,
                            pendingShuffleEnabled,
                            if (shouldResumeExistingItem) mediaController.currentPosition else pendingPositionMs
                        )
                    } ?: pendingQueue.takeIf { it.isNotEmpty() }?.let {
                        replaceQueue(it, null, pendingPositionMs, pendingPlayWhenReady, pendingShuffleEnabled)
                    }
                }
                onStateChanged(mediaController)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun load(file: MusicFile, playWhenReady: Boolean = false) {
        val entry = QueueEntry.create(file)
        load(entry, listOf(entry), playWhenReady, false)
    }

    fun load(
        entry: QueueEntry,
        queue: List<QueueEntry>,
        playWhenReady: Boolean = false,
        shuffleEnabled: Boolean = false,
        positionMs: Long = 0L
    ) {
        pendingEntry = entry
        pendingQueue = queue.withEntry(entry)
        pendingPlayWhenReady = playWhenReady
        pendingShuffleEnabled = shuffleEnabled
        pendingPositionMs = positionMs
        controller?.let { player ->
            val mediaItems = pendingQueue.map(::toMediaItem)
            val selectedIndex = pendingQueue.indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
            player.setMediaItems(mediaItems, selectedIndex, positionMs.coerceAtLeast(0L))
            // Shuffle is represented by the ordered queue itself so the visible next-track order stays exact.
            player.shuffleModeEnabled = false
            player.prepare()
            if (playWhenReady) player.play() else player.pause()
            onStateChanged(player)
        }
    }

    fun replaceQueue(
        queue: List<QueueEntry>,
        currentEntryId: String?,
        positionMs: Long,
        playWhenReady: Boolean,
        shuffleEnabled: Boolean
    ) {
        pendingQueue = queue
        pendingEntry = queue.firstOrNull { it.id == currentEntryId }
        pendingPositionMs = positionMs.coerceAtLeast(0L)
        pendingPlayWhenReady = playWhenReady
        pendingShuffleEnabled = shuffleEnabled
        controller?.let { player ->
            if (queue.isEmpty()) {
                player.clearMediaItems()
                return@let
            }
            val selectedIndex = queue.indexOfFirst { it.id == currentEntryId }.coerceAtLeast(0)
            player.setMediaItems(queue.map(::toMediaItem), selectedIndex, pendingPositionMs)
            player.shuffleModeEnabled = false
            player.prepare()
            if (playWhenReady) player.play() else player.pause()
            onStateChanged(player)
        }
    }

    fun updateQueueMetadata(queue: List<QueueEntry>) {
        if (queue.map { it.id } != pendingQueue.map { it.id }) return
        pendingQueue = queue
        pendingEntry = queue.firstOrNull { it.id == pendingEntry?.id }
        controller?.let { player ->
            queue.forEachIndexed { index, entry ->
                if (index < player.mediaItemCount && player.getMediaItemAt(index).mediaId == entry.id) {
                    player.replaceMediaItem(index, toMediaItem(entry))
                }
            }
        }
    }

    fun currentPositionOrNull(): Long? = controller?.currentPosition

    fun addQueueEntry(entry: QueueEntry, index: Int) {
        val insertionIndex = index.coerceIn(0, pendingQueue.size)
        pendingQueue = pendingQueue.toMutableList().apply { add(insertionIndex, entry) }
        controller?.let { player ->
            player.addMediaItem(insertionIndex.coerceIn(0, player.mediaItemCount), toMediaItem(entry))
        }
    }

    fun moveQueueEntry(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in pendingQueue.indices || toIndex !in pendingQueue.indices) return
        pendingQueue = pendingQueue.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        controller?.let { player ->
            if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount) {
                player.moveMediaItem(fromIndex, toIndex)
            }
        }
    }

    fun removeQueueItems(fromIndex: Int, toIndexExclusive: Int) {
        val start = fromIndex.coerceIn(0, pendingQueue.size)
        val end = toIndexExclusive.coerceIn(start, pendingQueue.size)
        if (start == end) return
        pendingQueue = pendingQueue.toMutableList().apply { subList(start, end).clear() }
        controller?.let { player ->
            val playerStart = fromIndex.coerceIn(0, player.mediaItemCount)
            val playerEnd = toIndexExclusive.coerceIn(playerStart, player.mediaItemCount)
            if (playerStart < playerEnd) player.removeMediaItems(playerStart, playerEnd)
        }
    }

    fun seekToQueueItem(entryId: String) {
        val index = pendingQueue.indexOfFirst { it.id == entryId }
        if (index >= 0) controller?.seekTo(index, 0L)
    }

    fun clearQueue() {
        pendingQueue = emptyList()
        pendingEntry = null
        pendingPositionMs = 0L
        controller?.clearMediaItems()
    }

    private fun toMediaItem(entry: QueueEntry): MediaItem {
        val file = entry.file
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
        return MediaItem.Builder()
            .setMediaId(entry.id)
            .setUri(file.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun List<QueueEntry>.withEntry(entry: QueueEntry): List<QueueEntry> =
        if (any { it.id == entry.id }) this else listOf(entry) + this

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
        pendingShuffleEnabled = enabled
        controller?.shuffleModeEnabled = false
    }

    fun setRepeatOneEnabled(enabled: Boolean) {
        pendingRepeatOneEnabled = enabled
        controller?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    fun release() {
        released = true
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
