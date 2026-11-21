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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.chrismdz.vinylplayer.ui.GroupedSurface
import com.chrismdz.vinylplayer.ui.SectionLabel
import com.chrismdz.vinylplayer.ui.VinylShapes
import com.chrismdz.vinylplayer.ui.VinylSpacing
import com.chrismdz.vinylplayer.ui.screenBackgroundBrush
import kotlinx.coroutines.launch

private enum class ShareStep { SELECT, CUSTOMIZE }

/**
 * "Compartir letra": pick lines, then style the card and save or share it.
 * [cover] is the shared song's own cover (the album cover only when the song has none); its colors come
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
    // 1 = automatic size for the number of lines; the user can make the lyrics smaller or bigger.
    var textScale by remember { mutableFloatStateOf(1f) }
    var font by remember { mutableStateOf(LyricsCardFont.STYLE) }
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
        cover = cover,
        textScale = textScale,
        font = font
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
        // Same page treatment as the library and detail screens.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).background(screenBackgroundBrush())) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = VinylSpacing.screenHorizontal, vertical = VinylSpacing.screenTop)
            ) {
                when (step) {
                    ShareStep.SELECT -> SelectStep(
                        songLabel = "$title · $artist",
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
                        songLabel = "$title · $artist",
                        content = content,
                        style = style,
                        schemes = schemes,
                        schemeIndex = schemeIndex.coerceIn(0, schemes.lastIndex),
                        textScale = textScale,
                        onTextScale = { textScale = it },
                        font = font,
                        onFont = { font = it },
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

/** Header like the app's top bars: navigation icon, title with the song below, optional action. */
@Composable
private fun ShareHeader(
    title: String,
    subtitle: String,
    navigationIcon: ImageVector,
    navigationLabel: String,
    onNavigate: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigate, modifier = Modifier.padding(end = VinylSpacing.xxs)) {
            Icon(navigationIcon, navigationLabel, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        action?.invoke()
    }
}

