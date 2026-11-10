package com.chrismdz.vinylplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chrismdz.vinylplayer.data.MusicFile

/** Spacing scale used across screens (4dp grid). */
internal object VinylSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Outer padding of every screen; pinned bars bleed through it to reach the screen edges. */
    val screenHorizontal = 20.dp
    val screenTop = 12.dp
}

/** Corner radii: artwork 4–8dp, rows/inputs 10–12dp, grouped surfaces 18dp, pills fully rounded. */
internal object VinylShapes {
    val artworkSmall = RoundedCornerShape(4.dp)
    val artwork = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(12.dp)
    val group = RoundedCornerShape(18.dp)
}

/**
 * Page background shared by the library and detail screens: a soft vertical fade from the top surface
 * to the page background. [pinnedBarColor] is the top of this gradient, so pinned bars blend in.
 */
@Composable
internal fun screenBackgroundBrush(): Brush = Brush.verticalGradient(
    listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
)

@Composable
internal fun pinnedBarColor(): Color = MaterialTheme.colorScheme.surfaceContainer

/** Fully rounded chip used for tabs, filters and secondary actions. */
@Composable
internal fun LibraryPill(
    label: String,
    selected: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.heightIn(min = 36.dp).clip(CircleShape).background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (label.isEmpty()) 10.dp else 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription, tint = content, modifier = Modifier.size(18.dp))
            if (label.isNotEmpty()) Spacer(Modifier.width(6.dp))
        }
        if (label.isNotEmpty()) {
            Text(
                label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** Filled, compact search input shared by the library, playlists, pickers and settings. */
@Composable
internal fun VinylSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.heightIn(min = 44.dp).clip(VinylShapes.control).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = VinylSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(VinylSpacing.xs))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Clear, "Limpiar búsqueda", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** The single circular play button used by playlists, artists and albums. */
@Composable
internal fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentDescription: String = "Reproducir"
) {
    Box(
        modifier.size(size).shadow(if (enabled) 8.dp else 0.dp, CircleShape).clip(CircleShape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription, tint = contentColor, modifier = Modifier.size(size * 0.56f))
    }
}

/** Rounded tinted square holding an icon; identifies settings categories and info rows. */
@Composable
internal fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Small uppercase header above a group of rows. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(start = VinylSpacing.xxs, bottom = VinylSpacing.xs)
    )
}

/** Rounded container that groups related rows; separate rows with [GroupDivider]. */
@Composable
internal fun GroupedSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(VinylShapes.group).background(MaterialTheme.colorScheme.surfaceContainer),
        content = content
    )
}

@Composable
internal fun GroupDivider(startIndent: Dp = VinylSpacing.md) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
internal fun vinylSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

/**
 * Bar pinned to the top of a detail screen once its header scrolls away: back button, title and an
 * optional row of actions (used by playlists). Artists and albums show only back + title.
 */
@Composable
internal fun CollapsedDetailBar(
    visible: Boolean,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = pinnedBarColor(),
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 3 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { -it / 3 }
    ) {
        // Edge to edge and up behind the status bar, like part of the screen rather than a floating card.
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bleedTop = statusBarTop + VinylSpacing.screenTop
        Column(
            Modifier
                .layout { measurable, constraints ->
                    val sides = (VinylSpacing.screenHorizontal * 2).roundToPx()
                    val top = bleedTop.roundToPx()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = constraints.minWidth + sides, maxWidth = constraints.maxWidth + sides)
                    )
                    layout(constraints.maxWidth, (placeable.height - top).coerceAtLeast(0)) {
                        placeable.place(-sides / 2, -top)
                    }
                }
                .background(background)
                .padding(top = bleedTop, start = VinylSpacing.screenHorizontal - VinylSpacing.xxs, end = VinylSpacing.screenHorizontal)
                .padding(bottom = if (actions != null) VinylSpacing.sm else VinylSpacing.xxs)
        ) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás", tint = contentColor) }
                Text(
                    title,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (actions != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.xxs), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

/** Square artwork with a music-note placeholder. */
@Composable
internal fun TrackArtwork(file: MusicFile?, size: Dp) {
    Box(
        Modifier.size(size).clip(VinylShapes.artworkSmall).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val artwork = file?.artwork
        if (artwork != null) Image(artwork.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size * 0.45f))
    }
}

/**
 * Standard song row for every list (tracks, playlists, artists): flat, compact, artwork with the
 * now-playing animation, title in the accent color while it plays.
 */
@Composable
internal fun TrackRow(
    file: MusicFile,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onRequestArtwork: (MusicFile) -> Unit,
    onClick: () -> Unit,
    onMore: () -> Unit,
    subtitle: String = file.artist
) {
    LaunchedEffect(file.uri, file.artwork) { if (file.artwork == null) onRequestArtwork(file) }
    Row(
        Modifier.fillMaxWidth().clip(VinylShapes.control).clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(VinylShapes.artworkSmall)) {
            TrackArtwork(file, 54.dp)
            if (isCurrent) NowPlayingOverlay(isPlaying, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f).padding(horizontal = VinylSpacing.sm)) {
            Text(
                file.title,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFavorite) {
                    Icon(Icons.Default.Favorite, "Favorito", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(VinylSpacing.xxs))
                }
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreVert, "Opciones de ${file.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
