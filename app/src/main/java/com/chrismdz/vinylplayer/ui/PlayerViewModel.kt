package com.chrismdz.vinylplayer.ui

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.MusicRepository
import com.chrismdz.vinylplayer.data.Playlist
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
import com.chrismdz.vinylplayer.data.RoomLyricsRepository
import com.chrismdz.vinylplayer.data.VinylDatabase
import com.chrismdz.vinylplayer.player.MusicPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.TimeUnit

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlayerUiState(
    val musicFile: MusicFile? = null,
    val library: List<MusicFile> = emptyList(),
    val queue: List<MusicFile> = emptyList(),
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
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val tonearmAnimationEnabled: Boolean = true,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository()
    private val lyricsRepository = RoomLyricsRepository(VinylDatabase.get(application).lyricsDao())
    private val preferences = application.getSharedPreferences("vinylplayer_settings", 0)
    private val favoriteUris = preferences.getStringSet("favorite_uris", emptySet()).orEmpty().toMutableSet()
    private val cachedLibrary = loadCachedLibrary()
    private val cachedCurrentFile = loadCachedCurrentFile()
    private val cachedCurrentPosition = loadCachedCurrentPosition(cachedCurrentFile)
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            musicFile = cachedCurrentFile,
            library = cachedLibrary,
            queue = cachedCurrentFile?.let { listOf(it) } ?: emptyList(),
            playlists = loadPlaylists(),
            favoriteUris = favoriteUris.toSet(),
            positionMs = cachedCurrentPosition,
            lyricsOffsetMs = loadLyricsOffset(cachedCurrentFile),
            isFavorite = cachedCurrentFile?.uri?.toString()?.let { it in favoriteUris } == true,
            shuffleEnabled = preferences.getBoolean("shuffle_enabled", false),
            tonearmAnimationEnabled = preferences.getBoolean("tonearm_animation", true)
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var musicPlayer: MusicPlayer? = null
    private var positionJob: Job? = null
    private var libraryJob: Job? = null
    private var lyricsJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var libraryLoaded = false
    private val artworkLoads = mutableSetOf<String>()

    fun initialize() {
        if (musicPlayer == null) {
            musicPlayer = MusicPlayer(getApplication()) { player ->
                _uiState.update { state ->
                    val current = state.queue.firstOrNull {
                        it.uri.toString() == player.currentMediaItem?.mediaId
                    } ?: state.library.firstOrNull {
                        it.uri.toString() == player.currentMediaItem?.mediaId
                    } ?: state.musicFile
                    state.copy(
                        musicFile = current,
                        isPlaying = player.isPlaying,
                        positionMs = player.currentPosition,
                        durationMs = player.duration.takeIf { duration -> duration > 0 }
                            ?: current?.durationMs
                            ?: 0L,
                        isFavorite = current?.uri?.toString()?.let { it in favoriteUris } == true,
                        favoriteUris = favoriteUris.toSet()
                    )
                }
                if (player.playbackState == Player.STATE_ENDED) handleTrackEnded()
            }

            positionJob = viewModelScope.launch {
                while (isActive) {
                    _uiState.update { state ->
                        val position = musicPlayer?.currentPosition() ?: state.positionMs
                        if (state.musicFile != null) savePlaybackPosition(position)
                        state.copy(positionMs = position)
                    }
                    delay(500)
                }
            }
        }
        loadLibrary()
        _uiState.value.musicFile?.let(::loadLyrics)
    }

    fun loadLibrary(force: Boolean = false) {
        if (libraryJob?.isActive == true || (libraryLoaded && !force)) return
        libraryJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.readLibraryMetadata(getApplication()) }
            }.onSuccess { files ->
                libraryLoaded = true
                saveLibraryCache(files)
                _uiState.update { state ->
                    state.copy(
                        library = files,
                        queue = if (state.queue.size <= 1 && state.musicFile != null) files else state.queue,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo leer la biblioteca")
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
                    _uiState.update { state ->
                        val updateFile: (MusicFile) -> MusicFile = { item ->
                            if (item.uri == file.uri) item.copy(artwork = artwork) else item
                        }
                        state.copy(
                            library = state.library.map(updateFile),
                            queue = state.queue.map(updateFile),
                            musicFile = state.musicFile?.let(updateFile)
                        )
                    }
                }
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
                        queue = state.queue.map(updateFile),
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
                playFile(musicFile, listOf(musicFile), false)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo abrir el archivo")
                }
            }
        }
    }

    fun playFromLibrary(file: MusicFile) {
        if (_uiState.value.musicFile?.uri == file.uri) return
        val queue = _uiState.value.library.ifEmpty { listOf(file) }
        playSelected(file, queue)
    }

    fun playFromQueue(file: MusicFile) {
        if (_uiState.value.musicFile?.uri == file.uri) return
        playSelected(file, _uiState.value.queue)
    }

    fun addToQueue(file: MusicFile) {
        _uiState.update { state ->
            if (state.queue.any { it.uri == file.uri }) state
            else state.copy(queue = state.queue + file)
        }
    }

    fun moveInQueue(itemUri: String, beforeUri: String) {
        _uiState.update { state ->
            val item = state.queue.firstOrNull { it.uri.toString() == itemUri } ?: return@update state
            val remaining = state.queue.filterNot { it.uri.toString() == itemUri }.toMutableList()
            val target = remaining.indexOfFirst { it.uri.toString() == beforeUri }
            if (target < 0) state else state.copy(queue = remaining.apply { add(target, item) })
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
            val updatedPlaylists = _uiState.value.playlists.map { playlist ->
                playlist.copy(songUris = playlist.songUris - file.uri.toString())
            }
            savePlaylists(updatedPlaylists)
            favoriteUris.remove(file.uri.toString())
            preferences.edit().putStringSet("favorite_uris", favoriteUris).apply()
            val currentWasDeleted = _uiState.value.musicFile?.uri == file.uri
            if (currentWasDeleted) musicPlayer?.pause()
            _uiState.update { state ->
                state.copy(
                    library = state.library.filterNot { it.uri == file.uri },
                    queue = state.queue.filterNot { it.uri == file.uri },
                    musicFile = if (currentWasDeleted) null else state.musicFile,
                    favoriteUris = favoriteUris.toSet(),
                    isFavorite = if (currentWasDeleted) false else state.isFavorite,
                    errorMessage = null
                )
            }
        }
    }

    fun playNext(file: MusicFile) {
        _uiState.update { state ->
            val remaining = state.queue.filterNot { it.uri == file.uri }.toMutableList()
            val currentIndex = remaining.indexOfFirst { it.uri == state.musicFile?.uri }
            val insertAt = if (currentIndex >= 0) currentIndex + 1 else remaining.size
            remaining.add(insertAt.coerceAtMost(remaining.size), file)
            state.copy(queue = remaining)
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

    private fun savePlaylists(playlists: List<Playlist>) {
        val json = JSONArray()
        playlists.forEach { playlist ->
            json.put(JSONArray().apply {
                put(playlist.name)
                put(JSONArray(playlist.songUris))
            })
        }
        preferences.edit().putString("playlists", json.toString()).apply()
        _uiState.update { it.copy(playlists = playlists) }
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
                    }))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun playSelected(file: MusicFile, queue: List<MusicFile>) {
        viewModelScope.launch {
            val detailedFile = runCatching {
                withContext(Dispatchers.IO) { repository.readMetadata(getApplication(), file.uri) }
            }.getOrDefault(file)
            val updatedQueue = queue.map { if (it.uri == detailedFile.uri) detailedFile else it }
            playFile(detailedFile, updatedQueue, true)
        }
    }

    private fun playFile(file: MusicFile, queue: List<MusicFile>, playWhenReady: Boolean) {
        saveCurrentFile(file)
        _uiState.update {
            it.copy(
                musicFile = file,
                queue = queue,
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
        musicPlayer?.load(file, queue, playWhenReady, _uiState.value.shuffleEnabled)
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
        val file = state.musicFile ?: return
        val lyrics = state.lyrics ?: return
        if (state.showTranslatedLyrics) {
            _uiState.update { it.copy(showTranslatedLyrics = false, translationError = null) }
            return
        }
        if (!lyrics.translatedText.isNullOrBlank() || !lyrics.translatedSyncedText.isNullOrBlank()) {
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
                withContext(Dispatchers.IO) { lyricsRepository.translateAndSaveLyrics(file) }
            }.onSuccess { translated ->
                _uiState.updateIfCurrent(file) {
                    it.copy(
                        lyrics = translated,
                        showTranslatedLyrics = !translated.translatedText.isNullOrBlank() || !translated.translatedSyncedText.isNullOrBlank(),
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
        val offset = (_uiState.value.lyricsOffsetMs + deltaMs).coerceIn(-5_000L, 5_000L)
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

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            musicPlayer?.pause()
        }
    }

    private fun loadLyricsOffset(file: MusicFile?): Long {
        val uri = file?.uri?.toString() ?: return 0L
        return runCatching {
            org.json.JSONObject(preferences.getString("lyrics_offsets", "{}") ?: "{}").optLong(uri, 0L)
        }.getOrDefault(0L)
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

    fun skipPrevious() {
        val state = _uiState.value
        val position = musicPlayer?.currentPosition() ?: state.positionMs
        if (position > 3_000L) {
            musicPlayer?.seekTo(0L)
            return
        }
        val currentIndex = state.queue.indexOfFirst { it.uri == state.musicFile?.uri }
        if (currentIndex > 0) {
            playSelected(state.queue[currentIndex - 1], state.queue)
        } else {
            musicPlayer?.seekTo(0L)
        }
    }

    fun skipNext() {
        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.uri == state.musicFile?.uri }
        val nextIndex = if (state.shuffleEnabled && state.queue.size > 1) {
            state.queue.indices.filter { it != currentIndex }.random()
        } else currentIndex + 1
        when {
            nextIndex < state.queue.size -> playSelected(state.queue[nextIndex], state.queue)
            state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty() -> {
                playSelected(state.queue.first(), state.queue)
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _uiState.value
        if (state.repeatMode == RepeatMode.ONE && state.musicFile != null) {
            playSelected(state.musicFile, state.queue)
        } else {
            val currentIndex = state.queue.indexOfFirst { it.uri == state.musicFile?.uri }
            val nextIndex = if (state.shuffleEnabled && state.queue.size > 1) {
                state.queue.indices.filter { it != currentIndex }.random()
            } else currentIndex + 1
            when {
                nextIndex < state.queue.size -> playSelected(state.queue[nextIndex], state.queue)
                state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty() -> {
                    playSelected(state.queue.first(), state.queue)
                }
            }
        }
    }

    fun seekTo(fraction: Float) {
        val duration = _uiState.value.durationMs
        if (duration > 0) musicPlayer?.seekTo((duration * fraction).toLong())
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
        _uiState.update {
            it.copy(
                repeatMode = when (it.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
            )
        }
    }

    fun toggleShuffle() {
        val enabled = !_uiState.value.shuffleEnabled
        preferences.edit().putBoolean("shuffle_enabled", enabled).apply()
        musicPlayer?.setShuffleEnabled(enabled)
        _uiState.update { it.copy(shuffleEnabled = enabled) }
    }

    fun setTonearmAnimationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("tonearm_animation", enabled).apply()
        _uiState.update { it.copy(tonearmAnimationEnabled = enabled) }
    }

    private fun saveLibraryCache(files: List<MusicFile>) {
        val json = JSONArray()
        files.forEach { file ->
            json.put(
                JSONArray().apply {
                    put(file.uri.toString())
                    put(file.title)
                    put(file.artist)
                    put(file.album)
                    put(file.durationMs)
                }
            )
        }
        preferences.edit().putString("library_cache", json.toString()).apply()
    }

    private fun saveCurrentFile(file: MusicFile) {
        val json = JSONArray().apply {
            put(file.uri.toString())
            put(file.title)
            put(file.artist)
            put(file.album)
            put(file.durationMs)
        }
        preferences.edit().putString("current_file_cache", json.toString()).apply()
    }

    private fun savePlaybackPosition(positionMs: Long) {
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
                            durationMs = item.getLong(4)
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
                durationMs = item.getLong(4)
            )
        }.getOrNull()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun release() {
        positionJob?.cancel()
        positionJob = null
        lyricsJob?.cancel()
        lyricsJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        musicPlayer?.release()
        musicPlayer = null
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }

    companion object {
        private val LYRICS_CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(7)
    }
}
