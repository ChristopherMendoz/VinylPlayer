package com.chrismdz.vinylplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chrismdz.vinylplayer.data.MusicFile
import kotlinx.coroutines.launch
import com.chrismdz.vinylplayer.data.Playlist

private val UnavailableColor = VinylPalette.Danger

/** Cover made from the playlist's albums: a 2×2 mosaic with four distinct albums, otherwise the first artwork. */
@Composable
internal fun PlaylistCover(
    files: List<MusicFile>,
    size: Dp,
    onRequestArtwork: (MusicFile) -> Unit,
    favorite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coverFiles = remember(files) { files.distinctBy { it.album }.take(4) }
    LaunchedEffect(coverFiles.map { it.uri }) {
        coverFiles.filter { it.artwork == null }.forEach(onRequestArtwork)
    }
    val shape = RoundedCornerShape(if (size > 100.dp) 8.dp else 6.dp)
    Box(
        modifier.size(size).clip(shape).background(
            if (favorite) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF3A2A0A)))
            else SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        ),
        contentAlignment = Alignment.Center
    ) {
        val artworks = coverFiles.mapNotNull { it.artwork }
        when {
            favorite -> Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(size * 0.4f))
            artworks.size >= 4 -> Column {
                artworks.take(4).chunked(2).forEach { pair ->
                    Row {
                        pair.forEach { bitmap ->
                            Image(bitmap.asImageBitmap(), null, Modifier.size(size / 2), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
            artworks.isNotEmpty() -> Image(artworks.first().asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Icon(Icons.Default.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.42f))
        }
    }
}

/** Row of the playlist list: cover, name and "Playlist · N canciones" subtitle. */
@Composable
internal fun PlaylistLibraryRow(
    name: String,
    subtitle: String,
    coverFiles: List<MusicFile>,
    favorite: Boolean,
    onRequestArtwork: (MusicFile) -> Unit,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistCover(coverFiles, 64.dp, onRequestArtwork, favorite)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onDelete != null) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, "Opciones", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Eliminar playlist") }, onClick = { menuOpen = false; confirmDelete = true })
                }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar “$name”?") },
            text = { Text("Se eliminará la playlist. Las canciones seguirán en tu biblioteca.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar", color = UnavailableColor) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

/**
 * Full-height playlist screen: everything scrolls together (search, cover, actions and tracks) so the
 * list gets the whole screen once the header scrolls away.
 */
@Composable
internal fun PlaylistDetailContent(
    playlist: Playlist,
    editable: Boolean,
    favorite: Boolean,
    originLabel: String?,
    library: List<MusicFile>,
    currentUri: android.net.Uri?,
    isPlaying: Boolean,
    favoriteUris: Set<String>,
    onBack: () -> Unit,
    onPlay: (files: List<MusicFile>, startIndex: Int, shuffle: Boolean) -> Unit,
    onQueue: (List<MusicFile>) -> Unit,
    onSave: (name: String, songUris: List<String>) -> String?,
    onAddSongs: (List<String>) -> Unit,
    onMore: (MusicFile) -> Unit,
    onRequestArtwork: (MusicFile) -> Unit,
    /** Owned by the caller so the scroll position survives leaving to the player and coming back. */
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    var query by rememberSaveable(playlist.name) { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val uri: (MusicFile) -> String = { it.uri.toString() }
    val allEntries = remember(library, playlist) {
        playlistSongEntries(library, playlist, "", uri, MusicFile::title, MusicFile::artist, MusicFile::album)
    }
    val visibleEntries = remember(allEntries, query) {
        if (query.isBlank()) allEntries
        else playlistSongEntries(library, playlist.copy(name = ""), query, uri, MusicFile::title, MusicFile::artist, MusicFile::album)
    }
    val allFiles = remember(allEntries) { allEntries.map { it.value } }
    val totalMinutes = remember(allFiles) { allFiles.sumOf { it.durationMs.coerceAtLeast(0) } / 60_000 }
    // Position of the action row inside the header: once it scrolls under the top, the pinned bar appears.
    var actionsTopPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > actionsTopPx }
    }
    // LazyColumn index of the playing track (header is item 0). Null when it isn't in the visible entries.
    val playingListIndex = remember(visibleEntries, currentUri) {
        visibleEntries.indexOfFirst { it.value.uri == currentUri }.takeIf { currentUri != null && it >= 0 }?.plus(1)
    }
    // The locate button only shows while the playing row is completely off screen.
    val showLocate by remember(playingListIndex) {
        derivedStateOf {
            val target = playingListIndex ?: return@derivedStateOf false
            listState.layoutInfo.visibleItemsInfo.none { it.index == target }
        }
    }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás", tint = MaterialTheme.colorScheme.onSurface) }
                    VinylSearchField(query, { query = it }, "Buscar en la playlist", Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                PlaylistCover(
                    allFiles, 220.dp, onRequestArtwork, favorite,
                    Modifier.align(Alignment.CenterHorizontally).shadow(18.dp, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(18.dp))
                Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${allFiles.size} canciones · ${formatTotalDuration(totalMinutes)}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
                originLabel?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(VinylSpacing.md))
                Row(
                    Modifier.onGloballyPositioned { actionsTopPx = it.positionInParent().y.toInt() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistActions(
                        editable = editable,
                        hasSongs = allFiles.isNotEmpty(),
                        playSize = 56.dp,
                        onAdd = { showAdd = true },
                        onEdit = { showEditor = true },
                        onQueue = { onQueue(allFiles) },
                        onShuffle = { onPlay(allFiles, 0, true) },
                        onPlay = { onPlay(allFiles, 0, false) }
                    )
                }
                Spacer(Modifier.height(VinylSpacing.sm))
            }
        }
        if (visibleEntries.isEmpty()) item(key = "empty") {
            Text(
                if (query.isBlank()) "Esta playlist está vacía. Usa Agregar para sumar canciones." else "No hay canciones que coincidan.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
        itemsIndexed(visibleEntries, key = { _, entry -> "${entry.index}:${entry.value.uri}" }) { _, entry ->
            val file = entry.value
            TrackRow(
                file = file,
                isCurrent = file.uri == currentUri,
                isPlaying = isPlaying,
                isFavorite = file.uri.toString() in favoriteUris,
                onRequestArtwork = onRequestArtwork,
                onClick = {
                    val start = allEntries.indexOfFirst { it.index == entry.index }.coerceAtLeast(0)
                    onPlay(allFiles, start, false)
                },
                onMore = { onMore(file) }
            )
        }
    }
    FastScrollbar(listState = listState, modifier = Modifier.align(Alignment.CenterEnd))
    AnimatedVisibility(
        visible = showLocate,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 20.dp),
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.6f),
        exit = fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.6f)
    ) {
        SmallFloatingActionButton(
            onClick = {
                val target = playingListIndex ?: return@SmallFloatingActionButton
                scope.launch {
                    // Lands the row about a third down the viewport so it isn't hidden under the pinned bar.
                    val offset = -listState.layoutInfo.viewportSize.height / 3
                    listState.animateScrollToItem(target, offset)
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Ir a la canción en reproducción")
        }
    }
    CollapsedDetailBar(
        visible = collapsed,
        title = playlist.name,
        onBack = onBack,
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        // The pinned bar keeps only the playlist options; playback stays in the header.
        PlaylistActions(
            editable = editable,
            hasSongs = allFiles.isNotEmpty(),
            playSize = 44.dp,
            showPlayback = false,
            onAdd = { showAdd = true },
            onEdit = { showEditor = true },
            onQueue = { onQueue(allFiles) },
            onShuffle = { onPlay(allFiles, 0, true) },
            onPlay = { onPlay(allFiles, 0, false) }
        )
    }
    }

    if (showEditor) {
        PlaylistEditDialog(
            playlist = playlist,
            library = library,
            onRequestArtwork = onRequestArtwork,
            onSave = onSave,
            onDismiss = { showEditor = false }
        )
    }
    if (showAdd) {
        AddSongsDialog(
            library = library,
            existing = remember(playlist) { playlist.songUris.toSet() },
            onRequestArtwork = onRequestArtwork,
            onAdd = { uris -> onAddSongs(uris); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }
}

/** Add / edit / queue pills plus shuffle and play; shown in the header and in the pinned bar. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.PlaylistActions(
    editable: Boolean,
    hasSongs: Boolean,
    playSize: Dp,
    showPlayback: Boolean = true,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onQueue: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        Modifier.weight(1f).padding(start = if (showPlayback) 0.dp else VinylSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.xs)
    ) {
        if (editable) {
            LibraryPill("Agregar", icon = Icons.Default.Add, onClick = onAdd)
            LibraryPill("Editar", icon = Icons.Default.Edit, onClick = onEdit)
        }
        LibraryPill("", icon = Icons.Default.QueueMusic, enabled = hasSongs, contentDescription = "Agregar a la fila", onClick = onQueue)
    }
    if (showPlayback) {
        IconButton(onClick = onShuffle, enabled = hasSongs) {
            Icon(Icons.Default.Shuffle, "Reproducir en aleatorio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        PlayButton(onClick = onPlay, enabled = hasSongs, size = playSize)
    }
}

private class EditRow(val key: Int, val uri: String)

/**
 * Editor: rename, remove and reorder by dragging the ≡ handle. Works on the stored list, so songs that
 * are not on this device are shown (as unavailable) and keep their place. Nothing is saved until "Guardar".
 */
@Composable
private fun PlaylistEditDialog(
    playlist: Playlist,
    library: List<MusicFile>,
    onRequestArtwork: (MusicFile) -> Unit,
    onSave: (name: String, songUris: List<String>) -> String?,
    onDismiss: () -> Unit
) {
    val filesByUri = remember(library) { library.associateBy { it.uri.toString() } }
    val rows = remember(playlist) { mutableStateListOf<EditRow>().apply { playlist.songUris.forEachIndexed { i, uri -> add(EditRow(i, uri)) } } }
    var name by remember(playlist) { mutableStateOf(playlist.name) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableListState(
        listState = listState,
        onMove = { from, to -> if (from in rows.indices && to in rows.indices) rows.add(to, rows.removeAt(from)) },
        indexOf = { key -> rows.indexOfFirst { it.key == key } }
    )
    val changed = name.trim() != playlist.name || rows.map { it.uri } != playlist.songUris
    val close = { if (changed) confirmDiscard = true else onDismiss() }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Default.Clear, "Cancelar", tint = MaterialTheme.colorScheme.onSurface) }
                    Text("Editar playlist", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    TextButton(onClick = {
                        val result = onSave(name, rows.map { it.uri })
                        if (result == null) onDismiss() else error = result
                    }) { Text("Guardar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    singleLine = true,
                    isError = error != null,
                    label = { Text("Nombre") },
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${rows.size} canciones · Arrastra ≡ o mantén presionada una canción para cambiar el orden",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().reorderable(reorder, handleWidth = 48.dp)
                ) {
                    itemsIndexed(rows, key = { _, row -> row.key }) { _, row ->
                        val file = filesByUri[row.uri]
                        LaunchedEffect(file?.uri, file?.artwork) { if (file != null && file.artwork == null) onRequestArtwork(file) }
                        val dragging = reorder.draggingKey == row.key
                        Row(
                            reorderableItem(reorder, row.key, RoundedCornerShape(8.dp))
                                .fillMaxWidth()
                                .background(if (dragging) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { rows.remove(row) }) {
                                Icon(Icons.Default.RemoveCircleOutline, "Quitar", tint = UnavailableColor)
                            }
                            TrackArtwork(file, 44.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    file?.title ?: "No disponible en este dispositivo",
                                    color = if (file == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    file?.artist ?: row.uri,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.DragHandle,
                                "Arrastrar para reordenar",
                                tint = if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                // The drag itself is handled by Modifier.reorderable on the list (handle strip = 48.dp).
                                modifier = Modifier.size(48.dp).padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("No se guardarán el nuevo orden, las canciones quitadas ni el nombre.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Descartar", color = UnavailableColor) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } }
        )
    }
}

/** Library picker for the playlist "Agregar" action; songs are appended in the order they are picked. */
@Composable
private fun AddSongsDialog(
    library: List<MusicFile>,
    existing: Set<String>,
    onRequestArtwork: (MusicFile) -> Unit,
    onAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val picked = remember { mutableStateListOf<String>() }
    // Only songs that are not in the playlist yet can be added.
    val candidates = remember(library, existing) { library.filter { it.uri.toString() !in existing } }
    val results = remember(candidates, query) {
        filterLibrary(candidates, query, MusicFile::title, MusicFile::artist, MusicFile::album).take(300)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, "Cerrar", tint = MaterialTheme.colorScheme.onSurface) }
                    Text("Agregar a la playlist", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                VinylSearchField(query, { query = it }, "Buscar en tu biblioteca", Modifier.fillMaxWidth())
                Text(
                    "${candidates.size} canciones disponibles que aún no están en la playlist",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (candidates.isEmpty()) {
                    Text(
                        "Todas las canciones de tu biblioteca ya están en esta playlist.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                }
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(results, key = { _, file -> file.uri.toString() }) { _, file ->
                        val uri = file.uri.toString()
                        val selected = uri in picked
                        LaunchedEffect(uri, file.artwork) { if (file.artwork == null) onRequestArtwork(file) }
                        Row(
                            Modifier.fillMaxWidth().clickable { if (selected) picked.remove(uri) else picked.add(uri) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(file, 44.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(file.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${file.artist} · ${file.album}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                if (selected) "Seleccionada" else "Agregar",
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Button(
                    onClick = { onAdd(picked.toList()) },
                    enabled = picked.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                ) { Text(if (picked.isEmpty()) "Selecciona canciones" else "Agregar ${picked.size}") }
            }
        }
    }
}

private fun formatTotalDuration(totalMinutes: Long): String =
    if (totalMinutes >= 60) "${totalMinutes / 60} h ${totalMinutes % 60} min" else "$totalMinutes min"
