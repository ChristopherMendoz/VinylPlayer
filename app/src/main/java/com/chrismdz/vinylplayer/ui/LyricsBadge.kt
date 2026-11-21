package com.chrismdz.vinylplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chrismdz.vinylplayer.data.MusicFile

/**
 * Song URIs that have lyrics in the local cache. Provided once at the root (MainActivity) from
 * [PlayerViewModel.songsWithLyrics], so every song row can read it without threading a parameter.
 */
internal val LocalSongsWithLyrics = compositionLocalOf { emptySet<String>() }

/** True when the lyrics cache holds plain or synced lyrics for this song. */
@Composable
internal fun hasCachedLyrics(file: MusicFile): Boolean =
    file.uri.toString() in LocalSongsWithLyrics.current

/**
 * Small "has lyrics" tag shown before the artist line: three text lines of decreasing length on a
 * tinted rounded square, in the accent color. Sized to the secondary text (14dp).
 */
@Composable
internal fun LyricsBadge(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(tint.copy(alpha = 0.20f))
            .semantics { contentDescription = "Letra disponible" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(14.dp).padding(horizontal = 3.dp, vertical = 3.5.dp)) {
            val stroke = 1.4.dp.toPx()
            val widths = floatArrayOf(1f, 0.72f, 0.88f)
            val gap = (size.height - stroke) / (widths.size - 1)
            widths.forEachIndexed { index, fraction ->
                val y = stroke / 2f + gap * index
                drawLine(
                    color = tint,
                    start = Offset(stroke / 2f, y),
                    end = Offset(stroke / 2f + (size.width - stroke) * fraction, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
