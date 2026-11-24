package com.example.ui.viewmodel

import java.time.ZoneId
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.repforge.app.ActiveWorkoutStore
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import com.repforge.app.NativeWorkoutSession

fun TrainPilotViewModel.startPlannedWorkout(programId: String? = null, dayIndex: Int? = null) {
    startCanonicalWorkout {
        val db = requireNotNull(nativeDatabase)
        calendarWriteJob?.join()
        check(calendarWriteError == null) { requireNotNull(calendarWriteError) }
        val state = _uiState.value
        val target = if (programId == null && dayIndex == null) withContext(Dispatchers.IO) {
            com.repforge.app.ui.home.NativeHomeRepository(db).read(
                requireNotNull(nativeSettingsStore).current().activeProgramId,
                state.language.code, Instant.now(), ZoneId.systemDefault()).target
        } else null
        val program = state.programs.firstOrNull { it.id == (target?.programId ?: programId) }
            ?: state.programs.firstOrNull { it.isActive }
        requireNotNull(program) { "Az edzésprogram nem található." }
        val saved = withContext(Dispatchers.IO) { db.programDao().getProgramWithDaysById(program.id) }
        val requestedDay = program.days.getOrNull(dayIndex ?: 0)
        val nativeDay = saved?.days?.singleOrNull {
            if (target != null) it.dayId == target.dayId
            else it.compositeId == requestedDay?.id || it.dayId == requestedDay?.id
        } ?: error("Az edzésnap megváltozott. Nyisd meg újra a programot.")
        com.repforge.app.NativeWorkoutSession.startProgramWorkout(requireNotNull(nativeContext),
            program.id, nativeDay.dayId, target?.scheduleId, database = db,
            automaticEnabled = state.autoModeEnabled).snapshot
    }
}

fun TrainPilotViewModel.openPlannedProgramDetails() {
    val state = _uiState.value
    val target = state.homeWorkoutTarget
    val program = state.programs.firstOrNull { it.id == target?.programId }
        ?: state.programs.firstOrNull { it.isActive } ?: return
    val index = program.days.indexOfFirst { it.id == target?.dayId ||
        it.id == "${program.id}:${target?.dayId}" }.coerceAtLeast(0)
    toggleInlineProgramEditor(program.id, index, INLINE_EDITOR_HOST_HOME)
}

fun TrainPilotViewModel.startCoachWorkout(programId: String, dayId: String, scheduleId: String?) {
    startCanonicalWorkout {
        com.repforge.app.NativeWorkoutSession.startProgramWorkout(requireNotNull(nativeContext),
            programId, dayId, scheduleId, database = requireNotNull(nativeDatabase),
            automaticEnabled = _uiState.value.autoModeEnabled).snapshot
    }
}

fun TrainPilotViewModel.startWorkoutWithExercises(exercisesToRun: List<ExerciseItem>, titleHu: String = "Gyors edzés") {
    val uniqueExercises = exercisesToRun.distinctBy { it.id }
    if (uniqueExercises.isEmpty()) {
        _uiState.update { it.copy(statusBannerMessage = tr("Nincs kiválasztott gyakorlat.", "No exercises selected.", "Keine Übungen ausgewählt.", "Niciun exercițiu selectat.")) }
        return
    }
    startCanonicalWorkout {
        com.repforge.app.NativeWorkoutSession.startQuickExercises(requireNotNull(nativeContext),
            uniqueExercises.map { it.id }, titleHu, _uiState.value.autoModeEnabled,
            requireNotNull(nativeDatabase))
    }
}

