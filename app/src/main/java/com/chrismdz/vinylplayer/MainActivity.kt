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
import androidx.compose.runtime.remember
import com.chrismdz.vinylplayer.ui.MainScreen
import com.chrismdz.vinylplayer.ui.PlayerViewModel
import com.chrismdz.vinylplayer.ui.VinylPlayerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestMusicPermissions()
        setContent {
            val settingsFlow = remember(playerViewModel) {
                playerViewModel.uiState.map { it.appSettings }.distinctUntilChanged()
            }
            val appSettings by settingsFlow.collectAsStateWithLifecycle(
                initialValue = playerViewModel.uiState.value.appSettings
            )
            VinylPlayerTheme(
                themeMode = appSettings.themeMode,
                accentColor = appSettings.accentColor,
                customAccentArgb = appSettings.customAccentArgb
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
        // Uses the saved library; MediaStore is only rescanned when the user taps Actualizar.
        playerViewModel.loadLibrary()
    }

    override fun onDestroy() {
        playerViewModel.release()
        super.onDestroy()
    }

}