/** Accent pill used for the main action of each step (Siguiente / Compartir). */
@Composable
private fun AccentPillButton(
    label: String,
    icon: ImageVector?,
    enabled: Boolean,
    busy: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (enabled) primary else primary.copy(alpha = 0.3f))
            .clickable(enabled = enabled && !busy, onClick = onClick)
            .padding(horizontal = VinylSpacing.lg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val content = MaterialTheme.colorScheme.onPrimary
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = content)
            Spacer(Modifier.width(VinylSpacing.xs))
        } else if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(VinylSpacing.xs))
        }
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Neutral pill for secondary actions (Guardar). */
@Composable
private fun NeutralPillButton(label: String, icon: ImageVector, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = VinylSpacing.lg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(VinylSpacing.xs))
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ColumnScope.SelectStep(
    songLabel: String,
    lines: List<String?>,
    selected: List<Int>,
    content: LyricsCardContent,
    style: LyricsCardStyle,
    colors: LyricsCardColors,
    onClose: () -> Unit,
    onToggle: (Int) -> Unit,
    onNext: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    ShareHeader(
        title = "Compartir letra",
        subtitle = songLabel,
        navigationIcon = Icons.Default.Clear,
        navigationLabel = "Cerrar",
        onNavigate = onClose
    ) {
        AccentPillButton("Siguiente", icon = null, enabled = selected.isNotEmpty(), onClick = onNext)
    }
    Spacer(Modifier.height(VinylSpacing.sm))
    // Summary: live miniature of the card + how many lines are in it.
    GroupedSurface {
        Row(Modifier.padding(VinylSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            LyricsCardPreview(content, style, colors, Modifier.width(72.dp).aspectRatio(9f / 16f))
            Column(Modifier.weight(1f).padding(start = VinylSpacing.md)) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (selected.isNotEmpty()) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = VinylSpacing.sm, vertical = VinylSpacing.xxs)
                ) {
                    Text(
                        "${selected.size} de $MAX_SHARED_LYRIC_LINES líneas",
                        color = if (selected.isNotEmpty()) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(VinylSpacing.xs))
                Text(
                    "Toca las líneas que quieres en la tarjeta. Se ordenan como en la canción.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    Spacer(Modifier.height(VinylSpacing.lg))
    SectionLabel("Letra")
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = VinylSpacing.md)) {
        itemsIndexed(lines) { index, line ->
            if (line == null) {
                Spacer(Modifier.height(VinylSpacing.md))
            } else {
                val isSelected = index in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(VinylShapes.control)
                        .background(if (isSelected) accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onToggle(index) }
                        .padding(horizontal = VinylSpacing.sm, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        line,
                        Modifier.weight(1f),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Icon(
                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(start = VinylSpacing.sm).size(22.dp)
                    )
                }
            }
        }
    }
}

private enum class CustomizePanel(val label: String) { STYLE("ESTILO"), FONT("FUENTE"), SIZE("TAMAÑO"), COLORS("COLORES") }

@Composable
private fun ColumnScope.CustomizeStep(
    songLabel: String,
    content: LyricsCardContent,
    style: LyricsCardStyle,
    schemes: List<LyricsCardColors>,
    schemeIndex: Int,
    textScale: Float,
    onTextScale: (Float) -> Unit,
    font: LyricsCardFont,
    onFont: (LyricsCardFont) -> Unit,
    busy: Boolean,
    onBack: () -> Unit,
    onStyle: (LyricsCardStyle) -> Unit,
    onScheme: (Int) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    var panel by remember { mutableStateOf(CustomizePanel.STYLE) }
    ShareHeader(
        title = "Personalizar",
        subtitle = songLabel,
        navigationIcon = Icons.Default.ArrowBack,
        navigationLabel = "Volver a las líneas",
        onNavigate = onBack
    )
    // Faithful preview: the exact drawing that will be exported, scaled to fit.
    Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = VinylSpacing.md), contentAlignment = Alignment.Center) {
        LyricsCardPreview(
            content, style, schemes[schemeIndex],
            Modifier.fillMaxHeight().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .shadow(16.dp, VinylShapes.group)
        )
    }
    // One control panel at a time, picked with the same switcher as Portada / Letra.
    Row(
        Modifier
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            .padding(3.dp)
    ) {
        CustomizePanel.entries.forEach { option ->
            val active = option == panel
            Box(
                Modifier
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (active) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { panel = option }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    option.label,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Spacer(Modifier.height(VinylSpacing.sm))
    GroupedSurface(Modifier.heightIn(min = 96.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(vertical = VinylSpacing.sm), contentAlignment = Alignment.CenterStart) {
            when (panel) {
                CustomizePanel.STYLE -> PillRow {
                    LyricsCardStyle.entries.forEach { option ->
                        LibraryPill(option.label, selected = option == style) { onStyle(option) }
                    }
                }
                CustomizePanel.FONT -> PillRow {
                    LyricsCardFont.entries.forEach { option ->
                        LibraryPill(option.label, selected = option == font) { onFont(option) }
                    }
                }
                CustomizePanel.SIZE -> Column(Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (textScale == 1f) "Automático" else "${(textScale * 100).roundToInt()} %",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        LibraryPill("Auto", selected = textScale == 1f) { onTextScale(1f) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = textScale,
                            onValueChange = { onTextScale((it * 20).roundToInt() / 20f) },
                            valueRange = LyricsCardRenderer.MIN_TEXT_SCALE..LyricsCardRenderer.MAX_TEXT_SCALE,
                            modifier = Modifier.weight(1f).padding(horizontal = VinylSpacing.sm),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text("A", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                CustomizePanel.COLORS -> PillRow(spacing = VinylSpacing.md) {
                    schemes.forEachIndexed { index, scheme ->
                        ColorSwatch(scheme, selected = index == schemeIndex) { onScheme(index) }
                    }
                }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = VinylSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        NeutralPillButton("Guardar", Icons.Default.Download, enabled = !busy, modifier = Modifier.weight(1f), onClick = onSave)
        AccentPillButton("Compartir", Icons.Default.Share, enabled = true, busy = busy, modifier = Modifier.weight(1f), onClick = onShare)
    }
}

/** Horizontally scrolling row of choices inside a grouped panel. */
@Composable
private fun PillRow(spacing: Dp = VinylSpacing.xs, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = VinylSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** Small circle showing a color combination; the selected one gets an accent ring. */
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
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    CircleShape
                )
                .clickable(onClick = onClick)
        )
        Text(
            scheme.label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = VinylSpacing.xxs)
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
    Canvas(modifier.clip(VinylShapes.control)) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.scale(size.width / LyricsCardRenderer.WIDTH, size.height / LyricsCardRenderer.HEIGHT)
            LyricsCardRenderer.draw(native, content, style, colors)
            native.restore()
        }
    }
}
