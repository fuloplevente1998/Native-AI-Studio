package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            Box { top?.invoke() }
        }.single().measure(barConstraints.copy(maxHeight = height - inset))
        val lower = subcompose("bottom") {
            Box { bottom?.invoke() }
        }.single().measure(barConstraints.copy(maxHeight = (height - inset - upper.height).coerceAtLeast(0)))
        val body = subcompose("body") {
            // The initial and terminal safe areas live INSIDE the scrolling viewport. The
            // measured heights are available in the same layout pass, including large font/rest dock.
            Box(Modifier.fillMaxSize().then(capture)) {
                content(Modifier.fillMaxSize(), (inset + upper.height).toDp(), lower.height.toDp())
            }
        }.single().measure(Constraints.fixed(width, height))
        layout(width, height) {
            body.placeRelative(0, 0)
            upper.placeRelative(0, inset)
            lower.placeRelative(0, height - lower.height)
        }
    }
}
