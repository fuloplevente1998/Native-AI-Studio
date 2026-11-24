package com.repforge.app.wear

import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal val wearMainRoutes = listOf("home", "journal", "metrics", "calendar")

internal fun wearAdjacentMainRoute(route: String?, direction: Int): String? {
    val index = wearMainRoutes.indexOf(route)
    if (index < 0 || direction !in listOf(-1, 1)) return null
    return wearMainRoutes.getOrNull(index + direction)
}

internal fun wearMainSwipeCanStart(x: Float, width: Float, edge: Float): Boolean =
    width > 2 * edge && x >= edge && x <= width - edge

@Composable
internal fun WearMainNavigation(
    route: String?,
    onPage: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val index = wearMainRoutes.indexOf(route)
    val currentOnPage = rememberUpdatedState(onPage)
    val theme = LocalWearHomeTheme.current
    val context = LocalContext.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val focus = remember { FocusRequester() }
    val rotary = remember(route) { WearRotaryEditor212() }
    val pixelsPerDetent = remember(context) { ViewConfiguration.get(context).scaledVerticalScrollFactor }
    val rotaryModifier = if (index < 0) Modifier else Modifier
        .focusRequester(focus)
        .onPreRotaryScrollEvent { event ->
            if (!windowFocused) false
            else {
                val steps = rotary.steps(event.verticalScrollPixels, pixelsPerDetent)
                if (steps != 0) wearAdjacentMainRoute(route, if (steps > 0) 1 else -1)?.let { currentOnPage.value(it) }
                true
            }
        }.focusable()
    LaunchedEffect(route, windowFocused) {
        rotary.reset()
        if (index >= 0 && windowFocused) focus.requestFocus()
    }
    val gesture = if (index < 0) Modifier else Modifier.pointerInput(route) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Leave both physical rim edges to the system. Do not consume taps or vertical drags.
            if (!wearMainSwipeCanStart(down.position.x, size.width.toFloat(), 24.dp.toPx()))
                return@awaitEachGesture
            var distance = 0f
            val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                val direction = if (over < 0) 1 else -1
                if (wearAdjacentMainRoute(route, direction) != null) {
                    distance = over
                    change.consume()
                }
            } ?: return@awaitEachGesture
            val completed = horizontalDrag(slop.id) { change ->
                distance += change.positionChange().x
                change.consume()
            }
            if (completed && kotlin.math.abs(distance) >= 36.dp.toPx()) {
                wearAdjacentMainRoute(route, if (distance < 0) 1 else -1)
                    ?.let { currentOnPage.value(it) }
            }
        }
    }
    Box(Modifier.fillMaxSize().background(theme.backgroundColor).then(rotaryModifier).then(gesture)) {
        Box(Modifier.fillMaxSize().padding(bottom = if (index >= 0) 10.dp else 0.dp)) {
            content()
        }
        if (index >= 0) {
            val pageNames = listOf("home", "journal", "metrics", "calendar")
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                    .testTag("wear-main-page-indicator")
                    .semantics { contentDescription = "${WearMenu216.text(pageNames[index])} · ${index + 1}/4" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(wearMainRoutes.size) { page ->
                    Box(Modifier.width(if (page == index) 12.dp else 5.dp).height(5.dp).background(
                        if (page == index) theme.accentColor else theme.outlineColor,
                        CircleShape,
                    ))
                }
            }
        }
    }
}
