package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.StudioWorkoutSessionPresentation
import kotlinx.coroutines.delay
import java.time.Instant

private fun feedbackWord(state: TrainPilotUiState, hu: String, en: String, de: String, ro: String) =
    when (state.language.name) { "EN" -> en; "DE" -> de; "RO" -> ro; else -> hu }

@Composable
internal fun StudioActiveWorkoutSessionInfo(state: TrainPilotUiState) {
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val current = frame?.takeIf { it.optBoolean("active") && it.optString("workoutId") == state.activeWorkoutId &&
        it.optLong("generation") == state.activeWorkoutGeneration }
    val snapshot = current ?: return
    val rawDay = StudioWorkoutSessionPresentation.dayLabel(snapshot)
        ?: state.activeWorkoutTitleHu.substringAfter("•", "").trim().takeIf { it.isNotBlank() }
        ?: return
    val formattedDay = if (rawDay.length == 1 && rawDay[0].isLetter()) {
        feedbackWord(state, "${rawDay.uppercase()} nap", "Day ${rawDay.uppercase()}", "Tag ${rawDay.uppercase()}", "Ziua ${rawDay.uppercase()}")
    } else rawDay
    val palette = LocalTrainPilotPalette.current
    Box(modifier = Modifier.testTag("active_workout_day_between_header_and_exercises")) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = palette.tintedSurface(0.22f),
            border = com.repforge.app.ui.theme.TrainPilotFrames.stroke(1.5.dp, palette.primary),
            modifier = Modifier.testTag("active_workout_day")
        ) {
            Text(
                text = formattedDay,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = palette.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}


@Composable
internal fun StudioActiveWorkoutHeaderSequence(
    state: TrainPilotUiState,
    header: @Composable () -> Unit,
    exerciseSequence: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("active_workout_header_sequence"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.testTag("active_workout_large_header")) { header() }
        StudioActiveWorkoutSessionInfo(state)
        Box(Modifier.testTag("active_workout_exercise_sequence_slot")) { exerciseSequence() }
    }
}


@Composable
internal fun StudioActiveExerciseFeedback(state: TrainPilotUiState,
    onEffort: (String, Int) -> Unit, onNote: (String, String) -> Unit) {
    val exercise = state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex) ?: return
    val same = state.exerciseFeedbackWorkoutId == state.activeWorkoutId
    ExerciseFeedbackChips(
        effort = if (same) StudioWorkoutFeedback.efforts.getOrNull(state.exerciseFeedbackChoices[exercise.id] ?: -1) else null,
        note = if (same) state.exerciseFeedbackNotes[exercise.id].orEmpty() else "",
        language = state.language.name.lowercase(),
        title = feedbackWord(state, "Milyen volt?", "How was it?", "Wie war es?", "Cum a fost?") + " · " + exercise.localizedName(state.language),
        onEffort = { onEffort(exercise.id, StudioWorkoutFeedback.efforts.indexOf(it)) },
        onNote = { onNote(exercise.id, it) }, enabled = state.isWorkoutSessionActive && !state.isSavingWorkout,
        tag = "active_feedback_${state.activeWorkoutId}_${exercise.id}")
}

/** The dialog belongs to this exact session/selection; a remote selection dismisses it. */
@Composable
internal fun rememberExerciseFeedbackNext(state: TrainPilotUiState, onNext: () -> Unit,
    onEffort: (String, Int) -> Unit, onNote: (String, String) -> Unit): () -> Unit {
    val exerciseId = state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex)?.id
    var pending by remember(state.activeWorkoutId, state.activeWorkoutGeneration, exerciseId,
        state.currentSubScreen, state.isWorkoutSessionActive) { mutableStateOf(false) }
    if (pending) TrainPilotAlertDialog(onDismissRequest = { pending = false },
        modifier = Modifier.testTag("active_feedback_prompt"),
        title = { Text(feedbackWord(state, "Gyakorlat visszajelzés", "Exercise feedback", "Übungsfeedback", "Feedback exercițiu")) },
        text = { StudioActiveExerciseFeedback(state, onEffort, onNote) },
        confirmButton = { TextButton(onClick = { if (pending) { pending = false; onNext() } },
            enabled = !state.isSavingWorkout, modifier = Modifier.testTag("active_feedback_next")) {
            Text(feedbackWord(state, "Tovább", "Continue", "Weiter", "Continuă"))
        } },
        dismissButton = { TextButton(onClick = { if (pending) { pending = false; onNext() } },
            enabled = !state.isSavingWorkout, modifier = Modifier.testTag("active_feedback_skip")) {
            Text(feedbackWord(state, "Kihagyás", "Skip", "Überspringen", "Omite"))
        } })
    return {
        if (state.isWorkoutSessionActive && !state.isSavingWorkout && exerciseId != null) {
            val answered = state.exerciseFeedbackWorkoutId == state.activeWorkoutId &&
                (StudioWorkoutFeedback.efforts.getOrNull(state.exerciseFeedbackChoices[exerciseId] ?: -1) != null ||
                    state.exerciseFeedbackNotes[exerciseId].orEmpty().isNotBlank())
            if (answered) onNext() else pending = true
        }
    }
}
