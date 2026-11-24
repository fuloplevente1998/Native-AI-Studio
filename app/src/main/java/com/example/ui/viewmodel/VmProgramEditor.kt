package com.example.ui.viewmodel

import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.MainTab
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.flow.map
import java.util.Locale

/** Legacy entry point: the separate editor page is retired; edit inline in the Programs list. */
fun TrainPilotViewModel.openProgramEditor(programId: String, dayIndex: Int = 0) {
    if (_uiState.value.currentTab != MainTab.PROGRAMS || _uiState.value.currentSubScreen != SubScreen.NONE) {
        selectTab(MainTab.PROGRAMS)
    }
    expandInlineProgramEditor(programId, dayIndex, INLINE_EDITOR_HOST_PROGRAMS)
}

/** Tap on a program card: expand its inline editor, or collapse it when already open. */
fun TrainPilotViewModel.toggleInlineProgramEditor(programId: String, dayIndex: Int = 0, host: String = INLINE_EDITOR_HOST_PROGRAMS) {
    val state = _uiState.value
    if (state.inlineEditorProgramId == programId && state.inlineEditorHost == host) {
        collapseInlineProgramEditor()
    } else {
        expandInlineProgramEditor(programId, dayIndex, host)
    }
}

fun TrainPilotViewModel.collapseInlineProgramEditor() {
    _uiState.update { it.copy(inlineEditorProgramId = null, inlineEditorHost = "", expandedEditorExerciseId = null) }
}

internal fun TrainPilotViewModel.expandInlineProgramEditor(programId: String, dayIndex: Int, host: String) {
    val currentPrograms = _uiState.value.programs
    if (programId.isNotBlank() && currentPrograms.isNotEmpty() && currentPrograms.none { it.id == programId }) {
        _uiState.update { state ->
            state.copy(
                statusBannerMessage = tr(
                    "A kiválasztott program már nem található.",
                    "Selected program was not found.",
                    "Das ausgewählte Programm wurde nicht gefunden.",
                    "Programul selectat nu a mai fost găsit.",
                    state.language
                )
            )
        }
        return
    }
    val resolvedId = programId.ifBlank { currentPrograms.firstOrNull { it.isActive }?.id ?: currentPrograms.firstOrNull()?.id.orEmpty() }
    if (resolvedId.isBlank()) return
    programEditorContinuation = nativeContext?.let {
        runCatching { com.repforge.app.NativeDataReplacement.continuationForCurrentData(it) }.getOrNull()
    }
    _uiState.update {
        it.copy(
            editingProgramId = resolvedId,
            editingDayIndex = dayIndex,
            expandedEditorExerciseId = null,
            inlineEditorProgramId = resolvedId,
            inlineEditorHost = host
        )
    }
}

fun TrainPilotViewModel.selectEditingDayIndex(dayIndex: Int) {
    _uiState.update { it.copy(editingDayIndex = dayIndex) }
}

fun TrainPilotViewModel.toggleExpandedEditorExercise(exerciseId: String) {
    _uiState.update {
        it.copy(
            expandedEditorExerciseId = if (it.expandedEditorExerciseId == exerciseId) null else exerciseId
        )
    }
}

fun TrainPilotViewModel.updateProgramExercisePrescription(
    exerciseId: String,
    sets: Int,
    reps: Int,
    weightKg: Float,
    restSec: Int
) {
    if (_uiState.value.libraryMutationBusy) return
    _uiState.update { state ->
        val updatedPrograms = state.programs.map { prog ->
            if (prog.id != state.editingProgramId) prog
            else {
                val updatedDays = prog.days.mapIndexed { idx, day ->
                    if (idx != state.editingDayIndex) day
                    else {
                        day.copy(
                            exercises = day.exercises.map { ex ->
                                if (ex.id != exerciseId) ex
                                else ex.copy(
                                    defaultSets = sets.coerceIn(1, 10),
                                    defaultReps = reps.coerceIn(1, 50), targetRepsText = reps.coerceIn(1, 50).toString(),
                                    defaultWeightKg = weightKg.coerceAtLeast(0f),
                                    defaultRestSec = restSec.coerceIn(15, 300)
                                )
                            }
                        )
                    }
                }
                prog.copy(days = updatedDays)
            }
        }
        state.copy(
            programs = updatedPrograms,
            expandedEditorExerciseId = null,
            statusBannerMessage = tr(
                "Gyakorlat beállításai elmentve",
                "Exercise settings saved",
                "Übungseinstellungen gespeichert",
                "Setările exercițiului au fost salvate",
                state.language
            )
        )
    }
    persistProgramsToRoomAsync()
}

