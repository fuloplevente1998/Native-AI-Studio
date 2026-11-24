package com.example.ui.viewmodel

import kotlinx.coroutines.sync.withLock
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun TrainPilotViewModel.studioModesText(hu: String, en: String, de: String, ro: String): String =
    when (_uiState.value.language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }

internal fun TrainPilotViewModel.persistStudioTimerSettings(rest: Int? = null, sound: Boolean? = null, vibration: Boolean? = null) {
    val store = nativeSettingsStore ?: return
    val context = nativeContext ?: return
    val scope = nativeScope ?: return
    val ticket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
    scope.launch(Dispatchers.IO) {
        try {
            studioTimerWriteMutex.withLock { NativeDataReplacement.mutate(context) {
                ticket.requireCurrent(context)
                store.updateTimerPreferences(restSeconds = rest, sound = sound, vibration = vibration)
            } }
        } catch (error: Exception) {
            if (error is CancellationException) currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(statusBannerMessage = studioModesText(
                "Az időzítő beállítása nem menthető.", "Could not save timer settings.",
                "Timereinstellungen konnten nicht gespeichert werden.", "Setările temporizatorului nu au putut fi salvate.")) }
        }
    }
}

fun TrainPilotViewModel.toggleSoundAlert(enabled: Boolean) {
    _uiState.update { it.copy(soundAlertEnabled = enabled) }
    persistStudioTimerSettings(sound = enabled)
}

fun TrainPilotViewModel.cycleDefaultRestTimer() {
    _uiState.update {
        val next = when (it.defaultRestSec) {
            60 -> 90
            90 -> 120
            120 -> 180
            else -> 60
        }
        it.copy(defaultRestSec = next, statusBannerMessage = studioModesText(
            "Alapértelmezett pihenőidő: $next mp", "Default rest time: $next sec",
            "Standard-Pausenzeit: $next Sek.", "Timp implicit de pauză: $next sec"))
    }
    persistStudioTimerSettings(rest = _uiState.value.defaultRestSec)
}

fun TrainPilotViewModel.toggleProgressionModal(show: Boolean) {
    studioModeGeneration++
    pendingStudioMode = null
    pendingStudioComparison = null
    _uiState.update {
        it.copy(showProgressionModal = show, progressionModeReview = null,
            progressionModeBusy = false)
    }
}

fun TrainPilotViewModel.discardProgressionPreview() {
    studioModeGeneration++
    pendingStudioMode = null
    pendingStudioComparison = null
    _uiState.update { it.copy(progressionModeReview = null, progressionModeBusy = false) }
}

/** No writes in preview: approval uses exactly this prepared DataStore snapshot. */
fun TrainPilotViewModel.previewProgressionSettings(style: String, rule: String, scopeName: String, inherit: Boolean) {
    val store = nativeSettingsStore ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.progressionModeBusy || !_uiState.value.showProgressionModal) return
    if (scopeName !in setOf("global", "program") || style !in setOf("normal", "hit") ||
        rule !in setOf("double", "linear", "rir") || (inherit && scopeName != "program")) return
    val token = ++studioModeGeneration
    pendingStudioMode = null
    pendingStudioComparison = null
    _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
    scope.launch {
        try {
            val saved = store.current()
            val mode = com.repforge.app.data.store.NativeTrainingModeChange
            val proposal = if (inherit) {
                mode.prepareInherit(saved.trainingModes197Json, saved.activeProgramId, saved.activeProgramId)
            } else {
                mode.prepare(saved.trainingModes197Json, saved.activeProgramId, scopeName,
                    if (scopeName == "program") saved.activeProgramId else null, style, rule)
            }
            val snapshot = withContext(Dispatchers.IO) {
                com.repforge.app.ui.settings.NativeTrainingModeComparison.read(
                    requireNotNull(nativeContext), proposal)
            }
            if (studioModeGeneration != token || !_uiState.value.showProgressionModal) return@launch
            pendingStudioMode = proposal
            pendingStudioComparison = snapshot
            _uiState.update { it.copy(
                progressionModeBusy = false,
                progressionModeReview = StudioTrainingModeReview(
                    beforeStyle = proposal.before.trainingStyle,
                    beforeProgression = proposal.before.progressionMode,
                    afterStyle = proposal.after.trainingStyle,
                    afterProgression = proposal.after.progressionMode,
                    inheritGlobal = proposal.inheritGlobal,
                    programName = snapshot.programName,
                    rows = snapshot.rows.map { row ->
                        StudioTrainingModeRow(
                            day = row.day, exercise = row.exercise,
                            beforeSets = row.beforeSets, afterSets = row.afterSets,
                            beforeReps = row.beforeReps, afterReps = row.afterReps,
                            restSeconds = row.restSeconds,
                            beforeWeight = row.beforeWeight, afterWeight = row.afterWeight,
                            rir = row.rir, reason = row.reason)
                    })
            ) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (studioModeGeneration == token) _uiState.update { it.copy(
                progressionModeBusy = false,
                statusBannerMessage = studioModesText(
                    "Nem sikerült elkészíteni a program-összehasonlítást.",
                    "Could not prepare the program comparison.",
                    "Programmvergleich konnte nicht erstellt werden.",
                    "Nu s-a putut pregăti comparația programului.")) }
        }
    }
}

