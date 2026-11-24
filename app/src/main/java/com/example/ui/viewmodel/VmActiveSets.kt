package com.example.ui.viewmodel

import com.example.model.ExerciseSetItem
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.repforge.app.ActiveWorkoutStore
import java.util.UUID

// Reject mirror/recovery callbacks before changing the local draft. The native
// commit still rechecks ownership/revision if a handoff races this admission.
internal fun TrainPilotViewModel.canEditActiveSets(): Boolean {
    val context = nativeContext ?: return false
    val state = _uiState.value
    if (!state.isWorkoutSessionActive || state.isSavingWorkout ||
        !state.phoneControlsWorkout || state.watchControlPending) return false
    return runCatching {
        val current = ActiveWorkoutStore.current(context) ?: return@runCatching false
        if (!current.optBoolean("active") || current.optBoolean("discardPending") ||
            current.optBoolean("finalizationPending") ||
            current.optString("workoutId") != state.activeWorkoutId ||
            current.optLong("generation") != state.activeWorkoutGeneration) return@runCatching false
        com.repforge.app.NativeWorkoutEditing.canEdit(context, state.activeWorkoutId, state.activeWorkoutGeneration)
    }.getOrDefault(false)
}

// Active Workout Actions (persisted canonically to ActiveWorkoutStore)
fun TrainPilotViewModel.toggleSetCompleted(setNumber: Int) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        var newlyCompleted = false
        val updated = state.activeSets.map { s ->
            if (s.setNumber == setNumber) {
                val nextCompleted = !s.completed
                if (nextCompleted && !s.completed) newlyCompleted = true
                s.copy(completed = nextCompleted)
            } else s
        }
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updated)
        } else state.activeSetsByExerciseId
        val base = state.copy(
            activeSets = updated,
            activeSetsByExerciseId = updatedMap
        )
        if (newlyCompleted) {
            com.repforge.app.StudioRestTimer.start(base, state.defaultRestSec)
        } else {
            base // Undoing completion must preserve the canonical rest deadline/state.
        }
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.adjustSetValues(setNumber: Int, deltaWeight: Float, deltaReps: Int, deltaRir: Int) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val updated = state.activeSets.map { s ->
            if (s.setNumber == setNumber) {
                s.copy(
                    weightKg = (s.weightKg + deltaWeight).coerceAtLeast(0f),
                    reps = (s.reps + deltaReps).coerceIn(1, 50),
                    rir = (s.rir + deltaRir).coerceIn(0, 10),
                    actualRirProvided = s.actualRirProvided || deltaRir != 0
                )
            } else s
        }
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updated)
        } else state.activeSetsByExerciseId
        state.copy(
            activeSets = updated,
            activeSetsByExerciseId = updatedMap
        )
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.updateSetValuesExact(setNumber: Int, weightKg: Float, reps: Int, rir: Int?) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val updated = state.activeSets.map { s ->
            if (s.setNumber == setNumber) {
                s.copy(
                    weightKg = weightKg.coerceAtLeast(0f),
                    reps = reps.coerceIn(1, 100),
                    rir = rir?.coerceIn(0, 10) ?: s.rir, actualRirProvided = rir != null
                )
            } else s
        }
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updated)
        } else state.activeSetsByExerciseId
        state.copy(
            activeSets = updated,
            activeSetsByExerciseId = updatedMap
        )
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.updateBilateralSetExact(setNumber: Int, leftSec: Int, rightSec: Int, rir: Int?) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val cleanLeft = leftSec.coerceIn(1, 600)
        val cleanRight = rightSec.coerceIn(1, 600)
        val cleanRir = rir?.coerceIn(0, 10)
        val updated = state.activeSets.map { s ->
            if (s.setNumber == setNumber) {
                s.copy(
                    leftSec = cleanLeft,
                    rightSec = cleanRight,
                    reps = maxOf(cleanLeft, cleanRight),
                    rir = cleanRir ?: s.rir, actualRirProvided = rir != null,
                    leftRecorded = true,
                    rightRecorded = true
                )
            } else s
        }
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updated)
        } else state.activeSetsByExerciseId
        state.copy(
            activeSets = updated,
            activeSetsByExerciseId = updatedMap,
            statusBannerMessage = tr(
                "$setNumber. sorozat frissítve (Bal: ${cleanLeft} mp • Jobb: ${cleanRight} mp)",
                "Set #$setNumber updated (Left: ${cleanLeft} sec • Right: ${cleanRight} sec)",
                "Satz #$setNumber aktualisiert (Links: ${cleanLeft} Sek. • Rechts: ${cleanRight} Sek.)",
                "Seria #$setNumber actualizată (Stânga: ${cleanLeft} sec • Dreapta: ${cleanRight} sec)",
                state.language
            )
        )
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.recordBilateralSideStop(isLeftSide: Boolean, measuredSec: Int, targetSetNumber: Int? = null) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val recordedSec = measuredSec.coerceAtLeast(1)
        val sets = state.activeSets
        val targetSet = when {
            targetSetNumber != null && sets.any { it.setNumber == targetSetNumber } ->
                sets.first { it.setNumber == targetSetNumber }
            isLeftSide ->
                sets.firstOrNull { !it.leftRecorded && !it.completed }
                    ?: sets.firstOrNull { !it.completed }
                    ?: sets.lastOrNull()
            else ->
                sets.firstOrNull { !it.rightRecorded && !it.completed }
                    ?: sets.firstOrNull { !it.completed }
                    ?: sets.lastOrNull()
        } ?: return@update state

        var bothSidesNowDone = false
        val updatedSets = sets.map { s ->
            if (s.setNumber == targetSet.setNumber) {
                val newLeft = if (isLeftSide) recordedSec else s.leftSec
                val newRight = if (!isLeftSide) recordedSec else s.rightSec
                val newLeftRec = if (isLeftSide) true else s.leftRecorded
                val newRightRec = if (!isLeftSide) true else s.rightRecorded
                val nowCompleted = s.completed || (newLeftRec && newRightRec)
                if (nowCompleted && !s.completed) {
                    bothSidesNowDone = true
                }
                s.copy(
                    leftSec = newLeft,
                    rightSec = newRight,
                    reps = maxOf(newLeft, newRight),
                    leftRecorded = newLeftRec,
                    rightRecorded = newRightRec,
                    completed = nowCompleted
                )
            } else s
        }

        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updatedSets)
        } else state.activeSetsByExerciseId

        val base = state.copy(
            activeSets = updatedSets,
            activeSetsByExerciseId = updatedMap,
            bilateralLeftSec = if (isLeftSide) recordedSec else state.bilateralLeftSec,
            bilateralRightSec = if (!isLeftSide) recordedSec else state.bilateralRightSec,
            statusBannerMessage = null
        )
        if (bothSidesNowDone) {
            com.repforge.app.StudioRestTimer.start(base, state.defaultRestSec)
        } else {
            base
        }
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.toggleAutoMode(enabled: Boolean) {
    _uiState.update { it.copy(autoModeEnabled = enabled) }
}

