package com.example.ui.viewmodel

import kotlinx.coroutines.sync.withLock
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.repforge.app.NativeWorkoutSession

fun TrainPilotViewModel.adjustRestTimer(deltaSec: Int) {
    val canControl = com.repforge.app.StudioRestTimer.controlsAllowed(nativeContext, _uiState.value)
    if (deltaSec != -1 && !canControl) return
    val wasRunning = _uiState.value.restTimerRunning
    _uiState.update { com.repforge.app.StudioRestTimer.adjust(nativeContext, it, deltaSec) }
    val nowState = _uiState.value
    if (canControl && (deltaSec != -1 || (wasRunning && !nowState.restTimerRunning))) {
        persistCanonicalActiveSessionAsync()
    }
}

fun TrainPilotViewModel.toggleRestTimerRunning() {
    if (!com.repforge.app.StudioRestTimer.controlsAllowed(nativeContext, _uiState.value)) return
    _uiState.update { com.repforge.app.StudioRestTimer.toggle(nativeContext, it) }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.skipRestTimer() {
    if (!com.repforge.app.StudioRestTimer.controlsAllowed(nativeContext, _uiState.value)) return
    _uiState.update { com.repforge.app.StudioRestTimer.skip(nativeContext, it) }
    persistCanonicalActiveSessionAsync()
}

fun TrainPilotViewModel.updateIntervalSettings(workSec: Int, restSec: Int, rounds: Int, sound: Boolean,
    vibration: Boolean, autoStep: Boolean, onCommitted: () -> Unit = {}) {
    val store = nativeSettingsStore
    val context = nativeContext
    val scope = nativeScope
    if (store == null || context == null || scope == null) {
        showBannerMessage(tr("Az időzítő beállítása nem menthető.", "Could not save timer settings.",
            "Timereinstellungen konnten nicht gespeichert werden.", "Setările temporizatorului nu au putut fi salvate."))
        return
    }
    if (studioIntervalWriteJob?.isActive == true) return
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrElse {
        showBannerMessage(it.message ?: "Could not save timer settings."); return
    }
    studioIntervalWriteError = null
    studioIntervalWriteJob = scope.launch {
        try {
            val config = com.repforge.app.NativeIntervalPreset.normalized(JSONObject().put("work", workSec)
                .put("rest", restSec).put("rounds", rounds).put("sound", sound)
                .put("vibration", vibration).put("autoAdvance", autoStep))
            withContext(Dispatchers.IO) { studioTimerWriteMutex.withLock {
                NativeDataReplacement.mutate(context) {
                    ticket.requireCurrent(context)
                    store.setIntervalPreset(config.getInt("work"), config.getInt("rest"), config.getInt("rounds"),
                        sound, vibration, autoStep)
                }
            } }
            ticket.requireCurrent(context)
            _uiState.update { it.copy(intervalWorkSec = config.getInt("work"),
                intervalRestSec = config.getInt("rest"), intervalRounds = config.getInt("rounds"),
                soundAlertEnabled = sound, vibrationEnabled = vibration, autoStepIntervals = autoStep,
                statusBannerMessage = tr("Intervallumok elmentve", "Intervals saved",
                    "Intervalle gespeichert", "Intervale salvate")) }
            onCommitted()
        } catch (error: Exception) {
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            studioIntervalWriteError = tr("Az időzítő beállítása nem menthető.", "Could not save timer settings.",
                "Timereinstellungen konnten nicht gespeichert werden.", "Setările temporizatorului nu au putut fi salvate.")
            showBannerMessage(requireNotNull(studioIntervalWriteError))
        }
    }
}

/** The form's explicit Start persists first, then prepares the existing canonical engine. */
fun TrainPilotViewModel.saveAndStartIntervals(work: Int, rest: Int, rounds: Int, sound: Boolean,
    vibration: Boolean, autoAdvance: Boolean) {
    if (nativeStartJob?.isActive == true || studioIntervalWriteJob?.isActive == true) return
    val context = nativeContext ?: run {
        updateIntervalSettings(work, rest, rounds, sound, vibration, autoAdvance)
        return
    }
    fun noActiveWorkout() = !_uiState.value.isWorkoutSessionActive &&
        ActiveWorkoutStore.current(context)?.optBoolean("active") != true &&
        PhoneWorkoutExecution.store(context).snapshot()?.optBoolean("active") != true
    val state = _uiState.value
    val exercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId && intervalEligible(it) }
        ?: state.exercises.firstOrNull(::intervalEligible)
    if (exercise == null || !runCatching { noActiveWorkout() }.getOrDefault(false)) {
        showBannerMessage(if (exercise == null) tr("Nincs elérhető egyoldali időalapú gyakorlat.",
            "No single-sided timed exercise is available.", "Keine einseitige zeitbasierte Übung verfügbar.",
            "Nu există un exercițiu cronometrat unilateral disponibil.") else tr(
            "Előbb fejezd be vagy vesd el az aktív edzést.", "Finish or discard the active workout first.",
            "Beende oder verwerfe zuerst das aktive Training.", "Încheie sau renunță mai întâi la antrenamentul activ."))
        return
    }
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrElse {
        showBannerMessage(it.message ?: "Could not start intervals."); return
    }
    val route = state.navigationRoute()
    val stack = state.backStack
    updateIntervalSettings(work, rest, rounds, sound, vibration, autoAdvance) {
        if (_uiState.value.navigationRoute() != route || _uiState.value.backStack != stack) return@updateIntervalSettings
        startCanonicalWorkout(allowExisting = false, stillRequested = {
            _uiState.value.currentTab == state.currentTab && _uiState.value.currentSubScreen == state.currentSubScreen &&
                _uiState.value.backStack == stack
        }, onPresented = { frame ->
            val raw = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
            val rows = raw.getJSONArray("sets")
            val set = (0 until rows.length()).map { rows.getJSONObject(it) }.first { !it.optBoolean("done") }
            _uiState.update { it.copy(pendingIntervalStart = com.repforge.app.NativeWorkoutTarget(
                frame.getString("workoutId"), frame.getLong("generation"), raw.getString("id"), set.getString("setId")),
                pendingIntervalOwnerEpoch = frame.optLong("ownerEpoch193"), pendingIntervalPauseRevision = frame.optLong("pauseRevision")) }
        }) {
            ticket.requireCurrent(context)
            check(noActiveWorkout()) { "Finish or discard the active workout first." }
            val frame = com.repforge.app.NativeWorkoutSession.startQuickExercises(context,
                listOf(exercise.id), tr("Intervallum edzés", "Interval workout", "Intervalltraining", "Antrenament pe intervale"),
                automaticEnabled = true, database = requireNotNull(nativeDatabase), requireNew = true)
            // A concurrent native start may recover a different session. Never control that session.
            val raw = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
            check(frame.optString("timerOwner", "phone") == "phone" && !frame.optBoolean("workoutPaused") &&
                raw.optString("id") == exercise.id && com.repforge.app.NativeSetStopwatch.timed(raw) &&
                !com.repforge.app.NativeSetStopwatch.bilateral(raw)) { "The active workout changed; start intervals explicitly." }
            frame
        }
    }
}

