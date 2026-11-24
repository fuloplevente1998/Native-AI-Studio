package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal fun wearWorkoutGesturePage(page: Int, dx: Float, threshold: Float): Int? = when {
    page == 0 && dx >= threshold -> 1
    page == 1 && dx <= -threshold -> 0
    else -> null
}

/** Observe only unhandled free-space taps; a child click or scroll always wins. */
@Composable
internal fun wearWorkoutGesture(
    enabled: Boolean,
    page: Int,
    onTap: () -> Unit,
    onPage: (Int) -> Unit,
): Modifier {
    val tap = rememberUpdatedState(onTap)
    val navigate = rememberUpdatedState(onPage)
    if (!enabled || page !in 0..1) return Modifier
    return Modifier.pointerInput(page) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            if (!wearMainSwipeCanStart(down.position.x, size.width.toFloat(), 24.dp.toPx())) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            var dx = 0f
            var dy = 0f
            var swiping = false
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (event.changes.count { it.pressed } > 1 || change.isConsumed) return@awaitEachGesture
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                if (!swiping && kotlin.math.abs(dy) > slop && kotlin.math.abs(dy) >= kotlin.math.abs(dx)) return@awaitEachGesture
                if (!swiping && kotlin.math.abs(dx) > slop) {
                    if (wearWorkoutGesturePage(page, dx, slop) == null) return@awaitEachGesture
                    swiping = true
                }
                if (swiping) change.consume()
                if (!change.pressed) {
                    if (swiping) wearWorkoutGesturePage(page, dx, 36.dp.toPx())?.let { navigate.value(it) }
                    else if (page == 0 && kotlin.math.abs(dx) <= slop && kotlin.math.abs(dy) <= slop &&
                        change.uptimeMillis - down.uptimeMillis <= 500L) {
                        change.consume()
                        tap.value()
                    }
                    break
                }
            } while (true)
        }
    }
}

@Composable
internal fun WearWorkoutControlOverlay(
    workout: WearWorkout,
    timerPause: (() -> Unit)?,
    workoutPause: (() -> Unit)? = null,
    onPhoneControl: (() -> Unit)? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalWearHomeTheme.current
    val dismiss by rememberUpdatedState(onDismiss)
    var pressed by remember { mutableStateOf(false) }
    var activity by remember { mutableIntStateOf(0) }
    LaunchedEffect(pressed, activity) {
        if (!pressed) { delay(3000L); dismiss() }
    }
    val blocked = WearUndo188.blocked(workout) || WearOwnership193.pending(workout)
    Box(
        Modifier.fillMaxSize().background(theme.backgroundColor.copy(alpha = .96f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        pressed = event.changes.any { it.pressed }
                        activity++
                    }
                }
            }
            .clickable(onClick = onDismiss).testTag("wear-workout-controls-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Label(workout.exercise?.name.orEmpty(), size = 11)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                RoundControl("‹", enabled = !blocked && workout.currentExercise > 0, description = wearText("Előző gyakorlat"), onClick = onPrevious)
                RoundControl(if (workoutPause == null && timerPause == null && onPhoneControl != null) "↗" else if (workoutPause == null && workout.automation?.paused == true) "▶" else "Ⅱ",
                    enabled = !blocked && (workoutPause != null || timerPause != null || onPhoneControl != null),
                    description = when {
                        workoutPause != null -> wearV2Text("pauseWorkout")
                        timerPause != null && workout.automation?.paused == true -> wearText("Automata folytatása")
                        timerPause != null -> wearText("Automata szüneteltetése")
                        onPhoneControl != null -> wearV2Text("phoneControl")
                        else -> wearV2Text("pauseWorkout")
                    },
                    onClick = { (workoutPause ?: timerPause ?: onPhoneControl)?.invoke(); onDismiss() })
                RoundControl("›", enabled = !blocked && workout.currentExercise < workout.exercises.lastIndex,
                    description = wearText("Következő gyakorlat"), onClick = onNext)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                RoundControl("■", Gold, enabled = workout.completedSets > 0 && !blocked,
                    description = wearText("Edzés befejezése"), onClick = onFinish)
                RoundControl("×", Danger, enabled = !blocked, description = WearMenu216.text("discard"), onClick = onDiscard)
                RoundControl("···", description = wearText("Edzésmenü"), onClick = onMore)
            }
            Label("3 s", size = 10)
        }
    }
}
