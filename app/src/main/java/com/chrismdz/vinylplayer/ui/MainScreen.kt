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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.chrismdz.vinylplayer.settings.CollectionViewMode
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
import com.chrismdz.vinylplayer.data.AudioTagWriter
import com.chrismdz.vinylplayer.data.QueueEntry
import com.chrismdz.vinylplayer.data.parseSyncedLyrics
import com.chrismdz.vinylplayer.data.Playlist
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportManager
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportSource
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.ArtistCatalogItem
import com.chrismdz.vinylplayer.data.ArtistTrackLink
import com.chrismdz.vinylplayer.data.filterArtistCatalog
import com.chrismdz.vinylplayer.data.sortArtistCatalog
import com.chrismdz.vinylplayer.data.consolidateAlbumArtists
import com.chrismdz.vinylplayer.settings.RepeatMode
import com.chrismdz.vinylplayer.settings.PlayerBackground
import com.chrismdz.vinylplayer.settings.AccentColorValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator

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
    var selectedArtistId by rememberSaveable { mutableStateOf<String?>(null) }
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
    // Lives here (not in the playlist screen) so going to the player and back keeps the scroll position.
    val playlistDetailListState = rememberLazyListState()
    LaunchedEffect(selectedPlaylistName) { playlistDetailListState.scrollToItem(0) }
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
    // Storage Access Framework: the user grants read access to the one document picked; no storage permission.
    val playlistImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::startPlaylistImport)
    }
    val playlistImportState by viewModel.playlistImportState.collectAsStateWithLifecycle()
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
    val isPlaylistDetail = currentView == MainView.LIBRARY && libraryTab == LibraryTab.PLAYLISTS && selectedPlaylistName != null
    val isArtistDetail = currentView == MainView.LIBRARY && libraryTab == LibraryTab.ARTISTS && selectedArtistId != null
    val usesCoverPalette = currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.COVER_COLORS
    val activePaletteArtwork = if (usesCoverPalette) state.musicFile?.artwork else null
    // Album detail: per-album backdrop, analysed off the main thread, cached and cross-faded.
    val albumAmbience = rememberAnimatedCoverAmbience(if (isAlbumDetail) selectedAlbumCover?.artwork else null)
    val paletteColors = remember(activePaletteArtwork) {
        activePaletteArtwork?.let(CoverPalette::fromBitmap)?.let(::paletteBackgroundColors).orEmpty()
    }
    val infoDisplayFile = infoTarget?.let { target ->
        state.library.firstOrNull { it.uri == target.uri }
            ?: state.musicFile?.takeIf { it.uri == target.uri }
            ?: target
    }
    val artistTrackLinksFlow = remember(selectedArtistId) {
        selectedArtistId?.let(viewModel::observeArtistTrackLinks) ?: flowOf(emptyList<ArtistTrackLink>())
    }
    val artistTrackLinks by artistTrackLinksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

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
    BackHandler(enabled = currentView != MainView.LIBRARY || isAlbumDetail || isPlaylistDetail || isArtistDetail) {
        when (currentView) {
            MainView.INFO -> currentView = infoReturnView
            MainView.SETTINGS -> if (settingsCategory != null) settingsCategoryName = null else currentView = settingsReturnView
            MainView.LIBRARY -> when {
                isPlaylistDetail -> selectedPlaylistName = null
                isArtistDetail -> selectedArtistId = null
                else -> selectedAlbumName = null
            }
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
                    .then(if (playerTab == PlayerTab.LYRICS) Modifier.blur(14.dp) else Modifier)
                    .alpha(if (playerTab == PlayerTab.LYRICS) 1f else 0.72f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (currentView == MainView.SETTINGS ||
                    (currentView == MainView.PLAYER && state.appSettings.playerBackground == PlayerBackground.SOLID)
                ) Modifier.background(MaterialTheme.colorScheme.background)
                else if (isAlbumDetail) Modifier.coverAmbienceBackground(albumAmbience) {
                    // The glow recedes a little as the track list scrolls over it.
                    val scrolled = if (albumDetailListState.firstVisibleItemIndex > 0) 1f
                    else albumDetailListState.firstVisibleItemScrollOffset / 900f
                    1f - scrolled.coerceIn(0f, 1f) * 0.25f
                }
                else if (usesCoverPalette) Modifier.background(
                    Brush.verticalGradient(
                        if (paletteColors.isNotEmpty()) paletteColors
                        else listOf(Color(0xFF263A38), Color(0xFF111817), Color(0xFF090B0B))
                    )
                )
                else if (useDarkColors && currentView == MainView.PLAYER && playerTab == PlayerTab.LYRICS) Modifier.background(
                    // Neutral scrim (no tint): keeps the cover's own colors while the lyrics stay readable.
                    Brush.verticalGradient(listOf(Color(0x59000000), Color(0x8C000000), Color(0xD9000000)))
                )
                // The player keeps a translucent overlay so the cover drawn behind it stays visible.
                else if (currentView == MainView.PLAYER) Modifier.background(
                    Brush.verticalGradient(
                        if (useDarkColors) listOf(Color(0x66101010), Color(0xA6000000), Color(0xF0000000))
                        else listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                // Same tokens as the pinned bars, in dark and light themes.
                else Modifier.background(screenBackgroundBrush())
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = VinylSpacing.screenHorizontal, vertical = VinylSpacing.screenTop)
        ) {
            if (currentView != MainView.SETTINGS && !isPlaylistDetail && !isArtistDetail && !isAlbumDetail) {
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
                    selectedArtistId = selectedArtistId,
                    artistTrackLinks = artistTrackLinks,
                    onTabChange = {
                        libraryTabName = it.name
                        selectedPlaylistName = null
                        selectedAlbumName = null
                        selectedArtistId = null
                    },
                    onPlaylistSelected = {
                        searchQuery = ""
                        selectedPlaylistName = it
                    },
                    playlistDetailListState = playlistDetailListState,
                    onSavePlaylist = { original, name, uris ->
                        viewModel.updatePlaylist(original, name, uris).also { error ->
                            if (error == null) selectedPlaylistName = name.trim()
                        }
                    },
                    onAddSongsToPlaylist = viewModel::addSongsToPlaylist,
                    onSkipPrevious = { viewModel.skipPrevious(forceTrack = true) },
                    onSkipNext = viewModel::skipNext,
                    onOpenAlbum = { album ->
                        searchQuery = ""
                        selectedArtistId = null
                        libraryTabName = LibraryTab.ALBUMS.name
                        selectedAlbumName = album
                    },
                    onAlbumSelected = { selectedAlbumName = it },
                    onArtistSelected = { selectedArtistId = it },
                    onOpenArtist = { artistId ->
                        searchQuery = ""
                        selectedAlbumName = null
                        libraryTabName = LibraryTab.ARTISTS.name
                        selectedArtistId = artistId
                    },
                    onCreatePlaylist = viewModel::createPlaylist,
                    onRemoveFromPlaylistAt = viewModel::removeFromPlaylistAt,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onImportPlaylist = { playlistImportLauncher.launch(PlaylistImportManager.PICKER_MIME_TYPES) },
                    onPlayPlaylist = { files, index, shuffle ->
                        viewModel.playPlaylist(files, index, shuffle)
                        currentView = MainView.PLAYER
                    },
                    onQueuePlaylist = { files ->
                        viewModel.addPlaylistToQueue(files)
                        android.widget.Toast.makeText(context, "${files.size} canciones agregadas a la fila", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onMore = { menuFile = it },
                    onAddToPlaylist = { playlistTarget = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    listState = libraryListState,
                    albumDetailListState = albumDetailListState,
                    albumGridState = albumGridState,
                    artistGridState = artistGridState,
                    onRefresh = { viewModel.loadLibrary(force = true) },
                    onToggleViewMode = { tab ->
                        viewModel.updateAppSettings { settings ->
                            when (tab) {
                                LibraryTab.ALBUMS -> settings.copy(albumViewMode = settings.albumViewMode.toggled())
                                LibraryTab.ARTISTS -> settings.copy(artistViewMode = settings.artistViewMode.toggled())
                                else -> settings
                            }
                        }
                    },
                    onRequestArtwork = viewModel::loadArtwork,
                    onRequestArtistImage = viewModel::loadArtistImage,
                    onOpenFile = {
                        viewModel.playFromLibrary(it)
                        currentView = MainView.PLAYER
                    },
                onOpenCurrent = { currentView = MainView.PLAYER },
                    onTogglePlayback = viewModel::togglePlayback,
                    onFavorite = viewModel::toggleFavorite,
                    isFavorite = state.isFavorite,
                    onPlayAlbum = viewModel::playAlbum,
                    onPlayGroupedTrack = { files, track ->
                        viewModel.playGroupedTrack(files, track)
                        currentView = MainView.PLAYER
                    }
                )
                MainView.PLAYER -> PlayerView(
                    viewModel = viewModel,
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
    if (playlistImportState != PlaylistImportState.Idle) {
        PlaylistImportFlow(
            state = playlistImportState,
            library = state.library,
            onRename = viewModel::renameImportedPlaylist,
            onSelectPlaylist = viewModel::selectImportedPlaylist,
            onLink = viewModel::linkImportEntry,
            onExclude = viewModel::setImportEntryExcluded,
            onReset = viewModel::resetImportEntry,
            onExcludeMissing = viewModel::excludeMissingImportEntries,
            onConfirm = viewModel::confirmPlaylistImport,
            onCancel = viewModel::cancelPlaylistImport,
            onPickAnother = {
                viewModel.cancelPlaylistImport()
                playlistImportLauncher.launch(PlaylistImportManager.PICKER_MIME_TYPES)
            },
            onOpenPlaylist = { name ->
                viewModel.dismissPlaylistImportResult()
                searchQuery = ""
                libraryTabName = LibraryTab.PLAYLISTS.name
                selectedPlaylistName = name
                currentView = MainView.LIBRARY
            },
            onDismissResult = viewModel::dismissPlaylistImportResult
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentView != MainView.LIBRARY || isAlbumDetail) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = VinylSpacing.xxs)) {
                Icon(Icons.Default.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            text = when (currentView) {
                MainView.LIBRARY -> "Tu biblioteca"
                MainView.PLAYER -> "Reproduciendo"
                MainView.INFO -> "Información de la canción"
                MainView.SETTINGS -> "Configuración"
            },
            color = MaterialTheme.colorScheme.onSurface,
            style = if (currentView == MainView.LIBRARY) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (currentView != MainView.INFO) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Configuración", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (currentView == MainView.PLAYER) {
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, "Más opciones", tint = MaterialTheme.colorScheme.onSurface)
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
    selectedArtistId: String?,
    artistTrackLinks: List<ArtistTrackLink>,
    onTabChange: (LibraryTab) -> Unit,
    onPlaylistSelected: (String?) -> Unit,
    onAlbumSelected: (String?) -> Unit,
    onArtistSelected: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRemoveFromPlaylistAt: (String, Int) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onImportPlaylist: () -> Unit,
    onPlayPlaylist: (List<MusicFile>, Int, Boolean) -> Unit,
    onQueuePlaylist: (List<MusicFile>) -> Unit,
    playlistDetailListState: LazyListState,
    onSavePlaylist: (original: String, name: String, songUris: List<String>) -> String?,
    onAddSongsToPlaylist: (String, List<String>) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (artistId: String) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onMore: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onAddToPlaylist: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    listState: LazyListState,
    albumDetailListState: LazyListState,
    onRefresh: () -> Unit,
    onToggleViewMode: (LibraryTab) -> Unit,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onRequestArtistImage: (ArtistCatalogItem) -> Unit,
    albumGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    artistGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenFile: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onPlayAlbum: (List<com.chrismdz.vinylplayer.data.MusicFile>, Boolean) -> Unit,
    onPlayGroupedTrack: (List<com.chrismdz.vinylplayer.data.MusicFile>, com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
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
    val artists = remember(state.artistCatalog, searchQuery) {
        sortArtistCatalog(filterArtistCatalog(state.artistCatalog, searchQuery))
    }
    val selectedArtist = remember(state.artistCatalog, selectedArtistId) {
        state.artistCatalog.firstOrNull { it.artistId == selectedArtistId }
    }
    val artistSongIds = remember(artistTrackLinks) { artistTrackLinks.mapTo(hashSetOf()) { it.songId } }
    val selectedArtistFiles = remember(state.library, artistSongIds) {
        state.library.filter { it.uri.toString() in artistSongIds }
    }
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
    val visiblePlaylistEntries = remember(state.library, selectedPlaylist, searchQuery) {
        playlistSongEntries(
            library = state.library,
            playlist = selectedPlaylist,
            query = searchQuery,
            uri = { it.uri.toString() },
            title = MusicFile::title,
            artist = MusicFile::artist,
            album = MusicFile::album
        )
    }
    val visiblePlaylistSongs = remember(visiblePlaylistEntries) { visiblePlaylistEntries.map { it.value } }
    val fullPlaylistEntries = remember(state.library, selectedPlaylist) {
        playlistSongEntries(state.library, selectedPlaylist, "", { it.uri.toString() }, MusicFile::title, MusicFile::artist, MusicFile::album)
    }
    val totalAlbums = remember(state.library) { state.library.map { it.album }.distinct().size }
    val totalArtists = state.artistCatalog.size
    val totalPlaylistCount = state.playlists.size + 1
    val (resultCount, totalCount, countUnit) = when (tab) {
        LibraryTab.TRACKS -> Triple(filteredLibrary.size, state.library.size, "canciones")
        LibraryTab.ALBUMS -> if (selectedAlbumName != null) {
            val albumFiles = state.library.filter { it.album == selectedAlbumName }
            Triple(albumFiles.size, albumFiles.size, "canciones del álbum")
        } else Triple(albums.size, totalAlbums, "álbumes")
        LibraryTab.ARTISTS -> if (selectedArtistId != null) {
            Triple(selectedArtistFiles.size, selectedArtist?.songCount ?: selectedArtistFiles.size, "canciones del artista")
        } else Triple(artists.size, totalArtists, "artistas")
        LibraryTab.PLAYLISTS -> if (selectedPlaylistName != null) {
            Triple(visiblePlaylistSongs.size, fullPlaylistEntries.size, "canciones")
        } else Triple(visiblePlaylists.size + if (showFavorites) 1 else 0, totalPlaylistCount, "listas")
    }
    val hasVisibleContent = when (tab) {
        LibraryTab.TRACKS -> filteredLibrary.isNotEmpty()
        LibraryTab.ALBUMS -> if (selectedAlbumName == null) albums.isNotEmpty() else selectedAlbumFiles.isNotEmpty()
        // The artist screen keeps its own back button even while its songs are still loading.
        LibraryTab.ARTISTS -> selectedArtistId != null || artists.isNotEmpty()
        // The playlist screen handles its own empty and no-results states (and keeps its back button).
        LibraryTab.PLAYLISTS -> selectedPlaylistName != null || showFavorites || visiblePlaylists.isNotEmpty()
    }
    val alphabetItems = when {
        tab == LibraryTab.ALBUMS && selectedAlbumName == null -> albums.keys.toList()
        tab == LibraryTab.ARTISTS && selectedArtistId == null -> artists.map { it.name }
        tab == LibraryTab.PLAYLISTS && selectedPlaylistName == null -> (if (showFavorites) listOf("Favoritos") else emptyList()) + visiblePlaylists.map { it.name }
        // Tracks are indexed by the field the library is sorted by, so letters match the visible order.
        else -> when (state.appSettings.librarySortOrder) {
            com.chrismdz.vinylplayer.settings.LibrarySortOrder.TITLE -> filteredLibrary.map { it.title }
            com.chrismdz.vinylplayer.settings.LibrarySortOrder.ARTIST -> filteredLibrary.map { it.artist }
            com.chrismdz.vinylplayer.settings.LibrarySortOrder.ALBUM -> filteredLibrary.map { it.album }
        }
    }
    val alphabetLetters = remember(alphabetItems) { alphabetItems.mapTo(hashSetOf(), ::indexLetterOf) }
    val indexScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        val isPlaylistDetail = tab == LibraryTab.PLAYLISTS && selectedPlaylistName != null
        val isArtistDetail = tab == LibraryTab.ARTISTS && selectedArtistId != null
        if ((tab != LibraryTab.ALBUMS || selectedAlbumName == null) && !isPlaylistDetail && !isArtistDetail) {
        VinylSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = when (tab) {
                LibraryTab.TRACKS -> "Buscar canción, artista o álbum"
                LibraryTab.ALBUMS -> "Buscar álbum, artista o canción"
                LibraryTab.PLAYLISTS -> if (selectedPlaylistName == null) "Buscar playlist" else "Buscar canción en playlist"
                LibraryTab.ARTISTS -> "Buscar artista"
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(VinylSpacing.sm))
        LibraryTabs(tab, onTabChange)
        Spacer(modifier = Modifier.height(VinylSpacing.xxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (searchQuery.isBlank() || (tab == LibraryTab.ALBUMS && selectedAlbumName != null)) {
                    "$resultCount $countUnit"
                } else "$resultCount de $totalCount $countUnit",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
            val viewMode = when (tab) {
                LibraryTab.ALBUMS -> state.appSettings.albumViewMode
                LibraryTab.ARTISTS -> state.appSettings.artistViewMode
                else -> null
            }
            if (viewMode != null) {
                IconButton(onClick = { onToggleViewMode(tab) }) {
                    // Shows the layout you'll switch to.
                    if (viewMode == CollectionViewMode.GRID) {
                        Icon(Icons.Default.ViewList, "Ver como lista", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(Icons.Default.GridView, "Ver como cuadrícula", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onRefresh, enabled = !state.isLibraryLoading) {
                if (state.isLibraryLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Refresh, "Actualizar biblioteca", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            }
        }
        if (tab == LibraryTab.ARTISTS && state.artistCatalogErrorMessage != null) {
            Text(
                "No se pudo actualizar el catálogo de artistas. Toca Actualizar para reintentar.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
        }

        if (tab == LibraryTab.ARTISTS && state.artistCatalog.isEmpty() &&
            (state.artistCatalogLoading || state.isLibraryLoading)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tab == LibraryTab.ARTISTS && state.artistCatalog.isEmpty() &&
            state.artistCatalogErrorMessage != null
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No se pudo cargar el catálogo de artistas", color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onRefresh) { Text("Reintentar") }
            }
        } else if (!hasVisibleContent && !state.sessionRestored) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (!hasVisibleContent) {
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
                        LibraryTab.ARTISTS -> if (selectedArtistId == null) "No hay artistas disponibles" else "No hay canciones para este artista"
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
                        onAddToPlaylist = onAddToPlaylist,
                        endPadding = 14.dp
                    )
                    LibraryTab.ALBUMS -> if (selectedAlbumName == null) {
                        AlbumGrid(
                            groups = albums,
                            viewMode = state.appSettings.albumViewMode,
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
                            onOpenFile = { file -> onPlayGroupedTrack(selectedAlbumFiles, file) },
                            onPlay = { shuffle -> onPlayAlbum(selectedAlbumFiles, shuffle) },
                            onChooseAlbumCover = onChooseAlbumCover,
                            selectedAlbumArtworkOverrideUri = albumArtworkOverrideUri,
                            onMore = onMore,
                            onBack = { onAlbumSelected(null) },
                            isPlaying = state.isPlaying,
                            findArtistId = { name ->
                                val identity = com.chrismdz.vinylplayer.data.artistIdentity(name)
                                state.artistCatalog.firstOrNull {
                                    com.chrismdz.vinylplayer.data.artistIdentity(it.name) == identity
                                }?.artistId
                            },
                            onOpenArtist = onOpenArtist
                        )
                    }
                    LibraryTab.ARTISTS -> if (selectedArtistId == null) {
                        ArtistGrid(
                            artists = artists,
                            viewMode = state.appSettings.artistViewMode,
                            images = state.artistImages,
                            gridState = artistGridState,
                            onRequestArtistImage = onRequestArtistImage,
                            onSelect = { onArtistSelected(it.artistId) }
                        )
                    } else {
                        ArtistDetailContent(
                            artist = selectedArtist,
                            tracks = selectedArtistFiles,
                            links = artistTrackLinks,
                            image = selectedArtist?.let { state.artistImages[it.artistId] },
                            listState = listState,
                            state = state,
                            onBack = { onArtistSelected(null) },
                            onRequestImage = onRequestArtistImage,
                            onRequestArtwork = onRequestArtwork,
                            onOpenFile = { file -> onPlayGroupedTrack(selectedArtistFiles, file) },
                            onPlayAll = onPlayAlbum,
                            onOpenAlbum = onOpenAlbum,
                            onMore = onMore
                        )
                    }
                    LibraryTab.PLAYLISTS -> {
                        val libraryByUri = remember(state.library) { state.library.associateBy { it.uri.toString() } }
                        if (selectedPlaylistName == null) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                item {
                                    Row(
                                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LibraryPill("Nueva playlist", icon = Icons.Default.Add) { showCreatePlaylist = true }
                                        LibraryPill("Importar", icon = Icons.Default.FolderOpen, onClick = onImportPlaylist)
                                    }
                                }
                                if (showFavorites) item(key = "favoritos") {
                                    val favoriteCount = state.library.count { it.uri.toString() in state.favoriteUris }
                                    PlaylistLibraryRow(
                                        name = "Favoritos",
                                        subtitle = "Playlist · $favoriteCount canciones",
                                        coverFiles = emptyList(),
                                        favorite = true,
                                        onRequestArtwork = onRequestArtwork,
                                        onClick = { onPlaylistSelected("Favoritos") },
                                        onDelete = null
                                    )
                                }
                                items(visiblePlaylists, key = { it.name }) { playlist ->
                                    val coverFiles = remember(playlist.songUris, libraryByUri) {
                                        playlist.songUris.asSequence().mapNotNull(libraryByUri::get).distinctBy { it.album }.take(4).toList()
                                    }
                                    PlaylistLibraryRow(
                                        name = playlist.name,
                                        subtitle = listOfNotNull(
                                            "Playlist · ${playlist.songUris.size} canciones",
                                            playlist.origin?.let { PlaylistImportSource.fromName(it.source).displayName }
                                        ).joinToString(" · "),
                                        coverFiles = coverFiles,
                                        favorite = false,
                                        onRequestArtwork = onRequestArtwork,
                                        onClick = { onPlaylistSelected(playlist.name) },
                                        onDelete = { onDeletePlaylist(playlist.name) }
                                    )
                                }
                            }
                        } else {
                            val detailPlaylist = selectedPlaylist ?: Playlist(selectedPlaylistName, emptyList())
                            val isFavorites = selectedPlaylistName == "Favoritos" && state.playlists.none { it.name == "Favoritos" }
                            PlaylistDetailContent(
                                playlist = detailPlaylist,
                                editable = !isFavorites && selectedPlaylist != null,
                                favorite = isFavorites,
                                originLabel = detailPlaylist.origin?.let { origin ->
                                    "Importada de " + PlaylistImportSource.fromName(origin.source).displayName +
                                        (origin.fileName?.let { " · $it" } ?: "")
                                },
                                library = state.library,
                                currentUri = state.musicFile?.uri,
                                isPlaying = state.isPlaying,
                                favoriteUris = state.favoriteUris,
                                onBack = { onPlaylistSelected(null) },
                                onPlay = onPlayPlaylist,
                                onQueue = onQueuePlaylist,
                                onSave = { name, uris -> onSavePlaylist(selectedPlaylistName, name, uris) },
                                onAddSongs = { uris -> onAddSongsToPlaylist(selectedPlaylistName, uris) },
                                onMore = onMore,
                                onRequestArtwork = onRequestArtwork,
                                listState = playlistDetailListState
                            )
                        }
                    }
                }
                if (alphabetItems.isNotEmpty() && tab != LibraryTab.PLAYLISTS &&
                    !(tab == LibraryTab.ALBUMS && selectedAlbumName != null) &&
                    !(tab == LibraryTab.ARTISTS && selectedArtistId != null)) {
                    val scrollbarModifier = Modifier.align(Alignment.CenterEnd).offset(x = (-2).dp)
                    when (tab) {
                        LibraryTab.TRACKS -> FastScrollbar(listState, scrollbarModifier)
                        LibraryTab.ALBUMS -> FastScrollbar(albumGridState, scrollbarModifier, minItems = 16)
                        LibraryTab.ARTISTS -> FastScrollbar(artistGridState, scrollbarModifier, minItems = 16)
                        LibraryTab.PLAYLISTS -> Unit
                    }
                    // The index sits in the screen's right margin so rows keep almost the full width.
                    AlphabetIndex(alphabetLetters, Modifier.align(Alignment.CenterEnd).offset(x = 19.dp)) { letter ->
                        val index = alphabetItems.indexOfFirst { indexLetterOf(it) == letter }
                        if (index >= 0) indexScope.launch {
                            when (tab) {
                                LibraryTab.ALBUMS -> albumGridState.scrollToItem(index)
                                LibraryTab.ARTISTS -> artistGridState.scrollToItem(index)
                                else -> listState.scrollToItem(index)
                            }
                        }
                    }
                }
            }
        }

        state.musicFile?.let { current ->
            // The album's chosen cover (it can differ from the song's own embedded art).
            val currentAlbumCover = remember(state.library, current.album, albumArtworkOverrideUri) {
                val tracks = state.library.filter { it.album == current.album }
                if (tracks.isEmpty()) null else {
                    val override = albumArtworkPreferences.getString(albumArtworkPreferenceKey(current.album, tracks), null)
                    chooseAlbumCoverTrack(tracks, override) { it.uri.toString() }
                }
            }
            LaunchedEffect(currentAlbumCover?.uri, currentAlbumCover?.artwork) {
                if (currentAlbumCover != null && currentAlbumCover.artwork == null) onRequestArtwork(currentAlbumCover)
            }
            Spacer(modifier = Modifier.height(12.dp))
            MiniPlayer(
                albumArtwork = currentAlbumCover?.artwork ?: current.artwork,
                file = current,
                isPlaying = state.isPlaying,
                onClick = onOpenCurrent,
                onTogglePlayback = onTogglePlayback,
                onFavorite = onFavorite,
                isFavorite = isFavorite,
                onSwipeLeft = onSkipNext,
                onSwipeRight = onSkipPrevious
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            LibraryTab.TRACKS to "Pistas",
            LibraryTab.ALBUMS to "Álbumes",
            LibraryTab.PLAYLISTS to "Playlists",
            LibraryTab.ARTISTS to "Artistas"
        ).forEach { (item, label) ->
            LibraryPill(label, selected = item == tab) { onTabChange(item) }
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
    onRemove: ((com.chrismdz.vinylplayer.data.MusicFile) -> Unit)? = null,
    /** Playlists may repeat a song, so they pass one key per position instead of the URI. */
    itemKeys: List<String>? = null,
    onOpenIndex: ((Int) -> Unit)? = null,
    endPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = endPadding)
    ) {
        itemsIndexed(files, key = { index, file -> itemKeys?.getOrNull(index) ?: file.uri.toString() }) { index, file ->
            MusicRow(
                file = file,
                isCurrent = file.uri == state.musicFile?.uri,
                isPlaying = state.isPlaying,
                isFavorite = file.uri.toString() in state.favoriteUris,
                onRequestArtwork = onRequestArtwork,
                onClick = { onOpenIndex?.invoke(index) ?: onOpenFile(file) },
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
    viewMode: CollectionViewMode,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    artworkPreferences: android.content.SharedPreferences,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onSelect: (String) -> Unit
) {
    val asList = viewMode == CollectionViewMode.LIST
    // One column for the list layout: same grid state, so scroll position, A–Z index and fast
    // scrollbar keep working in both layouts.
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (asList) 1 else 2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, end = 14.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(if (asList) 2.dp else 22.dp)
    ) {
        gridItems(groups.entries.toList(), key = { it.key }) { entry ->
            val coverPreferenceKey = albumArtworkPreferenceKey(entry.key, entry.value)
            val overrideUri = artworkPreferences.getString(coverPreferenceKey, null)
            val cover = chooseAlbumCoverTrack(entry.value, overrideUri) { it.uri.toString() }
            val albumArtist = remember(entry.value) {
                consolidateAlbumArtists(entry.value.map(MusicFile::artist)).primaryArtist ?: "Varios artistas"
            }
            LaunchedEffect(cover?.uri, cover?.artwork) {
                if (cover != null && cover.artwork == null) onRequestArtwork(cover)
            }
            if (asList) {
                AlbumListRow(
                    title = entry.key,
                    artist = albumArtist,
                    trackCount = entry.value.size,
                    artwork = cover?.artwork,
                    onClick = { onSelect(entry.key) }
                )
            } else {
                AlbumCard(
                    title = entry.key,
                    artist = albumArtist,
                    trackCount = entry.value.size,
                    artwork = cover?.artwork,
                    onClick = { onSelect(entry.key) }
                )
            }
        }
    }
}

/** One album in the collection: framed cover that sinks slightly under the finger, then title and artist. */
@Composable
private fun AlbumCard(
    title: String,
    artist: String,
    trackCount: Int,
    artwork: android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "album-card-press"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(14.dp, AlbumCoverShape)
                .clip(AlbumCoverShape)
        ) {
            if (artwork != null) {
                Image(artwork.asImageBitmap(), "Portada de $title", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                AlbumCoverPlaceholder(title, Modifier.fillMaxSize(), iconSize = 44.dp)
            }
            // Hairline edge so dark covers don't melt into the black background.
            Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.07f), AlbumCoverShape))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            artist,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            if (trackCount == 1) "1 canción" else "$trackCount canciones",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

private val AlbumCoverShape = RoundedCornerShape(12.dp)

/** List layout of an album: small framed cover, title, artist and track count. */
@Composable
private fun AlbumListRow(
    title: String,
    artist: String,
    trackCount: Int,
    artwork: android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    CollectionListRow(
        title = title,
        subtitle = "$artist · " + if (trackCount == 1) "1 canción" else "$trackCount canciones",
        onClick = onClick
    ) {
        Box(Modifier.size(58.dp).shadow(6.dp, AlbumListCoverShape).clip(AlbumListCoverShape)) {
            if (artwork != null) {
                Image(artwork.asImageBitmap(), "Portada de $title", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                AlbumCoverPlaceholder(title, Modifier.fillMaxSize(), iconSize = 26.dp)
            }
            Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.07f), AlbumListCoverShape))
        }
    }
}

private val AlbumListCoverShape = RoundedCornerShape(8.dp)

/** Shared row for the album and artist list layouts: leading image, two lines of text, chevron. */
@Composable
private fun CollectionListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
    }
}

private fun CollectionViewMode.toggled(): CollectionViewMode =
    if (this == CollectionViewMode.GRID) CollectionViewMode.LIST else CollectionViewMode.GRID

/**
 * Stand-in sleeve for albums without artwork: a dark two-tone gradient whose hue is derived from
 * the album name (stable between launches), with a record icon, so the grid never shows gaps.
 */
@Composable
private fun AlbumCoverPlaceholder(name: String, modifier: Modifier = Modifier, iconSize: Dp = 48.dp) {
    val colors = remember(name) {
        val hue = ((name.hashCode() % 360) + 360) % 360f
        listOf(
            Color(com.chrismdz.vinylplayer.settings.AccentColorValue.fromHsl(hue, 0.32f, 0.26f)),
            Color(com.chrismdz.vinylplayer.settings.AccentColorValue.fromHsl(hue + 36f, 0.28f, 0.12f))
        )
    }
    Box(modifier.background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Album, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(iconSize))
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
    onMore: (MusicFile) -> Unit,
    onBack: () -> Unit,
    isPlaying: Boolean = false,
    /** Library artist id for a credited name, or null when that artist has no profile. */
    findArtistId: (String) -> String? = { null },
    onOpenArtist: (artistId: String) -> Unit = {}
) {
    var titleBottomPx by remember { mutableStateOf(Int.MAX_VALUE) }
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleBottomPx }
    }
    val totalMinutes = remember(files) { files.sumOf { it.durationMs.coerceAtLeast(0) } / 60_000 }
    val durationText = if (totalMinutes >= 60) "${totalMinutes / 60} h ${totalMinutes % 60} min" else "$totalMinutes min"
    val countText = if (files.size == 1) "1 canción" else "${files.size} canciones"
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
    // Same cached, animated colors as the screen backdrop (see AlbumAmbience.kt).
    val ambience = rememberAnimatedCoverAmbience(cover?.artwork)
    // Controls keep VinylPlayer's gold; the cover only sets the atmosphere around them.
    val accent = MaterialTheme.colorScheme.primary
    // Cover and title ease in when an album opens.
    val appear = remember(name) { Animatable(0f) }
    LaunchedEffect(name) { appear.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing)) }
    LaunchedEffect(cover?.uri, cover?.artwork) {
        if (cover != null && cover.artwork == null) onRequestArtwork(cover)
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "album-detail-$name") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, "Atrás", tint = Color.White)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    val coverSide = minOf(maxWidth * 0.78f, 340.dp)
                    Box(
                        modifier = Modifier
                            .size(coverSide)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                // Entrance, then a gentle parallax shrink as the tracks scroll over it.
                                val scrolled = if (listState.firstVisibleItemIndex > 0) 1f
                                else (listState.firstVisibleItemScrollOffset / coverSide.toPx()).coerceIn(0f, 1f)
                                val scale = (0.9f + 0.1f * appear.value) * (1f - 0.1f * scrolled)
                                scaleX = scale
                                scaleY = scale
                                alpha = appear.value * (1f - 0.4f * scrolled)
                                translationY = (1f - appear.value) * 28.dp.toPx() + listState.firstVisibleItemScrollOffset * 0.3f * (1f - scrolled)
                            }
                            // Colored shadow: the cover seems to glow with its own light.
                            .shadow(30.dp, AlbumDetailCoverShape, ambientColor = ambience.primary, spotColor = ambience.primary)
                            .clip(AlbumDetailCoverShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cover?.artwork != null) {
                            Image(cover.artwork.asImageBitmap(), "Portada del álbum $name", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            AlbumCoverPlaceholder(name, Modifier.fillMaxSize(), iconSize = 72.dp)
                        }
                        Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.08f), AlbumDetailCoverShape))
                    }
                }
                Spacer(Modifier.height(26.dp))
                Column(Modifier.fillMaxWidth().graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 16.dp.toPx()
                }) {
                    Text(
                        "ÁLBUM",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name,
                        modifier = Modifier.fillMaxWidth()
                            .onGloballyPositioned { titleBottomPx = (it.positionInParent().y + it.size.height).toInt() }
                            .clickable { albumTitleExpanded = !albumTitleExpanded },
                        color = Color.White,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (albumTitleExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    albumArtists.primaryArtist?.let { primaryArtist ->
                        val primaryArtistId = remember(primaryArtist, findArtistId) { findArtistId(primaryArtist) }
                        Row(
                            Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (primaryArtistId != null) Modifier.clickable(onClickLabel = "Ver perfil de $primaryArtist") {
                                        onOpenArtist(primaryArtistId)
                                    } else Modifier
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                primaryArtist,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Chevron hints that the name opens the artist's profile.
                            if (primaryArtistId != null) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (albumArtists.collaborators.isNotEmpty()) {
                        Text(
                            featuringText,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clickable { featuringExpanded = !featuringExpanded },
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            maxLines = if (featuringExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (otherLeadArtists.isNotEmpty()) {
                        Text(
                            "Otros artistas principales · ${otherLeadArtists.joinToString(", ")}",
                            color = Color.White.copy(alpha = 0.66f),
                            fontSize = 13.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(
                        Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AlbumMetaChip(Icons.Default.MusicNote, countText)
                        if (totalMinutes > 0) AlbumMetaChip(Icons.Default.Schedule, durationText)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f)) {
                            LibraryPill("Portada", icon = Icons.Default.LibraryMusic) { showAlbumArtworkPicker = true }
                        }
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                .clickable { onPlay(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shuffle, "Reproducir álbum aleatoriamente", tint = accent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        PlayButton(
                            onClick = { onPlay(false) },
                            size = 60.dp,
                            contentDescription = "Reproducir álbum"
                        )
                    }
                }
                // Separates the album's identity from its track list.
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Canciones",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                }
            }
        }
        itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
            AlbumTrackRow(
                index = index,
                file = file,
                isCurrent = file.uri == currentFile?.uri,
                isPlaying = isPlaying,
                accent = accent,
                onClick = { onOpenFile(file) },
                onMore = { onMore(file) }
            )
        }
        if (files.isNotEmpty()) item(key = "album-detail-footer-$name") {
            Text(
                "$countText · $durationText",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp)
            )
        }
    }
    FastScrollbar(listState, Modifier.align(Alignment.CenterEnd), minItems = 25)
    CollapsedDetailBar(
        visible = collapsed,
        title = name,
        onBack = onBack,
        modifier = Modifier.align(Alignment.TopCenter),
        // Darker take on the album color, so the pinned bar belongs to the same atmosphere.
        background = lerp(ambience.primary, VinylPalette.Background, 0.35f),
        contentColor = Color.White
    )
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

private val AlbumDetailCoverShape = RoundedCornerShape(14.dp)

/** Small translucent capsule for album facts (track count, duration). */
@Composable
private fun AlbumMetaChip(icon: ImageVector, text: String) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(32.dp)) {
            if (isCurrent) {
                NowPlayingBars(isPlaying, Modifier.size(16.dp), color = accent)
            } else {
                Text((index + 1).toString(), color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                file.title,
                color = if (isCurrent) accent else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.artist,
                color = if (isCurrent) accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (file.durationMs > 0) {
            Text(
                formatTime(file.durationMs),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, "Opciones de ${file.title}", tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ArtistGrid(
    artists: List<ArtistCatalogItem>,
    viewMode: CollectionViewMode,
    images: Map<String, android.graphics.Bitmap>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onRequestArtistImage: (ArtistCatalogItem) -> Unit,
    onSelect: (ArtistCatalogItem) -> Unit
) {
    val asList = viewMode == CollectionViewMode.LIST
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (asList) 1 else 2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 14.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (asList) 2.dp else 18.dp)
    ) {
        gridItems(artists, key = { it.artistId }) { artist ->
            LaunchedEffect(artist.artistId, artist.imageStatus) { onRequestArtistImage(artist) }
            if (asList) {
                CollectionListRow(
                    title = artist.name,
                    subtitle = "Artista · " + if (artist.songCount == 1) "1 canción" else "${artist.songCount} canciones",
                    onClick = { onSelect(artist) }
                ) {
                    ArtistImageTile(
                        images[artist.artistId],
                        Modifier.size(58.dp).shadow(6.dp, CircleShape).clip(CircleShape),
                        artist.name
                    )
                }
            } else Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(artist) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtistImageTile(
                    images[artist.artistId],
                    Modifier.padding(horizontal = 6.dp).fillMaxWidth().aspectRatio(1f).shadow(10.dp, CircleShape).clip(CircleShape),
                    artist.name
                )
                Text(
                    artist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    "Artista · " + if (artist.songCount == 1) "1 canción" else "${artist.songCount} canciones",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ArtistImageTile(image: android.graphics.Bitmap?, modifier: Modifier, artistName: String) {
    if (image != null) {
        Image(image.asImageBitmap(), "Imagen de $artistName", modifier, contentScale = ContentScale.Crop)
    } else {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
        }
    }
}

private enum class ArtistTrackFilter(val label: String) { ALL("Todas"), PRIMARY("Principal"), FEATURED("Colaboraciones") }

@Composable
private fun ArtistDetailContent(
    artist: ArtistCatalogItem?,
    tracks: List<MusicFile>,
    links: List<ArtistTrackLink>,
    image: android.graphics.Bitmap?,
    listState: LazyListState,
    state: PlayerUiState,
    onBack: () -> Unit,
    onRequestImage: (ArtistCatalogItem) -> Unit,
    onRequestArtwork: (MusicFile) -> Unit,
    onOpenFile: (MusicFile) -> Unit,
    onPlayAll: (List<MusicFile>, Boolean) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onMore: (MusicFile) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val linksBySongId = remember(links) { links.associateBy { it.songId } }
    var filterName by rememberSaveable(artist?.artistId) { mutableStateOf(ArtistTrackFilter.ALL.name) }
    val filter = ArtistTrackFilter.valueOf(filterName)
    val isPrimary: (MusicFile) -> Boolean = { linksBySongId[it.uri.toString()]?.role == "PRIMARY" }
    val primaryCount = remember(tracks, linksBySongId) { tracks.count(isPrimary) }
    val featuredCount = tracks.size - primaryCount
    val visibleTracks = remember(tracks, linksBySongId, filter) {
        when (filter) {
            ArtistTrackFilter.ALL -> tracks
            ArtistTrackFilter.PRIMARY -> tracks.filter(isPrimary)
            ArtistTrackFilter.FEATURED -> tracks.filterNot(isPrimary)
        }
    }
    val albums = remember(tracks) { tracks.groupBy { it.album }.entries.sortedByDescending { it.value.size } }
    LaunchedEffect(artist?.artistId, artist?.imageStatus) { artist?.let(onRequestImage) }
    var titleBottomPx by remember { mutableStateOf(Int.MAX_VALUE) }
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > titleBottomPx }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "artist-detail-header-${artist?.artistId}") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface) }
                    }
                    artist?.let { selected ->
                        Box(
                            Modifier.size(206.dp).shadow(20.dp, CircleShape).clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape)
                                .padding(5.dp)
                        ) {
                            ArtistImageTile(image, Modifier.fillMaxSize().clip(CircleShape), selected.name)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            selected.name,
                            modifier = Modifier.onGloballyPositioned { titleBottomPx = (it.positionInParent().y + it.size.height).toInt() },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOf(
                                if (selected.songCount == 1) "1 canción" else "${selected.songCount} canciones",
                                if (albums.size == 1) "1 álbum" else "${albums.size} álbumes"
                            ).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            fontSize = 14.sp
                        )
                        if (image != null) {
                            val imageCredit = buildList {
                                add(selected.imageSource ?: "Imagen guardada")
                                selected.imageAttribution
                                    ?.takeIf { it.isNotBlank() && it != selected.imageSource }
                                    ?.let { add(it) }
                                selected.imageLicense?.takeIf(String::isNotBlank)?.let { add(it) }
                            }.joinToString(" · ")
                            Text(
                                imageCredit,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable(enabled = selected.imagePageUrl != null) {
                                        selected.imagePageUrl?.let { pageUrl -> runCatching { uriHandler.openUri(pageUrl) } }
                                    }
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onPlayAll(visibleTracks, true) }, enabled = visibleTracks.isNotEmpty()) {
                            Icon(Icons.Default.Shuffle, "Aleatorio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        PlayButton(onClick = { onPlayAll(visibleTracks, false) }, enabled = visibleTracks.isNotEmpty())
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtistTrackFilter.entries.forEach { option ->
                            val count = when (option) {
                                ArtistTrackFilter.ALL -> tracks.size
                                ArtistTrackFilter.PRIMARY -> primaryCount
                                ArtistTrackFilter.FEATURED -> featuredCount
                            }
                            if (option == ArtistTrackFilter.ALL || count > 0) {
                                LibraryPill("${option.label} $count", selected = option == filter) { filterName = option.name }
                            }
                        }
                    }
                    if (albums.size > 1 || (albums.size == 1 && tracks.size > 1)) {
                        Text(
                            "Álbumes",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums, key = { it.key }) { (albumName, albumTracks) ->
                                val cover = albumTracks.firstOrNull { it.artwork != null } ?: albumTracks.first()
                                LaunchedEffect(cover.uri, cover.artwork) { if (cover.artwork == null) onRequestArtwork(cover) }
                                Column(Modifier.width(132.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpenAlbum(albumName) }) {
                                    Box(Modifier.size(132.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        cover.artwork?.let {
                                            Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } ?: Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
                                    }
                                    Text(albumName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                    Text(
                                        if (albumTracks.size == 1) "1 canción" else "${albumTracks.size} canciones",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Canciones",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp)
                    )
                }
            }
            items(visibleTracks, key = { it.uri.toString() }) { file ->
                TrackRow(
                    file = file,
                    isCurrent = file.uri == state.musicFile?.uri,
                    isPlaying = state.isPlaying,
                    isFavorite = file.uri.toString() in state.favoriteUris,
                    onRequestArtwork = onRequestArtwork,
                    onClick = { onOpenFile(file) },
                    onMore = { onMore(file) },
                    subtitle = (if (isPrimary(file)) "" else "Colaboración · ") + file.album
                )
            }
        }
        FastScrollbar(listState = listState, modifier = Modifier.align(Alignment.CenterEnd))
        CollapsedDetailBar(
            visible = collapsed,
            title = artist?.name.orEmpty(),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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

@Composable
private fun PlaylistRow(
    name: String,
    count: Int,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                listOfNotNull("$count canciones", subtitle).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                fontSize = 12.sp
            )
        }
        onDelete?.let { delete ->
            IconButton(onClick = delete) { Icon(Icons.Default.MoreVert, "Opciones", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)) }
        }
    }
}

@Composable
private fun MusicRow(
    file: com.chrismdz.vinylplayer.data.MusicFile,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    isFavorite: Boolean,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    TrackRow(
        file = file,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        onRequestArtwork = onRequestArtwork,
        onClick = onClick,
        onMore = onMore,
        subtitle = "${file.artist} · ${file.album}"
    )
}

/**
 * Mini player. A horizontal swipe on it changes song (left: previous, right: next); vertical drags are
 * left to the list behind, and taps still open the player or hit the buttons.
 */
@Composable
private fun MiniPlayer(
    file: com.chrismdz.vinylplayer.data.MusicFile,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    /** Cover of the album the song belongs to (the album's chosen cover), falling back to the song's own. */
    albumArtwork: android.graphics.Bitmap? = file.artwork
) {
    // Same cached analysis as the album screens, cross-faded when the song (or album) changes.
    val backgroundColor = rememberAnimatedMiniPlayerColor(albumArtwork ?: file.artwork)
    val foregroundColor = Color(
        AccentColorValue.highContrastForeground(backgroundColor.toArgb(), MaterialTheme.colorScheme.background.toArgb())
    )
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    var widthPx by remember { mutableStateOf(1) }
    val currentSwipeLeft by androidx.compose.runtime.rememberUpdatedState(onSwipeLeft)
    val currentSwipeRight by androidx.compose.runtime.rememberUpdatedState(onSwipeRight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .graphicsLayer {
                translationX = offsetX.value
                alpha = 1f - (abs(offsetX.value) / widthPx).coerceIn(0f, 1f) * 0.7f
            }
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val threshold = widthPx * 0.22f
                            val offset = offsetX.value
                            if (abs(offset) < threshold) {
                                offsetX.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.75f))
                                return@launch
                            }
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val direction = if (offset < 0) -1f else 1f
                            offsetX.animateTo(direction * widthPx, androidx.compose.animation.core.tween(120))
                            if (direction < 0) currentSwipeLeft() else currentSwipeRight()
                            // The new song slides in from the opposite side.
                            offsetX.snapTo(-direction * widthPx * 0.4f)
                            offsetX.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 500f))
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } }
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                }
            }
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
    viewModel: PlayerViewModel,
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
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
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
            CoverPlayerContent(
                file = file,
                state = state,
                positionMs = playbackPositionMs,
                onSeek = onSeek,
                onFavorite = onFavorite,
                onTogglePlayback = onTogglePlayback,
                onPrevious = onPrevious,
                onNext = onNext,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
                onQueue = onQueue
            )
        } else {
            var followLyrics by remember(file.uri) { mutableStateOf(true) }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LyricsCoverHeader(file = file, isFavorite = state.isFavorite, onFavorite = onFavorite)
                // The lyrics get all the free height, right under the header.
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    key(file.uri) {
                        LyricsOverlay(
                            lyrics = state.lyrics,
                            status = state.lyricsStatus,
                            positionMs = playbackPositionMs,
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
                Slider(
                    value = progress(playbackPositionMs, state.durationMs),
                    onValueChange = {
                        onSeek(it)
                        followLyrics = true
                    },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = playerSliderColors()
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
                    Text(formatTime(playbackPositionMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
                    Text(formatTime(state.durationMs), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
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

/**
 * "Portada" tab: the vinyl takes all the free height (no scrolling), then title/artist with favorite,
 * a slim progress bar with times, the transport row and the queue shortcut, evenly spaced to the bottom.
 */
@Composable
private fun CoverPlayerContent(
    file: MusicFile,
    state: PlayerUiState,
    positionMs: Long,
    onSeek: (Float) -> Unit,
    onFavorite: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = VinylSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight)
            VinylView(
                artwork = file.artwork,
                isPlaying = state.isPlaying,
                tonearmAnimationEnabled = state.tonearmAnimationEnabled,
                modifier = Modifier.size(side)
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    file.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    "${file.artist} · ${file.album}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp).basicMarquee()
                )
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (state.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(VinylSpacing.sm))
        Slider(
            value = progress(positionMs, state.durationMs),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(28.dp),
            colors = playerSliderColors()
        )
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(state.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(VinylSpacing.sm))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Shuffle,
                    "Reproducción aleatoria",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, "Anterior", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
            }
            Box(
                Modifier.size(76.dp).shadow(10.dp, CircleShape).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary).clickable(onClick = onTogglePlayback),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, "Siguiente", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repetición: ${state.repeatMode.title}",
                    tint = if (state.repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(VinylSpacing.sm))
        LibraryPill("Fila de reproducción", icon = Icons.Default.QueueMusic, onClick = onQueue)
        Spacer(Modifier.height(VinylSpacing.xs))
    }
}

/** Lyrics tab header: centered cover, then title (favorite at the end) and artist · album. */
@Composable
private fun LyricsCoverHeader(file: MusicFile, isFavorite: Boolean, onFavorite: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Smaller cover on short screens so the lyrics keep most of the height.
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        val coverSize = when {
            screenHeight < 640.dp -> 72.dp
            screenHeight < 760.dp -> 92.dp
            else -> 112.dp
        }
        Column(Modifier.fillMaxWidth().padding(top = VinylSpacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(coverSize).shadow(14.dp, VinylShapes.artwork).clip(VinylShapes.artwork)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (file.artwork != null) {
                    Image(file.artwork.asImageBitmap(), "Portada de ${file.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(coverSize * 0.4f))
                }
            }
            Spacer(Modifier.height(VinylSpacing.xs))
            Box(Modifier.fillMaxWidth()) {
                Text(
                    file.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp).basicMarquee()
                )
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                listOf(file.artist, file.album).filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = VinylSpacing.md).basicMarquee()
            )
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
                        Text(if (showTranslation) "Quitar traducción" else "Traducir", fontSize = 12.sp, maxLines = 1)
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
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Anterior", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp)) }
            Surface(Modifier.size(if (compact) 52.dp else 70.dp), CircleShape, MaterialTheme.colorScheme.primary) {
                IconButton(onClick = onTogglePlayback) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Siguiente", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp)) }
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
    // The original lyrics always drive the layout and timing; a translation is shown small under each
    // line (Spotify style). Translated synced lyrics keep the original timestamps, so lines pair up.
    val activeSyncedText = lyrics?.syncedText ?: if (showTranslation) lyrics?.translatedSyncedText else null
    val activePlainText = lyrics?.text ?: if (showTranslation) lyrics?.translatedText else null
    val syncedLines = remember(activeSyncedText) {
        activeSyncedText?.let(::parseSyncedLyrics).orEmpty()
    }
    val syncedTranslations: List<String?> = remember(syncedLines, lyrics?.translatedSyncedText, showTranslation) {
        if (!showTranslation || lyrics?.syncedText == null) return@remember emptyList()
        val translated = lyrics.translatedSyncedText?.let(::parseSyncedLyrics).orEmpty()
        if (translated.size == syncedLines.size) translated.map { it.text }
        else {
            val byTime = translated.groupBy { it.startMs }
            syncedLines.map { line -> byTime[line.startMs]?.firstOrNull()?.text }
        }
    }
    val plainLines = remember(activePlainText) { activePlainText?.lines().orEmpty() }
    val plainTranslations: List<String?> = remember(plainLines, lyrics?.translatedText, showTranslation) {
        if (!showTranslation || lyrics?.text == null) return@remember emptyList()
        val translated = lyrics.translatedText?.lines().orEmpty()
        if (translated.size == plainLines.size) translated else emptyList()
    }
    val playbackTime = positionMs + offsetMs
    val currentLine = remember(syncedLines, playbackTime) {
        var low = 0
        var high = syncedLines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (syncedLines[middle].startMs <= playbackTime) low = middle + 1 else high = middle
        }
        low - 1
    }
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
        // Lines are already windowed (previous … next), so the block starts at the top instead of
        // being pushed to the middle by half a screen of padding.
        val topInset = 8.dp
        val bottomInset = 24.dp
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress to isAutoScrolling }.collect { (scrolling, autoScrolling) ->
                if (scrolling && !autoScrolling) onManualScroll()
            }
        }
        LaunchedEffect(currentLine, status, firstVisibleLine, followLyrics) {
            if (followLyrics && status == LyricsStatus.AVAILABLE && syncedLines.isNotEmpty()) {
                isAutoScrolling = true
                try {
                    listState.animateScrollToItem(0)
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
                            top = topInset,
                            bottom = bottomInset
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
                                val lineAlpha = when (distance) {
                                    0 -> 1f
                                    1 -> 0.72f
                                    else -> 0.42f
                                }
                                val lineSize = when (distance) {
                                    0 -> fontSizeSp + 5
                                    1 -> fontSizeSp
                                    else -> (fontSizeSp - 3).coerceAtLeast(13)
                                }
                                LyricLineWithTranslation(
                                    text = line.text,
                                    translation = syncedTranslations.getOrNull(absoluteIndex),
                                    alpha = lineAlpha,
                                    fontSizeSp = lineSize,
                                    fontWeight = if (!boldText) FontWeight.Normal else if (distance == 0) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        } else {
                            itemsIndexed(plainLines) { index, line ->
                                Box(Modifier.padding(vertical = 5.dp)) {
                                    LyricLineWithTranslation(
                                        text = line,
                                        translation = plainTranslations.getOrNull(index),
                                        alpha = 0.88f,
                                        fontSizeSp = fontSizeSp,
                                        fontWeight = if (boldText) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
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

/** A lyric line with its translation in a smaller, lighter line underneath (only when it differs). */
@Composable
private fun LyricLineWithTranslation(
    text: String,
    translation: String?,
    alpha: Float,
    fontSizeSp: Int,
    fontWeight: FontWeight
) {
    val shadow = androidx.compose.ui.text.TextStyle(
        shadow = Shadow(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), blurRadius = 8f)
    )
    Column(Modifier.fillMaxWidth()) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            style = shadow
        )
        translation?.takeIf { it.isNotBlank() && !it.trim().equals(text.trim(), ignoreCase = true) }?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.8f),
                fontSize = (fontSizeSp * 0.62f).coerceAtLeast(12f).sp,
                fontWeight = FontWeight.Normal,
                lineHeight = (fontSizeSp * 0.62f * 1.3f).coerceAtLeast(16f).sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                style = shadow
            )
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
    OptionsSheet(onDismiss = onDismiss, file = file) {
        MenuAction("Reproducir a continuación", onPlayNext, icon = Icons.Default.PlaylistPlay)
        MenuAction("Agregar a la fila", onAddToQueue, icon = Icons.Default.QueueMusic)
        MenuAction("Agregar a playlist", onAddToPlaylist, icon = Icons.Default.PlaylistAdd)
        SheetDivider()
        MenuAction("Información de la canción", onInfo, icon = Icons.Default.Info)
        MenuAction("Compartir archivo", onShare, icon = Icons.Default.Share)
        MenuAction("Establecer como tono de llamada", onRingtone, icon = Icons.Default.RingVolume)
        SheetDivider()
        MenuAction("Eliminar canción", onDelete, VinylPalette.Danger, icon = Icons.Default.Delete)
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
    OptionsSheet(onDismiss = onDismiss, file = file) {
        SheetSectionLabel("Reproducción")
        MenuAction("Apagado automático", onSleepTimer, icon = Icons.Default.Timer)
        MenuAction("Velocidad", onSpeed, icon = Icons.Default.Speed)
        MenuAction("Ecualizador", onEqualizer, icon = Icons.Default.GraphicEq)
        if (file != null) {
            SheetDivider()
            SheetSectionLabel("Canción")
            MenuAction("Agregar a playlist", onAddToPlaylist, icon = Icons.Default.PlaylistAdd)
            MenuAction("Información de la canción", onInfo, icon = Icons.Default.Info)
            MenuAction("Compartir archivo", onShare, icon = Icons.Default.Share)
            MenuAction("Establecer como tono de llamada", onRingtone, icon = Icons.Default.RingVolume)
        }
    }
}

/** Bottom sheet shared by the song and player menus: song header (artwork, title, artist) + actions. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OptionsSheet(
    onDismiss: () -> Unit,
    file: MusicFile?,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = VinylSpacing.md).padding(bottom = VinylSpacing.lg)
        ) {
            if (file != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.xs, vertical = VinylSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(file, 56.dp)
                    Column(Modifier.weight(1f).padding(start = VinylSpacing.sm)) {
                        Text(file.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${file.artist} · ${file.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SheetDivider()
            }
            content()
        }
    }
}

@Composable
private fun SheetDivider() {
    androidx.compose.material3.HorizontalDivider(
        Modifier.padding(vertical = VinylSpacing.xs),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = VinylSpacing.xs, top = VinylSpacing.xs, bottom = VinylSpacing.xxs)
    )
}

@Composable
private fun MenuAction(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(VinylShapes.control).clickable(onClick = onClick)
            .padding(horizontal = VinylSpacing.xs, vertical = VinylSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, null, tint = if (color == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(VinylSpacing.md))
        }
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
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
    // Local copy reordered live while dragging; the player only receives the final move on drop.
    val displayedUpcoming = remember { upcoming.toMutableStateList() }
    val queueReorder = rememberReorderableListState(
        listState = queueListState,
        onMove = { from, to ->
            if (from in displayedUpcoming.indices && to in displayedUpcoming.indices) {
                displayedUpcoming.add(to, displayedUpcoming.removeAt(from))
            }
        },
        indexOf = { key -> displayedUpcoming.indexOfFirst { it.id == key } },
        onDragStopped = { key, completed ->
            val id = key as? String
            val newIndex = displayedUpcoming.indexOfFirst { it.id == id }
            if (completed && id != null && newIndex >= 0 && newIndex != upcoming.indexOfFirst { it.id == id }) {
                // Expressed as "insert before the entry that now follows it" (or at the end), which is
                // what moveInQueue understands; the current song and its position are untouched.
                val next = displayedUpcoming.getOrNull(newIndex + 1)
                onMove(id, next?.id, next == null)
            }
        }
    )
    // Follow the real queue, except mid-drag: a refresh (artwork, song change) must not yank the row.
    LaunchedEffect(upcoming, queueReorder.draggingKey) {
        if (queueReorder.draggingKey == null && displayedUpcoming.toList() != upcoming) {
            displayedUpcoming.clear()
            displayedUpcoming.addAll(upcoming)
        }
    }
    var showQueueOptions by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var queueMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(queueMessage) {
        if (queueMessage != null) {
            delay(1800)
            queueMessage = null
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
                    onSelect = { onSelect(currentEntry) },
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
                        modifier = Modifier.fillMaxWidth().weight(1f).reorderable(queueReorder),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(displayedUpcoming, key = { it.id }) { entry ->
                            QueueRow(
                                entry = entry,
                                isCurrent = false,
                                modifier = reorderableItem(queueReorder, entry.id),
                                isDragging = entry.id == queueReorder.draggingKey,
                                onSelect = { onSelect(entry) },
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
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemove: () -> Unit,
    onInfo: () -> Unit
) {
    val file = entry.file
    var showActions by remember(entry.id) { mutableStateOf(false) }
    // Long-press dragging, lift and scale come from the queue list (see ReorderableList.kt).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDragging) Color(0x665E4923) else if (isCurrent) Color(0x334CAF70) else Color.Transparent)
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
