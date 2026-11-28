package com.example.ui.viewmodel

import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Log / Diary Actions
fun TrainPilotViewModel.updateLogSearch(query: String) {
    _uiState.update { it.copy(logSearchQuery = query) }
}

fun TrainPilotViewModel.openLogWorkoutDetail(workoutId: String) {
    if (workoutId.isBlank()) return
    if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL &&
        _uiState.value.selectedLogWorkoutId == workoutId) return
    _uiState.update {
        val back = (it.backStack + it.navigationRoute()).takeLast(16)
        it.copy(
            selectedLogWorkoutId = workoutId,
            logWorkoutMuscleLoads = emptyList(),
            expandedLogExerciseIndex = -1,
            currentTab = MainTab.LOG,
            currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL,
            backStack = back
        )
    }
    loadWorkoutMuscleSummary(workoutId)
}

fun TrainPilotViewModel.openLoggedWorkoutRouteMap(workoutId: String) {
    if (workoutId.isBlank()) return
    _uiState.update { state ->
        state.copy(currentTab = MainTab.LOG, currentSubScreen = SubScreen.LOG_ROUTE_MAP,
            selectedLogWorkoutId = workoutId, backStack = (state.backStack + state.navigationRoute()).takeLast(16))
    }
}

fun TrainPilotViewModel.openCalendarEventWorkoutDetail(eventId: String) {
    val state = _uiState.value
    val event = state.calendarEvents.firstOrNull { it.id == eventId }
    val targetEventDate = event?.dateIso?.trim().orEmpty()
    fun normalizeDayToken(raw: String): String =
        raw.substringAfterLast(':').trim().removePrefix("day_").removeSuffix(" nap").trim().lowercase(Locale.ROOT)
    val matched = state.loggedWorkouts.firstOrNull { it.id == eventId }
        ?: state.loggedWorkouts.firstOrNull { it.scheduleId.isNotBlank() && it.scheduleId == eventId }
        ?: if (event != null && targetEventDate.isNotBlank()) {
            val sameDateUnboundWorkouts = state.loggedWorkouts.filter { entry ->
                if (entry.scheduleId.isNotBlank() && entry.scheduleId != eventId) return@filter false
                val entryDate = entry.savedStartIso.take(10).ifBlank {
                    entry.dateLabelHu.take(10).replace(". ", "-").replace('.', '-').trim('-')
                }
                entryDate == targetEventDate
            }
            val eventTitlePrefix = event.titleHu.substringBefore("•").trim()
            sameDateUnboundWorkouts.firstOrNull { entry ->
                val progMatches = if (event.programId.isNotBlank() && entry.programId.isNotBlank()) {
                    entry.programId == event.programId
                } else if (event.programId.isNotBlank()) {
                    eventTitlePrefix.isNotBlank() && entry.titleHu.contains(eventTitlePrefix, ignoreCase = true)
                } else true
                val dayMatches = if (event.dayId.isNotBlank() && entry.dayId.isNotBlank()) {
                    normalizeDayToken(entry.dayId) == normalizeDayToken(event.dayId) ||
                        normalizeDayToken(entry.dayId) == normalizeDayToken(event.splitBadge)
                } else true
                progMatches && dayMatches
            } ?: sameDateUnboundWorkouts.singleOrNull { entry ->
                (event.programId.isBlank() && entry.programId.isBlank()) ||
                    (event.programId.isNotBlank() && entry.programId == event.programId) ||
                    (eventTitlePrefix.isNotBlank() && entry.titleHu.contains(eventTitlePrefix, ignoreCase = true))
            }
        } else null

    if (matched != null) {
        openLogWorkoutDetail(matched.id)
    } else {
        _uiState.update {
            it.copy(
                statusBannerMessage = tr(
                    "Ehhez a naptári eseményhez nem található naplózott edzés.",
                    "No logged workout found for this calendar event.",
                    "Für diesen Kalendereintrag wurde kein protokolliertes Training gefunden.",
                    "Nu s-a găsit niciun antrenament salvat pentru acest eveniment din calendar.",
                    it.language
                )
            )
        }
    }
}

internal fun TrainPilotViewModel.onLoggedWorkoutSaved(context: android.content.Context) {
    refreshAfterCloudSync(context)
    reportOperation(tr("Edzés mentve.", "Workout saved.", "Training gespeichert.", "Antrenament salvat."),
        com.example.ui.components.OperationFeedbackKind.SUCCESS)
}

fun TrainPilotViewModel.onLoggedWorkoutDeleted(id: String) {
    _uiState.update { state -> state.copy(
        loggedWorkouts = state.loggedWorkouts.filterNot { it.id == id },
        selectedLogWorkoutId = if (state.selectedLogWorkoutId == id) "" else state.selectedLogWorkoutId)
    }
    if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL && _uiState.value.selectedLogWorkoutId.isBlank())
        navigateBack()
    nativeContext?.let(::refreshAfterCloudSync)
    reportOperation(tr("Edzés törölve.", "Workout deleted.", "Training gelöscht.", "Antrenament șters."),
        com.example.ui.components.OperationFeedbackKind.SUCCESS)
}

fun TrainPilotViewModel.updateLoggedWorkoutHealthSources(workoutId: String, wear: Boolean, healthConnect: Boolean) {
    _uiState.update { state -> state.copy(loggedWorkouts = state.loggedWorkouts.map { entry ->
        if (entry.id == workoutId) entry.copy(sourceWatch = wear, sourceHealthConnect = healthConnect) else entry
    }) }
}

fun TrainPilotViewModel.selectLogDetailTab(index: Int) {
    _uiState.update { it.copy(logDetailTabIndex = index) }
}

fun TrainPilotViewModel.toggleExpandedLogExercise(index: Int) {
    _uiState.update {
        it.copy(expandedLogExerciseIndex = if (it.expandedLogExerciseIndex == index) -1 else index)
    }
}

// Progress / Fejlődés Actions
fun TrainPilotViewModel.reloadPersonalRecords() {
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    val revision = ++personalRecordsRevision
    personalRecordsJob?.cancel()
    _uiState.update { it.copy(savedRecordsLoading = true, savedRecordsError = false) }
    personalRecordsJob = scope.launch(Dispatchers.IO) {
        try {
            val records = com.repforge.app.PersonalRecordCalculator226.savedExerciseBests(db)
            withContext(Dispatchers.Main) {
                if (revision == personalRecordsRevision) _uiState.update {
                    it.copy(savedExerciseBests = records, savedRecordsLoading = false, savedRecordsError = false)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (revision == personalRecordsRevision) _uiState.update { it.copy(savedRecordsLoading = false, savedRecordsError = true) }
        }
    }
}
