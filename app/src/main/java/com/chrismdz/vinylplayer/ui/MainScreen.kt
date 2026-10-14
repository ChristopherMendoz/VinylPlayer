package com.chrismdz.vinylplayer.ui

import android.content.Intent
import android.content.Context
import android.media.RingtoneManager
import android.app.Activity
import android.app.PendingIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
import com.chrismdz.vinylplayer.data.QueueEntry
import com.chrismdz.vinylplayer.data.parseSyncedLyrics
import com.chrismdz.vinylplayer.data.Playlist
import com.chrismdz.vinylplayer.data.MusicFile
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

private enum class MainView {
    LIBRARY,
    PLAYER,
    INFO
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
fun MainScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentView by remember { mutableStateOf(MainView.LIBRARY) }
    var showSettings by remember { mutableStateOf(false) }
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
    var playlistTarget by remember { mutableStateOf<MusicFile?>(null) }
    var infoTarget by remember { mutableStateOf<MusicFile?>(null) }
    var infoReturnView by remember { mutableStateOf(MainView.LIBRARY) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val libraryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val albumGridState = rememberLazyGridState()
    val artistGridState = rememberLazyGridState()
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val file = awaitingDeleteApproval
        if (result.resultCode == Activity.RESULT_OK && file != null) viewModel.removeDeletedSong(file)
        awaitingDeleteApproval = null
    }
    val playerTab = PlayerTab.valueOf(playerTabName)
    val libraryTab = LibraryTab.valueOf(libraryTabName)
    val infoDisplayFile = infoTarget?.let { target ->
        state.musicFile?.takeIf { it.uri == target.uri }
            ?: state.library.firstOrNull { it.uri == target.uri }
            ?: target
    }

