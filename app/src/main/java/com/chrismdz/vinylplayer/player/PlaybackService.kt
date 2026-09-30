package com.chrismdz.vinylplayer.player

import android.media.audiofx.Equalizer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var equalizer: Equalizer? = null
    private val equalizerPreferences by lazy { getSharedPreferences("vinylplayer_settings", MODE_PRIVATE) }
    private val equalizerListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("eq_band_") == true) applyEqualizer()
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        equalizer = runCatching {
            Equalizer(0, player!!.audioSessionId).apply { enabled = true }
        }.getOrNull()
        equalizerPreferences.registerOnSharedPreferenceChangeListener(equalizerListener)
        applyEqualizer()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    private fun applyEqualizer() {
        val effect = equalizer ?: return
        val bands = effect.numberOfBands.toInt()
        for (index in 0 until bands.coerceAtMost(5)) {
            val value = equalizerPreferences.getInt("eq_band_$index", 0).coerceIn(-1000, 1000)
            runCatching { effect.setBandLevel(index.toShort(), value.toShort()) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        equalizerPreferences.unregisterOnSharedPreferenceChangeListener(equalizerListener)
        equalizer?.release()
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
