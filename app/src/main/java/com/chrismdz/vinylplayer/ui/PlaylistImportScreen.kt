package com.chrismdz.vinylplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chrismdz.vinylplayer.data.MusicFile
import com.chrismdz.vinylplayer.data.playlistimport.ImportReviewItem
import com.chrismdz.vinylplayer.data.playlistimport.MatchStatus
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportResult
import com.chrismdz.vinylplayer.data.playlistimport.PlaylistImportReview

private val AmbiguousColor = Color(0xFFFFB74D)
private val MissingColor = Color(0xFFFF6B6B)

private enum class ImportFilter(val label: String) {
    PENDING("Pendientes"),
    ALL("Todas"),
    MATCHED("Reconocidas"),
    EXCLUDED("Excluidas")
}

/** Progress, review, result and error steps of the playlist import, layered over the current screen. */
@Composable
internal fun PlaylistImportFlow(
    state: PlaylistImportState,
    library: List<MusicFile>,
    onRename: (String) -> Unit,
    onSelectPlaylist: (Int) -> Unit,
    onLink: (Int, String) -> Unit,
    onExclude: (Int, Boolean) -> Unit,
    onReset: (Int) -> Unit,
    onExcludeMissing: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPickAnother: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onDismissResult: () -> Unit
) {
    when (state) {
        PlaylistImportState.Idle -> Unit
        is PlaylistImportState.Working -> ImportProgressDialog(state.message, onCancel)
        is PlaylistImportState.Failed -> AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("No se pudo importar") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onPickAnother) { Text("Elegir otro archivo") } },
            dismissButton = { TextButton(onClick = onCancel) { Text("Cerrar") } }
        )
        is PlaylistImportState.Reviewing -> ImportReviewDialog(
            review = state.review,
            nameError = state.nameError,
            saving = false,
            library = library,
            onRename = onRename,
            onSelectPlaylist = onSelectPlaylist,
            onLink = onLink,
            onExclude = onExclude,
            onReset = onReset,
            onExcludeMissing = onExcludeMissing,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
        is PlaylistImportState.Saving -> ImportReviewDialog(
            review = state.review,
            nameError = null,
            saving = true,
            library = library,
            onRename = {},
            onSelectPlaylist = {},
            onLink = { _, _ -> },
            onExclude = { _, _ -> },
            onReset = {},
            onExcludeMissing = {},
            onConfirm = {},
            onCancel = {}
        )
        is PlaylistImportState.Finished -> ImportResultDialog(state.result, onOpenPlaylist, onDismissResult)
    }
}

