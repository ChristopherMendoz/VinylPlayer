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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.data.LyricsStatus
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
                modifier = Modifier.fillMaxSize().alpha(0.72f),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x66101010), Color(0xA6000000), Color(0xF0000000))
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
            currentUri = state.musicFile?.uri,
            onSelect = {
                viewModel.playFromQueue(it)
                currentView = MainView.PLAYER
                showQueue = false
            },
            repeatMode = state.repeatMode,
            onRepeat = viewModel::cycleRepeatMode,
            onMove = viewModel::moveInQueue,
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTabSwitcher(tab, onTabChange)
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
            if (tab == PlayerTab.LYRICS) {
                LyricsOverlay(
                    lyrics = state.lyrics,
                    status = state.lyricsStatus,
                    positionMs = state.positionMs,
                    offsetMs = state.lyricsOffsetMs,
                    onOffsetChange = onLyricsOffsetChange,
                    showTranslation = state.showTranslatedLyrics,
                    translationLoading = state.translationLoading,
                    translationError = state.translationError,
                    onToggleTranslation = onToggleLyricsTranslation
                )
            }
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
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color(0xFFE4B45B),
                activeTrackColor = Color(0xFFE4B45B),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Text(formatTime(state.durationMs), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onShuffle) {
                Icon(Icons.Default.Shuffle, "Reproducción aleatoria", tint = if (state.shuffleEnabled) Color(0xFFE4B45B) else Color.White)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ArrowBack, "Anterior", tint = Color.White) }
            Surface(Modifier.size(70.dp), CircleShape, Color(0xFFE4B45B)) {
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
        }
        TextButton(onClick = onQueue) {
            Icon(Icons.Default.QueueMusic, null)
            Spacer(Modifier.width(6.dp))
            Text("Fila de reproducción")
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
    onOffsetChange: (Long) -> Unit,
    showTranslation: Boolean,
    translationLoading: Boolean,
    translationError: String?,
    onToggleTranslation: () -> Unit
) {
    val activeSyncedText = if (showTranslation) lyrics?.translatedSyncedText else lyrics?.syncedText
    val activePlainText = if (showTranslation) lyrics?.translatedText else lyrics?.text
    val syncedLines = remember(activeSyncedText) {
        activeSyncedText?.let(::parseSyncedLyrics).orEmpty()
    }
    val currentLine = syncedLines.indexOfLast { it.startMs <= positionMs + offsetMs }
    val listState = rememberLazyListState()
    LaunchedEffect(currentLine, status) {
        if (status == LyricsStatus.AVAILABLE && currentLine >= 0) {
            listState.animateScrollToItem(currentLine)
        }
    }
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        when (status) {
            LyricsStatus.AVAILABLE -> {
                if (syncedLines.isNotEmpty()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = onToggleTranslation, enabled = !translationLoading) {
                                Text(if (showTranslation) "Ver original" else "Traducir al español")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { onOffsetChange(-500L) }) { Text("−0,5 s") }
                            Text("Ajuste ${formatOffset(offsetMs)}", color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp)
                            TextButton(onClick = { onOffsetChange(500L) }) { Text("+0,5 s") }
                        }
                        if (translationLoading) {
                            Text(
                                "Traduciendo letra...",
                                color = Color(0xFFE4B45B),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        translationError?.let { error ->
                            Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(syncedLines) { index, line ->
                                Text(
                                    line.text,
                                    color = if (index == currentLine) Color(0xFFE4B45B) else Color.White.copy(alpha = 0.82f),
                                    fontSize = if (index == currentLine) 22.sp else 19.sp,
                                    fontWeight = if (index == currentLine) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(Color.Black.copy(alpha = 0.85f), blurRadius = 8f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = onToggleTranslation, enabled = !translationLoading) {
                                Text(if (showTranslation) "Ver original" else "Traducir al español")
                            }
                        }
                        translationError?.let { error ->
                            Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 28.dp)
                        ) {
                            items(activePlainText?.lines().orEmpty()) { line ->
                                Text(
                                    line,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 19.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(Color.Black.copy(alpha = 0.85f), blurRadius = 8f)
                                    )
                                )
                            }
                        }
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
    queue: List<com.chrismdz.vinylplayer.data.MusicFile>,
    currentUri: android.net.Uri?,
    onSelect: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    repeatMode: RepeatMode,
    onRepeat: () -> Unit,
    onMove: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderedQueue = remember(queue, currentUri) {
        val currentIndex = queue.indexOfFirst { it.uri == currentUri }
        if (currentIndex > 0) queue.drop(currentIndex) + queue.take(currentIndex) else queue
    }
    val queueListState = rememberLazyListState()
    var draggingUri by remember { mutableStateOf<String?>(null) }
    var dropTargetUri by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = 20.dp)
        ) {
            Text("Fila de reproducción", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                text = queue.firstOrNull { it.uri == currentUri }?.let { "Reproduciendo ${it.title}" }
                    ?: "Lista actual",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(14.dp))
            if (orderedQueue.isEmpty()) {
                Text("La fila está vacía.", color = Color.White.copy(alpha = 0.7f))
            } else {
                LazyColumn(state = queueListState, modifier = Modifier.heightIn(max = 430.dp)) {
                    items(orderedQueue, key = { it.uri.toString() }) { file ->
                        QueueRow(
                            file = file,
                            isCurrent = file.uri == currentUri,
                            orderedQueue = orderedQueue,
                            onSelect = onSelect,
                            isDragging = file.uri.toString() == draggingUri,
                            isDropTarget = file.uri.toString() == dropTargetUri,
                            onDragStart = {
                                draggingUri = file.uri.toString()
                                dropTargetUri = null
                            },
                            onDragTarget = { target -> dropTargetUri = target.uri.toString() },
                            onDragEnd = {
                                draggingUri = null
                                dropTargetUri = null
                            },
                            onMove = { item, steps ->
                                val from = orderedQueue.indexOfFirst { it.uri == item.uri }
                                val target = (from + steps).coerceIn(0, orderedQueue.lastIndex)
                                if (from >= 0 && target != from) onMove(item.uri.toString(), orderedQueue[target].uri.toString())
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
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
}

@Composable
private fun QueueRow(
    file: com.chrismdz.vinylplayer.data.MusicFile,
    isCurrent: Boolean,
    orderedQueue: List<com.chrismdz.vinylplayer.data.MusicFile>,
    onSelect: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    isDragging: Boolean,
    isDropTarget: Boolean,
    onDragStart: () -> Unit,
    onDragTarget: (com.chrismdz.vinylplayer.data.MusicFile) -> Unit,
    onDragEnd: () -> Unit,
    onMove: (com.chrismdz.vinylplayer.data.MusicFile, Int) -> Unit
) {
    var dragDistance = 0f
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
            .pointerInput(file.uri) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragDistance = 0f; onDragStart() },
                    onDragCancel = { dragDistance = 0f; onDragEnd() },
                    onDragEnd = { dragDistance = 0f; onDragEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount.y
                        val rowHeight = 72.dp.toPx()
                        val steps = (dragDistance / rowHeight).toInt()
                        if (steps != 0) {
                            val from = orderedQueue.indexOfFirst { it.uri == file.uri }
                            val target = (from + steps).coerceIn(0, orderedQueue.lastIndex)
                            if (from >= 0 && target != from) onDragTarget(orderedQueue[target])
                            onMove(file, steps)
                            dragDistance -= steps * rowHeight
                        }
                    }
                )
            }
            .clickable { onSelect(file) }
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
        Icon(
            if (isCurrent) Icons.Default.Pause else Icons.Default.QueueMusic,
            null,
            tint = if (isCurrent) Color.White else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun progress(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
