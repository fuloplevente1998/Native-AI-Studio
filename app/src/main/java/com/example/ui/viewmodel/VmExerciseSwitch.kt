package com.example.ui.viewmodel

import com.example.model.ExerciseItem
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.repforge.app.NativeWorkoutSession
import com.example.ui.viewmodel.TrainPilotViewModel.ExerciseSelection

internal fun TrainPilotViewModel.currentExerciseSelection(): ExerciseSelection {
    val ctx = requireNotNull(nativeContext)
    val frame = com.repforge.app.NativeWorkoutEditing.project(ctx)
    check(frame.optString("workoutId") == _uiState.value.activeWorkoutId) { "Az edzés időközben megváltozott." }
    val exercise = frame.getJSONArray("exercises").getJSONObject(frame.getInt("currentExercise"))
    return ExerciseSelection(com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
        exercise.getString("id"), exercise.getJSONArray("sets").getJSONObject(0).getString("setId")),
        com.repforge.app.NativeDataReplacement.continuationForCurrentData(ctx))
}

fun TrainPilotViewModel.openActiveExerciseLibrary() {
    try {
        exerciseSelection = currentExerciseSelection()
        navigateToSubScreen(SubScreen.EXERCISE_LIBRARY)
        if (_uiState.value.currentSubScreen != SubScreen.EXERCISE_LIBRARY) exerciseSelection = null
    } catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) } }
}

fun TrainPilotViewModel.selectLibraryExercise(exercise: ExerciseItem) {
    val selection = exerciseSelection
    if (selection != null) {
        exerciseSelection = null
        switchCurrentActiveExercise(exercise, selection, resume = true)
    } else if (exercise.isTimed || exercise.isBilateral) startQuickTimedWithExercise(exercise)
    else startQuickStrengthWithExercise(exercise)
}

fun TrainPilotViewModel.switchCurrentActiveExercise(exercise: ExerciseItem) {
    if (!canEditActiveSets()) return
    try {
        val state = _uiState.value
        if (state.activeWorkoutExercises.any { it.id == exercise.id }) {
            // A queued pill is navigation, not an asynchronous library replacement.
            // Commit immediately: a previous library/presentation job must not drop a tap.
            val context = requireNotNull(nativeContext)
            val frame = com.repforge.app.NativeWorkoutEditing.project(context)
            check(frame.optString("workoutId") == state.activeWorkoutId &&
                frame.optLong("generation") == state.activeWorkoutGeneration) { "Az edzés időközben megváltozott." }
            val rows = frame.getJSONArray("exercises")
            val selected = (0 until rows.length()).map { rows.getJSONObject(it) }
                .single { it.optString("id") == exercise.id }
            com.repforge.app.NativeWorkoutEditing.selectExercise(context,
                com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
                    exercise.id, selected.getJSONArray("sets").getJSONObject(0).getString("setId")))
            // The existing snapshot observer projects the committed selection and mode.
            return
        }
        switchCurrentActiveExercise(exercise, currentExerciseSelection())
    }
    catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) } }
}

internal fun TrainPilotViewModel.switchCurrentActiveExercise(exercise: ExerciseItem, selection: ExerciseSelection, resume: Boolean = false) {
    if (exerciseSelectionJob?.isActive == true) return
    val ctx = nativeContext ?: return
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    exerciseSelectionJob = scope.launch(Dispatchers.Main.immediate) {
        try {
            selection.data.requireCurrent(ctx)
            val replacement = withContext(Dispatchers.IO) {
                val source = requireNotNull(db.exerciseLibraryDao().getExerciseById(exercise.id)) { "A gyakorlat már nem található." }
                com.repforge.app.NativeWorkoutSession.buildExercise(db, source, requireNotNull(nativeSettingsStore).current())
            }
            selection.data.requireCurrent(ctx)
            var saved = com.repforge.app.StudioWorkoutBridge.switchExercise(ctx, selection.target, replacement)
            // The library's explicit Start action resumes this exact edited session.
            if (resume && saved.optBoolean("workoutPaused")) saved = com.repforge.app.StudioWorkoutBridge.pause(ctx, false)
            presentCanonicalWorkout(saved)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message ?: "A gyakorlat nem cserélhető.") } }
    }
}

fun TrainPilotViewModel.nextActiveExercise() = stepActiveExercise(1)

fun TrainPilotViewModel.previousActiveExercise() = stepActiveExercise(-1)

/** Navigation selects an existing durable row; it never rebuilds a workout from UI defaults. */
internal fun TrainPilotViewModel.stepActiveExercise(offset: Int) {
    if (!canEditActiveSets() || _uiState.value.activeWorkoutExercises.isEmpty()) return
    val context = nativeContext ?: return
    try {
        val state = _uiState.value
        val frame = com.repforge.app.NativeWorkoutEditing.project(context)
        check(frame.getString("workoutId") == state.activeWorkoutId &&
            frame.getLong("generation") == state.activeWorkoutGeneration) { "Az edzés időközben megváltozott." }
        val rows = frame.getJSONArray("exercises")
        val currentIndex = frame.getInt("currentExercise")
        check(currentIndex in 0 until rows.length()) { "Az aktuális gyakorlat nem található." }
        val targetIndex = currentIndex + offset
        if (targetIndex !in 0 until rows.length()) {
            // The common navigation path pauses the native measurement before showing its summary.
            if (offset > 0) navigateToSubScreen(SubScreen.WORKOUT_SUMMARY) else navigateBack()
            return
        }
        val exercise = rows.getJSONObject(targetIndex)
        val setId = exercise.getJSONArray("sets").getJSONObject(0).getString("setId")
        com.repforge.app.NativeWorkoutEditing.selectExercise(context,
            com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
                exercise.getString("id"), setId))
        // snapshotFlow projects the committed selection, preserving all recorded values and IDs.
    } catch (error: Exception) {
        _uiState.update { it.copy(statusBannerMessage = error.message ?: tr(
            "A gyakorlat nem választható ki.", "Could not select the exercise.",
            "Die Übung konnte nicht ausgewählt werden.", "Exercițiul nu a putut fi selectat.")) }
    }
}

fun TrainPilotViewModel.updateOutdoorWorkoutMetrics(elapsedLabel: String, distanceKm: Float, paceLabel: String) {
    _uiState.update {
        it.copy(
            outdoorElapsedLabel = elapsedLabel,
            outdoorDistanceKm = distanceKm,
            outdoorPaceLabel = paceLabel
        )
    }
}
