package com.chrismdz.vinylplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val IndexLetters: List<Char> = listOf('#') + ('A'..'Z')

/** Bucket used by the alphabet index: the first letter in uppercase, or '#' for digits and symbols. */
internal fun indexLetterOf(text: String): Char {
    val first = java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD).firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first else '#'
}

/**
 * A–Z index that always fits its height (each letter gets an equal share), reacts on touch-down without
 * drag slop, vibrates once per letter change and shows the selected letter in a bubble. Letters without
 * items are dimmed; selecting one jumps to the next letter that has items.
 */
@Composable
internal fun AlphabetIndex(
    availableLetters: Set<Char>,
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var touchY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(modifier.width(24.dp).fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (activeLetter != null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                .pointerInput(availableLetters) {
                    fun select(y: Float) {
                        touchY = y.coerceIn(0f, size.height.toFloat())
                        val index = (touchY / size.height * IndexLetters.size).toInt().coerceIn(0, IndexLetters.lastIndex)
                        val letter = IndexLetters.drop(index).firstOrNull { it in availableLetters }
                            ?: IndexLetters.take(index).lastOrNull { it in availableLetters }
                            ?: return
                        if (letter != activeLetter) {
                            activeLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(letter)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        select(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            select(change.position.y)
                        }
                        activeLetter = null
                    }
                }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IndexLetters.forEach { letter ->
                val available = letter in availableLetters
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        letter.toString(),
                        color = when {
                            letter == activeLetter -> MaterialTheme.colorScheme.primary
                            available -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        },
                        fontSize = 10.sp,
                        fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        // Bubble drawn outside the narrow strip, next to the finger.
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn(tween(90)) + scaleIn(tween(90), initialScale = 0.7f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.7f),
            modifier = Modifier.offset {
                val bubble = with(density) { 56.dp.roundToPx() }
                IntOffset(-with(density) { 64.dp.roundToPx() }, (touchY - bubble / 2f).roundToInt())
            }
        ) {
            // requiredSize: the 24dp strip must not squeeze the bubble; it overflows to the left instead.
            Box(
                Modifier.requiredSize(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (activeLetter ?: ' ').toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Fast-scroll thumb for a [LazyListState]. See [FastScrollbarCore]. */
@Composable
internal fun FastScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    minItems: Int = 30
) {
    val scope = rememberCoroutineScope()
    val total by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    val visible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.size } }
    val first by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    FastScrollbarCore(
        totalItems = total, visibleItems = visible, firstVisibleIndex = first,
        isScrolling = listState.isScrollInProgress, modifier = modifier, minItems = minItems
    ) { index -> scope.launch { listState.scrollToItem(index) } }
}

/** Fast-scroll thumb for a [LazyGridState] (albums and artists). */
@Composable
internal fun FastScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    minItems: Int = 30
) {
    val scope = rememberCoroutineScope()
    val total by remember { derivedStateOf { gridState.layoutInfo.totalItemsCount } }
    val visible by remember { derivedStateOf { gridState.layoutInfo.visibleItemsInfo.size } }
    val first by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    FastScrollbarCore(
        totalItems = total, visibleItems = visible, firstVisibleIndex = first,
        isScrolling = gridState.isScrollInProgress, modifier = modifier, minItems = minItems
    ) { index -> scope.launch { gridState.scrollToItem(index) } }
}

/**
 * Draggable thumb that follows the scroll position and jumps anywhere when dragged. Only the thumb takes
 * touches (28dp hit area) so rows beside it keep working.
 */
@Composable
private fun FastScrollbarCore(
    totalItems: Int,
    visibleItems: Int,
    firstVisibleIndex: Int,
    isScrolling: Boolean,
    modifier: Modifier,
    minItems: Int,
    scrollTo: (Int) -> Unit
) {
    if (totalItems < minItems) return
    val density = LocalDensity.current
    val thumbHeight = 52.dp
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val scrollFraction = (firstVisibleIndex.toFloat() / (totalItems - visibleItems).coerceAtLeast(1)).coerceIn(0f, 1f)
    val alpha by animateFloatAsState(if (dragging || isScrolling) 1f else 0.45f, tween(250), label = "scrollbarAlpha")

    BoxWithConstraints(modifier.width(28.dp).fillMaxHeight()) {
        val trackPx = with(density) { (maxHeight - thumbHeight).toPx() }.coerceAtLeast(1f)
        val fraction = if (dragging) dragFraction else scrollFraction
        val thumbOffsetPx = (trackPx * fraction).roundToInt()
        val currentTrackPx by rememberUpdatedState(trackPx)
        val currentTotal by rememberUpdatedState(totalItems)
        val currentScrollFraction by rememberUpdatedState(scrollFraction)
        val currentScrollTo by rememberUpdatedState(scrollTo)

        Box(
            Modifier.align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbOffsetPx) }
                .width(28.dp)
                .height(thumbHeight)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragFraction = currentScrollFraction
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, delta ->
                        change.consume()
                        dragFraction = (dragFraction + delta / currentTrackPx).coerceIn(0f, 1f)
                        currentScrollTo(((currentTotal - 1) * dragFraction).roundToInt())
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                Modifier.padding(end = 2.dp)
                    .width(if (dragging) 8.dp else 5.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

/** Animated equalizer bars marking the track that is playing; bars rest when playback is paused. */
@Composable
internal fun NowPlayingBars(isPlaying: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (isPlaying) AnimatedBars(modifier, color) else Bars(listOf(0.45f, 0.75f, 0.35f), modifier, color)
}

@Composable
private fun AnimatedBars(modifier: Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "nowPlaying")
    val heights = listOf(520, 380, 640).mapIndexed { index, duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(duration, delayMillis = index * 90, easing = if (index == 1) LinearEasing else FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }
    Bars(heights.map { it.value }, modifier, color)
}

@Composable
private fun Bars(fractions: List<Float>, modifier: Modifier, color: Color) {
    Row(modifier.size(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        fractions.forEach { fraction ->
            Box(
                Modifier.weight(1f).fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                    .background(color)
            )
        }
    }
}

/** Dark scrim with [NowPlayingBars] laid over a track's artwork. */
@Composable
internal fun NowPlayingOverlay(isPlaying: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        NowPlayingBars(isPlaying, Modifier.size(20.dp))
    }
}
