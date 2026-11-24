package com.example.ui.viewmodel

import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.repforge.app.ui.progress.NativeProgressQuery

fun TrainPilotViewModel.selectProgressMainSection(section: Int) {
    if (section != 0 && section != 2) return
    _uiState.update { it.copy(progressMainSection = section) }
    if (section == 2) reloadPersonalRecords()
}

fun TrainPilotViewModel.selectProgressExercise(name: String) {
    _uiState.update { it.copy(selectedProgressExercise = name) }
}

fun TrainPilotViewModel.selectProgressRange(index: Int) {
    val range = index.coerceIn(0, 3)
    val days = progressPeriodDays(range)
    _uiState.update { it.copy(selectedProgressRangeIndex = range, selectedVolumeBarIndex = -1,
        muscleLoadPeriodDays = days, realMuscleLoads = emptyList()) }
    loadMuscleLoadSummary(days)
}

fun TrainPilotViewModel.selectVolumeBar(index: Int) {
    _uiState.update { it.copy(selectedVolumeBarIndex = index) }
}

fun TrainPilotViewModel.selectExpandedStatsCard(index: Int) {
    _uiState.update {
        it.copy(expandedStatsCardIndex = if (it.expandedStatsCardIndex == index) -1 else index)
    }
}

fun TrainPilotViewModel.selectStatsSubTab(index: Int) {
    _uiState.update { it.copy(statsSubTab = index) }
}

fun TrainPilotViewModel.toggleCardioFilterStepsMode(stepsMode: Boolean) {
    _uiState.update { it.copy(cardioFilterStepsMode = stepsMode) }
}

fun TrainPilotViewModel.openExerciseVideoModal(exerciseTitle: String, exerciseId: String? = null) {
    _uiState.update {
        it.copy(videoModalExerciseTitle = exerciseTitle, videoModalExerciseId = exerciseId)
    }
}

fun TrainPilotViewModel.closeExerciseVideoModal() {
    _uiState.update {
        it.copy(videoModalExerciseTitle = null, videoModalExerciseId = null)
    }
}

internal fun TrainPilotViewModel.muscleItems(rows: List<com.repforge.app.data.db.ProgressMuscleSummary>) =
    NativeProgressQuery.muscle(rows, _uiState.value.language.name.lowercase()).map { m ->
        com.example.ui.components.MuscleLoadItem(
            id = m.muscleKey, nameHu = m.label,
            setsText = "${m.setsCount} sorozat", percentage = (m.loadPercentage * 100).toInt(),
            ratio = m.loadPercentage,
            levelLabelHu = when {
                m.setsCount == 0 -> "Nincs terhelés"
                m.loadPercentage >= 0.75f -> "Nagyobb terhelés"
                m.loadPercentage >= 0.4f -> "Közepes terhelés"
                else -> "Kisebb terhelés"
            }, topExercisesHu = emptyList(), exerciseIdToOpen = "", setsCount = m.setsCount)
    }

fun TrainPilotViewModel.loadMuscleLoadSummary(days: Int = progressPeriodDays(_uiState.value.selectedProgressRangeIndex)) {
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    val window = StudioProgressWindow(days)
    scope.launch(Dispatchers.IO) {
        try {
            val entries = db.workoutHistoryDao().getProgressMuscleWorkoutRows(window.fromEpoch(), window.toEpoch(), _uiState.value.language.name.lowercase())
            val targets = _uiState.value.exercises.associate { it.id to it.primaryMusclesHu }
            val byMuscle = entries.groupBy {
                NativeProgressQuery.detailedMuscleGroup(it.muscleKey, targets[it.exerciseId].orEmpty())
            }
            val ordered = listOf("chest", "back", "shoulders", "arms", "biceps", "triceps", "forearms",
                "core", "legs", "quads", "hamstrings", "glutes", "calves")
            val names = listOf("Mell", "Hát", "Váll", "Kar", "Bicepsz", "Tricepsz", "Alkar",
                "Törzs", "Láb", "Combfeszítő", "Combhajlító", "Far", "Vádli")
            val counts = byMuscle.mapValues { (_, rows) -> rows.sumOf { it.setsCount } }
            val largest = ordered.maxOf { counts[it] ?: 0 }.coerceAtLeast(1)
            val items = ordered.zip(names).mapNotNull { (key, name) ->
                val count = counts[key] ?: 0
                if (count == 0 && key !in setOf("chest", "back", "shoulders", "core", "legs")) return@mapNotNull null
                val ratio = count.toFloat() / largest
                com.example.ui.components.MuscleLoadItem(key, name, "", (ratio * 100).toInt(),
                    ratio, "", emptyList(), "", count, byMuscle[key].orEmpty())
            }
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    if (progressPeriodDays(state.selectedProgressRangeIndex) == days)
                        state.copy(realMuscleLoads = items, muscleLoadPeriodDays = days) else state
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load real muscle summary", e) }
    }
}

internal fun TrainPilotViewModel.loadWorkoutMuscleSummary(workoutId: String) {
    val db = nativeDatabase ?: return
    val scope = nativeScope ?: return
    scope.launch(Dispatchers.IO) {
        try {
            val items = muscleItems(db.workoutHistoryDao().getWorkoutMuscleSummary(workoutId))
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    if (state.selectedLogWorkoutId == workoutId) state.copy(logWorkoutMuscleLoads = items) else state
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load workout muscle summary", e) }
    }
}
