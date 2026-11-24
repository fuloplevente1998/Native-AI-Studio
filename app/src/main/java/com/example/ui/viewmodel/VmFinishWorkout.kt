package com.example.ui.viewmodel

import com.example.model.ExerciseSetItem
import com.example.model.LoggedWorkoutEntry
import com.example.model.workoutsInCalendarWeekToDate
import com.example.model.workoutsInPeriodToDate
import com.example.model.progressPeriodDays
import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.WearSyncRepository
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.repforge.app.TrainPilotWorkoutFinalization
import kotlin.math.roundToInt

fun TrainPilotViewModel.finishAndSaveWorkout() {
    val state = _uiState.value
    if (state.isSavingWorkout) return
    val ctx = nativeContext
    val db = nativeDatabase
    val scope = nativeScope
    if (ctx == null || db == null || scope == null) {
        _uiState.update { it.copy(statusBannerMessage = "A natív edzésmotor nem érhető el.") }
        return
    }
    _uiState.update { it.copy(isSavingWorkout = true) }
    scope.launch(Dispatchers.IO) {
        try {
            val activeToFinalize = withContext(Dispatchers.Main.immediate) {
                // Never fabricate a GPS set, a start timestamp or a replacement workout.
                if (com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(ActiveWorkoutStore.current(ctx)))
                    com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                val current = com.repforge.app.NativeWorkoutEditing.project(ctx)
                check(current.optBoolean("active") && current.optString("workoutId") == state.activeWorkoutId)
                val exercises = current.getJSONArray("exercises")
                val completed = (0 until exercises.length()).sumOf { index ->
                    val sets = exercises.getJSONObject(index).getJSONArray("sets")
                    (0 until sets.length()).count { sets.getJSONObject(it).optBoolean("done") }
                }
                check(completed > 0 || com.repforge.app.StudioOutdoorFinish.canSaveTimeOnly(current)) {
                    "Nincs hitelesen mért idő vagy rögzített sorozat. A mérés elvethető, de üres edzés nem menthető."
                }
                if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) {
                    requireNotNull(summaryFeedbackTicket).requireCurrent(ctx)
                    com.repforge.app.StudioWorkoutFeedback.chosen(state.workoutSummaryMoodIndex,
                        state.workoutSummaryNote, System.currentTimeMillis())?.let { current.put("feedback", it) }
                }
                if (state.exerciseFeedbackWorkoutId == state.activeWorkoutId &&
                    (state.exerciseFeedbackChoices.isNotEmpty() || state.exerciseFeedbackNotes.isNotEmpty())) {
                    requireNotNull(exerciseFeedbackTicket).requireCurrent(ctx)
                    com.repforge.app.StudioWorkoutFeedback.attachExerciseEdits(current, state.exerciseFeedbackWorkoutId,
                        state.exerciseFeedbackChoices, state.exerciseFeedbackNotes)
                }
                current
            }
            val workoutId = activeToFinalize.getString("workoutId")
            val workoutTitle = activeToFinalize.optString("programName", "Edzés")
            val isOutdoorSession = state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS
            val exercises = activeToFinalize.getJSONArray("exercises")
            val totalCompletedSets = (0 until exercises.length()).sumOf { i ->
                val sets = exercises.getJSONObject(i).getJSONArray("sets")
                (0 until sets.length()).count { sets.getJSONObject(it).optBoolean("done") }
            }
            val saved = TrainPilotWorkoutFinalization(ctx, db).finishWorkout(activeToFinalize)

            // Verify persistence by querying Room directly and build LoggedWorkoutEntry from Room truth
            val finalWorkoutId = saved.history.optString("workoutId", workoutId)
            val savedInDb = db.workoutHistoryDao().getWorkoutWithDetails(finalWorkoutId)
            if (savedInDb != null) {
                saved.closedSnapshot?.let { runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(ctx, it) } }
                runCatching { WearSyncRepository.refreshWatchHomeAsync(ctx) }
                runCatching { WearSyncRepository.publishWatchHomeSnapshot(ctx) }

                val dbW = savedInDb.workout
                val realElapsedMs = dbW.durationSeconds.coerceAtLeast(0).toLong() * 1000L
                val realElapsedSec = (realElapsedMs / 1000L).toInt()
                val realDurationMins = ((realElapsedMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                val realWorkTimeLabel = String.format("%02d:%02d", realElapsedSec / 60, realElapsedSec % 60)

                val dbDetails = savedInDb.exercises.map { exWithSets ->
                    val ex = exWithSets.exercise
                    val mappedSets = exWithSets.sets.mapIndexed { sIdx, sEntity ->
                        ExerciseSetItem(
                            setNumber = if (sEntity.setNumber > 0) sEntity.setNumber else (sIdx + 1),
                            weightKg = (sEntity.weight ?: 0.0).toFloat(),
                            reps = sEntity.reps.toIntOrNull() ?: 0,
                            rir = sEntity.actualRir ?: 2, actualRirProvided = sEntity.actualRir != null,
                            completed = sEntity.done,
                            distanceMeters = sEntity.distanceMeters
                        )
                    }
                    com.example.model.LoggedExerciseDetail(
                        nameHu = ex.nameHu.ifBlank { "Gyakorlat" },
                        summaryPrescription = if (isOutdoorSession || ex.measurementType == "distance") "Kültéri aktivitás" else "${mappedSets.count { it.completed }}/${mappedSets.size} sorozat",
                        sets = mappedSets,
                        measurementType = ex.measurementType,
                        repUnit = ex.repUnit
                    )
                }
                val summaryNamesFromDb = savedInDb.exercises.filter { ex -> ex.sets.any { it.done } }
                    .map { it.exercise.nameHu }.joinToString(", ").ifBlank { workoutTitle }

                val newEntry = LoggedWorkoutEntry(
                    id = finalWorkoutId,
                    dateLabelHu = dbW.started.take(16).replace('T', ' '),
                    titleHu = dbW.programName.ifBlank { workoutTitle },
                    programId = dbW.programId, dayId = if (dbW.quickWorkout) "" else dbW.dayId,
                    dayName = if (dbW.quickWorkout) "" else dbW.dayName,
                    scheduleId = dbW.scheduleId.orEmpty(),
                    durationMinutes = realDurationMins,
                    workTimeLabel = realWorkTimeLabel,
                    restTimeLabel = "—",
                    totalSets = if (dbW.completedSetsCount > 0) dbW.completedSetsCount else totalCompletedSets,
                    totalVolumeKg = dbW.totalVolumeKg.roundToInt(),
                    summaryExercisesHu = summaryNamesFromDb,
                    hasPr = dbW.hasPr226,
                    prRecordsJson = dbW.prRecordsJson226,
                    isOutdoorRun = isOutdoorSession,
                    routeKeys = savedInDb.exercises.flatMap { it.sets }.flatMap {
                        listOfNotNull(it.gpsRouteKey163, it.gpsRouteKey193)
                    }.filter(String::isNotBlank).distinct(),
                    sourceWatch = com.repforge.app.NativeSavedWearHealth.read(dbW) != null, sourceHealthConnect = !dbW.health240Json.isNullOrBlank(),
                    originDevice = dbW.originDevice,
                    savedStartIso = dbW.started,
                    savedFinishIso = dbW.finished,
                    photoCount = savedInDb.photos.size,
                    distanceKm = (savedInDb.exercises.flatMap { it.sets }.filter { it.done }
                        .sumOf { it.distanceMeters ?: 0.0 } / 1000.0).toFloat(),
                    paceLabel = savedInDb.exercises.flatMap { it.sets }.filter { it.done }.sumOf { it.distanceMeters ?: 0.0 }
                        .takeIf { it > 0 }?.let { meters ->
                            val pace = (dbW.durationSeconds / (meters / 1000.0)).toInt()
                            "%d:%02d /km".format(pace / 60, pace % 60)
                        } ?: "—",
                    exerciseDetails = dbDetails
                )

                withContext(Dispatchers.Main) {
                    _uiState.update { st ->
                        val updatedLogs = listOf(newEntry) + st.loggedWorkouts.filterNot { it.id == finalWorkoutId }
                        st.copy(
                            isWorkoutSessionActive = false,
                            activeWorkoutId = "",
                            workoutStartedEpochMs = 0L,
                            loggedWorkouts = updatedLogs,
                            workoutGoalCompleted = updatedLogs.workoutsInPeriodToDate(st.workoutGoalPeriod, LocalDate.now()).size,
                            weeklyGoalCompleted = updatedLogs.workoutsInCalendarWeekToDate(LocalDate.now()).size,
                            quickWorkoutQueue = emptyList(),
                            currentTab = MainTab.LOG,
                            currentSubScreen = SubScreen.NONE,
                            statusBannerMessage = "Edzés sikeresen lezárva és mentve ($totalCompletedSets kész sorozat • $realWorkTimeLabel)!"
                        )
                    }
                }
                loadMuscleLoadSummary(progressPeriodDays(_uiState.value.selectedProgressRangeIndex))
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.update { st ->
                        st.copy(statusBannerMessage = "Hiba: Az edzés mentése nem igazolható vissza a Room adatbázisból!")
                    }
                }
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("TrainPilotViewModel", "Failed to finalize workout canonically", e)
            withContext(Dispatchers.Main) {
                _uiState.update { st ->
                    st.copy(statusBannerMessage = "Hiba az edzés mentésekor: ${e.localizedMessage ?: "adatbázishiba"}")
                }
            }
        } finally { _uiState.update { it.copy(isSavingWorkout = false) } }
    }
}
