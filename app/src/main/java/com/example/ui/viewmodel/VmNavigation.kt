package com.example.ui.viewmodel

import com.example.model.HomeDemoMode
import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.TrainPilotWorkoutFinalization

fun TrainPilotViewModel.selectTab(tab: MainTab) {
    _uiState.update { it.copy(pendingIntervalStart = null) }
    libraryAddsToProgramDay = false
    if (!pauseWhenLeavingMeasurement()) return
    _uiState.update { state ->
        if (state.currentTab == tab && state.currentSubScreen == SubScreen.NONE) {
            state
        } else {
            val updatedStack = (state.backStack + state.navigationRoute()).takeLast(16)
            state.copy(
                currentTab = tab,
                currentSubScreen = SubScreen.NONE,
                expandedHealthWeeklyGoals = state.expandedHealthWeeklyGoals &&
                    !(tab == MainTab.HEALTH && state.currentTab != MainTab.HEALTH),
                expandedHealthJournal = state.expandedHealthJournal && tab != MainTab.HEALTH,
                expandedHealthConnect = state.expandedHealthConnect && tab != MainTab.HEALTH,
                backStack = updatedStack
            )
        }
    }
}

fun TrainPilotViewModel.navigateToSubScreen(subScreen: SubScreen, targetTab: MainTab? = null) {
    if (subScreen != SubScreen.ACTIVE_TIMED) _uiState.update { it.copy(pendingIntervalStart = null) }
    if (subScreen == SubScreen.PROGRAMS_LIST) {
        selectTab(MainTab.PROGRAMS)
        return
    }
    if (subScreen !in setOf(SubScreen.EXERCISE_LIBRARY, SubScreen.EXERCISE_DETAIL)) exerciseSelection = null
    // 2779: a library opened from anywhere else never writes into a program day.
    if (subScreen != SubScreen.EXERCISE_DETAIL) libraryAddsToProgramDay = false
    if (subScreen != _uiState.value.currentSubScreen && !pauseWhenLeavingMeasurement()) return
    val isActiveMeasurement = subScreen == SubScreen.ACTIVE_STRENGTH ||
        subScreen == SubScreen.ACTIVE_TIMED ||
        subScreen == SubScreen.ACTIVE_BILATERAL ||
        subScreen == SubScreen.ACTIVE_OUTDOOR_GPS

    if (subScreen == SubScreen.ACTIVE_OUTDOOR_GPS && _uiState.value.activeWorkoutSubScreen != SubScreen.ACTIVE_OUTDOOR_GPS) {
        startOutdoorWorkout("Kültéri futás")
        return
    }

    _uiState.update { state ->
        if (state.currentSubScreen == subScreen && state.currentTab == (targetTab ?: state.currentTab))
            return@update state
        val updatedStack = (state.backStack + state.navigationRoute()).takeLast(16)
        state.copy(
            currentTab = targetTab ?: state.currentTab,
            currentSubScreen = subScreen,
            localBackupStatus = if (subScreen == SubScreen.BACKUP_RESTORE && state.localBackupStatus?.busy != true)
                null else state.localBackupStatus,
            privatePhotoWorkoutId = if (subScreen == SubScreen.PRIVATE_PHOTOS) null else state.privatePhotoWorkoutId,
            // A route is a preview until the native engine has committed a session.
            isWorkoutSessionActive = state.isWorkoutSessionActive,
            activeWorkoutSubScreen = if (isActiveMeasurement) subScreen else state.activeWorkoutSubScreen,
            backStack = updatedStack
        )
    }
}

fun TrainPilotViewModel.openLogWorkoutPhotos() {
    val id = _uiState.value.selectedLogWorkoutId ?: return
    navigateToSubScreen(SubScreen.PRIVATE_PHOTOS)
    if (_uiState.value.currentSubScreen == SubScreen.PRIVATE_PHOTOS)
        _uiState.update { it.copy(privatePhotoWorkoutId = id) }
}

fun TrainPilotViewModel.returnToActiveWorkout() {
    if (!pauseActiveWorkout(false)) return
    _uiState.update { state ->
        val targetSub = state.activeWorkoutSubScreen
        val updatedStack = (state.backStack + state.navigationRoute()).takeLast(16)
        state.copy(
            currentTab = MainTab.WORKOUT,
            currentSubScreen = targetSub,
            selectedExerciseId = state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex)?.id ?: state.selectedExerciseId,
            isWorkoutSessionActive = true,
            backStack = updatedStack
        )
    }
}

