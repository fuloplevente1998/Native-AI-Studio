package com.example.ui.viewmodel

import com.example.ui.components.OperationFeedbackKind

import kotlinx.coroutines.sync.withLock
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
import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import java.util.Locale

// Program & Exercise Editor Actions
internal suspend fun TrainPilotViewModel.awaitStudioProgramWrites() {
    programWriteJob?.join()
    check(programWriteError == null) { requireNotNull(programWriteError) }
}

internal fun TrainPilotViewModel.mutateStudioLibrary(
    success: String,
    editor: Boolean = false,
    selectLastDay: Boolean = false,
    onCommitted: (TrainPilotUiState) -> TrainPilotUiState = { it },
    action: suspend (Context, TrainPilotDatabase, TrainPilotSettingsStore) -> String?
) {
    if (_uiState.value.libraryMutationBusy) return
    fun unavailable() { reportOperation(tr(
        "A mentés nem sikerült. Próbáld újra.", "Could not save. Please try again.",
        "Speichern fehlgeschlagen. Bitte erneut versuchen.", "Salvarea a eșuat. Încearcă din nou."), OperationFeedbackKind.FAILED) }
    val context = nativeContext ?: run { unavailable(); return }
    val db = nativeDatabase ?: run { unavailable(); return }
    val store = nativeSettingsStore ?: run { unavailable(); return }
    val scope = nativeScope ?: run { unavailable(); return }
    val ticket = try { com.repforge.app.NativeDataReplacement.continuationForCurrentData(context) }
        catch (_: Exception) { unavailable(); return }
    val editorTicket = if (editor) programEditorContinuation ?: ticket else null
    nativeReloadJob?.cancel()
    _uiState.update {
        it.copy(libraryMutationBusy = true, statusBannerMessage = null, operationBanner = null)
    }
    scope.launch {
        try {
            awaitStudioProgramWrites()
            val focus = withContext(Dispatchers.IO) {
                programWriteMutex.withLock {
                    com.repforge.app.NativeDataReplacement.mutate(context) {
                        ticket.requireCurrent(context)
                        editorTicket?.requireCurrent(context)
                        action(context, db, store)
                    }
                }
            }
            val loaded = withContext(Dispatchers.IO) {
                val rows = Triple(db.exerciseLibraryDao().getAllExercises(), db.programDao().getProgramsWithDays(),
                    com.repforge.app.NativeExerciseFavorites.load(db).toSet())
                rows to store.current().activeProgramId
            }
            ticket.requireCurrent(context)
            val rows = loaded.first
            val exercises = rows.first.map(::mapExerciseEntity)
            val programs = mapProgramRows(rows.second.map { it.program }, rows.second.flatMap { it.days },
                exercises.associateBy { it.id }, loaded.second)
            _uiState.update { state ->
                val editing = focus ?: state.editingProgramId
                val missing = programs.none { it.id == editing }
                onCommitted(state.copy(exercises = exercises, programs = programs, favoriteExerciseIds = rows.third,
                    editingProgramId = if (missing) programs.firstOrNull()?.id.orEmpty() else editing,
                    programsTabCustomOnly = if (focus != null && focus != state.editingProgramId &&
                        programs.any { it.id == focus && it.isCustom }) true else state.programsTabCustomOnly,
                    editingDayIndex = if (selectLastDay) (programs.firstOrNull { it.id == focus }?.days?.lastIndex ?: 0).coerceAtLeast(0)
                        else if (focus != null) state.editingDayIndex.coerceIn(0,
                        (programs.firstOrNull { it.id == focus }?.days?.lastIndex ?: 0).coerceAtLeast(0)) else state.editingDayIndex,
                    currentSubScreen = if (missing && state.currentSubScreen == SubScreen.PROGRAM_EDITOR)
                        SubScreen.PROGRAMS_LIST else state.currentSubScreen,
                    expandedEditorExerciseId = null,
                    inlineEditorProgramId = state.inlineEditorProgramId?.let { open ->
                        when {
                            missing -> null
                            open == state.editingProgramId -> editing
                            programs.none { it.id == open } -> null
                            else -> open
                        }
                    }
                ).withOperationBanner(success, OperationFeedbackKind.SUCCESS))
            }
        } catch (error: Exception) {
            // A closed Room database cancels its own query scope. Report that failure while
            // preserving cancellation of this screen's actual mutation/lifecycle scope.
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            val detail = error.message.orEmpty()
            val lang = _uiState.value.language
            val message = when {
                detail.startsWith("Exercise is used", ignoreCase = true) -> tr(
                    "A programban vagy naplóban használt gyakorlat nem törölhető.",
                    "An exercise used in a program or log cannot be deleted.",
                    "Eine in einem Programm oder Protokoll verwendete Übung kann nicht gelöscht werden.",
                    "Un exercițiu utilizat într-un program sau jurnal nu poate fi șters.",
                    lang
                )
                detail.contains("Program missing", true) -> tr(
                    "A kiválasztott program már nem található.",
                    "Selected program was not found.",
                    "Das ausgewählte Programm wurde nicht gefunden.",
                    "Programul selectat nu a mai fost găsit.",
                    lang
                )
                detail.contains("Deactivate", true) -> tr(
                    "Törlés előtt válassz másik aktív programot.",
                    "Select another active program before deleting.",
                    "Wähle vor dem Löschen ein anderes aktives Programm.",
                    "Selectează alt program activ înainte de ștergere.",
                    lang
                )
                detail.contains("active", true) || detail.contains("transferring", true) -> tr(
                    "Előbb fejezd be az aktív vagy átadás alatt lévő edzést. Törlés előtt válassz másik aktív programot.",
                    "Finish the active or transferring workout first. Select another active program before deleting.",
                    "Beende zuerst das aktive oder in Übergabe befindliche Training. Wähle vor dem Löschen ein anderes aktives Programm.",
                    "Finalizează mai întâi antrenamentul activ sau în curs de predare. Selectează alt program activ înainte de ștergere.",
                    lang
                )
                detail.contains("planned calendar workouts", true) ->
                    com.example.localization.CalendarFeedback.plannedProgram(lang)
                detail.contains("history", true) || detail.contains("calendar", true) -> tr(
                    "A naplóban vagy a naptárban használt program/nap nem törölhető.",
                    "A program/day used in history or calendar cannot be deleted.",
                    "Ein im Protokoll oder Kalender verwendetes Programm bzw. ein Tag kann nicht gelöscht werden.",
                    "Un program sau o zi utilizată în jurnal sau calendar nu poate fi ștearsă.",
                    lang
                )
                detail.contains("Built-in", true) -> tr(
                    "A beépített sablon nem törölhető; készíts saját másolatot.",
                    "Built-in templates cannot be deleted; create a custom copy.",
                    "Standardvorlagen können nicht gelöscht werden; erstelle eine eigene Kopie.",
                    "Șabloanele predefinite nu pot fi șterse; creează o copie personalizată.",
                    lang
                )
                detail.contains("changed", true) || detail.contains("replacement", true) -> tr(
                    "Az adatok megváltoztak. Nyisd meg újra a szerkesztőt.",
                    "Data changed. Reopen the editor.",
                    "Die Daten haben sich geändert. Öffne den Editor erneut.",
                    "Datele s-au modificat. Redeschide editorul.",
                    lang
                )
                else -> tr(
                    "A művelet nem sikerült. Ellenőrizd a megadott adatokat és nyisd meg újra.",
                    "Operation failed. Check your input and reopen.",
                    "Vorgang fehlgeschlagen. Prüfe die Eingaben und öffne erneut.",
                    "Operațiunea a eșuat. Verifică datele și redeschide.",
                    lang
                )
            }
            reportOperation(message, OperationFeedbackKind.FAILED)
        } finally { _uiState.update { it.copy(libraryMutationBusy = false) } }
    }
}