fun TrainPilotViewModel.openExerciseLibraryForProgramDay() {
    navigateToSubScreen(SubScreen.EXERCISE_LIBRARY)
    libraryAddsToProgramDay = _uiState.value.currentSubScreen == SubScreen.EXERCISE_LIBRARY
}

/** Library/detail "+" inside the editor flow: same copy-on-write save as every other editor change. */
fun TrainPilotViewModel.addExerciseToActiveProgramDay(exercise: ExerciseItem) {
    if (_uiState.value.libraryMutationBusy || !libraryAddsToProgramDay) return
    editStudioProgram(tr(
        "${exercise.nameHu} hozzáadva az edzésnaphoz",
        "${exercise.localizedName(AppLanguage.EN)} added to workout day",
        "${exercise.localizedName(AppLanguage.DE)} zum Trainingstag hinzugefügt",
        "${exercise.localizedName(AppLanguage.RO)} adăugat în ziua de antrenament"
    )) { draft, dayId ->
        require(dayId != null)
        draft.copy(days = draft.days.map { day ->
            if (day.id != dayId || day.entries.any { com.repforge.app.NativeProgramEditor.exerciseId(it) == exercise.id }) day
            else com.repforge.app.NativeProgramEditor.addExercise(day, exercise.id)
        })
    }
}

fun TrainPilotViewModel.moveEditorExercise(fromIndex: Int, toIndex: Int) {
    if (_uiState.value.libraryMutationBusy) return
    _uiState.update { state ->
        val updatedPrograms = state.programs.map { prog ->
            if (prog.id != state.editingProgramId) prog
            else {
                val updatedDays = prog.days.mapIndexed { idx, day ->
                    if (idx != state.editingDayIndex) day
                    else {
                        val list = day.exercises.toMutableList()
                        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                            val moved = list.removeAt(fromIndex)
                            list.add(toIndex, moved)
                        }
                        day.copy(exercises = list)
                    }
                }
                prog.copy(days = updatedDays)
            }
        }
        state.copy(
            programs = updatedPrograms,
            statusBannerMessage = tr(
                "Gyakorlatsorrend frissítve",
                "Exercise order updated",
                "Übungsreihenfolge aktualisiert",
                "Ordinea exercițiilor a fost actualizată",
                state.language
            )
        )
    }
    persistProgramsToRoomAsync()
}

fun TrainPilotViewModel.moveProgramOrder(fromIndex: Int, toIndex: Int) {
    if (_uiState.value.libraryMutationBusy) return
    _uiState.update { state ->
        val visiblePrograms = if (state.programsTabCustomOnly) {
            state.programs.filter { it.isCustom }
        } else {
            state.programs.filter { !it.isCustom }
        }
        if (fromIndex !in visiblePrograms.indices || toIndex !in visiblePrograms.indices || fromIndex == toIndex) {
            return@update state
        }
        val reorderedVisible = visiblePrograms.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
        var visibleCursor = 0
        val merged = state.programs.map { prog ->
            val belongsToCurrentTab = if (state.programsTabCustomOnly) prog.isCustom else !prog.isCustom
            if (belongsToCurrentTab) {
                reorderedVisible[visibleCursor++]
            } else {
                prog
            }
        }
        state.copy(
            programs = merged,
            statusBannerMessage = tr(
                "Programok sorrendje frissítve",
                "Program order updated",
                "Programmreihenfolge aktualisiert",
                "Ordinea programelor a fost actualizată",
                state.language
            )
        )
    }
    persistProgramsToRoomAsync()
}

fun TrainPilotViewModel.createNewCustomProgram(nameHu: String, subtitleHu: String) {
    val origin = _uiState.value
    if (origin.libraryMutationBusy) return
    val name = nameHu.trim().ifBlank { "Egyedi edzésterv" }
    val description = subtitleHu.trim().ifBlank { "Saját összeállítású program" }
    val starterA = origin.exercises.take(4).map { it.id }
    val starterB = origin.exercises.drop(4).take(4).map { it.id }
    mutateStudioLibrary(tr(
        "Új egyedi program elmentve: $name", "Custom program saved: $name",
        "Eigenes Programm gespeichert: $name", "Program personalizat salvat: $name"),
        onCommitted = { updated ->
            if (updated.currentTab != origin.currentTab || updated.currentSubScreen != origin.currentSubScreen) updated
            else updated.copy(editingDayIndex = 0, currentSubScreen = SubScreen.PROGRAM_EDITOR,
                backStack = (updated.backStack + origin.navigationRoute()).takeLast(16))
        }) { context, db, store ->
        com.repforge.app.NativeCustomLibraryMutation.createProgram(context, db, store,
            name, description, origin.language.name.lowercase(Locale.ROOT), starterA, starterB,
            com.repforge.app.NativeDataReplacement.continuationForCurrentData(context))
    }
}