fun TrainPilotViewModel.stopActiveWorkoutSession(expectedWorkoutId: String? = null, expectedGeneration: Long? = null) {
    if (_uiState.value.isSavingWorkout) return
    val ctx = nativeContext ?: return
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    val targetId = expectedWorkoutId ?: _uiState.value.activeWorkoutId.takeIf(String::isNotBlank) ?: return
    val targetGeneration = expectedGeneration?.takeIf { it > 0 } ?: _uiState.value.activeWorkoutGeneration.takeIf { it > 0 }
    _uiState.update { it.copy(isSavingWorkout = true) }
    scope.launch(Dispatchers.Main.immediate) {
        try {
            check(!com.repforge.app.NativeDataReplacement.blocked(ctx)) { "Adatcsere van folyamatban." }
            val snapshot = ActiveWorkoutStore.current(ctx)
            val execution = PhoneWorkoutExecution.store(ctx).snapshot()
            val alreadyClosed = snapshot?.optString("workoutId") == targetId && !snapshot.optBoolean("active") &&
                execution?.optString("workoutId") == targetId && !execution.optBoolean("active") &&
                (targetGeneration == null || execution.optLong("generation") == targetGeneration)
            val active = snapshot?.takeIf { it.optBoolean("active") }
            if (!alreadyClosed) check(active?.optString("workoutId") == targetId &&
                (targetGeneration == null || active.optLong("generation") == targetGeneration) &&
                _uiState.value.activeWorkoutId == targetId) { "Az elvetendő edzés megváltozott. Nyisd meg újra a műveletet." }
            if (active != null) {
                check(active.optString("workoutId") == _uiState.value.activeWorkoutId)
                TrainPilotWorkoutFinalization(ctx, db).discardWorkout(active)
            }
            _uiState.update {
                if (it.activeWorkoutId.isNotBlank() && it.activeWorkoutId != targetId) return@update it
                it.copy(isWorkoutSessionActive = false, activeWorkoutId = "", activeWorkoutGeneration = 0L, workoutStartedEpochMs = 0,
                outdoorRouteKey = "", outdoorGpsEnabled = false, homeDemoMode = HomeDemoMode.PLANNED_NEXT,
                activeWorkoutExercises = emptyList(), activeSets = emptyList(), activeSetsByExerciseId = emptyMap(),
                backStack = it.backStack.filterNot { route -> route.subScreen in activeMeasurementScreens },
                currentSubScreen = SubScreen.NONE, currentTab = MainTab.HOME,
                statusBannerMessage = if (alreadyClosed) "Az edzés már lezárult." else "Edzés elvetve.") }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            _uiState.update { it.copy(statusBannerMessage = error.message ?: "Az edzés elvetése nem sikerült.") }
            if (targetId != null) refreshAfterCloudSync(ctx)
        } finally { _uiState.update { it.copy(isSavingWorkout = false) } }
    }
}

/**
 * 2782: a sheet route closes back to the screen it was opened from. Ignored when that sheet is
 * no longer the current route (a duplicate dismiss used to pop one level further, often to Home).
 */
fun TrainPilotViewModel.dismissSheetRoute(route: SubScreen, depth: Int) {
    val state = _uiState.value
    if (state.currentSubScreen == route && state.backStack.size == depth) navigateBack()
}

fun TrainPilotViewModel.navigateBack(): Boolean {
    _uiState.update { it.copy(pendingIntervalStart = null) }
    if (_uiState.value.currentSubScreen == SubScreen.EXERCISE_LIBRARY) {
        exerciseSelection = null
        libraryAddsToProgramDay = false
    }
    if (!pauseWhenLeavingMeasurement()) return true
    val current = _uiState.value
    if (current.backStack.isNotEmpty()) {
        val previous = current.backStack.last()
        _uiState.update {
            val restored = it.restoreNavigation(previous)
            restored.copy(backStack = it.backStack.dropLast(1),
                logWorkoutMuscleLoads = if (restored.selectedLogWorkoutId != it.selectedLogWorkoutId)
                    emptyList() else it.logWorkoutMuscleLoads)
        }
        if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL)
            loadWorkoutMuscleSummary(_uiState.value.selectedLogWorkoutId)
        return true
    } else if (current.currentSubScreen != SubScreen.NONE) {
        _uiState.update { it.copy(currentSubScreen = SubScreen.NONE) }
        return true
    } else if (current.currentTab != MainTab.HOME) {
        _uiState.update { it.copy(currentTab = MainTab.HOME) }
        return true
    }
    return false
}
