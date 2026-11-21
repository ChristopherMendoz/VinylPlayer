package com.chrismdz.vinylplayer.ui.lyricsshare

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.chrismdz.vinylplayer.data.Lyrics
import com.chrismdz.vinylplayer.ui.LibraryPill
import com.chrismdz.vinylplayer.ui.VinylPalette
import kotlinx.coroutines.launch

private enum class ShareStep { SELECT, CUSTOMIZE }

/**
 * "Compartir letra": pick lines, then style the card and save or share it.
 * [cover] is the album's cover (same one the album screen and mini player use); its colors come
 * from VinylPlayer's existing cover color system through [LyricsCardPalette].
 */
@Composable
internal fun LyricsShareDialog(
    title: String,
    artist: String,
    cover: Bitmap?,
    lyrics: Lyrics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lines = remember(lyrics) { shareableLyricLines(lyrics) }
    val selected = remember(lyrics) { mutableStateListOf<Int>() }
    var step by remember { mutableStateOf(ShareStep.SELECT) }
    var style by remember { mutableStateOf(LyricsCardStyle.CLASSIC) }
    // Recomputed for each cover, so another album's palette is never reused by mistake.
    val schemes by produceState(initialValue = LyricsCardPalette.fallback(), cover) {
        value = LyricsCardPalette.schemesFor(cover)
    }
    var schemeIndex by remember(cover) { mutableIntStateOf(0) }
    val colors = schemes.getOrElse(schemeIndex) { schemes.first() }
    val content = LyricsCardContent(
        title = title,
        artist = artist,
        lines = selected.sorted().mapNotNull { lines.getOrNull(it) },
        cover = cover
    )
    var busy by remember { mutableStateOf(false) }

    fun save() {
        if (busy) return
        busy = true
        scope.launch {
            val image = LyricsCardExporter.render(content, style, colors)
            val saved = LyricsCardExporter.saveToGallery(context, image, title)
            busy = false
            Toast.makeText(
                context,
                if (saved) "Imagen guardada en Imágenes/VinylPlayer" else "No se pudo guardar la imagen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val legacyWritePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else Toast.makeText(context, "Se necesita permiso para guardar en la galería", Toast.LENGTH_SHORT).show()
    }
    fun requestSave() {
        val needsPermission = LyricsCardExporter.needsLegacyWritePermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) legacyWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
    }
    fun share() {
        if (busy) return
        busy = true
        scope.launch {
            runCatching {
                val image = LyricsCardExporter.render(content, style, colors)
                LyricsCardExporter.share(context, image, "$title · $artist")
            }.onFailure {
                Toast.makeText(context, "No se pudo compartir la imagen", Toast.LENGTH_SHORT).show()
            }
            busy = false
        }
    }

    Dialog(
        onDismissRequest = { if (step == ShareStep.CUSTOMIZE) step = ShareStep.SELECT else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                when (step) {
                    ShareStep.SELECT -> SelectStep(
                        lines = lines,
                        selected = selected,
                        content = content,
                        style = style,
                        colors = colors,
                        onClose = onDismiss,
                        onToggle = { index ->
                            when {
                                index in selected -> selected.remove(index)
                                selected.size >= MAX_SHARED_LYRIC_LINES -> Toast.makeText(
                                    context,
                                    "Puedes elegir hasta $MAX_SHARED_LYRIC_LINES líneas",
                                    Toast.LENGTH_SHORT
                                ).show()
                                else -> selected.add(index)
                            }
                        },
                        onNext = { step = ShareStep.CUSTOMIZE }
                    )
                    ShareStep.CUSTOMIZE -> CustomizeStep(
                        content = content,
                        style = style,
                        schemes = schemes,
                        schemeIndex = schemeIndex.coerceIn(0, schemes.lastIndex),
                        busy = busy,
                        onBack = { step = ShareStep.SELECT },
                        onStyle = { style = it },
                        onScheme = { schemeIndex = it },
                        onSave = ::requestSave,
                        onShare = ::share
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SelectStep(
    lines: List<String?>,
    selected: List<Int>,
    content: LyricsCardContent,
    style: LyricsCardStyle,
    colors: LyricsCardColors,
    onClose: () -> Unit,
    onToggle: (Int) -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Clear, "Cerrar", tint = MaterialTheme.colorScheme.onSurface) }
        Text(
            "Compartir letra",
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onNext, enabled = selected.isNotEmpty()) {
            Text("Siguiente", fontWeight = FontWeight.Bold)
        }
    }
    // Live miniature of the card while choosing lines.
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LyricsCardPreview(content, style, colors, Modifier.width(96.dp).aspectRatio(9f / 16f).shadow(8.dp, RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                "${selected.size}/$MAX_SHARED_LYRIC_LINES líneas",
                color = if (selected.size >= MAX_SHARED_LYRIC_LINES) VinylPalette.Gold else MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Toca las líneas que quieres en la tarjeta. Se mantienen en el orden de la canción.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
        itemsIndexed(lines) { index, line ->
            if (line == null) {
                Spacer(Modifier.height(14.dp))
            } else {
                val isSelected = index in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VinylPalette.Gold.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onToggle(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        line,
                        Modifier.weight(1f),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        fontSize = 17.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Icon(
                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isSelected) VinylPalette.Gold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(start = 12.dp).size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CustomizeStep(
    content: LyricsCardContent,
    style: LyricsCardStyle,
    schemes: List<LyricsCardColors>,
    schemeIndex: Int,
    busy: Boolean,
    onBack: () -> Unit,
    onStyle: (LyricsCardStyle) -> Unit,
    onScheme: (Int) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver a las líneas", tint = MaterialTheme.colorScheme.onSurface) }
        Text("Personalizar", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    // Faithful preview: the exact drawing that will be exported, scaled to fit.
    Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        LyricsCardPreview(
            content, style, schemes[schemeIndex],
            Modifier.fillMaxHeight().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .shadow(16.dp, RoundedCornerShape(16.dp))
        )
    }
    Text("Estilo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LyricsCardStyle.entries.forEach { option ->
            LibraryPill(option.label, selected = option == style) { onStyle(option) }
        }
    }
    Text("Colores del álbum", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        schemes.forEachIndexed { index, scheme ->
            ColorSwatch(scheme, selected = index == schemeIndex) { onScheme(index) }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onSave, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 50.dp)) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Guardar")
        }
        Button(
            onClick = onShare,
            enabled = !busy,
            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Compartir")
        }
    }
}

/** Small circle showing a color combination; the selected one gets a gold ring. */
@Composable
private fun ColorSwatch(scheme: LyricsCardColors, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val fill = if (scheme.background.size > 1) {
            Modifier.background(Brush.linearGradient(scheme.background.map(::Color)))
        } else Modifier.background(Color(scheme.background.first()))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(fill)
                .border(if (selected) 3.dp else 1.dp, if (selected) VinylPalette.Gold else Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable(onClick = onClick)
        )
        Text(
            scheme.label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** The card drawn by [LyricsCardRenderer] at 1080×1920 and scaled to this composable's size. */
@Composable
private fun LyricsCardPreview(
    content: LyricsCardContent,
    style: LyricsCardStyle,
    colors: LyricsCardColors,
    modifier: Modifier
) {
    Canvas(modifier.clip(RoundedCornerShape(12.dp))) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.scale(size.width / LyricsCardRenderer.WIDTH, size.height / LyricsCardRenderer.HEIGHT)
            LyricsCardRenderer.draw(native, content, style, colors)
            native.restore()
        }
    }
}