internal fun TrainPilotViewModel.startCanonicalWorkout(allowExisting: Boolean = true, stillRequested: () -> Boolean = { true },
    onPresented: (JSONObject) -> Unit = {}, start: suspend () -> JSONObject) {
    if (nativeStartJob?.isActive == true || _uiState.value.isSavingWorkout) return
    if (_uiState.value.isWorkoutSessionActive && _uiState.value.workoutStartedEpochMs > 0) {
        if (!allowExisting) {
            showBannerMessage(tr("Előbb fejezd be vagy vesd el az aktív edzést.", "Finish or discard the active workout first.",
                "Beende oder verwerfe zuerst das aktive Training.", "Încheie sau renunță mai întâi la antrenamentul activ."))
            return
        }
        returnToActiveWorkout()
        return
    }
    val scope = nativeScope ?: return
    nativeStartJob = scope.launch(Dispatchers.Main.immediate) {
        try {
            programWriteJob?.join()
            check(programWriteError == null) { requireNotNull(programWriteError) }
            studioIntervalWriteJob?.join()
            check(studioIntervalWriteError == null) { requireNotNull(studioIntervalWriteError) }
            if (!stillRequested()) return@launch
            val frame = start() // Canonical engine commits before the UI advertises a workout.
            if (presentCanonicalWorkout(frame, stillRequested)) {
                onPresented(frame)
                queueWatchMeasurementStart(frame)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            _uiState.update { it.copy(statusBannerMessage = error.message ?: "Az edzés nem indítható.") }
        }
    }
}

internal suspend fun TrainPilotViewModel.presentCanonicalWorkout(requested: JSONObject, stillRequested: () -> Boolean = { true }): Boolean {
    val library = withContext(Dispatchers.IO) {
        requireNotNull(nativeDatabase).exerciseLibraryDao().getAllExercises().associateBy { it.id }
    }
    val frame = requireNotNull(ActiveWorkoutStore.current(requireNotNull(nativeContext)))
    check(frame.optBoolean("active") && frame.optString("workoutId") == requested.optString("workoutId") &&
        frame.optLong("generation") == requested.optLong("generation")) { "Az edzés időközben megváltozott." }
    if (!stillRequested()) {
        // The requested frame is durable, but leaving during preparation cancels measurement launch.
        runCatching { com.repforge.app.StudioWorkoutBridge.pause(requireNotNull(nativeContext), true) }
        return false
    }
    val ids = frame.getJSONArray("exercises")
    val exercises = (0 until ids.length()).map { index ->
        val raw = ids.getJSONObject(index)
        com.example.ui.components.withNativeStrengthUnits(
            mapExerciseEntity(requireNotNull(library[raw.getString("id")])),
            raw.opt("loadType") as? String, raw.opt("repUnit") as? String).copy(
                measurementType = raw.optString("measurementType"),
                isTimed = com.repforge.app.NativeSetStopwatch.timed(raw),
                isBilateral = com.repforge.app.NativeSetStopwatch.bilateral(raw))
    }
    val sets = (0 until ids.length()).associate { index ->
        val raw = ids.getJSONObject(index)
        val rows = raw.getJSONArray("sets")
        raw.getString("id") to (0 until rows.length()).map { rowIndex ->
            val row = rows.getJSONObject(rowIndex)
            ExerciseSetItem(rowIndex + 1, row.optDouble("weight", 0.0).toFloat(),
                row.optString("reps").toIntOrNull() ?: 0, row.optInt("actualRir", 2), row.optBoolean("done"),
                leftSec = row.optInt("leftSeconds", row.optInt("leftSec", exercises[index].targetSeconds)),
                rightSec = row.optInt("rightSeconds", row.optInt("rightSec", exercises[index].targetSeconds)),
                leftRecorded = row.optInt("leftSeconds") > 0 || row.optBoolean("leftRecorded"),
                rightRecorded = row.optInt("rightSeconds") > 0 || row.optBoolean("rightRecorded"),
                id = row.getString("setId"), actualRirProvided = row.has("actualRir") && !row.isNull("actualRir"))
        }
    }
    val currentIndex = frame.optInt("currentExercise").coerceIn(exercises.indices)
    val first = exercises[currentIndex]
    val target = targetSubScreenForExercise(first)
    _uiState.update { state -> state.copy(
        isWorkoutSessionActive = true, activeWorkoutId = frame.getString("workoutId"), activeWorkoutGeneration = frame.getLong("generation"),
        workoutStartedEpochMs = Instant.parse(frame.getString("started")).toEpochMilli(),
        activeWorkoutSubScreen = target, activeWorkoutTitleHu = frame.optString("programName"),
        activeWorkoutExercises = exercises, activeExerciseIndex = currentIndex, selectedExerciseId = first.id,
        activeSets = sets.getValue(first.id), activeSetsByExerciseId = sets,
        phoneControlsWorkout = frame.optString("timerOwner", "phone") == "phone" &&
            !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame) &&
            !com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
        watchControlPending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
        currentSubScreen = target, showQuickWorkoutSheet = false, timedCurrentSec = 0,
        backStack = (state.backStack + state.navigationRoute()).takeLast(16),
        statusBannerMessage = null) }
    return true
}

