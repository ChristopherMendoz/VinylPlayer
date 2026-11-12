package com.chrismdz.vinylplayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drag-and-drop reordering for a [LazyColumn], shared by "Editar playlist" and the play queue.
 *
 * Design notes (these are what keep a drag alive across the whole list):
 * - The gesture lives on the LazyColumn itself ([reorderable]), never on a row. A row moves,
 *   gets recomposed and can leave composition while auto-scrolling; the list container doesn't,
 *   so the gesture is never cancelled and its coordinates never shift under the finger.
 * - The finger position is stored as an absolute Y inside the list. The dragged row's visual
 *   offset is recomputed every frame from that Y and the row's current layout slot, so moves and
 *   scrolling can't make it drift away from the finger.
 * - The drop target is whatever row sits under the dragged row's center, so a fast drag can jump
 *   several positions in one step, and auto-scroll keeps bringing new rows under the finger.
 *
 * Indices passed to [onMove] and returned by [indexOf] are LazyColumn item indices; [indexOf]
 * reads the caller's real list, so moves are always computed from data, never from a stale layout.
 */
@Stable
internal class ReorderableListState(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val edgeZonePx: Float,
    private val maxScrollPerFramePx: Float,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val indexOf: (key: Any) -> Int,
    private val onDragStopped: (key: Any, completed: Boolean) -> Unit,
    private val canDrag: (key: Any) -> Boolean
) {
    /** Key of the row being dragged, or null when idle. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var fingerY by mutableFloatStateOf(0f)
    private var grabOffset = 0f
    private var startIndex = -1
    private var autoScrollJob: Job? = null

    /** Row settling into its slot after being dropped, and its remaining offset. */
    private var releasingKey by mutableStateOf<Any?>(null)
    private val releaseOffset = Animatable(0f)
    private var releaseJob: Job? = null

    /** Visual Y offset that keeps the dragged (or just released) row where it should be drawn. */
    fun translationFor(key: Any): Float {
        if (key == draggingKey) {
            val item = visibleItem(key) ?: return 0f
            return draggedTop(item) - item.top()
        }
        return if (key == releasingKey) releaseOffset.value else 0f
    }

    internal fun itemAt(y: Float): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.top() && y < it.top() + it.size }

    internal fun startDrag(y: Float): Boolean {
        val item = itemAt(y) ?: return false
        val key = item.key
        if (!canDrag(key)) return false
        releaseJob?.cancel()
        releasingKey = null
        draggingKey = key
        fingerY = y
        grabOffset = y - item.top()
        startIndex = indexOf(key)
        autoScrollJob?.cancel()
        autoScrollJob = scope.launch { autoScroll() }
        return true
    }

    internal fun dragTo(y: Float) {
        if (draggingKey == null) return
        fingerY = y
        moveIfNeeded()
    }

    internal fun endDrag(completed: Boolean) {
        val key = draggingKey ?: return
        autoScrollJob?.cancel()
        autoScrollJob = null
        val leftover = translationFor(key)
        // A cancelled gesture puts the row back where it started.
        val currentIndex = indexOf(key)
        if (!completed && currentIndex >= 0 && startIndex >= 0 && currentIndex != startIndex) {
            onMove(currentIndex, startIndex)
        }
        draggingKey = null
        startIndex = -1
        if (completed && leftover != 0f) {
            releasingKey = key
            releaseJob = scope.launch {
                releaseOffset.snapTo(leftover)
                releaseOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                releasingKey = null
            }
        }
        onDragStopped(key, completed)
    }

    private fun moveIfNeeded() {
        val key = draggingKey ?: return
        val info = listState.layoutInfo
        val dragged = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        // Layout hasn't measured the previous move (or a data refresh) yet; wait for the next pass.
        val from = indexOf(key)
        if (from < 0 || dragged.index != from) return
        val center = draggedTop(dragged) + dragged.size / 2f
        val target = info.visibleItemsInfo.firstOrNull { item ->
            item.key != key && canDrag(item.key) && center >= item.top() && center < item.top() + item.size
        } ?: return
        val to = indexOf(target.key)
        if (to < 0 || to != target.index || to == from) return
        // LazyColumn keeps its first visible item anchored by key; pin the scroll position so moving
        // that item doesn't drag the whole list along with it.
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        onMove(from, to)
        if (from == firstIndex || to == firstIndex) listState.requestScrollToItem(firstIndex, firstOffset)
    }

    private suspend fun autoScroll() {
        while (scope.isActive && draggingKey != null) {
            withFrameNanos { }
            val height = listState.layoutInfo.viewportSize.height.toFloat()
            if (height <= 0f) continue
            val zone = edgeZonePx.coerceAtMost(height / 3f)
            val speed = when {
                fingerY < zone -> -scrollSpeed(depth = (zone - fingerY) / zone)
                fingerY > height - zone -> scrollSpeed(depth = (fingerY - (height - zone)) / zone)
                else -> 0f
            }
            if (speed != 0f) listState.dispatchRawDelta(speed)
            // New rows slide under the finger while scrolling, even if the finger doesn't move.
            moveIfNeeded()
        }
    }

    /**
     * Auto-scroll speed in px per frame for a finger [depth] into the edge zone: 0 at the zone's
     * inner border, 1 at the list edge, above 1 once the finger is outside the list.
     * Inside the zone it eases in (fine control near the edge); outside the list it keeps
     * accelerating with distance, so dragging the finger higher (or lower) runs through a long
     * list faster, up to [MAX_SPEED_MULTIPLIER] times the speed at the edge.
     */
    private fun scrollSpeed(depth: Float): Float {
        val multiplier = if (depth <= 1f) depth * depth
        else (1f + (depth - 1f) * OUTSIDE_ACCELERATION).coerceAtMost(MAX_SPEED_MULTIPLIER)
        return maxScrollPerFramePx * multiplier.coerceAtLeast(0f)
    }

    /**
     * Where the dragged row is drawn: under the finger, but clamped to the list's visible area.
     * Pushing past the top (or bottom) keeps the row pinned at the edge, e.g. sitting as the first
     * track, instead of following the finger out of the list where it would be clipped away.
     */
    private fun draggedTop(item: LazyListItemInfo): Float {
        val maxTop = (listState.layoutInfo.viewportSize.height - item.size).toFloat().coerceAtLeast(0f)
        return (fingerY - grabOffset).coerceIn(0f, maxTop)
    }

    /** Item top in the list's own coordinates (item offsets exclude the leading content padding). */
    private fun LazyListItemInfo.top(): Float = (offset - listState.layoutInfo.viewportStartOffset).toFloat()

    private fun visibleItem(key: Any): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    private companion object {
        /** Extra speed per edge-zone height the finger travels outside the list. */
        const val OUTSIDE_ACCELERATION = 1.5f
        const val MAX_SPEED_MULTIPLIER = 4f
    }
}

