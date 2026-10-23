package com.chrismdz.vinylplayer.ui

import android.content.Intent
import android.content.Context
import android.media.RingtoneManager
import android.app.Activity
import android.app.PendingIntent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
import com.chrismdz.vinylplayer.data.AudioTagWriter
import com.chrismdz.vinylplayer.data.QueueEntry
import com.chrismdz.vinylplayer.data.parseSyncedLyrics
import com.chrismdz.vinylplayer.data.Playlist
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.consolidateAlbumArtists
import com.chrismdz.vinylplayer.settings.RepeatMode
import com.chrismdz.vinylplayer.settings.PlayerBackground
import com.chrismdz.vinylplayer.settings.AccentColorValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

private enum class MainView {
    LIBRARY,
    PLAYER,
    INFO,
    SETTINGS
}

private enum class PlayerTab {
    COVER,
    LYRICS
}

private enum class LibraryTab {
    TRACKS,
    ALBUMS,
    PLAYLISTS,
    ARTISTS
}

@Composable
fun MainScreen(viewModel: PlayerViewModel, onRequestAudioPermission: () -> Unit = {}) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentView by remember { mutableStateOf(MainView.LIBRARY) }
    var settingsReturnView by remember { mutableStateOf(MainView.LIBRARY) }
    var settingsCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var playerTabName by rememberSaveable { mutableStateOf(PlayerTab.COVER.name) }
    var libraryTabName by rememberSaveable { mutableStateOf(LibraryTab.TRACKS.name) }
    var selectedPlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlayerMenu by remember { mutableStateOf(false) }
    var menuFile by remember { mutableStateOf<MusicFile?>(null) }
    var pendingDeleteFile by remember { mutableStateOf<MusicFile?>(null) }
    var awaitingDeleteApproval by remember { mutableStateOf<MusicFile?>(null) }
    var pendingTagWriteIntent by remember { mutableStateOf<PendingIntent?>(null) }
    var playlistTarget by remember { mutableStateOf<MusicFile?>(null) }
    var infoTarget by remember { mutableStateOf<MusicFile?>(null) }
    var infoReturnView by remember { mutableStateOf(MainView.LIBRARY) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val libraryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val albumDetailListState = rememberLazyListState()
    val albumGridState = rememberLazyGridState()
    val artistGridState = rememberLazyGridState()
    val albumArtworkPreferences = remember(context) {
        context.getSharedPreferences("vinylplayer_album_artwork", Context.MODE_PRIVATE)
    }
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val file = awaitingDeleteApproval
        if (result.resultCode == Activity.RESULT_OK && file != null) viewModel.removeDeletedSong(file)
        awaitingDeleteApproval = null
    }
    val tagWriteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.completeSongTagWrite(result.resultCode == Activity.RESULT_OK) { pendingTagWriteIntent = it }
    }
    val playerTab = PlayerTab.valueOf(playerTabName)
    val useDarkColors = when (state.appSettings.themeMode) {
        com.chrismdz.vinylplayer.settings.ThemeMode.DARK -> true
        com.chrismdz.vinylplayer.settings.ThemeMode.LIGHT -> false
        com.chrismdz.vinylplayer.settings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val settingsCategory = settingsCategoryName?.let { name ->
        runCatching { SettingsCategory.valueOf(name) }.getOrNull()
    }
    val libraryTab = LibraryTab.valueOf(libraryTabName)
    val selectedAlbumTracks = if (libraryTab == LibraryTab.ALBUMS && selectedAlbumName != null) {
        state.library.filter { it.album == selectedAlbumName }
    } else emptyList()
    val selectedAlbumArtworkKey = selectedAlbumName?.let { albumArtworkPreferenceKey(it, selectedAlbumTracks) }
    var selectedAlbumArtworkOverrideUri by rememberSaveable(selectedAlbumArtworkKey) {
        mutableStateOf(selectedAlbumArtworkKey?.let { albumArtworkPreferences.getString(it, null) })
    }
    val selectedAlbumCover = chooseAlbumCoverTrack(
        selectedAlbumTracks,
        selectedAlbumArtworkOverrideUri,
        { it.uri.toString() }
    )
    val isAlbumDetail = currentView == MainView.LIBRARY && libraryTab == LibraryTab.ALBUMS && selectedAlbumName != null
    val usesCoverPalette = (currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.COVER_COLORS) ||
        isAlbumDetail
    val activePaletteArtwork = when {
        currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.COVER_COLORS -> state.musicFile?.artwork
        currentView == MainView.LIBRARY -> selectedAlbumCover?.artwork
        else -> null
    }
    val paletteColors = remember(activePaletteArtwork) {
        activePaletteArtwork?.let(CoverPalette::fromBitmap)?.let(::paletteBackgroundColors).orEmpty()
    }
    val infoDisplayFile = infoTarget?.let { target ->
        state.library.firstOrNull { it.uri == target.uri }
            ?: state.musicFile?.takeIf { it.uri == target.uri }
            ?: target
    }

    LaunchedEffect(Unit) { viewModel.initialize() }
    LaunchedEffect(infoTarget?.uri) {
        infoTarget?.let(viewModel::loadFullArtwork)
    }
    LaunchedEffect(pendingTagWriteIntent) {
        val request = pendingTagWriteIntent ?: return@LaunchedEffect
        pendingTagWriteIntent = null
        tagWriteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }
    LaunchedEffect(selectedAlbumCover) {
        selectedAlbumCover?.let(viewModel::loadArtwork)
    }
    LaunchedEffect(selectedAlbumCover?.uri) {
        selectedAlbumCover?.let(viewModel::loadFullArtwork)
    }
    LaunchedEffect(currentView, state.musicFile?.uri, state.appSettings.playerBackground) {
        if (currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.COVER_COLORS) {
            state.musicFile?.let(viewModel::loadArtwork)
        }
    }
    LaunchedEffect(state.musicFile?.uri, state.musicFile?.artwork) {
        state.musicFile?.let(viewModel::loadArtwork)
    }
    LaunchedEffect(searchQuery) {
        libraryListState.scrollToItem(0)
    }
    LaunchedEffect(selectedAlbumName) {
        if (selectedAlbumName != null) albumDetailListState.scrollToItem(0)
    }
    LaunchedEffect(state.appSettings.lyricsEnabled) {
        if (!state.appSettings.lyricsEnabled && playerTabName == PlayerTab.LYRICS.name) {
            playerTabName = PlayerTab.COVER.name
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.release() } }
    BackHandler(enabled = currentView != MainView.LIBRARY || isAlbumDetail) {
        when (currentView) {
            MainView.INFO -> currentView = infoReturnView
            MainView.SETTINGS -> if (settingsCategory != null) settingsCategoryName = null else currentView = settingsReturnView
            MainView.LIBRARY -> selectedAlbumName = null
            MainView.PLAYER -> currentView = MainView.LIBRARY
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.BLURRED_COVER) state.musicFile?.artwork?.let { artwork ->
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (playerTab == PlayerTab.LYRICS) Modifier.blur(32.dp) else Modifier)
                    .alpha(if (playerTab == PlayerTab.LYRICS) 0.9f else 0.72f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (currentView == MainView.SETTINGS ||
                    (currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.SOLID)
                ) Modifier.background(MaterialTheme.colorScheme.background)
                else if (usesCoverPalette) Modifier.background(
                    Brush.verticalGradient(
                        if (paletteColors.isNotEmpty()) paletteColors
                        else listOf(Color(0xFF263A38), Color(0xFF111817), Color(0xFF090B0B))
                    )
                )
                else Modifier.background(Brush.verticalGradient(
                    if (!useDarkColors) {
                        listOf(Color(0xDDF6F3ED), Color(0xEEF6F3ED), Color(0xFFF6F3ED))
                    } else if (currentView == MainView.PLAYER && playerTab == PlayerTab.LYRICS) {
                        listOf(Color(0x66B91E3A), Color(0xAA260E18), Color(0xF00A090A))
                    } else {
                        listOf(Color(0x66101010), Color(0xA6000000), Color(0xF0000000))
                    }
                ))
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (currentView != MainView.SETTINGS) {
                TopBar(
                    currentView = currentView,
                    isAlbumDetail = isAlbumDetail,
                    onBack = {
                        if (isAlbumDetail) selectedAlbumName = null
                        else currentView = if (currentView == MainView.INFO) infoReturnView else MainView.LIBRARY
                    },
                    onSettings = {
                        settingsReturnView = currentView
                        settingsCategoryName = null
                        currentView = MainView.SETTINGS
                    },
                    onMore = { showPlayerMenu = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (currentView) {
                MainView.LIBRARY -> LibraryView(
                    state = state,
                    tab = libraryTab,
                    selectedPlaylistName = selectedPlaylistName,
                    selectedAlbumName = selectedAlbumName,
                    albumArtworkPreferences = albumArtworkPreferences,
                    albumArtworkOverrideUri = selectedAlbumArtworkOverrideUri,
                    onChooseAlbumCover = { file ->
                        selectedAlbumArtworkKey?.let { key ->
                            if (file == null) albumArtworkPreferences.edit().remove(key).apply()
                            else albumArtworkPreferences.edit().putString(key, file.uri.toString()).apply()
                            selectedAlbumArtworkOverrideUri = file?.uri?.toString()
                        }
                    },
                    selectedArtistName = selectedArtistName,
                    onTabChange = {
                        libraryTabName = it.name
                        selectedPlaylistName = null
                        selectedAlbumName = null
                        selectedArtistName = null
                    },
                    onPlaylistSelected = { selectedPlaylistName = it },
                    onAlbumSelected = { selectedAlbumName = it },
                    onArtistSelected = { selectedArtistName = it },
                    onCreatePlaylist = viewModel::createPlaylist,
                    onRemoveFromPlaylist = viewModel::removeFromPlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onMore = { menuFile = it },
                    onAddToPlaylist = { playlistTarget = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    listState = libraryListState,
                    albumDetailListState = albumDetailListState,
                    albumGridState = albumGridState,
                    artistGridState = artistGridState,
                    onRefresh = { viewModel.loadLibrary(force = true) },
                    onRequestArtwork = viewModel::loadArtwork,
                    onOpenFile = {
                        viewModel.playFromLibrary(it)
                        currentView = MainView.PLAYER
                    },
                onOpenCurrent = { currentView = MainView.PLAYER },
                    onTogglePlayback = viewModel::togglePlayback,
                    onFavorite = viewModel::toggleFavorite,
                    isFavorite = state.isFavorite,
                    onPlayAlbum = viewModel::playAlbum,
                    onPlayAlbumTrack = { files, track ->
                        viewModel.playAlbumTrack(files, track)
                        currentView = MainView.PLAYER
                    }
                )
                MainView.PLAYER -> PlayerView(
                    state = state,
                    tab = playerTab,
                    onTabChange = { playerTabName = it.name },
                    onLyricsOffsetChange = viewModel::adjustLyricsOffset,
                    onToggleLyricsTranslation = viewModel::toggleLyricsTranslation,
                    onTogglePlayback = viewModel::togglePlayback,
                    onSeek = viewModel::seekTo,
                    onPrevious = viewModel::skipPrevious,
                    onNext = viewModel::skipNext,
                    onFavorite = viewModel::toggleFavorite,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::cycleRepeatMode,
                    onQueue = { showQueue = true }
                )
                MainView.INFO -> infoDisplayFile?.let { file ->
                    SongInfoView(
                        file = file,
                        errorMessage = state.songTagErrorMessage,
                        onBack = { currentView = infoReturnView },
                        onEditTags = { updatedFile, title, artist, album ->
                            viewModel.editSongTags(updatedFile, title, artist, album) { pendingIntent ->
                                pendingTagWriteIntent = pendingIntent
                            }
                        }
                    )
                }
                MainView.SETTINGS -> SettingsScreen(
                    category = settingsCategory,
                    onCategorySelected = { settingsCategoryName = it?.name },
                    onBack = { currentView = settingsReturnView },
                    settings = state.appSettings,
                    songCount = state.library.size,
                    isLibraryLoading = state.isLibraryLoading,
                    lastLibraryScanAt = state.lastLibraryScanAt,
                    errorMessage = state.errorMessage,
                    onSettingsChange = { updated -> viewModel.updateAppSettings { updated } },
                    onRefreshLibrary = { viewModel.loadLibrary(force = true) },
                    onRequestAudioPermission = onRequestAudioPermission,
                    onOpenEqualizer = { showEqualizer = true },
                    onStartSleepTimer = viewModel::startSleepTimer,
                    onCancelSleepTimer = viewModel::cancelSleepTimer,
                    onClearPendingQueue = viewModel::clearPendingQueue,
                    onRestoreAppearance = viewModel::resetAppearanceSettings,
                    onResetAll = viewModel::resetAllSettings
                )
            }
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f))
                    .padding(12.dp)
            )
        }
    }

    if (showQueue) {
        QueueDialog(
            queue = state.queue,
            currentEntryId = state.currentQueueEntryId,
            onSelect = { entry ->
                viewModel.playFromQueue(entry)
                currentView = MainView.PLAYER
                showQueue = false
            },
            repeatMode = state.repeatMode,
            onRepeat = viewModel::cycleRepeatMode,
            onMove = viewModel::moveInQueue,
            onPlayNext = { viewModel.playNext(it.file) },
            onAddToQueue = { viewModel.addToQueue(it.file) },
            onRemove = viewModel::removeFromQueue,
            onClearPending = viewModel::clearPendingQueue,
            onInfo = { entry ->
                infoTarget = entry.file
                infoReturnView = currentView
                currentView = MainView.INFO
                showQueue = false
            },
            onBackToLibrary = { currentView = MainView.LIBRARY; showQueue = false },
            onDismiss = { showQueue = false }
        )
    }
    menuFile?.let { file ->
        SongOptionsDialog(
            file = file,
            onPlayNext = { viewModel.playNext(file); menuFile = null },
            onAddToQueue = { viewModel.addToQueue(file); menuFile = null },
            onAddToPlaylist = { playlistTarget = file; menuFile = null },
            onInfo = { infoTarget = file; infoReturnView = currentView; currentView = MainView.INFO; menuFile = null },
            onDelete = { menuFile = null; pendingDeleteFile = file },
            onShare = { shareFile(context, file); menuFile = null },
            onRingtone = { setAsRingtone(context, file); menuFile = null },
            onDismiss = { menuFile = null }
        )
    }
    if (showPlayerMenu) {
        PlayerOptionsDialog(
            file = state.musicFile,
            onSleepTimer = { showPlayerMenu = false; showSleepDialog = true },
            onSpeed = { showPlayerMenu = false; showSpeedDialog = true },
            onEqualizer = { showPlayerMenu = false; showEqualizer = true },
            onAddToPlaylist = {
                state.musicFile?.let { playlistTarget = it }
                showPlayerMenu = false
            },
            onInfo = {
                state.musicFile?.let { infoTarget = it; infoReturnView = currentView; currentView = MainView.INFO }
                showPlayerMenu = false
            },
            onRingtone = { state.musicFile?.let { setAsRingtone(context, it) }; showPlayerMenu = false },
            onShare = { state.musicFile?.let { shareFile(context, it) }; showPlayerMenu = false },
            onDismiss = { showPlayerMenu = false }
        )
    }
    playlistTarget?.let { file ->
        PlaylistPickerDialog(
            playlists = state.playlists,
            onSelect = { name ->
                if (name == "Favorito") viewModel.toggleFavorite(file) else viewModel.addToPlaylist(name, file)
                playlistTarget = null
            },
            onCreate = viewModel::createPlaylist,
            onDismiss = { playlistTarget = null }
        )
    }
    if (showSpeedDialog) {
        SpeedDialog(
            onSelect = { speed -> viewModel.setPlaybackSpeed(speed); showSpeedDialog = false },
            onDismiss = { showSpeedDialog = false }
        )
    }
    if (showSleepDialog) {
        SleepTimerDialog(
            onSelect = { minutes -> viewModel.startSleepTimer(minutes); showSleepDialog = false },
            onDismiss = { showSleepDialog = false }
        )
    }
    if (showEqualizer) {
        EqualizerDialog(onDismiss = { showEqualizer = false })
    }
    pendingDeleteFile?.let { file ->
        DeleteSongDialog(
            file = file,
            onConfirm = {
                viewModel.deleteSong(file) { request: PendingIntent ->
                    awaitingDeleteApproval = file
                    deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                }
                pendingDeleteFile = null
            },
            onDismiss = { pendingDeleteFile = null }
        )
    }
}

@Composable
private fun TopBar(
    currentView: MainView,
    isAlbumDetail: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentView != MainView.LIBRARY || isAlbumDetail) {
                IconButton(
                    onClick = onBack,
                    modifier = if (isAlbumDetail) Modifier.background(Color.Black.copy(alpha = 0.28f), CircleShape) else Modifier
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Atrás",
                        tint = if (isAlbumDetail) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (currentView == MainView.LIBRARY) {
                Icon(Icons.Default.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (!isAlbumDetail) {
                Text(
                    text = when (currentView) {
                        MainView.LIBRARY -> "Biblioteca musical"
                        MainView.PLAYER -> "Reproduciendo"
                        MainView.INFO -> "Información de la canción"
                        MainView.SETTINGS -> "Configuración"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentView != MainView.INFO && !isAlbumDetail) {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (currentView == MainView.PLAYER) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, "Más opciones", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun LibraryView(
    state: PlayerUiState,
    tab: LibraryTab,
    selectedPlaylistName: String?,
    selectedAlbumName: String?,
    albumArtworkPreferences: android.content.SharedPreferences,
    albumArtworkOverrideUri: String?,
    onChooseAlbumCover: (MusicFile?) -> Unit,
    selectedArtistName: String?,
    onTabChange: (LibraryTab) -> Unit,
    onPlaylistSelected: (String?) -> Unit,
    onAlbumSelected: (String?) -> Unit,
    onArtistSelected: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (String, com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onMore: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onAddToPlaylist: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    listState: LazyListState,
    albumDetailListState: LazyListState,
    onRefresh: () -> Unit,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    albumGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    artistGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenFile: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onPlayAlbum: (List<com.chrismdz.vinylplayer.data.MusicFile>, Boolean) -> Unit,
    onPlayAlbumTrack: (List<com.chrismdz.vinylplayer.data.MusicFile>, com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onOpenCurrent: () -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val filteredLibrary = remember(state.library, searchQuery) {
        filterLibrary(state.library, searchQuery, MusicFile::title, MusicFile::artist, MusicFile::album)
    }
    val selectedAlbumFiles = remember(state.library, selectedAlbumName) {
        selectedAlbumName?.let { album -> state.library.filter { it.album == album } }.orEmpty()
    }
    val albums = remember(state.library, searchQuery) {
        sortNamedGroups(groupMatchingLibraryItems(
            library = state.library,
            query = searchQuery,
            key = MusicFile::album,
            title = MusicFile::title,
            artist = MusicFile::artist,
            album = MusicFile::album
        ))
    }
    val artists = remember(filteredLibrary) { filteredLibrary.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var playlistName by rememberSaveable { mutableStateOf("") }
    val visiblePlaylists = remember(state.playlists, searchQuery) { filterPlaylists(state.playlists, searchQuery) }
    val showFavorites = searchQuery.isBlank() || "Favoritos".contains(searchQuery.trim(), ignoreCase = true)
    val selectedPlaylist = remember(state.library, state.favoriteUris, state.playlists, selectedPlaylistName) {
        when (selectedPlaylistName) {
            "Favoritos" -> Playlist("Favoritos", state.library.filter { it.uri.toString() in state.favoriteUris }.map { it.uri.toString() })
            null -> null
            else -> state.playlists.firstOrNull { it.name == selectedPlaylistName }
        }
    }
    val visiblePlaylistSongs = remember(state.library, selectedPlaylist, searchQuery) {
        playlistSongs(
            library = state.library,
            playlist = selectedPlaylist,
            query = searchQuery,
            uri = { it.uri.toString() },
            title = MusicFile::title,
            artist = MusicFile::artist,
            album = MusicFile::album
        )
    }
    val totalAlbums = remember(state.library) { state.library.map { it.album }.distinct().size }
    val totalArtists = remember(state.library) { state.library.map { it.artist }.distinct().size }
    val totalPlaylistCount = state.playlists.size + 1
    val (resultCount, totalCount, countUnit) = when (tab) {
        LibraryTab.TRACKS -> Triple(filteredLibrary.size, state.library.size, "canciones")
        LibraryTab.ALBUMS -> if (selectedAlbumName != null) {
            val albumFiles = state.library.filter { it.album == selectedAlbumName }
            Triple(albumFiles.size, albumFiles.size, "canciones del álbum")
        } else Triple(albums.size, totalAlbums, "álbumes")
        LibraryTab.ARTISTS -> if (selectedArtistName != null) {
            val artistFiles = state.library.filter { it.artist == selectedArtistName }
            Triple(artists[selectedArtistName]?.size ?: 0, artistFiles.size, "canciones del artista")
        } else Triple(artists.size, totalArtists, "artistas")
        LibraryTab.PLAYLISTS -> if (selectedPlaylistName != null) {
            val fullPlaylistCount = playlistSongs(state.library, selectedPlaylist, "", { it.uri.toString() }, MusicFile::title, MusicFile::artist, MusicFile::album).size
            Triple(visiblePlaylistSongs.size, fullPlaylistCount, "canciones")
        } else Triple(visiblePlaylists.size + if (showFavorites) 1 else 0, totalPlaylistCount, "listas")
    }
    val hasVisibleContent = when (tab) {
        LibraryTab.TRACKS -> filteredLibrary.isNotEmpty()
        LibraryTab.ALBUMS -> if (selectedAlbumName == null) albums.isNotEmpty() else selectedAlbumFiles.isNotEmpty()
        LibraryTab.ARTISTS -> if (selectedArtistName == null) artists.isNotEmpty() else artists[selectedArtistName]?.isNotEmpty() == true
        LibraryTab.PLAYLISTS -> if (selectedPlaylistName != null) visiblePlaylistSongs.isNotEmpty() else showFavorites || visiblePlaylists.isNotEmpty()
    }
    val alphabetItems = when {
        tab == LibraryTab.ALBUMS && selectedAlbumName == null -> albums.keys.toList()
        tab == LibraryTab.ARTISTS && selectedArtistName == null -> artists.keys.toList()
        tab == LibraryTab.PLAYLISTS && selectedPlaylistName == null -> (if (showFavorites) listOf("Favoritos") else emptyList()) + visiblePlaylists.map { it.name }
        else -> filteredLibrary.map { it.title }
    }
    var alphabetTarget by remember { mutableStateOf(-1) }
    LaunchedEffect(alphabetTarget, tab, selectedAlbumName, selectedArtistName, selectedPlaylistName) {
        if (alphabetTarget >= 0) {
            when (tab) {
                LibraryTab.ALBUMS -> albumGridState.scrollToItem(alphabetTarget)
                LibraryTab.ARTISTS -> artistGridState.scrollToItem(alphabetTarget)
                else -> listState.scrollToItem(alphabetTarget)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (tab != LibraryTab.ALBUMS || selectedAlbumName == null) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(when (tab) {
                LibraryTab.TRACKS -> "Buscar canción, artista o álbum"
                LibraryTab.ALBUMS -> "Buscar álbum, artista o canción"
                LibraryTab.PLAYLISTS -> if (selectedPlaylistName == null) "Buscar playlist" else "Buscar canción en playlist"
                LibraryTab.ARTISTS -> "Buscar artista o canción"
            }) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Limpiar búsqueda")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        LibraryTabs(tab, onTabChange)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (searchQuery.isBlank() || (tab == LibraryTab.ALBUMS && selectedAlbumName != null)) {
                    "$resultCount $countUnit"
                } else "$resultCount de $totalCount $countUnit",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
            TextButton(onClick = onRefresh) { Text("Actualizar") }
        }
        }

        if (!hasVisibleContent) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LibraryMusic, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    if (searchQuery.isBlank()) when (tab) {
                        LibraryTab.TRACKS -> "No hay canciones disponibles"
                        LibraryTab.ALBUMS -> "No hay álbumes disponibles"
                        LibraryTab.PLAYLISTS -> if (selectedPlaylistName == null) "No hay playlists guardadas" else "Esta playlist está vacía"
                        LibraryTab.ARTISTS -> "No hay artistas disponibles"
                    } else "No se encontraron resultados",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (searchQuery.isBlank() && state.library.isEmpty()) "Concede el permiso de audio y toca Actualizar."
                    else "Prueba con otro nombre o borra la búsqueda.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    LibraryTab.TRACKS -> SongList(
                        files = filteredLibrary,
                        listState = listState,
                        state = state,
                        onRequestArtwork = onRequestArtwork,
                        onOpenFile = onOpenFile,
                        onMore = onMore,
                        onAddToPlaylist = onAddToPlaylist
                    )
                    LibraryTab.ALBUMS -> if (selectedAlbumName == null) {
                        AlbumGrid(
                            groups = albums,
                            gridState = albumGridState,
                            artworkPreferences = albumArtworkPreferences,
                            onRequestArtwork = onRequestArtwork,
                            onSelect = onAlbumSelected
                        )
                    } else {
                        AlbumDetailContent(
                            name = selectedAlbumName,
                            files = selectedAlbumFiles,
                            cover = chooseAlbumCoverTrack(
                                selectedAlbumFiles,
                                albumArtworkOverrideUri,
                                { it.uri.toString() }
                            ),
                            currentFile = state.musicFile,
                            listState = albumDetailListState,
                            onRequestArtwork = onRequestArtwork,
                            onOpenFile = { file -> onPlayAlbumTrack(selectedAlbumFiles, file) },
                            onPlay = { shuffle -> onPlayAlbum(selectedAlbumFiles, shuffle) },
                            onChooseAlbumCover = onChooseAlbumCover,
                            selectedAlbumArtworkOverrideUri = albumArtworkOverrideUri,
                            onMore = onMore
                        )
                    }
                    LibraryTab.ARTISTS -> if (selectedArtistName == null) {
                        ArtistGrid(
                            groups = artists,
                            gridState = artistGridState,
                            onRequestArtwork = onRequestArtwork,
                            onSelect = onArtistSelected
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            TextButton(onClick = { onArtistSelected(null) }) { Text("‹ Artistas") }
                            GroupedSongList(
                                groups = artists.filterKeys { it == selectedArtistName },
                                listState = listState,
                                state = state,
                                onRequestArtwork = onRequestArtwork,
                                onOpenFile = onOpenFile,
                                onMore = onMore,
                                onAddToPlaylist = onAddToPlaylist
                            )
                        }
                    }
                    LibraryTab.PLAYLISTS -> {
                        if (selectedPlaylistName == null) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (showFavorites) item {
                                    PlaylistRow(
                                        name = "Favoritos",
                                        count = state.library.count { it.uri.toString() in state.favoriteUris },
                                        onClick = { onPlaylistSelected("Favoritos") }
                                    )
                                }
                                items(visiblePlaylists, key = { it.name }) { playlist ->
                                    PlaylistRow(
                                        name = playlist.name,
                                        count = playlist.songUris.size,
                                        onClick = { onPlaylistSelected(playlist.name) },
                                        onDelete = { onDeletePlaylist(playlist.name) }
                                    )
                                }
                                item {
                                    TextButton(onClick = { showCreatePlaylist = true }) {
                                        Text("+ Nueva Playlist", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onPlaylistSelected(null) }) { Text("‹ Listas") }
                                    Text(
                                        selectedPlaylistName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                SongList(
                                    files = visiblePlaylistSongs,
                                    listState = rememberLazyListState(),
                                    state = state,
                                    onRequestArtwork = onRequestArtwork,
                                    onOpenFile = onOpenFile,
                                    onRemove = { file -> onRemoveFromPlaylist(selectedPlaylistName, file) },
                                    onMore = onMore,
                                    onAddToPlaylist = onAddToPlaylist
                                )
                            }
                        }
                    }
                }
                if (alphabetItems.isNotEmpty() && !(tab == LibraryTab.PLAYLISTS && selectedPlaylistName != null) &&
                    !(tab == LibraryTab.ALBUMS && selectedAlbumName != null) &&
                    !(tab == LibraryTab.ARTISTS && selectedArtistName != null)) {
                    AlphabetIndex(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) { letter ->
                        val index = indexForLetter(alphabetItems, letter)
                        if (index >= 0) alphabetTarget = index
                    }
                }
            }
        }

        state.musicFile?.let { current ->
            Spacer(modifier = Modifier.height(12.dp))
            MiniPlayer(
                file = current,
                isPlaying = state.isPlaying,
                onClick = onOpenCurrent,
                onTogglePlayback = onTogglePlayback,
                onFavorite = onFavorite,
                isFavorite = isFavorite
            )
        }
    }
    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Nueva Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreatePlaylist(playlistName)
                    playlistName = ""
                    showCreatePlaylist = false
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun LibraryTabs(tab: LibraryTab, onTabChange: (LibraryTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            LibraryTab.TRACKS to "Pistas",
            LibraryTab.ALBUMS to "Álbumes",
            LibraryTab.PLAYLISTS to "Playlist",
            LibraryTab.ARTISTS to "Artistas"
        ).forEach { (item, label) ->
            TextButton(onClick = { onTabChange(item) }) {
                Text(
                    label,
                    color = if (item == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontWeight = if (item == tab) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SongList(
    files: List<com.chrismdz.vinylplayer.data.MusicFile>,
    listState: LazyListState,
    state: PlayerUiState,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onOpenFile: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onMore: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onAddToPlaylist: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onRemove: ((com.chrismdz.vinylplayer.data.MusicFile) -> Unit)? = null
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.uri.toString() }) { file ->
            MusicRow(
                file = file,
                isCurrent = file.uri == state.musicFile?.uri,
                isFavorite = file.uri.toString() in state.favoriteUris,
                onRequestArtwork = onRequestArtwork,
                onClick = { onOpenFile(file) },
                onMore = { onMore(file) },
                onAddToPlaylist = { onAddToPlaylist(file) },
                onRemove = onRemove?.let { callback -> { callback(file) } }
            )
        }
    }
}

@Composable
private fun AlbumGrid(
    groups: Map<String, List<com.chrismdz.vinylplayer.data.MusicFile>>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    artworkPreferences: android.content.SharedPreferences,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(groups.entries.toList(), key = { it.key }) { entry ->
            val coverPreferenceKey = albumArtworkPreferenceKey(entry.key, entry.value)
            val overrideUri = artworkPreferences.getString(coverPreferenceKey, null)
            val cover = chooseAlbumCoverTrack(entry.value, overrideUri) { it.uri.toString() }
            LaunchedEffect(cover?.uri, cover?.artwork) {
                if (cover != null && cover.artwork == null) onRequestArtwork(cover)
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(entry.key) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
            ) {
                ArtworkTile(cover)
                Column(Modifier.padding(10.dp)) {
                    Text(entry.key, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${entry.value.size} pistas", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailContent(
    name: String,
    files: List<MusicFile>,
    cover: MusicFile?,
    currentFile: MusicFile?,
    listState: LazyListState,
    onRequestArtwork: (MusicFile) -> Unit,
    onOpenFile: (MusicFile) -> Unit,
    onPlay: (shuffle: Boolean) -> Unit,
    onChooseAlbumCover: (MusicFile?) -> Unit,
    selectedAlbumArtworkOverrideUri: String?,
    onMore: (MusicFile) -> Unit
) {
    val albumArtists = remember(files) {
        consolidateAlbumArtists(files.map(MusicFile::artist))
    }
    val otherLeadArtists = albumArtists.participants.filter {
        it != albumArtists.primaryArtist && it !in albumArtists.collaborators
    }
    var albumTitleExpanded by remember(name) { mutableStateOf(false) }
    val featuringText = "Featuring · ${albumArtists.collaborators.joinToString(", ")}"
    var featuringExpanded by remember(name, featuringText) { mutableStateOf(false) }
    var showAlbumArtworkPicker by remember(name) { mutableStateOf(false) }
    val palette = remember(cover?.artwork) { cover?.artwork?.let(CoverPalette::fromBitmap).orEmpty() }
    val accent = readableAlbumAccent(palette.firstOrNull())
    val playForeground = Color(AccentColorValue.highContrastForeground(accent.toArgb(), Color.Black.toArgb()))
    LaunchedEffect(cover?.uri, cover?.artwork) {
        if (cover != null && cover.artwork == null) onRequestArtwork(cover)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item(key = "album-detail-$name") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    val coverSide = minOf(maxWidth * 0.76f, 340.dp)
                    Box(
                        modifier = Modifier
                            .size(coverSide)
                            .align(Alignment.Center)
                            .shadow(18.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cover?.artwork != null) {
                            Image(cover.artwork.asImageBitmap(), "Portada del álbum $name", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(54.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    name,
                    modifier = Modifier.fillMaxWidth().clickable { albumTitleExpanded = !albumTitleExpanded },
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (albumTitleExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                albumArtists.primaryArtist?.let { primaryArtist ->
                    Text(
                        primaryArtist,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
                if (albumArtists.collaborators.isNotEmpty()) {
                    Text(
                        featuringText,
                        modifier = Modifier.fillMaxWidth().clickable { featuringExpanded = !featuringExpanded },
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 15.sp,
                        maxLines = if (featuringExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (otherLeadArtists.isNotEmpty()) {
                    Text(
                        "Otros artistas principales · ${otherLeadArtists.joinToString(", ")}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Álbum · ${files.size} canciones", color = Color.White.copy(alpha = 0.68f), fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    IconButton(
                        onClick = { onPlay(true) },
                        modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(Icons.Default.Shuffle, "Reproducir álbum aleatoriamente", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onPlay(false) },
                        modifier = Modifier.size(58.dp).background(accent, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Reproducir álbum", tint = playForeground, modifier = Modifier.size(34.dp))
                    }
                    IconButton(
                        onClick = { showAlbumArtworkPicker = true },
                        modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(Icons.Default.LibraryMusic, "Elegir portada del álbum", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
            AlbumTrackRow(
                index = index,
                file = file,
                isCurrent = file.uri == currentFile?.uri,
                accent = accent,
                onClick = { onOpenFile(file) },
                onMore = { onMore(file) }
            )
        }
    }

    if (showAlbumArtworkPicker) {
        AlbumArtworkPickerDialog(
            tracks = files,
            selectedTrackUri = selectedAlbumArtworkOverrideUri,
            onRequestArtwork = onRequestArtwork,
            onSelectTrack = { track ->
                onChooseAlbumCover(track)
                showAlbumArtworkPicker = false
            },
            onDismiss = { showAlbumArtworkPicker = false }
        )
    }
}

@Composable
private fun AlbumArtworkPickerDialog(
    tracks: List<MusicFile>,
    selectedTrackUri: String?,
    onRequestArtwork: (MusicFile) -> Unit,
    onSelectTrack: (MusicFile?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Portada del álbum") },
        text = {
            Column(Modifier.heightIn(max = 440.dp)) {
                Text("Elige una pista para usar su portada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    item(key = "default-cover") {
                        AlbumArtworkChoiceRow(
                            track = tracks.firstOrNull(),
                            title = "Predeterminada · primer track",
                            subtitle = tracks.firstOrNull()?.title ?: "",
                            selected = selectedTrackUri == null,
                            onRequestArtwork = onRequestArtwork,
                            onClick = { onSelectTrack(null) }
                        )
                    }
                    items(tracks, key = { "cover-${it.uri}" }) { track ->
                        AlbumArtworkChoiceRow(
                            track = track,
                            title = track.title,
                            subtitle = if (track.artwork == null) "Esta pista no tiene portada cargada" else "Usar esta portada",
                            selected = selectedTrackUri == track.uri.toString(),
                            onRequestArtwork = onRequestArtwork,
                            onClick = { onSelectTrack(track) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun AlbumArtworkChoiceRow(
    track: MusicFile?,
    title: String,
    subtitle: String,
    selected: Boolean,
    onRequestArtwork: (MusicFile) -> Unit,
    onClick: () -> Unit
) {
    LaunchedEffect(track?.uri, track?.artwork) {
        if (track != null && track.artwork == null) onRequestArtwork(track)
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkTile(track, Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    file: MusicFile,
    isCurrent: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 8.dp, end = 0.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            Icon(Icons.Default.PlayArrow, "Reproduciendo", tint = accent, modifier = Modifier.width(28.dp))
        } else {
            Text((index + 1).toString(), color = Color.White.copy(alpha = 0.62f), fontSize = 15.sp, modifier = Modifier.width(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(file.title, color = if (isCurrent) accent else Color.White, fontSize = 16.sp, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            Text(
                file.artist,
                color = if (isCurrent) accent else Color.White.copy(alpha = 0.68f),
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, "Opciones de ${file.title}", tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ArtistGrid(
    groups: Map<String, List<com.chrismdz.vinylplayer.data.MusicFile>>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(groups.entries.toList(), key = { it.key }) { entry ->
            val cover = entry.value.firstOrNull()
            LaunchedEffect(cover?.uri, cover?.artwork) {
                if (cover != null && cover.artwork == null) onRequestArtwork(cover)
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(entry.key) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    ArtworkTile(cover, Modifier.size(108.dp).clip(CircleShape))
                }
                Column(Modifier.padding(10.dp)) {
                    Text(entry.key, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("${entry.value.size} pistas", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ArtworkTile(
    file: com.chrismdz.vinylplayer.data.MusicFile?,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1f)
) {
    if (file?.artwork != null) {
        Image(file.artwork.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
    }
}

private fun indexForLetter(items: List<String>, letter: Char): Int {
    return if (letter == '#') {
        items.indexOfFirst { it.trim().firstOrNull()?.isLetter() != true }
    } else {
        items.indexOfFirst { it.trim().firstOrNull()?.uppercaseChar() == letter }
    }
}

@Composable
private fun GroupedSongList(
    groups: Map<String, List<com.chrismdz.vinylplayer.data.MusicFile>>,
    listState: LazyListState,
    state: PlayerUiState,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onOpenFile: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onMore: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onAddToPlaylist: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { (name, files) ->
            item(key = "header-$name") {
                Text(name, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            items(files, key = { it.uri.toString() }) { file ->
                MusicRow(
                    file = file,
                    isCurrent = file.uri == state.musicFile?.uri,
                    isFavorite = file.uri.toString() in state.favoriteUris,
                    onRequestArtwork = onRequestArtwork,
                    onClick = { onOpenFile(file) },
                    onMore = { onMore(file) },
                    onAddToPlaylist = { onAddToPlaylist(file) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text("$count canciones", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 12.sp)
        }
        onDelete?.let { delete ->
            IconButton(onClick = delete) { Icon(Icons.Default.MoreVert, "Opciones", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)) }
        }
    }
}

@Composable
private fun AlphabetIndex(modifier: Modifier = Modifier, onLetterClick: (Char) -> Unit) {
    val letters = listOf('#') + ('A'..'Z').toList()
    Column(
        modifier = modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { offset ->
                    val index = (offset.y / size.height * letters.size).roundToInt().coerceIn(0, letters.lastIndex)
                    onLetterClick(letters[index])
                },
                onVerticalDrag = { change, _ ->
                    change.consume()
                    val index = (change.position.y / size.height * letters.size).roundToInt().coerceIn(0, letters.lastIndex)
                    onLetterClick(letters[index])
                }
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                letter.toString(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontSize = 9.sp,
                modifier = Modifier.clickable { onLetterClick(letter) }.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun MusicRow(
    file: com.chrismdz.vinylplayer.data.MusicFile,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    LaunchedEffect(file.uri, file.artwork) {
        if (file.artwork == null) onRequestArtwork(file)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Color(0x55E4B45B) else MaterialTheme.colorScheme.surface.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.artwork != null) {
            Image(
                bitmap = file.artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(file.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${file.artist} · ${file.album}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (isFavorite) Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, "Opciones", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun MiniPlayer(
    file: com.chrismdz.vinylplayer.data.MusicFile,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val backgroundColor = remember(file.artwork) { miniPlayerBackgroundColor(file.artwork) }
    val foregroundColor = Color(
        AccentColorValue.highContrastForeground(backgroundColor.toArgb(), MaterialTheme.colorScheme.background.toArgb())
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        file.artwork?.let {
            Image(it.asImageBitmap(), null, Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(file.title, color = foregroundColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${file.artist} · ${file.album}", color = foregroundColor.copy(alpha = 0.72f), fontSize = 11.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorito",
                tint = foregroundColor
            )
        }
        IconButton(onClick = onTogglePlayback) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir", tint = foregroundColor)
        }
    }
}

@Composable
private fun PlayerView(
    state: PlayerUiState,
    tab: PlayerTab,
    onTabChange: (PlayerTab) -> Unit,
    onLyricsOffsetChange: (Long) -> Unit,
    onToggleLyricsTranslation: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit
) {
    val file = state.musicFile
    if (file == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecciona una canción desde la biblioteca.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTabSwitcher(tab, state.appSettings.lyricsEnabled, onTabChange)
        if (tab == PlayerTab.COVER || !state.appSettings.lyricsEnabled) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VinylView(
                        artwork = file.artwork,
                        isPlaying = state.isPlaying,
                        tonearmAnimationEnabled = state.tonearmAnimationEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(file.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = onFavorite) {
                        Icon(if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "${file.artist} · ${file.album}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = progress(state.positionMs, state.durationMs),
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = playerSliderColors()
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(formatTime(state.positionMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 11.sp)
                    Text(formatTime(state.durationMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 11.sp)
                }
                PlayerTransportControls(
                    state = state,
                    onTogglePlayback = onTogglePlayback,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onQueue = onQueue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            var followLyrics by remember(file.uri) { mutableStateOf(true) }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val coverSize = when {
                    maxHeight < 420.dp -> 52.dp
                    maxHeight < 540.dp -> 68.dp
                    maxHeight < 640.dp -> 84.dp
                    else -> 100.dp
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LyricsPlayerHeader(
                        file = file,
                        state = state,
                        isFavorite = state.isFavorite,
                        onFavorite = onFavorite,
                        onSeek = {
                            onSeek(it)
                            followLyrics = true
                        },
                        coverSize = coverSize
                    )
                    if (state.lyricsStatus == LyricsStatus.AVAILABLE) {
                        LyricsOptionsRow(
                            showTranslation = state.showTranslatedLyrics,
                            translationEnabled = state.appSettings.lyricsTranslationEnabled,
                            translationLoading = state.translationLoading,
                            translationError = state.translationError,
                            offsetMs = state.lyricsOffsetMs,
                            onToggleTranslation = onToggleLyricsTranslation,
                            onOffsetChange = onLyricsOffsetChange
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        key(file.uri) {
                            LyricsOverlay(
                                lyrics = state.lyrics,
                                status = state.lyricsStatus,
                                positionMs = state.positionMs,
                                offsetMs = state.lyricsOffsetMs,
                                showTranslation = state.showTranslatedLyrics,
                                fontSizeSp = state.appSettings.lyricsFontSizeSp,
                                boldText = state.appSettings.lyricsBoldText,
                                previousLines = state.appSettings.lyricsPreviousLines,
                                nextLines = state.appSettings.lyricsNextLines,
                                followLyrics = followLyrics,
                                onManualScroll = { followLyrics = false },
                                onResumeFollow = { followLyrics = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    PlayerTransportControls(
                        state = state,
                        onTogglePlayback = onTogglePlayback,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onShuffle = onShuffle,
                        onRepeat = onRepeat,
                        onQueue = onQueue,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsPlayerHeader(
    file: MusicFile,
    state: PlayerUiState,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onSeek: (Float) -> Unit,
    coverSize: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(coverSize)
                .shadow(14.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF31131B)),
            contentAlignment = Alignment.Center
        ) {
            if (file.artwork != null) {
                Image(
                    bitmap = file.artwork.asImageBitmap(),
                    contentDescription = "Portada de ${file.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 32.dp)) {
            Text(
                file.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 44.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorito",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (coverSize < 84.dp) {
            Text(
                listOf(file.artist, file.album).filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                fontSize = 12.sp,
                maxLines = 1
            )
        } else {
            Text(file.artist, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 1)
            if (file.album.isNotBlank()) {
                Text(file.album, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 11.sp, maxLines = 1)
            }
        }
        Slider(
            value = progress(state.positionMs, state.durationMs),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth(),
            colors = playerSliderColors()
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 11.sp)
            Text(formatTime(state.durationMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LyricsOptionsRow(
    showTranslation: Boolean,
    translationEnabled: Boolean,
    translationLoading: Boolean,
    translationError: String?,
    offsetMs: Long,
    onToggleTranslation: () -> Unit,
    onOffsetChange: (Long) -> Unit
) {
    var fineStep by rememberSaveable { mutableStateOf(true) }
    val stepMs = if (fineStep) 500L else 5_000L
    val stepLabel = if (fineStep) "0,5 s" else "5 s"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compactControls = maxWidth < 340.dp
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (translationEnabled) {
                    TextButton(onClick = onToggleTranslation, enabled = !translationLoading, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                        Text(if (showTranslation) "Original" else "Traducir", fontSize = 12.sp, maxLines = 1)
                    }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
                if (compactControls) {
                    Spacer(Modifier.weight(1f))
                } else {
                    IconButton(onClick = { onOffsetChange(-stepMs) }, modifier = Modifier.size(42.dp)) { Text("−$stepLabel", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp) }
                    Text(formatOffset(offsetMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 11.sp)
                    IconButton(onClick = { onOffsetChange(stepMs) }, modifier = Modifier.size(42.dp)) { Text("+$stepLabel", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp) }
                }
                TextButton(
                    onClick = { fineStep = !fineStep },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Paso $stepLabel", fontSize = 10.sp, maxLines = 1)
                }
                IconButton(
                    onClick = { onOffsetChange(-offsetMs) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Restablecer sincronización", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                }
            }
            if (compactControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { onOffsetChange(-stepMs) }, modifier = Modifier.size(42.dp)) { Text("−$stepLabel", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp) }
                    Text(formatOffset(offsetMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 11.sp)
                    IconButton(onClick = { onOffsetChange(stepMs) }, modifier = Modifier.size(42.dp)) { Text("+$stepLabel", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp) }
                }
            }
            if (translationLoading || translationError != null) {
                Text(
                    text = if (translationLoading) "Traduciendo letra..." else translationError.orEmpty(),
                    color = if (translationLoading) Color(0xFFFFD5A6) else Color(0xFFFFB4AB),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun playerSliderColors() = androidx.compose.material3.SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
)

@Composable
private fun PlayerTransportControls(
    state: PlayerUiState,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(if (compact) 2.dp else 8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onShuffle) {
                Icon(Icons.Default.Shuffle, "Reproducción aleatoria", tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ArrowBack, "Anterior", tint = MaterialTheme.colorScheme.onSurface) }
            Surface(Modifier.size(if (compact) 52.dp else 70.dp), CircleShape, MaterialTheme.colorScheme.primary) {
                IconButton(onClick = onTogglePlayback) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowBack, "Siguiente", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.graphicsLayer { scaleX = -1f }) }
            IconButton(onClick = onRepeat) {
                Icon(
                    if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Modo: ${state.repeatMode.name}",
                    tint = if (state.repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primary
                )
            }
            if (compact) {
                IconButton(onClick = onQueue) {
                    Icon(Icons.Default.QueueMusic, "Fila de reproducción", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (!compact) {
            TextButton(onClick = onQueue, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.QueueMusic, null)
                Spacer(Modifier.width(6.dp))
                Text("Fila de reproducción")
            }
        }
    }
}

private enum class SongTagField(val label: String) {
    TITLE("Título de la canción"),
    ARTIST("Artista"),
    ALBUM("Álbum")
}

@Composable
private fun SongInfoView(
    file: MusicFile,
    errorMessage: String?,
    onBack: () -> Unit,
    onEditTags: (MusicFile, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var editField by remember(file.uri) { mutableStateOf<SongTagField?>(null) }
    val canEditTags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && AudioTagWriter.supportsFileName(file.filePath)
    val fileSize = remember(file.uri) {
        runCatching {
            context.contentResolver.openAssetFileDescriptor(file.uri, "r")?.use { it.length }
        }.getOrNull() ?: 0L
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        file.artwork?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Portada",
                modifier = Modifier
                    .padding(horizontal = 74.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Fit
            )
        }
        InfoEntry("Título de la canción", file.title, canEditTags, onClick = { editField = SongTagField.TITLE })
        InfoEntry("Artista", file.artist, canEditTags, onClick = { editField = SongTagField.ARTIST })
        InfoEntry("Álbum", file.album, canEditTags, onClick = { editField = SongTagField.ALBUM })
        if (!canEditTags) {
            Text(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) "La edición segura de etiquetas requiere Android 10 o posterior."
                else "La edición de etiquetas no está disponible para este formato.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        InfoEntry("Duración", formatTime(file.durationMs))
        InfoEntry("Tamaño del archivo", formatFileSize(fileSize))
        InfoEntry("Portada de canción personalizada", "", true)
        InfoEntry("Ruta del archivo", file.filePath ?: file.uri.toString(), valueMaxLines = Int.MAX_VALUE)
        InfoEntry("Especificar manualmente el archivo de letra", "Solo admite archivos de letras en formatos lrc/alm3/qrc/txt", true)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) { Text("Volver") }
    }
    editField?.let { field ->
        val initial = when (field) {
            SongTagField.TITLE -> file.title
            SongTagField.ARTIST -> file.artist
            SongTagField.ALBUM -> file.album
        }
        SongTagEditDialog(field, initial, onDismiss = { editField = null }) { value ->
            onEditTags(
                file,
                if (field == SongTagField.TITLE) value else file.title,
                if (field == SongTagField.ARTIST) value else file.artist,
                if (field == SongTagField.ALBUM) value else file.album
            )
            editField = null
        }
    }
}

@Composable
private fun SongTagEditDialog(field: SongTagField, initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(field, initialValue) { mutableStateOf(initialValue) }
    val isValid = field != SongTagField.TITLE || value.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar " + field.label.lowercase()) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(field.label) }
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = isValid) { Text("Guardar en archivo") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun InfoEntry(
    label: String,
    value: String,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
    valueMaxLines: Int = 2
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (value.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 15.sp, maxLines = valueMaxLines)
            }
        }
        if (showArrow) Text("›", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 32.sp)
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)))
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Desconocido"
    val megabytes = bytes / 1_000_000.0
    return if (megabytes >= 1.0) String.format(java.util.Locale.US, "%.1f MB", megabytes)
    else "${bytes / 1_000} KB"
}

private fun paletteBackgroundColors(palette: List<Int>): List<Color> {
    val colors = palette.map { Color(it) }
    val dominant = colors.firstOrNull() ?: return emptyList()
    val combined = colors.take(3).fold(Triple(0f, 0f, 0f)) { acc, color ->
        Triple(acc.first + color.red, acc.second + color.green, acc.third + color.blue)
    }.let { (red, green, blue) ->
        val count = colors.take(3).size.toFloat()
        Color(red / count, green / count, blue / count)
    }
    return listOf(
        shade(dominant, 0.58f),
        shade(combined, 0.36f),
        Color(0xFF090B0B)
    )
}

private fun readableAlbumAccent(argb: Int?): Color {
    val source = argb?.let(::Color) ?: Color.White
    val hsl = AccentColorValue.toHsl(source.toArgb())
    if (hsl.lightness >= 0.72f) return source
    return Color(AccentColorValue.fromHsl(hsl.hue, hsl.saturation, 0.72f))
}

private fun miniPlayerBackgroundColor(artwork: android.graphics.Bitmap?): Color {
    val argb = artwork?.let(CoverPalette::fromBitmap)?.firstOrNull() ?: return Color(0xFF202020)
    val hsl = AccentColorValue.toHsl(argb)
    val saturation = if (hsl.saturation < 0.08f) 0f else hsl.saturation.coerceAtLeast(0.38f).coerceAtMost(0.92f)
    return Color(AccentColorValue.fromHsl(hsl.hue, saturation, hsl.lightness.coerceIn(0.23f, 0.37f)))
}

private fun albumArtworkPreferenceKey(albumName: String, tracks: List<MusicFile>): String {
    val primaryArtist = consolidateAlbumArtists(tracks.map(MusicFile::artist)).primaryArtist.orEmpty()
    return "album-cover-${android.net.Uri.encode(albumName)}-${android.net.Uri.encode(primaryArtist)}"
}

private fun shade(color: Color, amount: Float): Color =
    Color(color.red * amount, color.green * amount, color.blue * amount, color.alpha)

@Composable
private fun PlayerTabSwitcher(
    tab: PlayerTab,
    lyricsEnabled: Boolean,
    onTabChange: (PlayerTab) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        PlayerTabButton("PORTADA", tab == PlayerTab.COVER) { onTabChange(PlayerTab.COVER) }
        if (lyricsEnabled) PlayerTabButton("LETRA", tab == PlayerTab.LYRICS) { onTabChange(PlayerTab.LYRICS) }
    }
}

@Composable
private fun PlayerTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.95f else 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp, vertical = 10.dp)
    )
}

@Composable
private fun LyricsOverlay(
    lyrics: Lyrics?,
    status: LyricsStatus,
    positionMs: Long,
    offsetMs: Long,
    showTranslation: Boolean,
    fontSizeSp: Int,
    boldText: Boolean,
    previousLines: Int,
    nextLines: Int,
    followLyrics: Boolean,
    onManualScroll: () -> Unit,
    onResumeFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSyncedText = if (showTranslation) lyrics?.translatedSyncedText else lyrics?.syncedText
    val activePlainText = if (showTranslation) lyrics?.translatedText else lyrics?.text
    val syncedLines = remember(activeSyncedText) {
        activeSyncedText?.let(::parseSyncedLyrics).orEmpty()
    }
    val currentLine = syncedLines.indexOfLast { it.startMs <= positionMs + offsetMs }
    val firstVisibleLine = if (currentLine >= 0) (currentLine - previousLines).coerceAtLeast(0) else 0
    val lastVisibleLine = if (currentLine >= 0) {
        (currentLine + nextLines).coerceAtMost(syncedLines.lastIndex)
    } else {
        nextLines.coerceAtMost(syncedLines.lastIndex)
    }
    val visibleSyncedLines = if (syncedLines.isNotEmpty() && lastVisibleLine >= firstVisibleLine) {
        syncedLines.subList(firstVisibleLine, lastVisibleLine + 1)
    } else {
        emptyList()
    }
    val listState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val verticalInset = (maxHeight / 2f - 34.dp).coerceAtLeast(24.dp)
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress to isAutoScrolling }.collect { (scrolling, autoScrolling) ->
                if (scrolling && !autoScrolling) onManualScroll()
            }
        }
        LaunchedEffect(currentLine, status, firstVisibleLine, followLyrics) {
            if (followLyrics && status == LyricsStatus.AVAILABLE && syncedLines.isNotEmpty()) {
                isAutoScrolling = true
                try {
                    val targetLine = if (currentLine < 0) 0 else currentLine - firstVisibleLine
                    listState.animateScrollToItem(targetLine.coerceIn(0, visibleSyncedLines.lastIndex.coerceAtLeast(0)))
                } finally {
                    isAutoScrolling = false
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (status) {
                LyricsStatus.AVAILABLE -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = if (syncedLines.isNotEmpty()) verticalInset else 0.dp,
                            bottom = if (syncedLines.isNotEmpty()) verticalInset else 0.dp
                        ),
                        verticalArrangement = if (syncedLines.isNotEmpty()) {
                            Arrangement.spacedBy(14.dp)
                        } else {
                            Arrangement.spacedBy(4.dp)
                        }
                    ) {
                        if (syncedLines.isNotEmpty()) {
                            itemsIndexed(visibleSyncedLines) { index, line ->
                                val absoluteIndex = index + firstVisibleLine
                                val distance = if (currentLine >= 0) abs(absoluteIndex - currentLine) else 1
                                Text(
                                    line.text,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = when (distance) {
                                        0 -> 1f
                                        1 -> 0.72f
                                        else -> 0.42f
                                    }),
                                    fontSize = when (distance) {
                                        0 -> (fontSizeSp + 5).sp
                                        1 -> fontSizeSp.sp
                                        else -> (fontSizeSp - 3).coerceAtLeast(13).sp
                                    },
                                    fontWeight = if (!boldText) FontWeight.Normal else if (distance == 0) FontWeight.Bold else FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), blurRadius = 8f)
                                    )
                                )
                            }
                        } else {
                            items(activePlainText?.lines().orEmpty()) { line ->
                                Text(
                                    line,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                    fontSize = fontSizeSp.sp,
                                    fontWeight = if (boldText) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), blurRadius = 8f)
                                    )
                                )
                            }
                        }
                    }
                    if (!followLyrics && syncedLines.isNotEmpty()) {
                        TextButton(
                            onClick = onResumeFollow,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xAA211116))
                        ) {
                            Text("Seguir letra", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }
                LyricsStatus.LOADING -> LyricsStatusMessage("Cargando letra...")
                LyricsStatus.NOT_FOUND -> LyricsStatusMessage("Letra no encontrada")
                LyricsStatus.OFFLINE -> LyricsStatusMessage("Sin conexión")
                LyricsStatus.TEMPORARY_ERROR -> LyricsStatusMessage("No se pudo cargar la letra")
                LyricsStatus.INSUFFICIENT_METADATA -> LyricsStatusMessage("Metadatos insuficientes")
                LyricsStatus.IDLE -> LyricsStatusMessage("Selecciona una canción")
            }
        }
    }
}

@Composable
private fun LyricsStatusMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp)
        )
    }
}

private fun formatOffset(offsetMs: Long): String {
    val seconds = offsetMs / 1_000.0
    return String.format(java.util.Locale.US, "%+.1f s", seconds)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SongOptionsDialog(
    file: MusicFile,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRingtone: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(file.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
            Text("${file.artist} · ${file.album}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            MenuAction("Reproducir siguiente", onPlayNext)
            MenuAction("Agregar a fila de reproducción", onAddToQueue)
            MenuAction("Agregar a Playlist", onAddToPlaylist)
            MenuAction("Información de la canción", onInfo)
            MenuAction("Establecer como tono de llamada", onRingtone)
            MenuAction("Compartir archivo", onShare)
            MenuAction("Eliminar canción", onDelete, Color(0xFFFF6B6B))
            MenuAction("Cancelar", onDismiss, Color(0xFFFF6B6B))
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerOptionsDialog(
    file: MusicFile?,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onEqualizer: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onInfo: () -> Unit,
    onRingtone: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Opciones", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            MenuAction("Apagado automático", onSleepTimer)
            MenuAction("Velocidad", onSpeed)
            MenuAction("Ecualizador", onEqualizer)
            if (file != null) {
                MenuAction("Agregar a Playlist", onAddToPlaylist)
                MenuAction("Información de la canción", onInfo)
                MenuAction("Establecer como tono de llamada", onRingtone)
                MenuAction("Compartir archivo", onShare)
            }
            MenuAction("Cancelar", onDismiss, Color(0xFFFF6B6B))
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun MenuAction(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        label,
        color = color,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var showNewName by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            MenuAction("Cancelar", onDismiss, Color(0xFFFF6B6B))
            MenuAction("＋   Nueva lista de reproducción", onClick = { showNewName = true })
            MenuAction("♡   Favorito", onClick = { onSelect("Favorito") })
            playlists.forEach { playlist ->
                MenuAction("♫   ${playlist.name}", onClick = { onSelect(playlist.name) })
            }
            if (showNewName) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nombre de la nueva lista") }
                )
                TextButton(onClick = {
                    val cleanName = newName.trim()
                    if (cleanName.isNotEmpty()) {
                        onCreate(cleanName)
                        onSelect(cleanName)
                    }
                }) { Text("Crear y agregar") }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SongInfoDialog(file: MusicFile, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.title) },
        text = {
            Text(
                "Artista: ${file.artist}\nÁlbum: ${file.album}\nDuración: ${formatTime(file.durationMs)}\n\n${file.uri}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SpeedDialog(onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(1.0f) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Selección múltiple", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            speeds.forEach { speed ->
                Row(
                    Modifier.fillMaxWidth().clickable { selected = speed; onSelect(speed) }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (speed == 1f) "Normal" else "${speed}X", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = if (speed == selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text(if (speed == selected) "●" else "○", color = if (speed == selected) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), fontSize = 27.sp)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SleepTimerDialog(onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Apagado automático", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(if (selected == 0) "Desactivado" else "$selected min", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
            Slider(
                value = selected.toFloat(),
                onValueChange = { selected = it.roundToInt().coerceIn(0, 90) },
                valueRange = 0f..90f,
                steps = 89,
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(0xFFFF6B6B), activeTrackColor = Color(0xFFFF6B6B))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 30, 60, 90).forEach { option -> Text(if (option == 0) "Desactivado" else "$option min", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFFFF6B6B)) }
                TextButton(onClick = { onSelect(selected) }) { Text("Finalizado", color = Color(0xFFFF6B6B)) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun shareFile(context: Context, file: MusicFile) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "audio/mpeg"
        putExtra(Intent.EXTRA_STREAM, file.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Compartir canción"))
}

private fun setAsRingtone(context: Context, file: MusicFile) {
    context.startActivity(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, file.title)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, file.uri)
    })
}

private fun openEqualizer(context: Context) {
    runCatching {
        context.startActivity(Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EqualizerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("vinylplayer_settings", Context.MODE_PRIVATE) }
    val bands = remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    fun saveValues(values: List<Float>) {
        bands.value = values
        preferences.edit().apply {
            values.forEachIndexed { index, value -> putInt("eq_band_$index", (value * 1000).toInt()) }
        }.apply()
    }
    LaunchedEffect(Unit) {
        bands.value = labels.indices.map { preferences.getInt("eq_band_$it", 0) / 1000f }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Ecualizador", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Ajusta el sonido de esta reproducción", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                labels.forEachIndexed { index, label ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(58.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                        Slider(
                            value = bands.value[index],
                            onValueChange = { value -> saveValues(bands.value.toMutableList().also { it[index] = value }) },
                            valueRange = -1f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Plano", "Bass", "Vocal").forEach { preset ->
                    TextButton(onClick = {
                        saveValues(when (preset) {
                            "Bass" -> listOf(0.75f, 0.45f, 0f, -0.2f, -0.2f)
                            "Vocal" -> listOf(-0.25f, 0f, 0.55f, 0.65f, 0.25f)
                            else -> listOf(0f, 0f, 0f, 0f, 0f)
                        })
                    }, modifier = Modifier.weight(1f)) { Text(preset) }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeleteSongDialog(file: MusicFile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar canción") },
        text = { Text("¿Quieres eliminar \"${file.title}\" del teléfono?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar", color = Color(0xFFFF6B6B)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QueueDialog(
    queue: List<QueueEntry>,
    currentEntryId: String?,
    onSelect: (QueueEntry) -> Unit,
    repeatMode: RepeatMode,
    onRepeat: () -> Unit,
    onMove: (String, String?, Boolean) -> Unit,
    onPlayNext: (QueueEntry) -> Unit,
    onAddToQueue: (QueueEntry) -> Unit,
    onRemove: (String) -> Unit,
    onClearPending: () -> Unit,
    onInfo: (QueueEntry) -> Unit,
    onBackToLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentIndex = queue.indexOfFirst { it.id == currentEntryId }
    val currentEntry = queue.getOrNull(currentIndex)
    val upcoming = queue.drop((currentIndex + 1).coerceIn(0, queue.size))
    val queueListState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    var dropAfterTarget by remember { mutableStateOf(false) }
    var draggedOffsetPx by remember { mutableStateOf(0f) }
    var showQueueOptions by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var queueMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(queueMessage) {
        if (queueMessage != null) {
            delay(1800)
            queueMessage = null
        }
    }
    LaunchedEffect(draggingId) {
        while (draggingId != null) {
            val visibleItems = queueListState.layoutInfo.visibleItemsInfo
            val draggedItem = visibleItems.firstOrNull { it.key == draggingId }
            if (draggedItem != null && visibleItems.isNotEmpty()) {
                val center = draggedItem.offset + draggedItem.size / 2 + draggedOffsetPx.toInt()
                val layout = queueListState.layoutInfo
                val firstIndex = visibleItems.first().index
                val lastIndex = visibleItems.last().index
                when {
                    center < layout.viewportStartOffset + 58 && firstIndex > 0 ->
                        queueListState.animateScrollToItem(firstIndex - 1)
                    center > layout.viewportEndOffset - 58 && lastIndex < upcoming.lastIndex ->
                        queueListState.animateScrollToItem(lastIndex + 1)
                }
            }
            delay(48)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Fila de reproducción", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (upcoming.size == 1) "1 canción pendiente" else "${upcoming.size} canciones pendientes",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontSize = 14.sp
                    )
                }
                Box {
                    IconButton(onClick = { showQueueOptions = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones de la fila", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(
                        expanded = showQueueOptions,
                        onDismissRequest = { showQueueOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Vaciar canciones pendientes") },
                            enabled = upcoming.isNotEmpty(),
                            onClick = {
                                showQueueOptions = false
                                if (upcoming.size > 5) confirmClear = true else onClearPending()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (currentEntry != null) {
                Text("Reproduciendo ahora", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                QueueRow(
                    entry = currentEntry,
                    isCurrent = true,
                    allowReorder = false,
                    isDragging = false,
                    isDropTarget = false,
                    onSelect = { onSelect(currentEntry) },
                    onDragStart = {},
                    onDragOffset = {},
                    onDragEnd = {},
                    onPlayNext = { onPlayNext(currentEntry); queueMessage = "Se reproducirá a continuación" },
                    onAddToQueue = { onAddToQueue(currentEntry); queueMessage = "Agregada al final de la fila" },
                    onRemove = { onRemove(currentEntry.id); queueMessage = "Quitada de la fila" },
                    onInfo = { onInfo(currentEntry) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("A continuación", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("${upcoming.size}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            when {
                upcoming.isEmpty() && currentEntry == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("La fila está vacía", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("Elige canciones de tu biblioteca para empezar.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                        TextButton(onClick = onBackToLibrary) { Text("Volver a la biblioteca", color = MaterialTheme.colorScheme.primary) }
                    }
                }
                upcoming.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No hay canciones pendientes", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
                    }
                }
                else -> {
                    LazyColumn(
                        state = queueListState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(upcoming, key = { it.id }) { entry ->
                            QueueRow(
                                entry = entry,
                                isCurrent = false,
                                allowReorder = true,
                                isDragging = entry.id == draggingId,
                                isDropTarget = entry.id == dropTargetId,
                                onSelect = { onSelect(entry) },
                                onDragStart = {
                                    draggingId = entry.id
                                    dropTargetId = entry.id
                                    dropAfterTarget = false
                                    draggedOffsetPx = 0f
                                },
                                onDragOffset = { offset ->
                                    draggedOffsetPx = offset
                                    val visible = queueListState.layoutInfo.visibleItemsInfo
                                    val dragged = visible.firstOrNull { it.key == entry.id }
                                    if (dragged == null) {
                                        dropTargetId = entry.id
                                        dropAfterTarget = false
                                    } else {
                                        val center = dragged.offset + dragged.size / 2 + offset.toInt()
                                        val target = visible
                                            .filter { it.key != entry.id }
                                            .minByOrNull { abs(it.offset + it.size / 2 - center) }
                                        val lastVisible = visible.lastOrNull()
                                        if (target != null && lastVisible?.index == upcoming.lastIndex && center > lastVisible.offset + lastVisible.size / 2) {
                                            dropTargetId = null
                                            dropAfterTarget = true
                                        } else {
                                            dropTargetId = target?.key as? String ?: entry.id
                                            dropAfterTarget = target != null && center > target.offset + target.size / 2
                                        }
                                    }
                                },
                                onDragEnd = { completed ->
                                    val movingId = draggingId
                                    if (completed && movingId != null && (dropTargetId != movingId || dropAfterTarget) &&
                                        (dropTargetId != null || dropAfterTarget)
                                    ) {
                                        onMove(movingId, dropTargetId, dropAfterTarget)
                                    }
                                    draggingId = null
                                    dropTargetId = null
                                    dropAfterTarget = false
                                    draggedOffsetPx = 0f
                                },
                                onPlayNext = { onPlayNext(entry); queueMessage = "Se reproducirá a continuación" },
                                onAddToQueue = { onAddToQueue(entry); queueMessage = "Agregada al final de la fila" },
                                onRemove = { onRemove(entry.id); queueMessage = "Quitada de la fila" },
                                onInfo = { onInfo(entry) }
                            )
                        }
                    }
                }
            }
            queueMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 5.dp)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onRepeat),
                    shape = RoundedCornerShape(14.dp),
                    color = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            null,
                            tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (repeatMode) {
                                RepeatMode.OFF -> "Repetir"
                                RepeatMode.ALL -> "Repetir lista"
                                RepeatMode.ONE -> "Repetir audio"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Vaciar canciones pendientes") },
            text = { Text("Se quitarán ${upcoming.size} canciones de la fila. La canción actual seguirá reproduciéndose.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearPending() }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    allowReorder: Boolean = true,
    onSelect: () -> Unit,
    isDragging: Boolean,
    isDropTarget: Boolean,
    onDragStart: () -> Unit,
    onDragOffset: (Float) -> Unit,
    onDragEnd: (Boolean) -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemove: () -> Unit,
    onInfo: () -> Unit
) {
    val file = entry.file
    var dragDistance by remember(entry.id) { mutableStateOf(0f) }
    var showActions by remember(entry.id) { mutableStateOf(false) }
    val rowScale by animateFloatAsState(if (isDragging) 1.035f else 1f, label = "queue-drag-scale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
            .shadow(if (isDragging) 12.dp else 0.dp, RoundedCornerShape(12.dp))
            .border(
                width = if (isDropTarget) 2.dp else 0.dp,
                color = if (isDropTarget) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isDragging) Color(0x665E4923) else if (isCurrent) Color(0x334CAF70) else Color.Transparent)
            .then(
                if (allowReorder) Modifier.pointerInput(entry.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragDistance = 0f; onDragStart() },
                        onDragCancel = { dragDistance = 0f; onDragEnd(false) },
                        onDragEnd = { dragDistance = 0f; onDragEnd(true) },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount.y
                            onDragOffset(dragDistance)
                        }
                    )
                } else Modifier
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.artwork != null) {
            Image(
                file.artwork.asImageBitmap(),
                null,
                Modifier.size(54.dp).clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(file.title, color = if (isCurrent) Color(0xFF41D476) else MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, maxLines = 1)
            Text(file.artist, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 13.sp, maxLines = 1)
        }
        if (isCurrent) {
            Icon(Icons.Default.Pause, "Reproduciendo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Box {
            IconButton(onClick = { showActions = true }) {
                Icon(Icons.Default.MoreVert, "Acciones de ${file.title}", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
            }
            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                DropdownMenuItem(text = { Text("Reproducir ahora") }, onClick = { showActions = false; onSelect() })
                if (!isCurrent) {
                    DropdownMenuItem(text = { Text("Reproducir a continuación") }, onClick = { showActions = false; onPlayNext() })
                }
                DropdownMenuItem(text = { Text("Agregar al final") }, onClick = { showActions = false; onAddToQueue() })
                DropdownMenuItem(text = { Text("Información") }, onClick = { showActions = false; onInfo() })
                DropdownMenuItem(text = { Text("Quitar de la fila") }, onClick = { showActions = false; onRemove() })
            }
        }
    }
}

private fun progress(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