fun TrainPilotViewModel.startQuickStrengthWithExercise(exercise: ExerciseItem) {
    val currentQueue = _uiState.value.quickWorkoutQueue
    val listToRun = if (currentQueue.any { it.id == exercise.id }) {
        listOf(exercise) + currentQueue.filter { it.id != exercise.id }
    } else if (currentQueue.isNotEmpty()) {
        listOf(exercise) + currentQueue
    } else {
        listOf(exercise)
    }
    startWorkoutWithExercises(listToRun, "Gyors súlyzós edzés")
}

fun TrainPilotViewModel.startQuickTimedWithExercise(exercise: ExerciseItem) {
    startWorkoutWithExercises(listOf(exercise), "Gyors időalapú edzés")
}

fun TrainPilotViewModel.startOutdoorWorkout(titleHu: String = "Kültéri futás") {
    val currentSub = _uiState.value.activeWorkoutSubScreen
    val durable = nativeContext?.let { ActiveWorkoutStore.current(it) }?.takeIf { it.optBoolean("active") }
    if (_uiState.value.isWorkoutSessionActive || durable != null) {
        if (currentSub == SubScreen.ACTIVE_OUTDOOR_GPS &&
            (durable == null || durable.optString("workoutId") == _uiState.value.activeWorkoutId)) {
            navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS, MainTab.WORKOUT)
        } else _uiState.update { it.copy(statusBannerMessage = tr(
            "Előbb fejezd be vagy vesd el az aktív edzést.", "Finish or discard the active workout first.",
            "Beende oder verwerfe zuerst das aktive Training.", "Încheie sau renunță mai întâi la antrenamentul activ.")) }
        return
    }

    val newWorkoutId = UUID.randomUUID().toString()
    val outdoorEx = ExerciseItem(
        id = "outdoor_run_ex",
        nameHu = titleHu,
        category = ExerciseCategory.CORE,
        primaryMusclesHu = "Kardió, Láb",
        equipmentHu = "GPS / Szabadban",
        isTimed = true
    )
    val defaultSets = listOf(
        ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 0, rir = 0, completed = false)
    )
    _uiState.update { state ->
        val back = (state.backStack + state.navigationRoute()).takeLast(16)
        state.copy(
            isWorkoutSessionActive = false,
            activeWorkoutId = newWorkoutId,
            activeWorkoutGeneration = 0L,
            workoutStartedEpochMs = 0L,
            activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
            activeWorkoutTitleHu = titleHu,
            activeWorkoutExercises = listOf(outdoorEx),
            activeExerciseIndex = 0,
            selectedExerciseId = outdoorEx.id,
            activeSets = defaultSets,
            activeSetsByExerciseId = mapOf(outdoorEx.id to defaultSets),
            outdoorDistanceKm = 0f,
            outdoorElapsedLabel = "00:00",
            outdoorPaceLabel = "0:00",
            currentSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
            currentTab = MainTab.WORKOUT,
            backStack = back,
            showQuickWorkoutSheet = false
        )
    }
}

fun TrainPilotViewModel.startGpsTrackingSession(activityName: String = "Kültéri futás", routeKey: String = ""): Boolean {
    val ctx = nativeContext ?: return false
    val previous = ActiveWorkoutStore.current(ctx)
    if (previous?.optBoolean("active") == true && previous.optString("workoutId") != _uiState.value.activeWorkoutId) {
        _uiState.update { it.copy(statusBannerMessage = "Előbb fejezd be a már aktív edzést.") }
        return false
    }
    if (previous?.optBoolean("active") == true) {
        _uiState.update { it.copy(outdoorRouteKey = routeKey, outdoorGpsEnabled = routeKey.isNotBlank()) }
        persistCanonicalActiveSessionAsync()
        return true
    }
    val nowMs = System.currentTimeMillis()
    val state = _uiState.value
    val nextState = run {
        val currentTitle = activityName.ifBlank { state.activeWorkoutTitleHu.ifBlank { "Kültéri futás" } }
        val outdoorEx = ExerciseItem(
            id = "outdoor_run_ex",
            nameHu = currentTitle,
            category = ExerciseCategory.CORE,
            primaryMusclesHu = "Kardió, Láb",
            equipmentHu = "GPS / Szabadban",
            isTimed = true
        )
        val defaultSets = listOf(
            ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 0, rir = 0, completed = false)
        )
        state.copy(
            isWorkoutSessionActive = true,
            outdoorRouteKey = routeKey,
            outdoorGpsEnabled = routeKey.isNotBlank(),
            activeWorkoutId = UUID.randomUUID().toString(),
            workoutStartedEpochMs = nowMs,
            activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
            activeWorkoutTitleHu = currentTitle,
            activeWorkoutExercises = listOf(outdoorEx),
            activeExerciseIndex = 0,
            selectedExerciseId = outdoorEx.id,
            activeSets = defaultSets,
            activeSetsByExerciseId = mapOf(outdoorEx.id to defaultSets),
            statusBannerMessage = if (routeKey.isBlank()) "Beltéri natív munkamenet előkészítve." else "Natív edzés előkészítve; GPS-visszaigazolásra vár."
        )
    }
    return try {
        val saved = com.repforge.app.StudioWorkoutBridge.persist(ctx, buildCanonicalSessionJsonFromState(nextState))
        check(saved.optString("workoutId") == nextState.activeWorkoutId) { "Másik natív edzés aktív. Nyisd meg a futó edzést." }
        _uiState.value = nextState
        queueWatchMeasurementStart(saved)
        true
    } catch (error: Exception) {
        _uiState.update { it.copy(statusBannerMessage = error.message ?: "A GPS-edzés nem indítható.") }
        false
    }
}

