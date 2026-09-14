package com.chrismdz.vinylplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.chrismdz.vinylplayer.ui.MainScreen
import com.chrismdz.vinylplayer.ui.PlayerViewModel
import com.chrismdz.vinylplayer.ui.VinylPlayerTheme

class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        setContent {
            VinylPlayerTheme {
                MainScreen(viewModel = playerViewModel)
            }
        }
    }

    override fun onDestroy() {
        playerViewModel.release()
        super.onDestroy()
    }
}