    LaunchedEffect(Unit) { viewModel.initialize() }
    LaunchedEffect(infoTarget?.uri) {
        infoTarget?.let(viewModel::loadFullArtwork)
    }
    LaunchedEffect(searchQuery) {
        libraryListState.scrollToItem(0)
    }
    DisposableEffect(Unit) { onDispose { viewModel.release() } }
    BackHandler(enabled = currentView != MainView.LIBRARY) {
        currentView = if (currentView == MainView.INFO) infoReturnView else MainView.LIBRARY
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentView == MainView.PLAYER) state.musicFile?.artwork?.let { artwork ->
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
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    if (currentView == MainView.PLAYER && playerTab == PlayerTab.LYRICS) {
                        listOf(Color(0x66B91E3A), Color(0xAA260E18), Color(0xF00A090A))
                    } else {
                        listOf(Color(0x66101010), Color(0xA6000000), Color(0xF0000000))
                    }
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            TopBar(
                currentView = currentView,
                onBack = { currentView = if (currentView == MainView.INFO) infoReturnView else MainView.LIBRARY },
                onSettings = { showSettings = true },
                onMore = { showPlayerMenu = true }
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (currentView) {
                MainView.LIBRARY -> LibraryView(
                    state = state,
                    tab = libraryTab,
                    selectedPlaylistName = selectedPlaylistName,
                    selectedAlbumName = selectedAlbumName,
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
                    isFavorite = state.isFavorite
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
                    SongInfoView(file = file, onBack = { currentView = infoReturnView })
                }
            }
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD1F0000))
                    .padding(12.dp)
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            tonearmAnimationEnabled = state.tonearmAnimationEnabled,
            onTonearmAnimationChange = viewModel::setTonearmAnimationEnabled,
            onDismiss = { showSettings = false }
        )
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
            if (currentView != MainView.LIBRARY) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Atrás", tint = Color.White)
                }
            } else {
                Icon(Icons.Default.LibraryMusic, null, tint = Color(0xFFE4B45B))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when (currentView) {
                    MainView.LIBRARY -> "Biblioteca musical"
                    MainView.PLAYER -> "Reproduciendo"
                    MainView.INFO -> "Información de la canción"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentView != MainView.INFO) {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, "Ajustes", tint = Color.White)
                }
            }
            if (currentView == MainView.PLAYER) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, "Más opciones", tint = Color.White)
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
    onRefresh: () -> Unit,
    onRequestArtwork: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    albumGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    artistGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onOpenFile: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onOpenCurrent: () -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val filteredLibrary = remember(state.library, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            state.library
        } else {
            state.library.filter { file ->
                file.title.contains(query, ignoreCase = true) ||
                    file.artist.contains(query, ignoreCase = true) ||
                    file.album.contains(query, ignoreCase = true)
            }
        }
    }
    val albums = remember(filteredLibrary) { filteredLibrary.groupBy { it.album }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    val artists = remember(filteredLibrary) { filteredLibrary.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var playlistName by rememberSaveable { mutableStateOf("") }
    val playlistSongs = remember(state.library, state.favoriteUris, state.playlists, selectedPlaylistName) {
        when (selectedPlaylistName) {
            "Favoritos" -> state.library.filter { it.uri.toString() in state.favoriteUris }
            null -> emptyList()
            else -> state.playlists.firstOrNull { it.name == selectedPlaylistName }?.songUris
                ?.let { ids -> state.library.filter { it.uri.toString() in ids } }
                ?: emptyList()
        }
    }
    val alphabetItems = when {
        tab == LibraryTab.ALBUMS && selectedAlbumName == null -> albums.keys.toList()
        tab == LibraryTab.ARTISTS && selectedArtistName == null -> artists.keys.toList()
        tab == LibraryTab.PLAYLISTS && selectedPlaylistName == null -> listOf("Favoritos") + state.playlists.map { it.name }
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Buscar canción, artista o álbum") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Limpiar búsqueda")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE4B45B),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                focusedLeadingIconColor = Color(0xFFE4B45B),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.65f),
                cursorColor = Color(0xFFE4B45B)
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
                text = if (searchQuery.isBlank()) {
                    "${state.library.size} canciones MP3"
                } else {
                    "${filteredLibrary.size} de ${state.library.size} canciones"
                },
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
            TextButton(onClick = onRefresh) { Text("Actualizar") }
        }

        if (filteredLibrary.isEmpty() && !(tab == LibraryTab.PLAYLISTS && selectedPlaylistName != null)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LibraryMusic, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    if (searchQuery.isBlank()) "No hay MP3 disponibles" else "No se encontraron canciones",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (searchQuery.isBlank()) "Concede el permiso de audio y toca Actualizar."
                    else "Prueba con otro nombre, artista o álbum.",
                    color = Color.White.copy(alpha = 0.6f),
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
                            onRequestArtwork = onRequestArtwork,
                            onSelect = onAlbumSelected
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            TextButton(onClick = { onAlbumSelected(null) }) { Text("‹ Álbumes") }
                            GroupedSongList(
                                groups = albums.filterKeys { it == selectedAlbumName },
                                listState = listState,
                                state = state,
                                onRequestArtwork = onRequestArtwork,
                                onOpenFile = onOpenFile,
                                onMore = onMore,
                                onAddToPlaylist = onAddToPlaylist
                            )
                        }
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
                                item {
                                    PlaylistRow(
                                        name = "Favoritos",
                                        count = state.library.count { it.uri.toString() in state.favoriteUris },
                                        onClick = { onPlaylistSelected("Favoritos") }
                                    )
                                }
                                items(state.playlists, key = { it.name }) { playlist ->
                                    PlaylistRow(
                                        name = playlist.name,
                                        count = playlist.songUris.size,
                                        onClick = { onPlaylistSelected(playlist.name) },
                                        onDelete = { onDeletePlaylist(playlist.name) }
                                    )
                                }
                                item {
                                    TextButton(onClick = { showCreatePlaylist = true }) {
                                        Text("+ Nueva Playlist", color = Color(0xFFE4B45B))
                                    }
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onPlaylistSelected(null) }) { Text("‹ Listas") }
                                    Text(
                                        selectedPlaylistName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                SongList(
                                    files = playlistSongs,
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
                    color = if (item == tab) Color(0xFFE4B45B) else Color.White.copy(alpha = 0.62f),
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
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.34f))
            ) {
                ArtworkTile(cover)
                Column(Modifier.padding(10.dp)) {
                    Text(entry.key, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${entry.value.size} pistas", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp)
                }
            }
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
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.34f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    ArtworkTile(cover, Modifier.size(108.dp).clip(CircleShape))
                }
                Column(Modifier.padding(10.dp)) {
                    Text(entry.key, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("${entry.value.size} pistas", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
        Box(modifier.background(Color(0xFF292929)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = Color(0xFFE4B45B), modifier = Modifier.size(34.dp))
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
                Text(name, color = Color(0xFFE4B45B), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.QueueMusic, null, tint = Color(0xFFE4B45B), modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text("$count canciones", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp)
        }
        onDelete?.let { delete ->
            IconButton(onClick = delete) { Icon(Icons.Default.MoreVert, "Opciones", tint = Color.White.copy(alpha = 0.75f)) }
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
                color = Color.White.copy(alpha = 0.72f),
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
            .background(if (isCurrent) Color(0x55E4B45B) else Color.Black.copy(alpha = 0.28f))
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
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF292929)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, null, tint = Color(0xFFE4B45B))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(file.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${file.artist} · ${file.album}",
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (isFavorite) Icon(Icons.Default.Favorite, null, tint = Color(0xFFE4B45B), modifier = Modifier.size(18.dp))
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, "Opciones", tint = Color.White.copy(alpha = 0.72f))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xDD202020))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        file.artwork?.let {
            Image(it.asImageBitmap(), null, Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(file.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${file.artist} · ${file.album}", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorito",
                tint = if (isFavorite) Color(0xFFE4B45B) else Color.White.copy(alpha = 0.8f)
            )
        }
        IconButton(onClick = onTogglePlayback) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir", tint = Color(0xFFE4B45B))
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
            Text("Selecciona una canción desde la biblioteca.", color = Color.White.copy(alpha = 0.7f))
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTabSwitcher(tab, onTabChange)
        if (tab == PlayerTab.COVER) {
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
                    Text(file.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = onFavorite) {
                        Icon(if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (state.isFavorite) Color(0xFFE4B45B) else Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "${file.artist} · ${file.album}",
                        color = Color.White.copy(alpha = 0.65f),
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
                    Text(formatTime(state.positionMs), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    Text(formatTime(state.durationMs), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
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
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 32.dp)) {
            Text(
                file.title,
                color = Color.White,
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
                    tint = if (isFavorite) Color(0xFFE4B45B) else Color.White
                )
            }
        }
        if (coverSize < 84.dp) {
            Text(
                listOf(file.artist, file.album).filter { it.isNotBlank() }.joinToString(" · "),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                maxLines = 1
            )
        } else {
            Text(file.artist, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 1)
            if (file.album.isNotBlank()) {
                Text(file.album, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, maxLines = 1)
            }
        }
        Slider(
            value = progress(state.positionMs, state.durationMs),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth(),
            colors = playerSliderColors()
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
            Text(formatTime(state.durationMs), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LyricsOptionsRow(
    showTranslation: Boolean,
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
                TextButton(onClick = onToggleTranslation, enabled = !translationLoading, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                    Text(if (showTranslation) "Original" else "Traducir", fontSize = 12.sp, maxLines = 1)
                }
                if (compactControls) {
                    Spacer(Modifier.weight(1f))
                } else {
                    IconButton(onClick = { onOffsetChange(-stepMs) }, modifier = Modifier.size(42.dp)) { Text("−$stepLabel", color = Color.White, fontSize = 10.sp) }
                    Text(formatOffset(offsetMs), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                    IconButton(onClick = { onOffsetChange(stepMs) }, modifier = Modifier.size(42.dp)) { Text("+$stepLabel", color = Color.White, fontSize = 10.sp) }
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
                    Icon(Icons.Default.Refresh, "Restablecer sincronización", tint = Color.White.copy(alpha = 0.76f))
                }
            }
            if (compactControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { onOffsetChange(-stepMs) }, modifier = Modifier.size(42.dp)) { Text("−$stepLabel", color = Color.White, fontSize = 10.sp) }
                    Text(formatOffset(offsetMs), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                    IconButton(onClick = { onOffsetChange(stepMs) }, modifier = Modifier.size(42.dp)) { Text("+$stepLabel", color = Color.White, fontSize = 10.sp) }
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
    thumbColor = Color(0xFFE4B45B),
    activeTrackColor = Color(0xFFE4B45B),
    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
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
                Icon(Icons.Default.Shuffle, "Reproducción aleatoria", tint = if (state.shuffleEnabled) Color(0xFFE4B45B) else Color.White)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ArrowBack, "Anterior", tint = Color.White) }
            Surface(Modifier.size(if (compact) 52.dp else 70.dp), CircleShape, Color(0xFFE4B45B)) {
                IconButton(onClick = onTogglePlayback) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir", tint = Color(0xFF17120A), modifier = Modifier.size(36.dp))
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.ArrowBack, "Siguiente", tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = -1f }) }
            IconButton(onClick = onRepeat) {
                Icon(
                    if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Modo: ${state.repeatMode.name}",
                    tint = if (state.repeatMode == RepeatMode.OFF) Color.White.copy(alpha = 0.55f) else Color(0xFFE4B45B)
                )
            }
            if (compact) {
                IconButton(onClick = onQueue) {
                    Icon(Icons.Default.QueueMusic, "Fila de reproducción", tint = Color.White)
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

@Composable
private fun SongInfoView(file: MusicFile, onBack: () -> Unit) {
    val context = LocalContext.current
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
                    .background(Color(0xFF111111)),
                contentScale = ContentScale.Fit
            )
        }
        InfoEntry("Canción", file.title, true)
        InfoEntry("Artista", file.artist, true)
        InfoEntry("Álbum", file.album, true)
        InfoEntry("Duración", formatTime(file.durationMs))
        InfoEntry("Tamaño del archivo", formatFileSize(fileSize))
        InfoEntry("Portada de canción personalizada", "", true)
        InfoEntry("Ruta del archivo", file.uri.path ?: file.uri.toString())
        InfoEntry("Especificar manualmente el archivo de letra", "Solo admite archivos de letras en formatos lrc/alm3/qrc/txt", true)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) { Text("Volver") }
    }
}

@Composable
private fun InfoEntry(label: String, value: String, showArrow: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (value.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(value, color = Color.White.copy(alpha = 0.58f), fontSize = 15.sp, maxLines = 2)
            }
        }
        if (showArrow) Text("›", color = Color.White.copy(alpha = 0.55f), fontSize = 32.sp)
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Desconocido"
    val megabytes = bytes / 1_000_000.0
    return if (megabytes >= 1.0) String.format(java.util.Locale.US, "%.1f MB", megabytes)
    else "${bytes / 1_000} KB"
}

@Composable
private fun PlayerTabSwitcher(
    tab: PlayerTab,
    onTabChange: (PlayerTab) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        PlayerTabButton("PORTADA", tab == PlayerTab.COVER) { onTabChange(PlayerTab.COVER) }
        PlayerTabButton("LETRA", tab == PlayerTab.LYRICS) { onTabChange(PlayerTab.LYRICS) }
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
        color = Color.White.copy(alpha = if (selected) 0.95f else 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
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
    val listState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val verticalInset = (maxHeight / 2f - 34.dp).coerceAtLeast(24.dp)
        val centerLyrics = followLyrics && currentLine >= 2 && syncedLines.isNotEmpty()
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress to isAutoScrolling }.collect { (scrolling, autoScrolling) ->
                if (scrolling && !autoScrolling) onManualScroll()
            }
        }
        LaunchedEffect(currentLine, status, centerLyrics, followLyrics, verticalInset) {
            if (followLyrics && status == LyricsStatus.AVAILABLE && syncedLines.isNotEmpty()) {
                isAutoScrolling = true
                try {
                    // Start at the top until there is enough previous context to center the active line.
                    val targetLine = if (currentLine in 0..1) 0 else currentLine.coerceAtLeast(0)
                    listState.animateScrollToItem(targetLine)
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
                            top = if (centerLyrics) verticalInset else 0.dp,
                            bottom = if (centerLyrics) verticalInset else 0.dp
                        ),
                        verticalArrangement = if (syncedLines.isNotEmpty()) {
                            Arrangement.spacedBy(14.dp)
                        } else {
                            Arrangement.spacedBy(4.dp)
                        }
                    ) {
                        if (syncedLines.isNotEmpty()) {
                            itemsIndexed(syncedLines) { index, line ->
                                val distance = if (currentLine >= 0) abs(index - currentLine) else 1
                                Text(
                                    line.text,
                                    color = Color.White.copy(alpha = when (distance) {
                                        0 -> 1f
                                        1 -> 0.72f
                                        else -> 0.42f
                                    }),
                                    fontSize = when (distance) {
                                        0 -> 27.sp
                                        1 -> 22.sp
                                        else -> 19.sp
                                    },
                                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(Color.Black.copy(alpha = 0.82f), blurRadius = 8f)
                                    )
                                )
                            }
                        } else {
                            items(activePlainText?.lines().orEmpty()) { line ->
                                Text(
                                    line,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(Color.Black.copy(alpha = 0.82f), blurRadius = 8f)
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
                            Text("Seguir letra", color = Color.White, fontSize = 11.sp)
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
            color = Color.White.copy(alpha = 0.82f),
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(file.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
            Text("${file.artist} · ${file.album}", color = Color.White.copy(alpha = 0.58f), fontSize = 13.sp, maxLines = 1)
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Opciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
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
    color: Color = Color.White
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Selección múltiple", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            speeds.forEach { speed ->
                Row(
                    Modifier.fillMaxWidth().clickable { selected = speed; onSelect(speed) }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (speed == 1f) "Normal" else "${speed}X", color = Color.White, fontSize = 17.sp, fontWeight = if (speed == selected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text(if (speed == selected) "●" else "○", color = if (speed == selected) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.55f), fontSize = 27.sp)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.13f)))
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Apagado automático", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(if (selected == 0) "Desactivado" else "$selected min", color = Color.White.copy(alpha = 0.7f), fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
            Slider(
                value = selected.toFloat(),
                onValueChange = { selected = it.roundToInt().coerceIn(0, 90) },
                valueRange = 0f..90f,
                steps = 89,
                colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color(0xFFFF6B6B), activeTrackColor = Color(0xFFFF6B6B))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 30, 60, 90).forEach { option -> Text(if (option == 0) "Desactivado" else "$option min", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f)) }
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Ecualizador", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Ajusta el sonido de esta reproducción", color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                labels.forEachIndexed { index, label ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(58.dp), fontSize = 10.sp, color = Color.White.copy(alpha = 0.68f))
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
private fun SettingsDialog(
    tonearmAnimationEnabled: Boolean,
    onTonearmAnimationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Animar brazo fonocaptor", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (tonearmAnimationEnabled) "Se retira al pausar y baja al reproducir." else "Permanece sobre el vinilo al pausar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(tonearmAnimationEnabled, onTonearmAnimationChange)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
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
        containerColor = Color(0xFF202020),
        contentColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.4f))
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
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 14.sp
                    )
                }
                Box {
                    IconButton(onClick = { showQueueOptions = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones de la fila", tint = Color.White)
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
                Text("Reproduciendo ahora", color = Color(0xFFE4B45B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                Text("A continuación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("${upcoming.size}", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            when {
                upcoming.isEmpty() && currentEntry == null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.QueueMusic, null, tint = Color(0xFFE4B45B), modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("La fila está vacía", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Elige canciones de tu biblioteca para empezar.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        TextButton(onClick = onBackToLibrary) { Text("Volver a la biblioteca", color = Color(0xFFE4B45B)) }
                    }
                }
                upcoming.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No hay canciones pendientes", color = Color.White.copy(alpha = 0.58f))
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
            queueMessage?.let { Text(it, color = Color(0xFFE4B45B), fontSize = 13.sp, modifier = Modifier.padding(vertical = 5.dp)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onRepeat),
                    shape = RoundedCornerShape(14.dp),
                    color = if (repeatMode == RepeatMode.OFF) Color(0xFF353535) else Color(0xFF4A3B20)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            null,
                            tint = if (repeatMode == RepeatMode.OFF) Color.White else Color(0xFFE4B45B)
                        )
                        Text(
                            text = when (repeatMode) {
                                RepeatMode.OFF -> "Repetir"
                                RepeatMode.ALL -> "Repetir lista"
                                RepeatMode.ONE -> "Repetir audio"
                            },
                            fontSize = 12.sp,
                            color = Color.White
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
                    Text("Vaciar", color = Color(0xFFE4B45B))
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
                color = if (isDropTarget) Color(0xFFE4B45B) else Color.Transparent,
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
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF383838)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.6f))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(file.title, color = if (isCurrent) Color(0xFF41D476) else Color.White, fontSize = 17.sp, maxLines = 1)
            Text(file.artist, color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp, maxLines = 1)
        }
        if (isCurrent) {
            Icon(Icons.Default.Pause, "Reproduciendo", tint = Color(0xFFE4B45B), modifier = Modifier.size(22.dp))
        }
        Box {
            IconButton(onClick = { showActions = true }) {
                Icon(Icons.Default.MoreVert, "Acciones de ${file.title}", tint = Color.White.copy(alpha = 0.78f))
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
