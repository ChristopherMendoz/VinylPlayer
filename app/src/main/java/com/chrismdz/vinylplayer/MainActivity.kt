package com.chrismdz.vinylplayer

import android.Manifest
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.chrismdz.vinylplayer.ui.MainScreen
import com.chrismdz.vinylplayer.ui.PlayerViewModel
import com.chrismdz.vinylplayer.ui.VinylPlayerTheme

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    private var hasResumedBefore = false
    private val libraryRescanHandler = Handler(Looper.getMainLooper())
    private val libraryRescan = Runnable { playerViewModel.loadLibrary(force = true) }
    private val libraryObserver = object : ContentObserver(libraryRescanHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            libraryRescanHandler.removeCallbacks(libraryRescan)
            libraryRescanHandler.postDelayed(libraryRescan, LIBRARY_RESCAN_DELAY_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMusicPermissions()
        setContent {
            val state by playerViewModel.uiState.collectAsStateWithLifecycle()
            VinylPlayerTheme(
                themeMode = state.appSettings.themeMode,
                accentColor = state.appSettings.accentColor,
                customAccentArgb = state.appSettings.customAccentArgb
            ) {
                MainScreen(viewModel = playerViewModel, onRequestAudioPermission = ::requestMusicPermissions)
            }
        }
    }

    private fun requestMusicPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissions(permissions, 100)
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.loadLibrary(force = hasResumedBefore)
        hasResumedBefore = true
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            libraryObserver
        )
    }

    override fun onStop() {
        libraryRescanHandler.removeCallbacks(libraryRescan)
        contentResolver.unregisterContentObserver(libraryObserver)
        super.onStop()
    }

    override fun onDestroy() {
        playerViewModel.release()
        super.onDestroy()
    }

    private companion object {
        const val LIBRARY_RESCAN_DELAY_MS = 900L
    }
}
