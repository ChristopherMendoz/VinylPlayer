package com.chrismdz.vinylplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
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
        playerViewModel.loadLibrary()
    }

    override fun onDestroy() {
        playerViewModel.release()
        super.onDestroy()
    }
}