internal fun TrainPilotViewModel.editStudioProgram(
    success: String,
    selectLastDay: Boolean = false,
    change: (com.repforge.app.NativeProgramEditor.Draft, String?) -> com.repforge.app.NativeProgramEditor.Draft
) {
    val state = _uiState.value
    val programId = state.editingProgramId
    val dayKey = state.programs.firstOrNull { it.id == programId }?.days?.getOrNull(state.editingDayIndex)?.id
    val language = state.language.name.lowercase(java.util.Locale.ROOT)
    mutateStudioLibrary(success, editor = true, selectLastDay = selectLastDay) { context, db, store ->
        val opened = db.programDao().getProgramWithDaysById(programId)
            ?: throw IllegalStateException("Program missing: $programId")
        val rawDayId = opened.days.firstOrNull { it.compositeId == dayKey || it.dayId == dayKey }?.dayId
        val draft = change(com.repforge.app.NativeProgramEditor.from(opened, language), rawDayId)
        com.repforge.app.NativeEditedProgramSave.save(context, db, store, opened, draft, language)
    }
}

/** Rename from the program quick-action menu (the inline editor has no name field). */
fun TrainPilotViewModel.renameProgram(programId: String, name: String) {
    val state = _uiState.value
    if (name.isBlank() || state.programs.none { it.id == programId }) return
    if (state.editingProgramId != programId) {
        if (state.inlineEditorProgramId != null && state.inlineEditorProgramId != programId) collapseInlineProgramEditor()
        programEditorContinuation = nativeContext?.let {
            runCatching { com.repforge.app.NativeDataReplacement.continuationForCurrentData(it) }.getOrNull()
        }
        _uiState.update { it.copy(editingProgramId = programId, editingDayIndex = 0) }
    }
    renameEditorProgram(name)
}

fun TrainPilotViewModel.renameEditorProgram(name: String) = editStudioProgram(
    tr("Programnév elmentve.", "Program name saved.", "Programmname gespeichert.", "Numele programului a fost salvat.")
) { draft, _ ->
    draft.copy(name = name.trim())
}

