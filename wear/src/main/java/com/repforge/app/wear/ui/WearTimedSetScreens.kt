package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlin.math.ceil
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
internal fun TimedSetScreen(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val prefix = "${workout.workoutId}:${workout.exercise?.id}:${set.number}"
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by
        rememberSaveable(prefix) {
            mutableStateOf(
                WearStopwatchStore.currentField(context, prefix)?.takeIf {
                    it == "leftSeconds" || it == "rightSeconds"
                }
                    ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds"
                    else "leftSeconds"
            )
        }
    val field = if (perSide) side else "reps"
    val key = "$prefix:$field"
    var timer by remember(key) { mutableStateOf(WearStopwatchStore.load(context, key)) }
    var elapsed by remember(key) { mutableLongStateOf(timer.elapsed()) }
    LaunchedEffect(key, timer) {
        while (true) {
            val now = timer.elapsed()
            if (now / 1000 != elapsed / 1000) elapsed = now
            delay(200)
        }
    }
    WorkoutPanel(
        workout,
        onMenu,
        actions = { layout ->
            WorkoutControls(
                workout,
                restRemaining,
                recordingEnabled =
                    !set.done && (timer.startedAt > 0 || elapsed > 0 || fieldValue(set, field) > 0),
                recordLabel = wearText("Mentés"),
                onRecord = {
                    val measured = timer.elapsed()
                    val seconds =
                        if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
                    onChange(field, seconds - fieldValue(set, field))
                    timer = WearStopwatchStore.save(context, WearStopwatch(key))
                    elapsed = 0L
                    if (
                        !perSide ||
                            (side == "leftSeconds" && set.rightSeconds > 0) ||
                            (side == "rightSeconds" && set.leftSeconds > 0)
                    )
                        onComplete()
                    else {
                        side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                        WearStopwatchStore.save(context, WearStopwatch("$prefix:$side"))
                    }
                },
                onPrevious = onPrevious,
                onNext = onNext,
                onSkipRest = onSkipRest,
                onFinish = onFinish,
                layout = layout,
            )
        },
    ) {
        run {
            SetHeading(
                workout,
                restRemaining,
                detail =
                    if (perSide)
                        wearFormat("%s · %d/%d", wearText(if (side == "leftSeconds") "Bal oldal" else "Jobb oldal"), workout.currentSetIndex + 1, workout.exercise?.sets?.size ?: 0)
                    else null,
            )
            LiveWorkoutHealth(workout, health)
        }
        val displayed =
            if (set.done || (timer.startedAt == 0L && elapsed == 0L)) fieldValue(set, field).toInt()
            else (elapsed / 1000).toInt()
        run {
            Box(
                Modifier.fillMaxWidth()
                    .heightIn(min = 34.dp)
                    .background(Card, RoundedCornerShape(10.dp))
                    .clickable(enabled = restRemaining == 0 && !set.done, role = Role.Button) {
                        timer = WearStopwatchStore.save(context, timer.toggle())
                    }
                    .semantics {
                        contentDescription =
                            if (timer.startedAt > 0) wearText("Stopper szüneteltetése")
                            else wearText("Stopper indítása")
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${if (set.done) "✓" else if (timer.startedAt > 0 || restRemaining > 0) "Ⅱ" else "▶"} ${formatSeconds(displayed)}",
                    color = White,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun TimedSetToolsScreen(
    workout: WearWorkout,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by
        rememberSaveable(workout.workoutId, workout.currentExercise, set.number) {
            mutableStateOf(
                WearStopwatchStore.currentField(
                        context,
                        "${workout.workoutId}:${workout.exercise?.id}:${set.number}",
                    )
                    ?.takeIf { it == "leftSeconds" || it == "rightSeconds" }
                    ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds"
                    else "leftSeconds"
            )
        }
    val field = if (perSide) side else "reps"
    val key = "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"
    var timer by remember(key) { mutableStateOf(WearStopwatchStore.load(context, key)) }
    var elapsed by remember(key) { mutableLongStateOf(timer.elapsed()) }
    LaunchedEffect(key, timer) {
        while (true) {
            val now = timer.elapsed()
            if (now / 1000 != elapsed / 1000) elapsed = now
            delay(200)
        }
    }
    fun recordTime() {
        val measured = timer.elapsed()
        val seconds = if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
        onChange(field, seconds - fieldValue(workout.currentSet ?: set, field))
        timer = WearStopwatchStore.save(context, WearStopwatch(key))
        if (
            !perSide ||
                (side == "leftSeconds" && set.rightSeconds > 0) ||
                (side == "rightSeconds" && set.leftSeconds > 0)
        )
            onComplete()
        else {
            side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
            WearStopwatchStore.save(
                context,
                WearStopwatch("${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"),
            )
        }
        onBack()
    }
    RoundList(workout.exercise?.name.orEmpty()) {
        centerItem {
            Label(
                wearFormat("Sorozat %d/%d", workout.currentSetIndex + 1, workout.exercise?.sets?.size ?: 0),
                Gold,
            )
        }
        if (perSide)
            centerItem {
                Label(
                    if (side == "leftSeconds") wearFormat("Bal oldal · jobb: %s mp", set.rightSeconds)
                    else wearFormat("Jobb oldal · bal: %s mp", set.leftSeconds)
                )
            }
        centerItem {
            Text(
                formatSeconds((elapsed / 1000).toInt()),
                color = White,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        centerItem {
            Label(
                workout.exercise?.targetReps?.let { if (it.isBlank()) wearText("Stopper") else wearFormat("Cél: %s mp", it) }
                    ?: wearText("Stopper")
            )
        }
        centerItem {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundControl(
                    if (timer.startedAt > 0) "Ⅱ" else "▶",
                    enabled = !set.done,
                    description =
                        if (timer.startedAt > 0) wearText("Stopper szüneteltetése") else wearText("Stopper indítása"),
                ) {
                    timer = WearStopwatchStore.save(context, timer.toggle())
                }
                RoundControl(
                    if (perSide) "B/J" else "↺",
                    enabled = !set.done,
                    description = if (perSide) wearText("Bal és jobb oldal váltása") else wearText("Stopper nullázása"),
                ) {
                    if (perSide) {
                        val measured = timer.elapsed()
                        if (measured > 0)
                            onChange(
                                field,
                                ceil(measured / 1000.0) -
                                    fieldValue(workout.currentSet ?: set, field),
                            )
                        timer = WearStopwatchStore.save(context, WearStopwatch(key))
                        side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                        WearStopwatchStore.save(
                            context,
                            WearStopwatch(
                                "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"
                            ),
                        )
                    } else timer = WearStopwatchStore.save(context, WearStopwatch(key))
                }
            }
        }

        centerItem {
            Pill(
                if (perSide) wearText(if (side == "leftSeconds") "✓ Bal idő rögzítése" else "✓ Jobb idő rögzítése")
                else wearText("✓ Idő rögzítése"),
                enabled =
                    !set.done &&
                        (timer.startedAt > 0 ||
                            timer.elapsedMillis > 0 ||
                            elapsed > 0 ||
                            fieldValue(set, field) > 0),
                size = 12,
                onClick = ::recordTime,
            )
        }

        centerItem {
            Pill(wearText("± Kézi időbeállítás"), tone = Secondary, enabled = !set.done, size = 11) {
                timer =
                    WearStopwatchStore.save(
                        context,
                        timer.copy(elapsedMillis = timer.elapsed(), startedAt = 0L),
                    )
                onEdit(field)
            }
        }
        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}