fun TrainPilotViewModel.approveProgressionSettings() {
    val proposal = pendingStudioMode ?: return
    val comparison = pendingStudioComparison ?: return
    val context = nativeContext ?: return
    val store = nativeSettingsStore ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.progressionModeBusy || _uiState.value.progressionModeReview == null) return
    val token = ++studioModeGeneration
    pendingStudioMode = null
    pendingStudioComparison = null
    _uiState.update { it.copy(progressionModeBusy = true) }
    scope.launch {
        try {
            check(withContext(Dispatchers.IO) {
                com.repforge.app.ui.settings.NativeTrainingModeComparison.isCurrent(
                    context, proposal, comparison)
            }) { "Az előnézetben használt program időközben megváltozott." }
            check(com.repforge.app.data.store.NativeTrainingModeChange.approve(store, proposal)) {
                "A beállítás időközben módosult. Nyisd meg újra az előnézetet."
            }
            if (studioModeGeneration == token) _uiState.update { it.copy(
                showProgressionModal = false, progressionModeReview = null,
                progressionModeBusy = false, statusBannerMessage = studioModesText(
                    "Edzésmód elmentve.", "Training mode saved.",
                    "Trainingsmodus gespeichert.", "Mod de antrenament salvat.")) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (studioModeGeneration == token) _uiState.update { it.copy(
                progressionModeReview = null, progressionModeBusy = false,
                statusBannerMessage = if (error.message?.contains("Finish the active workout") == true)
                    studioModesText(
                        "Az edzésmód csak az aktív edzés befejezése után módosítható.",
                        "Finish the active workout before changing training mode.",
                        "Trainingsmodus erst nach Abschluss des aktiven Trainings ändern.",
                        "Finalizează antrenamentul activ înainte de schimbarea modului.")
                else studioModesText(
                    "Az edzésmód nem menthető. Nyisd meg újra az előnézetet.",
                    "Could not save the training mode. Reopen the preview.",
                    "Trainingsmodus konnte nicht gespeichert werden. Vorschau erneut öffnen.",
                    "Modul de antrenament nu a putut fi salvat. Redeschide previzualizarea.")) }
        }
    }
}

fun TrainPilotViewModel.undoProgressionSettings(scopeName: String) {
    val store = nativeSettingsStore ?: return
    val scope = nativeScope ?: return
    if (scopeName !in setOf("global", "program") || _uiState.value.progressionModeBusy) return
    val token = ++studioModeGeneration
    pendingStudioMode = null
    pendingStudioComparison = null
    _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
    scope.launch {
        try {
            val saved = store.current()
            check(com.repforge.app.data.store.NativeTrainingModeChange.undo(store, scopeName,
                if (scopeName == "program") saved.activeProgramId else null)) {
                "Nincs visszavonható módváltás."
            }
            if (studioModeGeneration == token) _uiState.update { it.copy(
                progressionModeBusy = false, showProgressionModal = false,
                statusBannerMessage = studioModesText(
                    "Az előző edzésmód visszaállítva.", "Previous training mode restored.",
                    "Vorheriger Trainingsmodus wiederhergestellt.",
                    "Modul de antrenament anterior a fost restaurat.")) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (studioModeGeneration == token) _uiState.update { it.copy(
                progressionModeBusy = false,
                statusBannerMessage = studioModesText(
                    "A módváltás nem vonható vissza.", "Could not undo training mode change.",
                    "Trainingsmoduswechsel konnte nicht rückgängig gemacht werden.",
                    "Schimbarea modului nu a putut fi anulată.")) }
        }
    }
}

fun TrainPilotViewModel.toggleVibrationAlert(enabled: Boolean) {
    _uiState.update { it.copy(vibrationEnabled = enabled) }
    persistStudioTimerSettings(vibration = enabled)
}