fun TrainPilotViewModel.pauseOutdoorMeasurement(paused: Boolean): Boolean {
    val ctx = nativeContext ?: return false
    return try { com.repforge.app.StudioWorkoutBridge.pause(ctx, paused); true }
    catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) }; false }
}

fun TrainPilotViewModel.selectOutdoorActivityType(activityName: String) {
    val outdoorEx = ExerciseItem(
        id = "outdoor_run_ex",
        nameHu = activityName,
        category = ExerciseCategory.CORE,
        primaryMusclesHu = "Kardió, Láb",
        equipmentHu = "GPS / Szabadban",
        isTimed = true
    )
    _uiState.update { state ->
        val updatedExs = state.activeWorkoutExercises.map { ex ->
            if (ex.id == "outdoor_run_ex") ex.copy(nameHu = activityName) else ex
        }
        state.copy(
            activeWorkoutTitleHu = activityName,
            activeWorkoutExercises = if (updatedExs.isNotEmpty()) updatedExs else listOf(outdoorEx),
            activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS
        )
    }
}

fun TrainPilotViewModel.addExerciseToWorkoutQueue(exercise: ExerciseItem) {
    _uiState.update { state ->
        val updatedQueue = if (state.quickWorkoutQueue.any { it.id == exercise.id }) {
            state.quickWorkoutQueue
        } else {
            state.quickWorkoutQueue + exercise
        }
        val updatedActive = if (state.activeWorkoutExercises.any { it.id == exercise.id }) {
            state.activeWorkoutExercises
        } else {
            state.activeWorkoutExercises + exercise
        }
        val updatedSetsMap = state.activeSetsByExerciseId + (exercise.id to buildDefaultSetsForExercise(exercise))
        state.copy(
            quickWorkoutQueue = updatedQueue,
            activeWorkoutExercises = updatedActive,
            activeSetsByExerciseId = updatedSetsMap,
            statusBannerMessage = "„${exercise.nameHu}” hozzáadva az edzéshez (${updatedQueue.size} gyakorlat kiválasztva)"
        )
    }
}

fun TrainPilotViewModel.removeExerciseFromWorkoutQueue(exerciseId: String) {
    _uiState.update { state ->
        val removed = state.quickWorkoutQueue.firstOrNull { it.id == exerciseId } ?: return@update state
        val name = removed.localizedName(state.language)
        state.copy(
            quickWorkoutQueue = state.quickWorkoutQueue.filterNot { it.id == exerciseId },
            statusBannerMessage = tr(
                "„$name” eltávolítva az összeállításból",
                "“$name” removed from workout selection",
                "„$name“ aus der Trainingsauswahl entfernt",
                "„$name” eliminat din selecția antrenamentului",
                state.language
            )
        )
    }
}

fun TrainPilotViewModel.startQueuedWorkout() {
    val queue = _uiState.value.quickWorkoutQueue
    if (queue.isNotEmpty()) {
        startWorkoutWithExercises(queue, "Egyéni összeállított edzés")
    } else {
        startPlannedWorkout()
    }
}
