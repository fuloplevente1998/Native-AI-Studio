package com.example.ui.theme

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** Visual displacement only. The existing click and scroll handlers keep ownership of input. */
@Stable
internal class GlassControlDrag {
    var offset by mutableStateOf(Offset.Zero)
        private set
    var held by mutableStateOf(false)
        private set

    fun observe(): Modifier = Modifier.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            offset = Offset.Zero
            held = true
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed ||
                        (change.isConsumed && change.position != change.previousPosition)) break
                    offset = change.position - down.position
                }
            } finally {
                held = false
            }
        }
    }
}

/** Bound the travel even if a drag continues beyond the control. */
internal fun glassDragTransform(offset: Offset, limit: Float, progress: Float): Offset =
    Offset(
        (offset.x * .22f).coerceIn(-limit, limit) * progress,
        (offset.y * .22f).coerceIn(-limit, limit) * progress
    )

internal fun glassDragStretch(translation: Float, size: Float): Float =
    (abs(translation) / size.coerceAtLeast(1f)).coerceAtMost(.065f)
