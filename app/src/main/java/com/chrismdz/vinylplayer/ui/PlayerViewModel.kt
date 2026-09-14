package com.chrismdz.vinylplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.MusicRepository
import com.chrismdz.vinylplayer.player.MusicPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val musicFile: MusicFile? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val tonearmAnimationEnabled: Boolean = true,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository()
    private val preferences = application.getSharedPreferences("vinylplayer_settings", 0)
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            tonearmAnimationEnabled = preferences.getBoolean("tonearm_animation", true)
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var musicPlayer: MusicPlayer? = null
    private var positionJob: Job? = null

    fun initialize() {
        if (musicPlayer != null) return

        musicPlayer = MusicPlayer(getApplication()) { player ->
            _uiState.update {
                it.copy(
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition,
                    durationMs = player.duration.takeIf { duration -> duration > 0 }
                        ?: it.musicFile?.durationMs
                        ?: 0L
                )
            }
        }

        positionJob = viewModelScope.launch {
            while (isActive) {
                musicPlayer?.let { player ->
                    _uiState.update { state ->
                        state.copy(positionMs = player.currentPosition())
                    }
                }
                delay(500)
            }
        }
    }

    fun selectMusic(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repository.readMetadata(getApplication(), uri)
            }.onSuccess { musicFile ->
                _uiState.value = PlayerUiState(
                    musicFile = musicFile,
                    durationMs = musicFile.durationMs,
                    tonearmAnimationEnabled = _uiState.value.tonearmAnimationEnabled
                )
                musicPlayer?.load(musicFile)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo abrir el archivo")
                }
            }
        }
    }

    fun togglePlayback() {
        if (_uiState.value.musicFile == null) return
        musicPlayer?.toggle()
    }

    fun seekTo(fraction: Float) {
        val duration = _uiState.value.durationMs
        if (duration > 0) {
            musicPlayer?.seekTo((duration * fraction).toLong())
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setTonearmAnimationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("tonearm_animation", enabled).apply()
        _uiState.update { it.copy(tonearmAnimationEnabled = enabled) }
    }

    fun release() {
        positionJob?.cancel()
        positionJob = null
        musicPlayer?.release()
        musicPlayer = null
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}
