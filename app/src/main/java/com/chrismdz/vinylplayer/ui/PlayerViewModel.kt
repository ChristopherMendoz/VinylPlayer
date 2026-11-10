package com.chrismdz.vinylplayer.ui

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.ArtistCatalogItem
import com.chrismdz.vinylplayer.data.ArtistImageRepository
import com.chrismdz.vinylplayer.data.ArtistImageWorkScheduler
import com.chrismdz.vinylplayer.data.ArtistLibraryRepository
import com.chrismdz.vinylplayer.data.ArtistTrackLink
import com.chrismdz.vinylplayer.data.sortArtistCatalog
import com.chrismdz.vinylplayer.data.QueueEntry
import com.chrismdz.vinylplayer.data.MusicRepository
import com.chrismdz.vinylplayer.data.AudioTagWriter
import com.chrismdz.vinylplayer.data.NeedsWriteApprovalException
import com.chrismdz.vinylplayer.data.Playlist
import com.chrismdz.vinylplayer.data.PlaylistOrigin
import com.chrismdz.vinylplayer.data.playlistimport.ImportReviewItem
import com.chrismdz.vinylplayer.data.playlistimport.MatchStatus
import com.chrismdz.vinylplayer.data.playlistimport.ParsedPlaylistFile
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportException
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportManager
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportResult
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportReview
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
import com.chrismdz.vinylplayer.data.RoomLyricsRepository
import com.chrismdz.vinylplayer.data.VinylDatabase
import com.chrismdz.vinylplayer.player.MusicPlayer
import com.chrismdz.vinylplayer.settings.AppSettings
import com.chrismdz.vinylplayer.settings.LibrarySortOrder
import com.chrismdz.vinylplayer.settings.QueueAddBehavior
import com.chrismdz.vinylplayer.settings.RepeatMode
import com.chrismdz.vinylplayer.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class PlayerUiState(
    val musicFile: MusicFile? = null,
    val library: List<MusicFile> = emptyList(),
    val queue: List<QueueEntry> = emptyList(),
    val currentQueueEntryId: String? = null,
    val playlists: List<Playlist> = emptyList(),
    val favoriteUris: Set<String> = emptySet(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyrics: Lyrics? = null,
    val lyricsStatus: LyricsStatus = LyricsStatus.IDLE,
    val lyricsOffsetMs: Long = 0L,
    val showTranslatedLyrics: Boolean = false,
    val translationLoading: Boolean = false,
    val translationError: String? = null,
    val isFavorite: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val isLibraryLoading: Boolean = false,
    val artistCatalog: List<ArtistCatalogItem> = emptyList(),
    val artistCatalogLoading: Boolean = true,
    val artistCatalogErrorMessage: String? = null,
    val artistImages: Map<String, Bitmap> = emptyMap(),
    val lastLibraryScanAt: Long? = null,
    val errorMessage: String? = null,
    val songTagErrorMessage: String? = null,
    val sessionRepeatMode: RepeatMode? = null,
    /** False until the saved library/queue have been read in the background at startup. */
    val sessionRestored: Boolean = true
) {
    val shuffleEnabled: Boolean get() = appSettings.shuffleEnabled
    val repeatMode: RepeatMode get() = sessionRepeatMode ?: appSettings.repeatMode
    val tonearmAnimationEnabled: Boolean get() = appSettings.tonearmAnimationEnabled
}

/** Steps of the playlist import flow; kept outside [PlayerUiState] so progress does not recompose the library. */
sealed interface PlaylistImportState {
    data object Idle : PlaylistImportState
    data class Working(val message: String) : PlaylistImportState
    data class Reviewing(val review: PlaylistImportReview, val nameError: String? = null) : PlaylistImportState
    data class Saving(val review: PlaylistImportReview) : PlaylistImportState
    data class Failed(val message: String) : PlaylistImportState
    data class Finished(val result: PlaylistImportResult) : PlaylistImportState
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private data class LibraryScanResult(
        val files: List<MusicFile>,
        val artistSyncError: String?
    )

    private data class PendingSongTagEdit(
        val file: MusicFile,
        val title: String,
        val artist: String,
        val album: String
    )

    private val repository = MusicRepository()
    private val database = VinylDatabase.get(application)
    private val artistLibraryRepository = ArtistLibraryRepository(database.artistDao())
    private val artistImageRepository = ArtistImageRepository(
        artistDao = database.artistDao(),
        providers = emptyList(),
        imageDirectory = File(application.filesDir, "artist-images")
    )
    private val artistImageWorkScheduler = ArtistImageWorkScheduler(application, database.artistDao())
    private val lyricsRepository = RoomLyricsRepository(database.lyricsDao())
    private val preferences = application.getSharedPreferences("vinylplayer_settings", 0)
    private val settingsPreferences = SettingsPreferences(application)
    private val initialSettings = settingsPreferences.load()
    private val favoriteUris = preferences.getStringSet("favorite_uris", emptySet()).orEmpty().toMutableSet()
    private class RestoredSession(
        val library: List<MusicFile>,
        val currentFile: MusicFile?,
        val queue: List<QueueEntry>,
        val currentEntryId: String?,
        val positionMs: Long,
        val lyricsOffsetMs: Long,
        val playlists: List<Playlist>
    )

    /** Reads the saved library, queue and current song. Pure preference parsing, safe off the main thread. */
    private fun restoreSession(): RestoredSession {
        val library = sortLibrary(loadCachedLibrary(), initialSettings.librarySortOrder)
        val currentFile = if (initialSettings.resumeLastSession) loadCachedCurrentFile() else null
        val persisted = if (initialSettings.keepQueueBetweenSessions) loadCachedQueue() else emptyList()
        val queue = when {
            persisted.isEmpty() -> currentFile?.let { listOf(QueueEntry.create(it)) }.orEmpty()
            currentFile == null -> persisted
            persisted.any { it.file.uri == currentFile.uri } -> persisted.map { entry ->
                if (entry.file.uri == currentFile.uri) entry.copy(file = currentFile) else entry
            }
            else -> listOf(QueueEntry.create(currentFile)) + persisted
        }
        val currentEntryId = preferences.getString("current_queue_entry_id", null)
            ?.takeIf { id -> initialSettings.resumeLastSession && queue.any { it.id == id } }
            ?: currentFile?.let { file -> queue.firstOrNull { it.file.uri == file.uri }?.id }
        return RestoredSession(
            library = library,
            currentFile = currentFile,
            queue = queue,
            currentEntryId = currentEntryId,
            positionMs = loadCachedCurrentPosition(currentFile),
            lyricsOffsetMs = loadLyricsOffset(currentFile),
            playlists = loadPlaylists()
        )
    }

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            favoriteUris = favoriteUris.toSet(),
            lastLibraryScanAt = preferences.getLong("last_library_scan_at", 0L).takeIf { it > 0L },
            appSettings = initialSettings,
            sessionRestored = false
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    /**
     * Parsing the saved library and queue (thousands of JSON rows) used to run in the constructor on the
     * main thread and froze startup for seconds. It now runs in the background; the player and the
     * library scan wait for it.
     */
    private val sessionRestore: Job = viewModelScope.launch {
        val restored = withContext(Dispatchers.Default) { restoreSession() }
        _playbackPositionMs.value = restored.positionMs
        _uiState.update { state ->
            state.copy(
                musicFile = restored.currentFile,
                library = state.library.ifEmpty { restored.library },
                queue = restored.queue,
                currentQueueEntryId = restored.currentEntryId,
                positionMs = restored.positionMs,
                lyricsOffsetMs = restored.lyricsOffsetMs,
                isFavorite = restored.currentFile?.uri?.toString()?.let { it in favoriteUris } == true,
                playlists = state.playlists.ifEmpty { restored.playlists },
                sessionRestored = true
            )
        }
    }

    private var musicPlayer: MusicPlayer? = null
    private var positionJob: Job? = null
    private var libraryJob: Job? = null
    private var lyricsJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var artistCatalogJob: Job? = null
    private var legacyArtistImageCleanupJob: Job? = null
    private var libraryLoaded = false
    private var initialLibraryCheck: Job? = null
    private var pendingForcedLibraryScan = false
    private val artworkLoads = mutableSetOf<String>()
    private val artistImageLoads = mutableSetOf<String>()
    private var pendingSongTagEdit: PendingSongTagEdit? = null
    private val playlistImportManager = PlaylistImportManager()
    private var playlistImportJob: Job? = null
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val cacheWriter = Dispatchers.Default.limitedParallelism(1)
    private var queueCacheJob: Job? = null
    private var libraryCacheJob: Job? = null
    private var parsedImport: ParsedPlaylistFile? = null
    private val _playlistImportState = MutableStateFlow<PlaylistImportState>(PlaylistImportState.Idle)
    val playlistImportState: StateFlow<PlaylistImportState> = _playlistImportState.asStateFlow()

    fun initialize() {
        if (!preferences.getBoolean(LEGACY_ARTIST_IMAGE_PREFETCH_CLEANUP_KEY, false) &&
            legacyArtistImageCleanupJob?.isActive != true
        ) {
            legacyArtistImageCleanupJob = viewModelScope.launch(Dispatchers.IO) {
                if (runCatching { artistImageWorkScheduler.cancelPendingPrefetch() }.isSuccess) {
                    preferences.edit().putBoolean(LEGACY_ARTIST_IMAGE_PREFETCH_CLEANUP_KEY, true).apply()
                }
            }
        }
        observeArtistCatalog()
        if (playerStartJob != null || musicPlayer != null) return
        playerStartJob = viewModelScope.launch {
            sessionRestore.join()
            startPlayer()
        }
    }

    private var playerStartJob: Job? = null

    private fun startPlayer() {
        if (musicPlayer == null) {
            musicPlayer = MusicPlayer(
                context = getApplication(),
                resumeOnConnect = _uiState.value.appSettings.resumeLastSession
            ) { player ->
                val previousState = _uiState.value
                val mediaId = player.currentMediaItem?.mediaId
                val currentEntry = previousState.queue.firstOrNull { it.id == mediaId }
                    ?: previousState.queue.firstOrNull { it.file.uri.toString() == mediaId }
                val current = currentEntry?.file ?: previousState.library.firstOrNull {
                    it.uri.toString() == mediaId
                } ?: previousState.musicFile
                val currentPosition = player.currentPosition.coerceAtLeast(0L)
                val trackChanged = currentEntry?.id != previousState.currentQueueEntryId ||
                    current?.uri != previousState.musicFile?.uri
                _playbackPositionMs.value = currentPosition

                _uiState.update { state ->
                    state.copy(
                        musicFile = current,
                        currentQueueEntryId = currentEntry?.id ?: state.currentQueueEntryId,
                        isPlaying = player.isPlaying,
                        positionMs = currentPosition,
                        durationMs = player.duration.takeIf { duration -> duration > 0 }
                            ?: current?.durationMs
                            ?: 0L,
                        lyricsOffsetMs = if (trackChanged && current != null) {
                            loadLyricsOffset(current)
                        } else {
                            state.lyricsOffsetMs
                        },
                        isFavorite = current?.uri?.toString()?.let { it in favoriteUris } == true,
                        favoriteUris = favoriteUris.toSet()
                    )
                }
                if (trackChanged) {
                    val currentState = _uiState.value
                    saveQueueCache(currentState.queue, currentState.currentQueueEntryId)
                    current?.let {
                        saveCurrentFile(it)
                        savePlaybackPosition(currentPosition)
                        loadLyrics(it)
                    }
                } else if (previousState.isPlaying && !player.isPlaying) {
                    savePlaybackPosition(currentPosition)
                }
                if (player.playbackState == Player.STATE_ENDED) handleTrackEnded()
            }

            val restoredState = _uiState.value
            musicPlayer?.replaceQueue(
                queue = restoredState.queue,
                currentEntryId = restoredState.currentQueueEntryId,
                positionMs = restoredState.positionMs,
                playWhenReady = restoredState.isPlaying,
                shuffleEnabled = restoredState.shuffleEnabled
            )
            musicPlayer?.setRepeatOneEnabled(restoredState.repeatMode == RepeatMode.ONE)

            var lastPositionSavedAt = android.os.SystemClock.elapsedRealtime()
            positionJob = viewModelScope.launch {
                while (isActive) {
                    val state = _uiState.value
                    if (state.isPlaying && state.musicFile != null) {
                        val position = musicPlayer?.currentPositionOrNull() ?: state.positionMs
                        _playbackPositionMs.value = position
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastPositionSavedAt >= POSITION_SAVE_INTERVAL_MS) {
                            savePlaybackPosition(position)
                            lastPositionSavedAt = now
                        }
                        delay(POSITION_POLL_INTERVAL_MS)
                    } else {
                        delay(POSITION_IDLE_INTERVAL_MS)
                    }
                }
            }
        }
        loadLibrary()
        _uiState.value.musicFile?.let(::loadLyrics)
    }

    /**
     * [force] = the user pressed Actualizar: rescan MediaStore. Without it, the saved library is used as
     * is; only a first run with nothing saved yet scans automatically.
     */
    fun loadLibrary(force: Boolean = false) {
        observeArtistCatalog()
        if (!force) {
            if (libraryLoaded || initialLibraryCheck?.isActive == true) return
            initialLibraryCheck = viewModelScope.launch {
                sessionRestore.join()
                if (_uiState.value.library.isEmpty()) loadLibrary(force = true) else libraryLoaded = true
            }
            return
        }
        if (libraryJob?.isActive == true) {
            if (force) pendingForcedLibraryScan = true
            return
        }
        if (libraryLoaded && !force) return
        if (force) pendingForcedLibraryScan = false
        _uiState.update { it.copy(isLibraryLoading = true) }
        val sortOrder = _uiState.value.appSettings.librarySortOrder
        val scanJob = viewModelScope.launch {
            sessionRestore.join()
            runCatching {
                withContext(Dispatchers.IO) {
                    val files = repository.readLibraryMetadata(getApplication())
                    val artistSyncError = runCatching {
                        artistLibraryRepository.refresh(files)
                    }
                        .exceptionOrNull()?.let { it.message ?: "No se pudo actualizar el catálogo" }
                    LibraryScanResult(sortLibrary(files, sortOrder), artistSyncError)
                }
            }.onSuccess { result ->
                val currentOrder = _uiState.value.appSettings.librarySortOrder
                val sortedFiles = if (currentOrder == sortOrder) result.files else sortLibrary(result.files, currentOrder)
                libraryLoaded = true
                saveLibraryCache(sortedFiles)
                val state = _uiState.value
                val creatingLibraryQueue = state.queue.size <= 1 && state.musicFile != null &&
                    state.sessionRepeatMode == null
                val nextQueue = if (creatingLibraryQueue) {
                    val currentEntry = state.queue.firstOrNull { it.id == state.currentQueueEntryId }
                        ?: QueueEntry.create(state.musicFile)
                    val fromLibrary = sortedFiles.map { file ->
                        if (file.uri == state.musicFile.uri) currentEntry.copy(file = file)
                        else QueueEntry.create(file)
                    }
                    if (fromLibrary.none { it.id == currentEntry.id }) listOf(currentEntry) + fromLibrary else fromLibrary
                } else {
                    // Map lookup: a linear search per entry froze the UI with long queues.
                    val filesByUri = sortedFiles.associateBy { it.uri.toString() }
                    state.queue.mapNotNull { entry ->
                        val file = filesByUri[entry.file.uri.toString()]
                        when {
                            file != null -> entry.copy(file = file)
                            entry.id == state.currentQueueEntryId -> entry
                            entry.file.uri.authority == "media" -> null
                            else -> entry
                        }
                    }
                }
                val activeIndex = nextQueue.indexOfFirst { it.id == state.currentQueueEntryId }
                val playbackOrder = if (state.shuffleEnabled && creatingLibraryQueue && activeIndex >= 0) {
                    nextQueue.take(activeIndex + 1) + nextQueue.drop(activeIndex + 1).shuffled()
                } else {
                    nextQueue
                }
                _uiState.update {
                    it.copy(
                        library = sortedFiles,
                        queue = playbackOrder,
                        isLibraryLoading = false,
                        lastLibraryScanAt = System.currentTimeMillis(),
                        errorMessage = null,
                        artistCatalogErrorMessage = result.artistSyncError?.let {
                            "No se pudo actualizar el catálogo de artistas"
                        }
                    )
                }
                preferences.edit().putLong("last_library_scan_at", _uiState.value.lastLibraryScanAt ?: 0L).apply()
                saveQueueCache(playbackOrder, state.currentQueueEntryId)
                if (playbackOrder.map { it.id } != state.queue.map { it.id }) {
                    musicPlayer?.replaceQueue(
                        queue = playbackOrder,
                        currentEntryId = state.currentQueueEntryId,
                        positionMs = musicPlayer?.currentPositionOrNull() ?: state.positionMs,
                        playWhenReady = state.isPlaying,
                        shuffleEnabled = state.shuffleEnabled
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLibraryLoading = false,
                        errorMessage = error.message ?: "No se pudo leer la biblioteca"
                    )
                }
            }
        }
        libraryJob = scanJob
        scanJob.invokeOnCompletion {
            viewModelScope.launch {
                if (libraryJob === scanJob) {
                    libraryJob = null
                    if (pendingForcedLibraryScan) {
                        pendingForcedLibraryScan = false
                        loadLibrary(force = true)
                    }
                }
            }
        }
    }

    fun loadArtwork(file: MusicFile) {
        val key = file.uri.toString()
        if (file.artwork != null || !artworkLoads.add(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            val artwork = repository.readArtwork(getApplication(), file.uri)
            withContext(Dispatchers.Main) {
                artworkLoads.remove(key)
                if (artwork != null) {
                    pendingArtworks[key] = artwork
                    if (artworkFlushJob?.isActive != true) {
                        artworkFlushJob = viewModelScope.launch {
                            delay(ARTWORK_BATCH_DELAY_MS)
                            flushArtworks()
                        }
                    }
                }
            }
        }
    }

    private val pendingArtworks = mutableMapOf<String, Bitmap>()
    private var artworkFlushJob: Job? = null

    /**
     * Applies every artwork decoded since the last flush in one state update. One update per artwork
     * re-copied the whole library and queue and recomposed the library for each visible row.
     * Unchanged items keep their instance, so state equality stays cheap.
     */
    private fun flushArtworks() {
        if (pendingArtworks.isEmpty()) return
        val loaded = pendingArtworks.toMap()
        pendingArtworks.clear()
        val updateFile: (MusicFile) -> MusicFile = { item ->
            loaded[item.uri.toString()]?.let { item.copy(artwork = it) } ?: item
        }
        _uiState.update { state ->
            state.copy(
                library = state.library.map(updateFile),
                queue = state.queue.map { entry ->
                    val updated = updateFile(entry.file)
                    if (updated === entry.file) entry else entry.copy(file = updated)
                },
                musicFile = state.musicFile?.let(updateFile)
            )
        }
    }

    private fun observeArtistCatalog() {
        if (artistCatalogJob?.isActive == true) return
        _uiState.update { it.copy(artistCatalogLoading = true) }
        artistCatalogJob = viewModelScope.launch {
            artistLibraryRepository.observeCatalog()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            artistCatalogLoading = false,
                            artistCatalogErrorMessage = error.message ?: "No se pudo cargar el catálogo de artistas"
                        )
                    }
                }
                .collect { artists ->
                    _uiState.update {
                        it.copy(
                            artistCatalog = sortArtistCatalog(artists),
                            artistCatalogLoading = false,
                            artistCatalogErrorMessage = null
                        )
                    }
                }
        }
    }

    fun observeArtistTrackLinks(artistId: String): kotlinx.coroutines.flow.Flow<List<ArtistTrackLink>> =
        artistLibraryRepository.observeTrackLinks(artistId)

    fun loadArtistImage(artist: ArtistCatalogItem) {
        if (_uiState.value.artistImages.containsKey(artist.artistId) ||
            !artistImageLoads.add(artist.artistId)
        ) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                legacyArtistImageCleanupJob?.join()
                val image = runCatching { artistImageRepository.loadCachedImage(artist.artistId) }.getOrNull()
                withContext(Dispatchers.Main) {
                    if (image != null) {
                        _uiState.update { current ->
                            val images = current.artistImages.toMutableMap()
                            images[artist.artistId] = image
                            while (images.size > MAX_CACHED_ARTIST_BITMAPS) images.remove(images.keys.first())
                            current.copy(artistImages = images)
                        }
                    }
                }
                if (image == null) {
                    runCatching {
                        artistImageWorkScheduler.scheduleArtist(
                            artist.artistId,
                            force = artist.imageStatus == com.chrismdz.vinylplayer.data.ArtistImageStatus.DOWNLOADED
                        )
                    }
                }
            } finally {
                artistImageLoads.remove(artist.artistId)
            }
        }
    }

    fun loadFullArtwork(file: MusicFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val artwork = repository.readFullArtwork(getApplication(), file.uri) ?: return@launch
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val updateFile: (MusicFile) -> MusicFile = { item ->
                        if (item.uri == file.uri) item.copy(artwork = artwork) else item
                    }
                    state.copy(
                        library = state.library.map(updateFile),
                        queue = state.queue.map { it.copy(file = updateFile(it.file)) },
                        musicFile = state.musicFile?.let(updateFile)
                    )
                }
            }
        }
    }

    fun selectMusic(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.readMetadata(getApplication(), uri)
                }
            }.onSuccess { musicFile ->
                val entry = QueueEntry.create(musicFile)
                clearAlbumPlaybackMode()
                playFile(entry, listOf(entry), false)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo abrir el archivo")
                }
            }
        }
    }

    fun playFromLibrary(file: MusicFile) {
        val currentState = _uiState.value
        if (currentState.musicFile?.uri == file.uri && currentState.sessionRepeatMode == null) return
        clearAlbumPlaybackMode()
        val state = _uiState.value
        val queue = state.library.ifEmpty { listOf(file) }.map { QueueEntry.create(it) }
        val selected = queue.firstOrNull { it.file.uri == file.uri } ?: QueueEntry.create(file)
        val baseQueue = if (selected in queue) queue else queue + selected
        val selectedIndex = baseQueue.indexOfFirst { it.id == selected.id }
        val playbackOrder = if (state.shuffleEnabled) {
            baseQueue.take(selectedIndex + 1) + baseQueue.drop(selectedIndex + 1).shuffled()
        } else baseQueue
        playSelected(selected, playbackOrder)
    }

    fun playAlbum(files: List<MusicFile>, shuffle: Boolean = false) {
        startGroupedPlayback(files, shuffle, null)
    }

    fun playGroupedTrack(files: List<MusicFile>, startingFile: MusicFile) {
        startGroupedPlayback(files, shuffle = false, startingFile = startingFile)
    }

    private fun startGroupedPlayback(files: List<MusicFile>, shuffle: Boolean, startingFile: MusicFile?) {
        val entries = files.distinctBy { it.uri }.map(QueueEntry::create)
        if (entries.isEmpty()) return
        val ordered = if (shuffle) entries.shuffled() else entries
        val selected = startingFile?.let { file -> ordered.firstOrNull { it.file.uri == file.uri } }
            ?: ordered.first()
        _uiState.update { it.copy(sessionRepeatMode = RepeatMode.ALL) }
        musicPlayer?.setRepeatOneEnabled(false)
        playSelected(selected, ordered)
    }

    fun playFromQueue(entry: QueueEntry) {
        if (_uiState.value.currentQueueEntryId == entry.id) return
        playSelected(entry, _uiState.value.queue)
    }

    private fun clearAlbumPlaybackMode() {
        val state = _uiState.value
        if (state.sessionRepeatMode == null) return
        _uiState.update { it.copy(sessionRepeatMode = null) }
        musicPlayer?.setRepeatOneEnabled(state.appSettings.repeatMode == RepeatMode.ONE)
    }

    fun addToQueue(file: MusicFile) {
        val state = _uiState.value
        if (!state.appSettings.allowQueueDuplicates && state.queue.any { it.file.uri == file.uri }) return
        if (state.appSettings.queueAddBehavior == QueueAddBehavior.PLAY_NEXT) {
            playNext(file)
            return
        }
        val entry = QueueEntry.create(file)
        val queue = state.queue + entry
        musicPlayer?.addQueueEntry(entry, queue.lastIndex)
        _uiState.update { it.copy(queue = queue) }
        saveQueueCache(queue, _uiState.value.currentQueueEntryId)
    }

    fun moveInQueue(itemId: String, targetId: String?, afterTarget: Boolean) {
        val state = _uiState.value
        val from = state.queue.indexOfFirst { it.id == itemId }
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
        if (from < 0 || from <= currentIndex) return
        val remaining = state.queue.toMutableList().apply { removeAt(from) }
        val anchorIndex = targetId?.let { id -> remaining.indexOfFirst { it.id == id } } ?: -1
        if (targetId != null && anchorIndex < 0) return
        val minimumIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        val destination = (if (targetId == null) remaining.size else anchorIndex + if (afterTarget) 1 else 0)
            .coerceIn(minimumIndex, remaining.size)
        val updated = remaining.toMutableList().apply { add(destination, state.queue[from]) }
        if (destination != from) {
            musicPlayer?.moveQueueEntry(from, destination)
            _uiState.update { it.copy(queue = updated) }
            saveQueueCache(updated, state.currentQueueEntryId)
        }
    }

    fun removeFromQueue(entryId: String) {
        val state = _uiState.value
        val index = state.queue.indexOfFirst { it.id == entryId }
        if (index < 0) return
        val removingCurrent = state.currentQueueEntryId == entryId
        val fallback = if (removingCurrent) {
            state.queue.getOrNull(index + 1)
                ?: state.queue.getOrNull(index - 1)
        } else null
        if (removingCurrent && fallback == null) {
            musicPlayer?.clearQueue()
            clearCurrentPlaybackCache()
            lyricsJob?.cancel()
            _uiState.update {
                it.copy(
                    queue = emptyList(),
                    currentQueueEntryId = null,
                    musicFile = null,
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    isFavorite = false,
                    lyrics = null,
                    lyricsStatus = LyricsStatus.IDLE,
                    showTranslatedLyrics = false,
                    translationLoading = false,
                    translationError = null
                )
            }
            saveQueueCache(emptyList(), null)
            return
        }
        if (removingCurrent) fallback?.let { musicPlayer?.seekToQueueItem(it.id) }
        musicPlayer?.removeQueueItems(index, index + 1)
        _uiState.update { current ->
            current.copy(
                queue = current.queue.filterNot { it.id == entryId },
                currentQueueEntryId = if (removingCurrent) fallback?.id else current.currentQueueEntryId,
                musicFile = if (removingCurrent) fallback?.file else current.musicFile,
                positionMs = if (removingCurrent) 0L else current.positionMs,
                durationMs = if (removingCurrent) fallback?.file?.durationMs ?: 0L else current.durationMs,
                lyrics = if (removingCurrent) null else current.lyrics,
                lyricsStatus = if (removingCurrent) LyricsStatus.IDLE else current.lyricsStatus
            )
        }
        saveQueueCache(_uiState.value.queue, _uiState.value.currentQueueEntryId)
        if (removingCurrent) fallback?.file?.let {
            saveCurrentFile(it)
            loadLyrics(it)
        }
    }

    fun clearPendingQueue() {
        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
        val firstPending = if (currentIndex >= 0) currentIndex + 1 else 0
        if (firstPending >= state.queue.size) return
        musicPlayer?.removeQueueItems(firstPending, state.queue.size)
        _uiState.update {
            it.copy(queue = if (currentIndex >= 0) it.queue.take(currentIndex + 1) else emptyList())
        }
        saveQueueCache(_uiState.value.queue, _uiState.value.currentQueueEntryId)
    }

    fun editSongTags(
        file: MusicFile,
        title: String,
        artist: String,
        album: String,
        onNeedsConfirmation: (PendingIntent) -> Unit
    ) {
        val cleanTitle = title.trim()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update { it.copy(songTagErrorMessage = "La edición segura de etiquetas requiere Android 10 o posterior") }
            return
        }
        if (cleanTitle.isEmpty()) {
            _uiState.update { it.copy(songTagErrorMessage = "El título de la canción no puede estar vacío") }
            return
        }
        val edit = PendingSongTagEdit(file, cleanTitle, artist.trim(), album.trim())
        _uiState.update { it.copy(songTagErrorMessage = null) }
        viewModelScope.launch {
            val supported = withContext(Dispatchers.IO) { AudioTagWriter.supports(getApplication(), file.uri) }
            if (!supported) {
                _uiState.update { it.copy(songTagErrorMessage = "Este formato no permite editar etiquetas") }
                return@launch
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val request = runCatching {
                    withContext(Dispatchers.IO) {
                        MediaStore.createWriteRequest(getApplication<Application>().contentResolver, listOf(file.uri))
                    }
                }.getOrElse { error ->
                    _uiState.update { it.copy(songTagErrorMessage = error.message ?: "Android no pudo solicitar permiso de escritura") }
                    return@launch
                }
                pendingSongTagEdit = edit
                onNeedsConfirmation(request)
            } else {
                saveSongTags(edit, onNeedsConfirmation)
            }
        }
    }

    fun completeSongTagWrite(granted: Boolean, onNeedsConfirmation: (PendingIntent) -> Unit = {}) {
        val edit = pendingSongTagEdit ?: return
        pendingSongTagEdit = null
        if (!granted) {
            _uiState.update { it.copy(songTagErrorMessage = "No se guardaron cambios en la canción") }
            return
        }
        saveSongTags(edit, onNeedsConfirmation)
    }

    private fun saveSongTags(edit: PendingSongTagEdit, onNeedsConfirmation: (PendingIntent) -> Unit) {
        _uiState.update { it.copy(songTagErrorMessage = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    AudioTagWriter.write(
                        getApplication(), edit.file.uri, edit.title, edit.artist, edit.album
                    )
                    repository.readMetadata(getApplication(), edit.file.uri)
                }
            }.onSuccess { indexed ->
                val updatedFile = indexed.copy(
                    title = edit.title,
                    artist = edit.artist,
                    album = edit.album,
                    artwork = indexed.artwork ?: edit.file.artwork,
                    artworkData = indexed.artworkData ?: edit.file.artworkData,
                    filePath = indexed.filePath ?: edit.file.filePath
                )
                val state = _uiState.value
                val updatedLibrary = state.library.map { if (it.uri == edit.file.uri) updatedFile else it }
                val updatedQueue = state.queue.map { entry ->
                    if (entry.file.uri == edit.file.uri) entry.copy(file = updatedFile) else entry
                }
                val updatedCurrent = state.musicFile?.let { if (it.uri == edit.file.uri) updatedFile else it }
                val artistSyncError = runCatching {
                    withContext(Dispatchers.IO) {
                        artistLibraryRepository.refresh(updatedLibrary)
                    }
                }.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        library = updatedLibrary,
                        queue = updatedQueue,
                        musicFile = updatedCurrent,
                        songTagErrorMessage = null,
                        artistCatalogErrorMessage = artistSyncError?.let {
                            "No se pudo actualizar el catálogo de artistas"
                        }
                    )
                }
                musicPlayer?.updateQueueMetadata(updatedQueue)
                saveLibraryCache(updatedLibrary)
                saveQueueCache(updatedQueue, state.currentQueueEntryId)
                updatedCurrent?.let(::saveCurrentFile)
            }.onFailure { error ->
                if (error is NeedsWriteApprovalException) {
                    pendingSongTagEdit = edit
                    onNeedsConfirmation(error.pendingIntent)
                    return@onFailure
                }
                val message = when (error) {
                    is SecurityException -> "Android no concedió permiso para modificar este archivo"
                    is UnsupportedOperationException -> error.message ?: "Este formato no permite editar etiquetas"
                    else -> error.message ?: "No se pudieron guardar las etiquetas"
                }
                _uiState.update { it.copy(songTagErrorMessage = message) }
            }
        }
    }

    private data class DeleteAttempt(val deleted: Boolean, val request: PendingIntent? = null)

    fun deleteSong(file: MusicFile, onNeedsConfirmation: (PendingIntent) -> Unit = {}) {
        viewModelScope.launch {
            val attempt = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    if (resolver.delete(file.uri, null, null) > 0) {
                        DeleteAttempt(deleted = true)
                    } else {
                        DeleteAttempt(
                            deleted = false,
                            request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                MediaStore.createDeleteRequest(resolver, listOf(file.uri))
                            } else null
                        )
                    }
                }.getOrElse {
                    DeleteAttempt(
                        deleted = false,
                        request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            runCatching {
                                MediaStore.createDeleteRequest(
                                    getApplication<Application>().contentResolver,
                                    listOf(file.uri)
                                )
                            }.getOrNull()
                        } else null
                    )
                }
            }
            if (attempt.request != null) {
                onNeedsConfirmation(attempt.request)
                return@launch
            }
            if (!attempt.deleted) {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar la canción") }
                return@launch
            }
            removeDeletedSong(file)
        }
    }

    fun removeDeletedSong(file: MusicFile) {
        viewModelScope.launch {
            val stateBeforeDelete = _uiState.value
            val updatedPlaylists = _uiState.value.playlists.map { playlist ->
                playlist.copy(songUris = playlist.songUris.filterNot { it == file.uri.toString() })
            }
            savePlaylists(updatedPlaylists)
            favoriteUris.remove(file.uri.toString())
            preferences.edit().putStringSet("favorite_uris", favoriteUris).apply()
            val currentWasDeleted = stateBeforeDelete.musicFile?.uri == file.uri
            val currentIndex = stateBeforeDelete.queue.indexOfFirst { it.id == stateBeforeDelete.currentQueueEntryId }
            val fallbackEntry = if (currentWasDeleted) {
                stateBeforeDelete.queue.drop(currentIndex + 1).firstOrNull { it.file.uri != file.uri }
                    ?: stateBeforeDelete.queue.take(currentIndex.coerceAtLeast(0)).lastOrNull { it.file.uri != file.uri }
            } else null
            if (currentWasDeleted) musicPlayer?.pause()
            val removedIndices = stateBeforeDelete.queue.indices
                .filter { stateBeforeDelete.queue[it].file.uri == file.uri }
                .sortedDescending()
            removedIndices.forEach { musicPlayer?.removeQueueItems(it, it + 1) }
            if (currentWasDeleted) {
                if (fallbackEntry == null) musicPlayer?.clearQueue()
                else musicPlayer?.seekToQueueItem(fallbackEntry.id)
            }
            _uiState.update { state ->
                val remainingQueue = state.queue.filterNot { it.file.uri == file.uri }
                state.copy(
                    library = state.library.filterNot { it.uri == file.uri },
                    queue = remainingQueue,
                    currentQueueEntryId = if (currentWasDeleted) fallbackEntry?.id else state.currentQueueEntryId,
                    musicFile = if (currentWasDeleted) fallbackEntry?.file else state.musicFile,
                    isPlaying = if (currentWasDeleted) false else state.isPlaying,
                    positionMs = if (currentWasDeleted) 0L else state.positionMs,
                    durationMs = if (currentWasDeleted) fallbackEntry?.file?.durationMs ?: 0L else state.durationMs,
                    lyrics = if (currentWasDeleted) null else state.lyrics,
                    lyricsStatus = if (currentWasDeleted) LyricsStatus.IDLE else state.lyricsStatus,
                    favoriteUris = favoriteUris.toSet(),
                    isFavorite = if (currentWasDeleted) false else state.isFavorite,
                    errorMessage = null
                )
            }
            val artistSyncError = runCatching {
                withContext(Dispatchers.IO) {
                    artistLibraryRepository.refresh(_uiState.value.library)
                }
            }.exceptionOrNull()
            if (artistSyncError != null) {
                _uiState.update {
                    it.copy(artistCatalogErrorMessage = "No se pudo actualizar el catálogo de artistas")
                }
            }
            saveQueueCache(_uiState.value.queue, _uiState.value.currentQueueEntryId)
            if (currentWasDeleted) {
                if (fallbackEntry != null) {
                    saveCurrentFile(fallbackEntry.file)
                    loadLyrics(fallbackEntry.file)
                } else {
                    clearCurrentPlaybackCache()
                }
            }
        }
    }

    fun playNext(file: MusicFile) {
        val state = _uiState.value
        if (state.musicFile?.uri == file.uri) return
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
        val insertAt = if (currentIndex >= 0) currentIndex + 1 else state.queue.size
        val existingIndex = state.queue.indexOfFirst { it.file.uri == file.uri }
        if (!state.appSettings.allowQueueDuplicates && existingIndex >= 0 && existingIndex < insertAt) return
        if (existingIndex >= insertAt) {
            if (existingIndex == insertAt) return
            val updated = state.queue.toMutableList().apply {
                add(insertAt, removeAt(existingIndex))
            }
            musicPlayer?.moveQueueEntry(existingIndex, insertAt)
            _uiState.update { it.copy(queue = updated) }
            saveQueueCache(updated, state.currentQueueEntryId)
        } else {
            val entry = QueueEntry.create(file)
            val updated = state.queue.toMutableList().apply { add(insertAt, entry) }
            musicPlayer?.addQueueEntry(entry, insertAt)
            _uiState.update { it.copy(queue = updated) }
            saveQueueCache(updated, state.currentQueueEntryId)
        }
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || _uiState.value.playlists.any { it.name.equals(cleanName, true) }) return
        savePlaylists(_uiState.value.playlists + Playlist(cleanName, emptyList()))
    }

    fun addToPlaylist(name: String, file: MusicFile) {
        val updated = _uiState.value.playlists.map { playlist ->
            if (playlist.name == name && file.uri.toString() !in playlist.songUris) {
                playlist.copy(songUris = playlist.songUris + file.uri.toString())
            } else playlist
        }
        savePlaylists(updated)
    }

    fun removeFromPlaylist(name: String, file: MusicFile) {
        savePlaylists(_uiState.value.playlists.map { playlist ->
            if (playlist.name == name) playlist.copy(songUris = playlist.songUris - file.uri.toString()) else playlist
        })
    }

    fun deletePlaylist(name: String) {
        savePlaylists(_uiState.value.playlists.filterNot { it.name == name })
    }

    /**
     * Saves the result of the playlist editor: new name, new order and removed entries in one write.
     * Returns an error message, or null when it was stored.
     */
    fun updatePlaylist(originalName: String, newName: String, songUris: List<String>): String? {
        val name = newName.trim()
        val playlists = _uiState.value.playlists
        val original = playlists.firstOrNull { it.name == originalName } ?: return "La playlist ya no existe."
        when {
            name.isEmpty() -> return "Escribe un nombre para la playlist."
            name.equals("Favoritos", ignoreCase = true) || name.equals("Favorito", ignoreCase = true) -> return "Ese nombre está reservado."
            playlists.any { it !== original && it.name.equals(name, ignoreCase = true) } -> return "Ya existe una playlist con ese nombre."
        }
        savePlaylists(playlists.map { if (it === original) it.copy(name = name, songUris = songUris) else it })
        return null
    }

    /** Appends songs chosen in the playlist's "Agregar" picker, keeping the order they were picked. */
    fun addSongsToPlaylist(name: String, uris: List<String>) {
        if (uris.isEmpty()) return
        savePlaylists(_uiState.value.playlists.map { playlist ->
            if (playlist.name == name) playlist.copy(songUris = playlist.songUris + uris) else playlist
        })
    }

    /** Removes one occurrence by its position, so repeated songs keep their other appearances. */
    fun removeFromPlaylistAt(name: String, position: Int) {
        savePlaylists(_uiState.value.playlists.map { playlist ->
            if (playlist.name == name && position in playlist.songUris.indices) {
                playlist.copy(songUris = playlist.songUris.toMutableList().apply { removeAt(position) })
            } else playlist
        })
    }

    /** Plays a playlist in its stored order, duplicates included, starting at [startIndex]. */
    fun playPlaylist(files: List<MusicFile>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (files.isEmpty()) return
        clearAlbumPlaybackMode()
        val entries = files.map(QueueEntry::create)
        val start = startIndex.coerceIn(0, entries.lastIndex)
        val (selected, order) = when {
            shuffle -> entries.shuffled().let { it.first() to it }
            _uiState.value.shuffleEnabled -> entries[start] to (entries.take(start + 1) + entries.drop(start + 1).shuffled())
            else -> entries[start] to entries
        }
        playSelected(selected, order)
    }

    /** Appends a whole playlist to the end of the queue in its order. */
    fun addPlaylistToQueue(files: List<MusicFile>) {
        val state = _uiState.value
        val toAdd = if (state.appSettings.allowQueueDuplicates) files else {
            val present = state.queue.mapTo(hashSetOf()) { it.file.uri }
            files.filter { present.add(it.uri) }
        }
        if (toAdd.isEmpty()) return
        val entries = toAdd.map(QueueEntry::create)
        musicPlayer?.addQueueEntries(entries, state.queue.size)
        val queue = state.queue + entries
        _uiState.update { it.copy(queue = queue) }
        saveQueueCache(queue, _uiState.value.currentQueueEntryId)
    }

    fun startPlaylistImport(uri: Uri) {
        playlistImportJob?.cancel()
        parsedImport = null
        _playlistImportState.value = PlaylistImportState.Working("Leyendo archivo…")
        playlistImportJob = viewModelScope.launch {
            runCatching {
                val document = withContext(Dispatchers.IO) { playlistImportManager.readDocument(getApplication(), uri) }
                _playlistImportState.value = PlaylistImportState.Working("Detectando formato…")
                val parsed = withContext(Dispatchers.Default) { playlistImportManager.detectAndParse(document) }
                parsedImport = parsed
                _playlistImportState.value = PlaylistImportState.Working("Reconociendo canciones locales…")
                buildImportReview(parsed, 0, document.fileName)
            }.onSuccess { review ->
                _playlistImportState.value = PlaylistImportState.Reviewing(review.withUniqueName())
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                parsedImport = null
                _playlistImportState.value = PlaylistImportState.Failed(
                    if (error is PlaylistImportException) error.message.orEmpty()
                    else {
                        android.util.Log.e("PlaylistImport", "Import failed", error)
                        val root = generateSequence(error) { it.cause }.last()
                        "Error interno al analizar el archivo (${root.javaClass.simpleName}: ${root.message ?: "sin detalle"})."
                    }
                )
            }
        }
    }

    private suspend fun buildImportReview(parsed: ParsedPlaylistFile, index: Int, fileName: String?): PlaylistImportReview {
        val library = _uiState.value.library
        val knownLinks = _uiState.value.playlists
            .mapNotNull { it.origin?.links }
            .fold(mutableMapOf<String, String>()) { all, links -> all.apply { putAll(links) } }
        return withContext(Dispatchers.Default) {
            playlistImportManager.buildReview(parsed, index, fileName, library, knownLinks)
        }
    }

    /** For files holding several playlists (e.g. an iTunes library); recomputes matches for the choice. */
    fun selectImportedPlaylist(index: Int) {
        val parsed = parsedImport ?: return
        val current = (_playlistImportState.value as? PlaylistImportState.Reviewing)?.review ?: return
        if (index == current.selectedPlaylistIndex) return
        playlistImportJob?.cancel()
        _playlistImportState.value = PlaylistImportState.Working("Reconociendo canciones locales…")
        playlistImportJob = viewModelScope.launch {
            val review = buildImportReview(parsed, index, current.fileName)
            _playlistImportState.value = PlaylistImportState.Reviewing(review.withUniqueName())
        }
    }

    fun renameImportedPlaylist(name: String) {
        updateImportReview { it.copy(playlistName = name.take(PlaylistImportManager.MAX_NAME_LENGTH)) }
    }

    fun linkImportEntry(index: Int, uri: String) {
        if (_uiState.value.library.none { it.uri.toString() == uri }) return
        updateImportItem(index) { it.copy(status = MatchStatus.MANUALLY_LINKED, selectedUri = uri, excluded = false) }
    }

    fun setImportEntryExcluded(index: Int, excluded: Boolean) {
        updateImportItem(index) { it.copy(excluded = excluded) }
    }

    fun resetImportEntry(index: Int) {
        updateImportItem(index) { ImportReviewItem(it.entry, it.autoMatch) }
    }

    fun excludeMissingImportEntries() {
        updateImportReview { review ->
            review.copy(items = review.items.map { item ->
                if (!item.excluded && (item.status == MatchStatus.NOT_FOUND || item.status == MatchStatus.MATCH_AMBIGUOUS)) {
                    item.copy(excluded = true)
                } else item
            })
        }
    }

    fun cancelPlaylistImport() {
        playlistImportJob?.cancel()
        playlistImportJob = null
        parsedImport = null
        _playlistImportState.value = PlaylistImportState.Idle
    }

    /**
     * Stores the reviewed playlist in one write. Nothing is persisted before this point, so cancelling
     * never leaves partial data; if the write fails the in-memory list is left untouched.
     */
    fun confirmPlaylistImport() {
        val review = (_playlistImportState.value as? PlaylistImportState.Reviewing)?.review ?: return
        val name = review.playlistName.trim()
        validateImportName(name)?.let { error ->
            _playlistImportState.value = PlaylistImportState.Reviewing(review, error)
            return
        }
        if (review.ambiguousCount > 0) {
            _playlistImportState.value = PlaylistImportState.Reviewing(review, "Resuelve o excluye las coincidencias ambiguas.")
            return
        }
        val libraryUris = _uiState.value.library.mapTo(hashSetOf()) { it.uri.toString() }
        val accepted = review.items.filter { it.willImport && it.selectedUri in libraryUris }
        if (accepted.isEmpty()) {
            _playlistImportState.value = PlaylistImportState.Reviewing(review, "No hay canciones locales para importar.")
            return
        }
        val playlist = Playlist(
            name = name,
            songUris = accepted.map { it.selectedUri!! },
            origin = PlaylistOrigin(
                source = review.source.name,
                format = review.format.name,
                fileName = review.fileName,
                importedAt = System.currentTimeMillis(),
                totalEntries = review.total,
                links = accepted.mapNotNull { item -> item.entry.referenceKey?.let { it to item.selectedUri!! } }.toMap()
            )
        )
        _playlistImportState.value = PlaylistImportState.Saving(review)
        playlistImportJob = viewModelScope.launch {
            val before = _uiState.value.playlists
            val updated = before + playlist
            // Same ordered lane as other playlist writes, so an older pending write cannot land after this one.
            val stored = withContext(cacheWriter) {
                preferences.edit().putString("playlists", encodePlaylists(updated)).commit()
            }
            if (!stored) {
                _playlistImportState.value = PlaylistImportState.Reviewing(review, "No se pudo guardar la playlist. Inténtalo de nuevo.")
                return@launch
            }
            val current = _uiState.value.playlists
            if (current == before) {
                _uiState.update { it.copy(playlists = updated) }
            } else {
                // Another playlist edit happened during the write: merge and persist both.
                savePlaylists(current.filterNot { it.name.equals(playlist.name, ignoreCase = true) } + playlist)
            }
            parsedImport = null
            _playlistImportState.value = PlaylistImportState.Finished(
                PlaylistImportResult(
                    playlistName = playlist.name,
                    source = review.source,
                    format = review.format,
                    totalEntries = review.total,
                    importedCount = accepted.size,
                    manuallyLinkedCount = accepted.count { it.status == MatchStatus.MANUALLY_LINKED },
                    skippedTitles = review.items.filterNot { it.willImport && it.selectedUri in libraryUris }
                        .map { it.entry.displayTitle }
                )
            )
        }
    }

    fun dismissPlaylistImportResult() {
        _playlistImportState.value = PlaylistImportState.Idle
    }

    private fun validateImportName(name: String): String? = when {
        name.isEmpty() -> "Escribe un nombre para la playlist."
        name.equals("Favoritos", ignoreCase = true) || name.equals("Favorito", ignoreCase = true) ->
            "Ese nombre está reservado."
        _uiState.value.playlists.any { it.name.equals(name, ignoreCase = true) } -> "Ya existe una playlist con ese nombre."
        else -> null
    }

    private fun PlaylistImportReview.withUniqueName(): PlaylistImportReview {
        val base = playlistName.trim().ifEmpty { "Playlist importada" }
        if (validateImportName(base) == null) return copy(playlistName = base)
        val unique = generateSequence(2) { it + 1 }.map { "$base ($it)" }.first { validateImportName(it) == null }
        return copy(playlistName = unique)
    }

    private fun updateImportReview(transform: (PlaylistImportReview) -> PlaylistImportReview) {
        val state = _playlistImportState.value as? PlaylistImportState.Reviewing ?: return
        _playlistImportState.value = PlaylistImportState.Reviewing(transform(state.review))
    }

    private fun updateImportItem(index: Int, transform: (ImportReviewItem) -> ImportReviewItem) {
        updateImportReview { review ->
            if (index !in review.items.indices) review
            else review.copy(items = review.items.toMutableList().apply { this[index] = transform(this[index]) })
        }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        _uiState.update { it.copy(playlists = playlists) }
        // Encoded on the ordered background lane: imported playlists carry large link tables.
        playlistCacheJob?.cancel()
        playlistCacheJob = viewModelScope.launch(cacheWriter) {
            preferences.edit().putString("playlists", encodePlaylists(playlists)).apply()
        }
    }

    private var playlistCacheJob: Job? = null

    /** `[name, [uris], origin?]` per playlist; the optional third element keeps older readers compatible. */
    private fun encodePlaylists(playlists: List<Playlist>): String {
        val json = JSONArray()
        playlists.forEach { playlist ->
            json.put(JSONArray().apply {
                put(playlist.name)
                put(JSONArray(playlist.songUris))
                playlist.origin?.let { origin ->
                    put(JSONObject().apply {
                        put("source", origin.source)
                        put("format", origin.format)
                        origin.fileName?.let { put("fileName", it) }
                        put("importedAt", origin.importedAt)
                        put("totalEntries", origin.totalEntries)
                        put("links", JSONObject().apply { origin.links.forEach { (key, uri) -> put(key, uri) } })
                    })
                }
            })
        }
        return json.toString()
    }

    private fun decodeOrigin(json: JSONObject?): PlaylistOrigin? {
        json ?: return null
        val links = json.optJSONObject("links")?.let { linksJson ->
            linksJson.keys().asSequence().associateWith { linksJson.getString(it) }
        }.orEmpty()
        return PlaylistOrigin(
            source = json.optString("source"),
            format = json.optString("format"),
            fileName = json.optString("fileName").takeIf(String::isNotBlank),
            importedAt = json.optLong("importedAt"),
            totalEntries = json.optInt("totalEntries"),
            links = links
        )
    }

    private fun loadPlaylists(): List<Playlist> {
        val value = preferences.getString("playlists", null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(value)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val item = json.getJSONArray(index)
                    val songs = item.getJSONArray(1)
                    add(Playlist(item.getString(0), buildList(songs.length()) {
                        for (songIndex in 0 until songs.length()) add(songs.getString(songIndex))
                    }, decodeOrigin(item.optJSONObject(2))))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun playSelected(entry: QueueEntry, queue: List<QueueEntry>) {
        val currentState = _uiState.value
        if (currentState.currentQueueEntryId != entry.id) {
            savePlaybackPosition(_playbackPositionMs.value)
        }
        playFile(entry, queue, true)
    }

    private fun playFile(entry: QueueEntry, queue: List<QueueEntry>, playWhenReady: Boolean) {
        val file = entry.file
        saveCurrentFile(file)
        _playbackPositionMs.value = 0L
        _uiState.update {
            it.copy(
                musicFile = file,
                queue = queue,
                currentQueueEntryId = entry.id,
                favoriteUris = favoriteUris.toSet(),
                isPlaying = playWhenReady,
                positionMs = 0L,
                durationMs = file.durationMs,
                lyricsOffsetMs = loadLyricsOffset(file),
                lyrics = null,
                lyricsStatus = LyricsStatus.IDLE,
                isFavorite = file.uri.toString() in favoriteUris,
                errorMessage = null
            )
        }
        savePlaybackPosition(0L)
        saveQueueCache(queue, entry.id)
        musicPlayer?.load(entry, queue, playWhenReady, _uiState.value.shuffleEnabled)
        loadLyrics(file)
    }

    private fun loadLyrics(file: MusicFile) {
        lyricsJob?.cancel()
        _uiState.update {
            it.copy(
                lyrics = null,
                lyricsStatus = LyricsStatus.LOADING,
                showTranslatedLyrics = false,
                translationLoading = false,
                translationError = null
            )
        }
        lyricsJob = viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                lyricsRepository.getLyrics(file.uri.toString())
            }
            val cacheAge = cached?.let { System.currentTimeMillis() - it.fetchedAt } ?: Long.MAX_VALUE
            if (cached != null && cacheAge in 0..LYRICS_CACHE_DURATION_MS) {
                _uiState.updateIfCurrent(file) {
                    it.copy(
                        lyrics = cached,
                        lyricsStatus = if (cached.found) LyricsStatus.AVAILABLE else LyricsStatus.NOT_FOUND,
                        showTranslatedLyrics = false,
                        translationLoading = false,
                        translationError = null
                    )
                }
                return@launch
            }
            if (!hasUsableLyricsMetadata(file)) {
                _uiState.updateIfCurrent(file) {
                    it.copy(lyricsStatus = LyricsStatus.INSUFFICIENT_METADATA)
                }
                return@launch
            }
            if (!hasNetwork()) {
                _uiState.updateIfCurrent(file) {
                    it.copy(lyricsStatus = LyricsStatus.OFFLINE)
                }
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    lyricsRepository.fetchAndSaveLyrics(file)
                }
            }.onSuccess { lyrics ->
                _uiState.updateIfCurrent(file) {
                    it.copy(
                        lyrics = lyrics,
                        lyricsStatus = if (lyrics.found) LyricsStatus.AVAILABLE else LyricsStatus.NOT_FOUND,
                        showTranslatedLyrics = false,
                        translationLoading = false,
                        translationError = null
                    )
                }
            }.onFailure {
                _uiState.updateIfCurrent(file) {
                    it.copy(lyricsStatus = LyricsStatus.TEMPORARY_ERROR)
                }
            }
        }
    }

    fun toggleLyricsTranslation() {
        val state = _uiState.value
        if (!state.appSettings.lyricsTranslationEnabled) return
        val file = state.musicFile ?: return
        val lyrics = state.lyrics ?: return
        val targetLanguage = state.appSettings.translationLanguage
        if (state.showTranslatedLyrics) {
            _uiState.update { it.copy(showTranslatedLyrics = false, translationError = null) }
            return
        }
        val hasTranslation = !lyrics.translatedText.isNullOrBlank() || !lyrics.translatedSyncedText.isNullOrBlank()
        if (hasTranslation && lyrics.translatedLanguage == targetLanguage.code) {
            _uiState.update { it.copy(showTranslatedLyrics = true, translationError = null) }
            return
        }
        if (!hasNetwork()) {
            _uiState.update { it.copy(translationError = "Conéctate a internet para traducir la letra") }
            return
        }
        _uiState.update { it.copy(translationLoading = true, translationError = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { lyricsRepository.translateAndSaveLyrics(file, targetLanguage.code) }
            }.onSuccess { translated ->
                _uiState.updateIfCurrent(file) {
                    it.copy(
                        lyrics = translated,
                        showTranslatedLyrics = translated.translatedLanguage == targetLanguage.code &&
                            (!translated.translatedText.isNullOrBlank() || !translated.translatedSyncedText.isNullOrBlank()),
                        translationLoading = false,
                        translationError = if (translated.translatedText.isNullOrBlank() && translated.translatedSyncedText.isNullOrBlank()) "No se pudo traducir esta letra" else null
                    )
                }
            }.onFailure { error ->
                _uiState.updateIfCurrent(file) {
                    it.copy(translationLoading = false, translationError = error.message ?: "No se pudo traducir la letra")
                }
            }
        }
    }

    private fun hasNetwork(): Boolean {
        val connectivity = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hasUsableLyricsMetadata(file: MusicFile): Boolean {
        val invalid = setOf("", "unknown", "artista desconocido", "track", "canción")
        val artist = file.artist.trim().lowercase()
        val title = file.title.trim().lowercase()
        return artist.length >= 2 && title.length >= 2 && artist !in invalid && title !in invalid
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val offset = (_uiState.value.lyricsOffsetMs + deltaMs).coerceIn(-600_000L, 600_000L)
        val file = _uiState.value.musicFile ?: return
        val offsets = runCatching {
            org.json.JSONObject(preferences.getString("lyrics_offsets", "{}") ?: "{}")
        }.getOrDefault(org.json.JSONObject())
        offsets.put(file.uri.toString(), offset)
        preferences.edit().putString("lyrics_offsets", offsets.toString()).apply()
        _uiState.update { it.copy(lyricsOffsetMs = offset) }
    }

    fun setPlaybackSpeed(speed: Float) {
        musicPlayer?.setSpeed(speed)
    }

    private fun loadLyricsOffset(file: MusicFile?): Long {
        val globalOffset = preferences.getLong("global_lyrics_offset_ms", 0L).coerceIn(-600_000L, 600_000L)
        val uri = file?.uri?.toString() ?: return globalOffset
        return runCatching {
            val offsets = org.json.JSONObject(preferences.getString("lyrics_offsets", "{}") ?: "{}")
            if (offsets.has(uri)) offsets.optLong(uri, globalOffset)
            else globalOffset
        }.getOrDefault(globalOffset)
    }

    private fun hasSongLyricsOffset(file: MusicFile?): Boolean {
        val uri = file?.uri?.toString() ?: return false
        return runCatching {
            org.json.JSONObject(preferences.getString("lyrics_offsets", "{}") ?: "{}").has(uri)
        }.getOrDefault(false)
    }

    private inline fun MutableStateFlow<PlayerUiState>.updateIfCurrent(
        file: MusicFile,
        crossinline transform: (PlayerUiState) -> PlayerUiState
    ) {
        update { state ->
            if (state.musicFile?.uri == file.uri) transform(state) else state
        }
    }

    fun togglePlayback() {
        if (_uiState.value.musicFile != null) musicPlayer?.toggle()
    }

    /**
     * Previous button: restarts the song after 3 s, otherwise goes back. [forceTrack] (mini player swipe)
     * always goes to the previous song in the queue.
     */
    fun skipPrevious(forceTrack: Boolean = false) {
        val state = _uiState.value
        val position = musicPlayer?.currentPositionOrNull() ?: _playbackPositionMs.value
        if (!forceTrack && position > 3_000L) {
            musicPlayer?.seekTo(0L)
            return
        }
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
        if (currentIndex > 0) {
            playSelected(state.queue[currentIndex - 1], state.queue)
        } else {
            musicPlayer?.seekTo(0L)
        }
    }

    fun skipNext() {
        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
        val nextIndex = currentIndex + 1
        when {
            nextIndex < state.queue.size -> playSelected(state.queue[nextIndex], state.queue)
            state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty() -> {
                if (state.shuffleEnabled && state.queue.size > 1) {
                    val shuffled = state.queue.shuffled()
                    playSelected(shuffled.first(), shuffled)
                } else {
                    playSelected(state.queue.first(), state.queue)
                }
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _uiState.value
        if (state.repeatMode == RepeatMode.ONE && state.musicFile != null) {
            state.queue.firstOrNull { it.id == state.currentQueueEntryId }?.let {
                playSelected(it, state.queue)
            }
        } else if (state.appSettings.autoAdvance || state.repeatMode == RepeatMode.ALL) {
            val currentIndex = state.queue.indexOfFirst { it.id == state.currentQueueEntryId }
            val nextIndex = currentIndex + 1
            when {
                nextIndex < state.queue.size -> playSelected(state.queue[nextIndex], state.queue)
                state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty() -> {
                    if (state.shuffleEnabled && state.queue.size > 1) {
                        val shuffled = state.queue.shuffled()
                        playSelected(shuffled.first(), shuffled)
                    } else {
                        playSelected(state.queue.first(), state.queue)
                    }
                }
            }
        }
    }

    fun seekTo(fraction: Float) {
        val duration = _uiState.value.durationMs
        if (duration > 0) {
            val position = (duration * fraction).toLong().coerceIn(0L, duration)
            _playbackPositionMs.value = position
            musicPlayer?.seekTo(position)
        }
    }

    fun toggleFavorite() {
        _uiState.value.musicFile?.let(::toggleFavorite)
    }

    fun toggleFavorite(file: MusicFile) {
        val uri = file.uri.toString()
        if (!favoriteUris.add(uri)) favoriteUris.remove(uri)
        preferences.edit().putStringSet("favorite_uris", favoriteUris).apply()
        _uiState.update {
            it.copy(
                isFavorite = it.musicFile?.uri?.toString() == uri && uri in favoriteUris,
                favoriteUris = favoriteUris.toSet()
            )
        }
    }

    fun cycleRepeatMode() {
        val state = _uiState.value
        val nextMode = when (state.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        if (state.sessionRepeatMode != null) {
            _uiState.update { it.copy(sessionRepeatMode = nextMode) }
            musicPlayer?.setRepeatOneEnabled(nextMode == RepeatMode.ONE)
        } else {
            updateAppSettings { it.copy(repeatMode = nextMode) }
        }
    }

    fun toggleShuffle() {
        updateAppSettings { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun setTonearmAnimationEnabled(enabled: Boolean) {
        updateAppSettings { it.copy(tonearmAnimationEnabled = enabled) }
    }

    fun setRepeatMode(mode: RepeatMode) {
        val hasSessionMode = _uiState.value.sessionRepeatMode != null
        updateAppSettings { it.copy(repeatMode = mode) }
        if (hasSessionMode) {
            _uiState.update { it.copy(sessionRepeatMode = mode) }
            musicPlayer?.setRepeatOneEnabled(mode == RepeatMode.ONE)
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        updateAppSettings { it.copy(shuffleEnabled = enabled) }
    }

    fun updateAppSettings(transform: (AppSettings) -> AppSettings) {
        val oldSettings = _uiState.value.appSettings
        val updatedSettings = transform(oldSettings)
        if (updatedSettings == oldSettings) return
        settingsPreferences.save(updatedSettings)
        applyAppSettings(updatedSettings, oldSettings)
    }

    fun resetAppearanceSettings() {
        val oldSettings = _uiState.value.appSettings
        val updated = settingsPreferences.resetAppearance()
        applyAppSettings(updated, oldSettings)
    }

    fun resetAllSettings() {
        val oldSettings = _uiState.value.appSettings
        val defaults = settingsPreferences.resetAll()
        applyAppSettings(defaults, oldSettings)
    }

    private fun applyAppSettings(updated: AppSettings, previous: AppSettings) {
        val state = _uiState.value
        var queue = state.queue
        if (previous.shuffleEnabled != updated.shuffleEnabled) {
            musicPlayer?.setShuffleEnabled(updated.shuffleEnabled)
            if (updated.shuffleEnabled && queue.isNotEmpty()) {
                val currentIndex = queue.indexOfFirst { it.id == state.currentQueueEntryId }
                queue = queue.take(currentIndex + 1) + queue.drop(currentIndex + 1).shuffled()
            }
        }
        if (queue.map { it.id } != state.queue.map { it.id }) {
            musicPlayer?.replaceQueue(
                queue = queue,
                currentEntryId = state.currentQueueEntryId,
                positionMs = musicPlayer?.currentPositionOrNull() ?: state.positionMs,
                playWhenReady = state.isPlaying,
                shuffleEnabled = updated.shuffleEnabled
            )
        }
        if (previous.repeatMode != updated.repeatMode) {
            musicPlayer?.setRepeatOneEnabled((state.sessionRepeatMode ?: updated.repeatMode) == RepeatMode.ONE)
        }
        val activeLyricsOffset = if (previous.globalLyricsOffsetMs != updated.globalLyricsOffsetMs &&
            !hasSongLyricsOffset(state.musicFile)
        ) {
            updated.globalLyricsOffsetMs
        } else {
            state.lyricsOffsetMs
        }
        val library = if (previous.librarySortOrder != updated.librarySortOrder) {
            sortLibrary(state.library, updated.librarySortOrder)
        } else state.library
        _uiState.update { it.copy(appSettings = updated, queue = queue, library = library, lyricsOffsetMs = activeLyricsOffset) }
        if (previous.librarySortOrder != updated.librarySortOrder) saveLibraryCache(library)
        if (previous.keepQueueBetweenSessions != updated.keepQueueBetweenSessions || queue != state.queue) {
            saveQueueCache(queue, state.currentQueueEntryId)
        }
        if (previous.resumeLastSession != updated.resumeLastSession) {
            if (updated.resumeLastSession) {
                state.musicFile?.let(::saveCurrentFile)
                savePlaybackPosition(_playbackPositionMs.value)
            } else {
                preferences.edit()
                    .remove("current_file_cache")
                    .remove("current_position_uri")
                    .remove("current_position_ms")
                    .apply()
            }
        }
        if (previous.lyricsTranslationEnabled && !updated.lyricsTranslationEnabled) {
            _uiState.update { it.copy(showTranslatedLyrics = false) }
        }
        if (previous.translationLanguage != updated.translationLanguage) {
            _uiState.update { it.copy(showTranslatedLyrics = false, translationError = null) }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            musicPlayer?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    private fun saveLibraryCache(files: List<MusicFile>) {
        libraryCacheJob?.cancel()
        libraryCacheJob = viewModelScope.launch(cacheWriter) { writeLibraryCache(files) }
    }

    private fun writeLibraryCache(files: List<MusicFile>) {
        val json = JSONArray()
        files.forEach { file ->
            json.put(
                JSONArray().apply {
                    put(file.uri.toString())
                    put(file.title)
                    put(file.artist)
                    put(file.album)
                    put(file.durationMs)
                    put(file.filePath ?: "")
                }
            )
        }
        preferences.edit().putString("library_cache", json.toString()).apply()
    }

    private fun sortLibrary(files: List<MusicFile>, order: LibrarySortOrder): List<MusicFile> = when (order) {
        LibrarySortOrder.TITLE -> files.sortedBy { it.title.lowercase() }
        LibrarySortOrder.ARTIST -> files.sortedWith(compareBy<MusicFile>({ it.artist.lowercase() }, { it.title.lowercase() }))
        LibrarySortOrder.ALBUM -> files.sortedWith(compareBy<MusicFile>({ it.album.lowercase() }, { it.title.lowercase() }))
    }

    private fun saveQueueCache(queue: List<QueueEntry>, currentEntryId: String?) {
        if (!_uiState.value.appSettings.keepQueueBetweenSessions) {
            queueCacheJob?.cancel()
            preferences.edit().remove("queue_cache").remove("current_queue_entry_id").apply()
            return
        }
        // Serializing a long queue on the main thread caused visible freezes on every track change.
        // Writes run on one background lane, so they keep their order; a newer save supersedes a pending one.
        queueCacheJob?.cancel()
        queueCacheJob = viewModelScope.launch(cacheWriter) { writeQueueCache(queue, currentEntryId) }
    }

    private fun writeQueueCache(queue: List<QueueEntry>, currentEntryId: String?) {
        val json = JSONArray()
        queue.forEach { entry ->
            val file = entry.file
            json.put(JSONArray().apply {
                put(entry.id)
                put(file.uri.toString())
                put(file.title)
                put(file.artist)
                put(file.album)
                put(file.durationMs)
                put(file.filePath ?: "")
            })
        }
        val editor = preferences.edit().putString("queue_cache", json.toString())
        if (currentEntryId != null && queue.any { it.id == currentEntryId }) {
            editor.putString("current_queue_entry_id", currentEntryId)
        } else {
            editor.remove("current_queue_entry_id")
        }
        editor.apply()
    }

    private fun saveCurrentFile(file: MusicFile) {
        if (!_uiState.value.appSettings.resumeLastSession) {
            preferences.edit().remove("current_file_cache").apply()
            return
        }
        val json = JSONArray().apply {
            put(file.uri.toString())
            put(file.title)
            put(file.artist)
            put(file.album)
            put(file.durationMs)
            put(file.filePath ?: "")
        }
        preferences.edit().putString("current_file_cache", json.toString()).apply()
    }

    private fun clearCurrentPlaybackCache() {
        preferences.edit()
            .remove("current_file_cache")
            .remove("current_position_uri")
            .remove("current_position_ms")
            .remove("current_queue_entry_id")
            .apply()
    }

    private fun savePlaybackPosition(positionMs: Long) {
        if (!_uiState.value.appSettings.resumeLastSession) return
        val uri = _uiState.value.musicFile?.uri?.toString() ?: return
        preferences.edit()
            .putString("current_position_uri", uri)
            .putLong("current_position_ms", positionMs.coerceAtLeast(0L))
            .apply()
    }

    private fun loadCachedCurrentPosition(file: MusicFile?): Long {
        val uri = preferences.getString("current_position_uri", null)
        return if (file != null && file.uri.toString() == uri) {
            preferences.getLong("current_position_ms", 0L)
        } else {
            0L
        }
    }

    private fun loadCachedLibrary(): List<MusicFile> {
        val cached = preferences.getString("library_cache", null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(cached)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val item = json.getJSONArray(index)
                    add(
                        MusicFile(
                            uri = Uri.parse(item.getString(0)),
                            title = item.getString(1),
                            artist = item.getString(2),
                            album = item.getString(3),
                            artwork = null,
                            artworkData = null,
                            durationMs = item.getLong(4),
                            filePath = item.optString(5).takeIf(String::isNotBlank)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadCachedQueue(): List<QueueEntry> {
        val cached = preferences.getString("queue_cache", null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(cached)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val item = json.getJSONArray(index)
                    add(
                        QueueEntry(
                            id = item.getString(0),
                            file = MusicFile(
                                uri = Uri.parse(item.getString(1)),
                                title = item.getString(2),
                                artist = item.getString(3),
                                album = item.getString(4),
                                artwork = null,
                                artworkData = null,
                                durationMs = item.getLong(5),
                                filePath = item.optString(6).takeIf(String::isNotBlank)
                            )
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun loadCachedCurrentFile(): MusicFile? {
        val cached = preferences.getString("current_file_cache", null) ?: return null
        return runCatching {
            val item = JSONArray(cached)
            MusicFile(
                uri = Uri.parse(item.getString(0)),
                title = item.getString(1),
                artist = item.getString(2),
                album = item.getString(3),
                artwork = null,
                artworkData = null,
                durationMs = item.getLong(4),
                filePath = item.optString(5).takeIf(String::isNotBlank)
            )
        }.getOrNull()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun release() {
        musicPlayer?.currentPositionOrNull()?.let(::savePlaybackPosition)
        positionJob?.cancel()
        positionJob = null
        lyricsJob?.cancel()
        lyricsJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        musicPlayer?.release()
        musicPlayer = null
        // Lets initialize() build a new player when the activity is recreated with this ViewModel.
        playerStartJob?.cancel()
        playerStartJob = null
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }

    companion object {
        private val LYRICS_CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(7)
        private const val MAX_CACHED_ARTIST_BITMAPS = 40
        private const val POSITION_POLL_INTERVAL_MS = 500L
        private const val ARTWORK_BATCH_DELAY_MS = 150L
        private const val POSITION_IDLE_INTERVAL_MS = 1_000L
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
        private const val LEGACY_ARTIST_IMAGE_PREFETCH_CLEANUP_KEY = "artist_image_prefetch_cleanup_complete"
    }
}