fun TrainPilotViewModel.saveEditorProgram(name: String, changes: List<com.example.model.StudioProgramPrescriptionDraft>) {
    val state = _uiState.value
    val programId = state.editingProgramId
    val language = state.language.name.lowercase(java.util.Locale.ROOT)
    mutateStudioLibrary(tr("Program elmentve.", "Program saved.", "Programm gespeichert.", "Program salvat."),
        editor = true) { context, db, store ->
        val opened = db.programDao().getProgramWithDaysById(programId)
            ?: throw IllegalStateException("Program missing: $programId")
        com.repforge.app.NativeEditedProgramSave.save(context, db, store, opened,
            com.repforge.app.StudioProgramSave.draft(opened, language, name, changes), language)
    }
}

fun TrainPilotViewModel.copyProgram(programId: String, fromEditor: Boolean = false) {
    val state = _uiState.value
    val copySuffix = tr(" – másolat", " – copy", " – Kopie", " – copie", state.language)
    mutateStudioLibrary(
        tr(
            "Saját programmásolat elkészült. Aktiválás a programlistában.",
            "Custom program copy created. Activate it in the program list.",
            "Eigene Programmkopie erstellt. In der Programmliste aktivieren.",
            "Copia personalizată a programului a fost creată. Activeaz-o în listă.",
            state.language
        ),
        editor = fromEditor
    ) { context, db, _ ->
        val opened = db.programDao().getProgramWithDaysById(programId)
            ?: throw IllegalStateException("Program missing: $programId")
        val language = state.language.name.lowercase(java.util.Locale.ROOT)
        val draft = com.repforge.app.NativeProgramEditor.from(opened, language)
        com.repforge.app.NativeProgramEditor.save(context, db, opened,
            draft.copy(name = (draft.name.take(107) + copySuffix)), language, clone = true)
    }
}

fun TrainPilotViewModel.copyEditorProgram() = copyProgram(_uiState.value.editingProgramId, fromEditor = true)

fun TrainPilotViewModel.deleteProgram(programId: String, fromEditor: Boolean = false) {
    mutateStudioLibrary(
        tr(
            "Program végleg törölve a tárból.",
            "Program permanently deleted.",
            "Programm dauerhaft gelöscht.",
            "Program șters definitiv."
        ),
        editor = fromEditor
    ) { context, db, store ->
        val opened = db.programDao().getProgramWithDaysById(programId)
            ?: throw IllegalStateException("Program missing: $programId")
        com.repforge.app.NativeProgramEditor.delete(context, db, opened, store)
        null
    }
}

fun TrainPilotViewModel.deleteEditorProgram() = deleteProgram(_uiState.value.editingProgramId, fromEditor = true)

fun TrainPilotViewModel.addEditorDay(name: String) = editStudioProgram(
    tr("Edzésnap hozzáadva.", "Workout day added.", "Trainingstag hinzugefügt.", "Zi de antrenament adăugată."),
    selectLastDay = true
) { draft, _ ->
    com.repforge.app.NativeProgramEditor.addDay(draft, name.trim())
}

fun TrainPilotViewModel.deleteEditorDay() = editStudioProgram(
    tr("Edzésnap törölve.", "Workout day deleted.", "Trainingstag gelöscht.", "Zi de antrenament ștearsă.")
) { draft, dayId ->
    require(dayId != null && draft.days.size > 1)
    draft.copy(days = draft.days.filterNot { it.id == dayId })
}

fun TrainPilotViewModel.removeEditorExercise(id: String) = editStudioProgram(
    tr(
        "Gyakorlat eltávolítva az edzésnapból.",
        "Exercise removed from workout day.",
        "Übung aus dem Trainingstag entfernt.",
        "Exercițiu eliminat din ziua de antrenament."
    )
) { draft, dayId ->
    require(dayId != null)
    draft.copy(days = draft.days.map { day -> if (day.id != dayId) day else day.copy(
        entries = day.entries.filterNot { com.repforge.app.NativeProgramEditor.exerciseId(it) == id }) })
}

fun TrainPilotViewModel.toggleExerciseFavorite(id: String) = mutateStudioLibrary(
    tr("Kedvencek frissítve.", "Favorites updated.", "Favoriten aktualisiert.", "Favorite actualizate.")
) { context, db, _ ->
    com.repforge.app.NativeExerciseFavorites.toggle(context, db, id)
    null
}

fun TrainPilotViewModel.toggleFavoriteExercisesOnly() {
    _uiState.update { it.copy(favoriteExercisesOnly = !it.favoriteExercisesOnly) }
}

fun TrainPilotViewModel.setProgramsTabCustomOnly(customOnly: Boolean) {
    _uiState.update { it.copy(programsTabCustomOnly = customOnly) }
}

fun TrainPilotViewModel.activateProgram(programId: String) {
    if (_uiState.value.libraryMutationBusy) return
    if (_uiState.value.programs.none { it.id == programId }) {
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
    _uiState.update { state ->
        val updated = state.programs.map { prog ->
            prog.copy(isActive = prog.id == programId)
        }
        state.copy(
            programs = updated,
            editingProgramId = programId,
            statusBannerMessage = tr(
                "Aktív program frissítve",
                "Active program updated",
                "Aktives Programm aktualisiert",
                "Program activ actualizat",
                state.language
            )
        )
    }
    persistProgramsToRoomAsync(activatedProgramId = programId)
}
