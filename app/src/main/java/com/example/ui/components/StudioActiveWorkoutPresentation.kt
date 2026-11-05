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
    var now by remember(state.activeWorkoutId) { mutableStateOf(Instant.now()) }
    val paused = current?.optBoolean("workoutPaused") != false
    LaunchedEffect(state.activeWorkoutId, paused) {
        now = Instant.now()
        while (!paused) { delay(1000); now = Instant.now() }
    }
    val snapshot = current ?: return
    val seconds = StudioWorkoutSessionPresentation.elapsedSeconds(snapshot, now)
    val day = StudioWorkoutSessionPresentation.dayLabel(snapshot)
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (day != null) Text(day, Modifier.weight(1f).testTag("active_workout_day"),
            style = MaterialTheme.typography.labelMedium, color = palette.primary)
        else Spacer(Modifier.weight(1f))
        Text((seconds?.let(StudioWorkoutSessionPresentation::durationLabel) ?: "—") +
            if (paused) " · " + feedbackWord(state, "Szünet", "Paused", "Pause", "Pauză") else "",
            Modifier.testTag("active_workout_elapsed"), style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary)
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