@Composable
private fun ImportProgressDialog(message: String, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Importar playlist") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                Spacer(Modifier.width(16.dp))
                Text(message)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

@Composable
private fun ImportResultDialog(
    result: PlaylistImportResult,
    onOpenPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playlist importada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("“${result.playlistName}” se creó con ${result.importedCount} de ${result.totalEntries} canciones.")
                Text(
                    "Origen: ${result.source.displayName} · ${result.format.displayName}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 13.sp
                )
                if (result.manuallyLinkedCount > 0) Text("${result.manuallyLinkedCount} vinculadas manualmente.", fontSize = 13.sp)
                if (result.skippedTitles.isNotEmpty()) {
                    val shown = result.skippedTitles.take(5).joinToString("\n") { "• $it" }
                    val more = (result.skippedTitles.size - 5).takeIf { it > 0 }?.let { "\n… y $it más" }.orEmpty()
                    Text("Omitidas (${result.skippedTitles.size}):\n$shown$more", fontSize = 13.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onOpenPlaylist(result.playlistName) }) { Text("Abrir playlist") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun ImportReviewDialog(
    review: PlaylistImportReview,
    nameError: String?,
    saving: Boolean,
    library: List<MusicFile>,
    onRename: (String) -> Unit,
    onSelectPlaylist: (Int) -> Unit,
    onLink: (Int, String) -> Unit,
    onExclude: (Int, Boolean) -> Unit,
    onReset: (Int) -> Unit,
    onExcludeMissing: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val filesByUri = remember(library) { library.associateBy { it.uri.toString() } }
    var filterName by rememberSaveable(review.selectedPlaylistIndex) {
        mutableStateOf(if (review.ambiguousCount + review.notFoundCount > 0) ImportFilter.PENDING.name else ImportFilter.ALL.name)
    }
    val filter = ImportFilter.valueOf(filterName)
    var pickerIndex by remember { mutableStateOf<Int?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val visible = remember(review.items, filter) {
        review.items.withIndex().filter { (_, item) ->
            when (filter) {
                ImportFilter.PENDING -> !item.excluded && (item.status == MatchStatus.MATCH_AMBIGUOUS || item.status == MatchStatus.NOT_FOUND)
                ImportFilter.ALL -> true
                ImportFilter.MATCHED -> item.willImport
                ImportFilter.EXCLUDED -> item.excluded
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!saving) showDiscard = true },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    IconButton(onClick = { showDiscard = true }, enabled = !saving) {
                        Icon(Icons.Default.Clear, "Cancelar importación", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Revisar importación", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = review.playlistName,
                            onValueChange = onRename,
                            enabled = !saving,
                            singleLine = true,
                            isError = nameError != null,
                            label = { Text("Nombre de la playlist") },
                            supportingText = { nameError?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { ImportSourceInfo(review, saving, onSelectPlaylist) }
                    item { ImportSummary(review) }
                    items(review.warnings) { warning ->
                        Text(
                            warning,
                            fontSize = 12.sp,
                            color = AmbiguousColor,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(AmbiguousColor.copy(alpha = 0.10f)).padding(10.dp)
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            ImportFilter.entries.forEach { option ->
                                TextButton(onClick = { filterName = option.name }) {
                                    Text(
                                        option.label,
                                        color = if (option == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                        fontWeight = if (option == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            if (review.ambiguousCount + review.notFoundCount > 0) {
                                TextButton(onClick = onExcludeMissing, enabled = !saving) {
                                    Text("Excluir pendientes", color = MissingColor)
                                }
                            }
                        }
                    }
                    if (visible.isEmpty()) item {
                        Text(
                            "No hay canciones en esta vista.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    items(visible, key = { it.index }) { (index, item) ->
                        ImportItemRow(
                            item = item,
                            filesByUri = filesByUri,
                            enabled = !saving,
                            onLink = { uri -> onLink(index, uri) },
                            onPick = { pickerIndex = index },
                            onExclude = { onExclude(index, !item.excluded) },
                            onReset = { onReset(index) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                ImportBottomBar(review, saving, onCancel = { showDiscard = true }, onImport = { showConfirm = true })
            }
        }
    }

    val pickerItem = pickerIndex?.let { review.items.getOrNull(it) }
    pickerIndex?.takeIf { pickerItem != null }?.let { index ->
        val item = pickerItem!!
        LocalSongPickerDialog(
            library = library,
            initialQuery = item.entry.title.orEmpty(),
            onSelect = { uri ->
                onLink(index, uri)
                pickerIndex = null
            },
            onDismiss = { pickerIndex = null }
        )
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("¿Cancelar la importación?") },
            text = { Text("No se guardará ninguna playlist ni las vinculaciones realizadas.") },
            confirmButton = { TextButton(onClick = { showDiscard = false; onCancel() }) { Text("Cancelar importación", color = MissingColor) } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Seguir revisando") } }
        )
    }
    if (showConfirm) {
        val skipped = review.total - review.importCount
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirmar importación") },
            text = {
                Text(
                    "Se creará “${review.playlistName.trim()}” con ${review.importCount} canciones " +
                        "(${review.automaticCount} reconocidas automáticamente, ${review.manualCount} vinculadas manualmente)." +
                        if (skipped > 0) " $skipped entradas se omitirán; el resto conserva su orden original." else ""
                )
            },
            confirmButton = { TextButton(onClick = { showConfirm = false; onConfirm() }) { Text("Importar") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Volver") } }
        )
    }
}

@Composable
private fun ImportSourceInfo(review: PlaylistImportReview, saving: Boolean, onSelectPlaylist: (Int) -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Origen: ${review.source.displayName}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Text("Formato: ${review.format.displayName}", color = secondary, fontSize = 13.sp)
        review.fileName?.let { Text("Archivo: $it", color = secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (review.availablePlaylists.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }, enabled = !saving) {
                    Text(
                        "Playlist del archivo: ${review.availablePlaylists[review.selectedPlaylistIndex]}",
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    review.availablePlaylists.forEachIndexed { index, label ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onSelectPlaylist(index) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSummary(review: PlaylistImportReview) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip("Total", review.total, MaterialTheme.colorScheme.onSurface)
        SummaryChip("Exactas", review.exactCount, MaterialTheme.colorScheme.primary)
        SummaryChip("Alta confianza", review.highConfidenceCount, MaterialTheme.colorScheme.primary)
        SummaryChip("Manuales", review.manualCount, MaterialTheme.colorScheme.primary)
        SummaryChip("Ambiguas", review.ambiguousCount, AmbiguousColor)
        SummaryChip("No encontradas", review.notFoundCount, MissingColor)
        SummaryChip("Excluidas", review.excludedCount, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun SummaryChip(label: String, value: Int, color: Color) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 11.sp)
    }
}

@Composable
private fun ImportItemRow(
    item: ImportReviewItem,
    filesByUri: Map<String, MusicFile>,
    enabled: Boolean,
    onLink: (String) -> Unit,
    onPick: () -> Unit,
    onExclude: () -> Unit,
    onReset: () -> Unit
) {
    val entry = item.entry
    val statusColor = when {
        item.excluded -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        item.status == MatchStatus.MATCH_AMBIGUOUS -> AmbiguousColor
        item.status == MatchStatus.NOT_FOUND -> MissingColor
        else -> MaterialTheme.colorScheme.primary
    }
    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${entry.position + 1}", color = secondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.displayTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.excluded) TextDecoration.LineThrough else null
                )
                val details = listOfNotNull(entry.displayArtist, entry.album, entry.durationMs?.let(::formatImportDuration))
                if (details.isNotEmpty()) Text(details.joinToString(" · "), color = secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (item.excluded) "Excluida" else item.status.label,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        item.selectedUri?.let(filesByUri::get)?.takeIf { !item.excluded }?.let { file ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 32.dp)) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${file.title} · ${file.artist} · ${formatImportDuration(file.durationMs)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!item.excluded && item.status != MatchStatus.MANUALLY_LINKED) {
            Text(item.autoMatch.reason, color = secondary, fontSize = 11.sp, modifier = Modifier.padding(start = 32.dp))
        }
        if (!item.excluded && item.status == MatchStatus.MATCH_AMBIGUOUS) {
            Column(Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Elige la canción correcta:", color = AmbiguousColor, fontSize = 12.sp)
                item.autoMatch.candidates.forEach { candidate ->
                    val file = filesByUri[candidate.uri] ?: return@forEach
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, AmbiguousColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable(enabled = enabled) { onLink(candidate.uri) }
                            .padding(8.dp)
                    ) {
                        Text(file.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${file.artist} · ${file.album} · ${formatImportDuration(file.durationMs)}",
                            color = secondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(candidate.reason, color = secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(Modifier.padding(start = 20.dp)) {
            TextButton(onClick = onPick, enabled = enabled) {
                Text(if (item.selectedUri == null) "Buscar en biblioteca" else "Cambiar", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
            TextButton(onClick = onExclude, enabled = enabled) {
                Text(if (item.excluded) "Incluir" else "Excluir", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), fontSize = 13.sp)
            }
            if (item.excluded || item.status != item.autoMatch.status || item.selectedUri != item.autoMatch.selectedUri) {
                TextButton(onClick = onReset, enabled = enabled) {
                    Text("Restaurar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ImportBottomBar(review: PlaylistImportReview, saving: Boolean, onCancel: () -> Unit, onImport: () -> Unit) {
    val skipped = review.total - review.importCount
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            when {
                review.ambiguousCount > 0 -> "Resuelve o excluye ${review.ambiguousCount} coincidencias ambiguas para continuar."
                skipped > 0 -> "Se importarán ${review.importCount} de ${review.total}; $skipped se omitirán."
                else -> "Se importarán las ${review.importCount} canciones."
            },
            color = if (review.ambiguousCount > 0) AmbiguousColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel, enabled = !saving) { Text("Cancelar", color = MissingColor) }
            Spacer(Modifier.weight(1f))
            if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            Button(
                onClick = onImport,
                enabled = !saving && review.ambiguousCount == 0 && review.importCount > 0 && review.playlistName.isNotBlank()
            ) { Text("Importar ${review.importCount}") }
        }
    }
}

/** Manual linking searches only the MediaStore catalog the app is already authorized to read. */
@Composable
private fun LocalSongPickerDialog(
    library: List<MusicFile>,
    initialQuery: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val results = remember(library, query) {
        filterLibrary(library, query, MusicFile::title, MusicFile::artist, MusicFile::album).take(200)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 620.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Elegir canción local", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Limpiar") }
                    },
                    placeholder = { Text("Título, artista o álbum") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (library.isEmpty()) {
                    Text("La biblioteca está vacía o no hay permiso para leer audio.", color = MissingColor, fontSize = 13.sp)
                }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results, key = { it.uri.toString() }) { file ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(file.uri.toString()) }.padding(10.dp)
                        ) {
                            Text(file.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${file.artist} · ${file.album} · ${formatImportDuration(file.durationMs)}",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}

private fun formatImportDuration(milliseconds: Long): String {
    if (milliseconds <= 0) return "--:--"
    val totalSeconds = milliseconds / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
