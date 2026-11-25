package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalTrainPilotPalette
import kotlin.math.roundToInt

/**
 * Drag state of one reorderable list. The lifted row follows the finger and the rows it passes
 * slide one place up or down live, so the gap always shows where it will land; the list itself is
 * written once on drop.
 */
class TrainPilotReorderState {
    var draggingKey by mutableStateOf<Any?>(null)
        internal set
    var offsetY by mutableFloatStateOf(0f)
        internal set
    internal val heights = mutableStateMapOf<Any, Int>()
    internal var draggingIndex by mutableStateOf(-1)
    internal var draggingLastIndex by mutableStateOf(-1)
    internal var draggingStepPx by mutableFloatStateOf(0f)
    internal var pointerY by mutableFloatStateOf(0f)
    internal var measuredOrder: List<Any> = emptyList()
    internal var gapPx: Float = 0f

    /** Target index for a drop of [index] after dragging [offsetY] px with rows [stepPx] apart. */
    fun dropTarget(index: Int, lastIndex: Int, stepPx: Float): Int {
        if (index < 0 || stepPx <= 0f) return index
        if (measuredOrder.size == lastIndex + 1) {
            return measuredReorderTarget(measuredOrder.map { heights[it]?.toFloat() ?: (stepPx - gapPx) },
                gapPx, index, offsetY)
        }
        return (index + (offsetY / stepPx).roundToInt()).coerceIn(0, lastIndex)
    }

    /** Live shift in px of the row at [index] while another row is dragged (0 when idle). */
    fun displacementFor(index: Int): Float {
        val from = draggingIndex
        if (draggingKey == null || from < 0 || index == from || draggingStepPx <= 0f) return 0f
        val target = dropTarget(from, draggingLastIndex, draggingStepPx)
        return when {
            target > from && index in (from + 1)..target -> -draggingStepPx
            target < from && index in target until from -> draggingStepPx
            else -> 0f
        }
    }
}

internal fun measuredReorderTarget(heights: List<Float>, gap: Float, index: Int, offset: Float): Int {
    val direction = if (offset < 0) -1 else 1
    val distance = kotlin.math.abs(offset)
    var current = index
    var traversed = 0f
    while (current + direction in heights.indices) {
        val next = current + direction
        // The lifted tile crosses the midpoint of each neighbouring slot.
        // This also allows a tall card to pass a shorter card using its top handle.
        if (distance < traversed + gap + heights[next] / 2f) break
        traversed += gap + heights[next]
        current = next
    }
    return current
}

@Composable
fun rememberTrainPilotReorderState(): TrainPilotReorderState = remember { TrainPilotReorderState() }

/** Applied to each reorderable row: measures it, lifts the dragged row and slides the others aside. */
fun Modifier.trainPilotReorderItem(state: TrainPilotReorderState, key: Any, index: Int, layerWhenIdle: Boolean = true): Modifier =
    composed {
        val shift by animateFloatAsState(
            targetValue = state.displacementFor(index),
            animationSpec = tween(durationMillis = 150),
            label = "reorderShift"
        )
        this
            .onSizeChanged { state.heights[key] = it.height }
            .then(
                if (state.draggingKey == key) Modifier.zIndex(1f).graphicsLayer {
                    translationY = state.offsetY
                    shadowElevation = 18f
                    scaleX = 1.02f
                    scaleY = 1.02f
                } else if (layerWhenIdle || state.draggingKey != null) Modifier.graphicsLayer {
                    translationY = if (state.draggingKey == null) 0f else shift
                } else Modifier
            )
    }

/**
 * The only reorder control of a row (no up/down arrows): a long press on the handle lifts the row,
 * it follows the finger, and the new position is saved once on release. The handle consumes its
 * own touches, so a long press here never opens the row's long-press menu, and dragging never
 * scrolls the page. TalkBack users get "move up / move down" actions instead of dragging.
 */
@Composable
fun TrainPilotReorderHandle(
    state: TrainPilotReorderState,
    key: Any,
    index: Int,
    lastIndex: Int,
    gap: Dp,
    label: String,
    moveUpLabel: String,
    moveDownLabel: String,
    testTag: String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.DragHandle,
    immediateDrag: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val haptics = LocalHapticFeedback.current
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    val currentIndex by rememberUpdatedState(index)
    val currentLast by rememberUpdatedState(lastIndex)
    val currentMove by rememberUpdatedState(onMove)
    var rootY by remember { mutableFloatStateOf(0f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 32.dp, height = 48.dp)
            .then(if (immediateDrag) Modifier.onGloballyPositioned { rootY = it.positionInRoot().y } else Modifier)
            .pointerInput(key, immediateDrag) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val start = if (immediateDrag) down else
                        awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    if (!immediateDrag) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.offsetY = 0f
                    state.draggingIndex = currentIndex
                    state.draggingLastIndex = currentLast
                    state.draggingStepPx = (state.heights[key] ?: 0) + gapPx
                    state.pointerY = rootY + start.position.y
                    state.draggingKey = key
                    start.consume()
                    try {
                        val completed = drag(start.id) { change ->
                            val delta = change.positionChange().y
                            state.offsetY += delta
                            state.pointerY += delta
                            change.consume()
                        }
                        val target = state.dropTarget(currentIndex, currentLast, state.draggingStepPx)
                        if (completed && target != currentIndex) currentMove(currentIndex, target)
                    } finally {
                        state.draggingKey = null
                        state.draggingIndex = -1
                        state.offsetY = 0f
                    }
                }
            }
            .semantics {
                contentDescription = label
                customActions = listOfNotNull(
                    if (index > 0) CustomAccessibilityAction(moveUpLabel) { onMove(index, index - 1); true } else null,
                    if (index < lastIndex) CustomAccessibilityAction(moveDownLabel) { onMove(index, index + 1); true } else null
                )
            }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (state.draggingKey == key) palette.primary else palette.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
