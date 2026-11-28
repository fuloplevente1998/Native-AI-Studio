package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/** Shared underlap geometry, independent of the Android-only optical backend.
 * The caller supplies the capture and bar materials; both bars are measured before the list.
 */
@Composable
internal fun MeasuredScrollChrome(
    modifier: Modifier = Modifier,
    rootInset: Dp,
    capture: Modifier = Modifier,
    panel: Modifier? = null,
    wrapBody: Boolean = false,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier, Dp, Dp) -> Unit
) {
    SubcomposeLayout(modifier) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val barConstraints = Constraints(minWidth = width, maxWidth = width, maxHeight = height)
        val inset = rootInset.roundToPx().coerceIn(0, height)
        val upper = subcompose("top") {
            // A painted but non-clickable header must not forward taps to an underlying row.
            Box(Modifier.pointerInput(Unit) {}) { top?.invoke() }
        }.single().measure(barConstraints.copy(maxHeight = height - inset))
        val lower = subcompose("bottom") {
            Box(Modifier.pointerInput(Unit) {}) { bottom?.invoke() }
        }.single().measure(barConstraints.copy(maxHeight = (height - inset - upper.height).coerceAtLeast(0)))
        val body = subcompose("body") {
            // The initial and terminal safe areas live INSIDE the scrolling viewport. The
            // measured heights are available in the same layout pass, including large font/rest dock.
            val bodySize = if (wrapBody) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
            Box(bodySize.then(capture)) {
                content(bodySize, (inset + upper.height).toDp(), lower.height.toDp())
            }
        }.single().measure(if (wrapBody) Constraints(minWidth = width, maxWidth = width, maxHeight = height)
            else Constraints.fixed(width, height))
        val panelHeight = (maxOf(body.height, inset + upper.height) - inset - lower.height).coerceIn(0, height - inset)
        val panelSurface = panel?.let {
            subcompose("panel") { Box(it) }.single()
                .measure(Constraints.fixed(width, if (wrapBody) panelHeight else height - inset - lower.height))
        }
        layout(width, height) {
            panelSurface?.placeRelative(0, inset)
            body.placeRelative(0, 0)
            upper.placeRelative(0, inset)
            lower.placeRelative(0, height - lower.height)
        }
    }
}