fun TrainPilotViewModel.deleteSet(setNumber: Int) {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val remaining = state.activeSets.filter { it.setNumber != setNumber }
        val renumbered = remaining.mapIndexed { idx, item ->
            item.copy(setNumber = idx + 1)
        }
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to renumbered)
        } else state.activeSetsByExerciseId
        state.copy(
            activeSets = renumbered,
            activeSetsByExerciseId = updatedMap,
            statusBannerMessage = tr(
                "$setNumber. sorozat törölve",
                "Set #$setNumber deleted",
                "Satz #$setNumber gelöscht",
                "Seria #$setNumber ștearsă",
                state.language
            )
        )
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.addSet() {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val lastSet = state.activeSets.lastOrNull()
        val nextNumber = (lastSet?.setNumber ?: 0) + 1
        val newSet = ExerciseSetItem(
            setNumber = nextNumber,
            weightKg = lastSet?.weightKg ?: 5.5f,
            reps = lastSet?.reps ?: 20,
            rir = lastSet?.rir ?: 2,
            completed = false,
            leftSec = lastSet?.leftSec ?: 30,
            rightSec = lastSet?.rightSec ?: 30,
            id = UUID.randomUUID().toString(),
            leftRecorded = false,
            rightRecorded = false
        )
        val updatedSets = state.activeSets + newSet
        val currentExId = state.selectedExerciseId
        val updatedMap = if (currentExId.isNotBlank()) {
            state.activeSetsByExerciseId + (currentExId to updatedSets)
        } else state.activeSetsByExerciseId
        state.copy(
            activeSets = updatedSets,
            activeSetsByExerciseId = updatedMap,
            statusBannerMessage = tr(
                "$nextNumber. sorozat hozzáadva",
                "Set #$nextNumber added",
                "Satz #$nextNumber hinzugefügt",
                "Seria #$nextNumber adăugată",
                state.language
            )
        )
    }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.undoLastCompletedSet() {
    if (!canEditActiveSets()) return
    _uiState.update { state ->
        val lastCompleted = state.activeSets.lastOrNull { it.completed }
        if (lastCompleted == null) state
        else {
            val updated = state.activeSets.map {
                if (it.setNumber == lastCompleted.setNumber) it.copy(completed = false) else it
            }
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updated)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updated,
                activeSetsByExerciseId = updatedMap,
                statusBannerMessage = tr(
                    "${lastCompleted.setNumber}. sorozat visszavonva",
                    "Set #${lastCompleted.setNumber} undone",
                    "Satz #${lastCompleted.setNumber} rückgängig gemacht",
                    "Seria #${lastCompleted.setNumber} anulată",
                    state.language
                )
            )
        }
    }
    persistCanonicalActiveSessionAsync()
}