@Composable
internal fun rememberReorderableListState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    indexOf: (key: Any) -> Int,
    onDragStopped: (key: Any, completed: Boolean) -> Unit = { _, _ -> },
    canDrag: (key: Any) -> Boolean = { true },
    edgeZone: Dp = 80.dp,
    maxScrollPerFrame: Dp = 14.dp
): ReorderableListState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnMove by rememberUpdatedState(onMove)
    val currentIndexOf by rememberUpdatedState(indexOf)
    val currentOnDragStopped by rememberUpdatedState(onDragStopped)
    val currentCanDrag by rememberUpdatedState(canDrag)
    return remember(listState, scope, density) {
        with(density) {
            ReorderableListState(
                listState = listState,
                scope = scope,
                edgeZonePx = edgeZone.toPx(),
                maxScrollPerFramePx = maxScrollPerFrame.toPx(),
                onMove = { from, to -> currentOnMove(from, to) },
                indexOf = { key -> currentIndexOf(key) },
                onDragStopped = { key, completed -> currentOnDragStopped(key, completed) },
                canDrag = { key -> currentCanDrag(key) }
            )
        }
    }
}

/**
 * Put on the LazyColumn. A long press anywhere on a row starts dragging it; when [handleWidth] is
 * set, touching the trailing strip of that width (the ≡ handle) starts dragging right away.
 */
internal fun Modifier.reorderable(
    state: ReorderableListState,
    handleWidth: Dp? = null,
    longPress: Boolean = true
): Modifier = pointerInput(state, handleWidth, longPress) {
    val handlePx = handleWidth?.toPx()
    awaitEachGesture {
        // Initial pass: the list sees the touch before its rows, the scroll and any bottom sheet.
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (state.itemAt(down.position.y) == null) return@awaitEachGesture
        val fromHandle = handlePx != null && down.position.x >= size.width - handlePx
        val startY = when {
            fromHandle -> {
                down.consume()
                down.position.y
            }
            longPress -> awaitLongPressInitialPass(down) ?: return@awaitEachGesture
            else -> return@awaitEachGesture
        }
        if (!state.startDrag(startY)) return@awaitEachGesture
        var completed = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                // Consumed so the list doesn't scroll and the row doesn't register a click.
                change.consume()
                if (change.changedToUpIgnoreConsumed()) {
                    completed = true
                    break
                }
                state.dragTo(change.position.y)
            }
        } finally {
            state.endDrag(completed)
        }
    }
}

/**
 * Waits for a long press on [down] and returns the finger's Y, or null if the finger lifts or moves
 * beyond touch slop first (a tap or a scroll).
 *
 * It listens in the Initial pass on purpose: rows are usually clickable, and a clickable child
 * consumes the down event in the Main pass. The stock awaitLongPressOrCancellation reads the Main
 * pass, sees that consumption and gives up immediately, so a long press on a tappable row (the
 * play queue) could never start a drag.
 */
private suspend fun AwaitPointerEventScope.awaitLongPressInitialPass(down: PointerInputChange): Float? {
    var lastY = down.position.y
    val interrupted = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                .firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
            if (change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull true
            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                return@withTimeoutOrNull true
            }
            lastY = change.position.y
        }
        @Suppress("UNREACHABLE_CODE")
        true
    }
    return if (interrupted == null) lastY else null
}

/**
 * Per-row modifier: neighbours slide smoothly into the gap, and the dragged row follows the
 * finger, slightly raised and enlarged, above the others.
 */
@Composable
internal fun LazyItemScope.reorderableItem(
    state: ReorderableListState,
    key: Any,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier {
    val dragging = state.draggingKey == key
    val scale by animateFloatAsState(if (dragging) 1.03f else 1f, label = "reorder-scale")
    val elevation by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "reorder-elevation")
    return Modifier
        .zIndex(if (dragging) 1f else 0f)
        // The dragged row is positioned from the finger, so it must not also animate its slot.
        .animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = if (dragging) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
        )
        .graphicsLayer {
            translationY = state.translationFor(key)
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation.toPx()
            this.shape = shape
            clip = false
        }
}