internal fun TrainPilotViewModel.intervalEligible(exercise: ExerciseItem): Boolean = exercise.isTimed && !exercise.isBilateral &&
    exercise.measurementType !in setOf("gps", "distance") && exercise.repUnit.trim().lowercase() !in setOf("m", "km")

/** Consume before requesting ARM. Recomposition/recovery must never replay Start. */
fun TrainPilotViewModel.consumeIntervalStart(target: com.repforge.app.NativeWorkoutTarget): Boolean {
    if (_uiState.value.pendingIntervalStart != target || _uiState.value.currentSubScreen != SubScreen.ACTIVE_TIMED) return false
    _uiState.update { it.copy(pendingIntervalStart = null) }
    return true
}

internal fun TrainPilotViewModel.admitSummaryFeedback(): Boolean {
    val context = nativeContext ?: return false
    val id = _uiState.value.activeWorkoutId
    if (id.isBlank() || !_uiState.value.isWorkoutSessionActive || _uiState.value.isSavingWorkout) return false
    val same = summaryFeedbackOwnerId == id &&
        runCatching { requireNotNull(summaryFeedbackTicket).requireCurrent(context) }.isSuccess
    if (!same) {
        summaryFeedbackTicket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return false
        summaryFeedbackOwnerId = id
        _uiState.update { it.copy(summaryFeedbackWorkoutId = id, workoutSummaryMoodIndex = -1, workoutSummaryNote = "") }
    }
    return true
}

fun TrainPilotViewModel.selectWorkoutSummaryMood(index: Int) {
    if (index !in -1..5 || !admitSummaryFeedback()) return
    _uiState.update { state ->
        if (!state.isWorkoutSessionActive || state.activeWorkoutId.isBlank()) state
        else state.copy(summaryFeedbackWorkoutId = state.activeWorkoutId, workoutSummaryMoodIndex = index,
            workoutSummaryNote = if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) state.workoutSummaryNote else "")
    }
}

fun TrainPilotViewModel.updateWorkoutSummaryNote(note: String) {
    if (!admitSummaryFeedback()) return
    _uiState.update { state ->
        if (!state.isWorkoutSessionActive || state.activeWorkoutId.isBlank()) state
        else state.copy(summaryFeedbackWorkoutId = state.activeWorkoutId, workoutSummaryNote = note.take(3000),
            workoutSummaryMoodIndex = if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) state.workoutSummaryMoodIndex else -1)
    }
}

fun TrainPilotViewModel.updateExerciseFeedback(id: String, index: Int? = null, note: String? = null) {
    val context = nativeContext ?: return
    val stateBefore = _uiState.value
    val ticketValid = exerciseFeedbackOwnerId == stateBefore.activeWorkoutId &&
        runCatching { requireNotNull(exerciseFeedbackTicket).requireCurrent(context) }.isSuccess
    if (!ticketValid) {
        exerciseFeedbackTicket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
        exerciseFeedbackOwnerId = stateBefore.activeWorkoutId
    }
    _uiState.update { state ->
        if (state.isSavingWorkout || !state.isWorkoutSessionActive || state.activeWorkoutId.isBlank() ||
            state.activeWorkoutExercises.none { it.id == id } || (index != null && index !in -1..5)) state
        else {
            val same = ticketValid && state.exerciseFeedbackWorkoutId == state.activeWorkoutId
            state.copy(exerciseFeedbackWorkoutId = state.activeWorkoutId,
                exerciseFeedbackChoices = (if (same) state.exerciseFeedbackChoices else emptyMap()).let {
                    if (index != null) it + (id to index) else it
                },
                exerciseFeedbackNotes = (if (same) state.exerciseFeedbackNotes else emptyMap()).let {
                    if (note != null) it + (id to note.take(3000)) else it
                })
        }
    }
}
