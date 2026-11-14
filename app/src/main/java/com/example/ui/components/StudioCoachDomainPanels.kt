package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.coach.*
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.trainPilotBorder
import androidx.compose.foundation.shape.RoundedCornerShape

private fun coachTr(lang: String, hu: String, en: String, de: String, ro: String): String =
    when (lang.lowercase().take(2)) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

/** Studio presentation of the canonical Coach; no local advice formula or silent prescription edits. */
@Composable
internal fun StudioCoachRecommendations(
    state: CoachUiState,
    onStart: (String, String, String?) -> Unit,
    onWorkout: () -> Unit,
    onExercise: (String) -> Unit,
    onPreviewVariation: (String) -> Unit,
    onCloseVariation: () -> Unit,
    onSaveVariation: () -> Unit,
    showTarget: Boolean = true
) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language.ifBlank { com.example.localization.LocalTrainPilotStrings.current.language.code }
    val text = nativeResources(LocalContext.current, lang)
    state.variationPreview?.let { preview ->
        AlertDialog(onDismissRequest = onCloseVariation,
            title = { Text(coachTr(lang, "Gyakorlatváltozat előnézete", "Exercise Variation Preview", "Vorschau der Übungsvariante", "Previzualizare variantă exercițiu")) },
            text = { Text(NativeCoachAdviceText.variation(text, preview.next)) },
            confirmButton = { com.repforge.app.ui.theme.TrainPilotSaveButton(onClick = onSaveVariation, enabled = !state.variationBusy,
                modifier = Modifier.testTag("studio_coach_variation_confirm")) { Text(coachTr(lang, "Változat mentése", "Save Variation", "Variante speichern", "Salvează varianta")) } },
            dismissButton = { TextButton(onClick = onCloseVariation, enabled = !state.variationBusy) { Text(coachTr(lang, "Mégse", "Cancel", "Abbrechen", "Anulează")) } })
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.initialLoading) CircularProgressIndicator(Modifier.size(24.dp).testTag("studio_coach_initial_loading"))
        if (state.error) Text(coachTr(lang, "A Coach adatai nem frissíthetők. A korábbi kijelzés nem friss mérés.", "Coach data could not be refreshed. Previous values are not a fresh measurement.", "Coach-Daten konnten nicht aktualisiert werden.", "Datele Coach nu pot fi actualizate."), color = palette.danger)
        if (showTarget) StudioCoachNextTrainingCard(state, onStart, onWorkout)
        state.exercises.forEach { row ->
            Surface(color = palette.surfaceCard, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("studio_coach_advice_${row.id}")
                    .trainPilotBorder(0.75.dp, palette.cardBorder, RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary,
                        modifier = Modifier.clickable { onExercise(row.id) })
                    Text(NativeCoachAdviceText.label(text, row.advice.action), color = palette.primary)
                    Text(NativeCoachAdviceText.reason(text, row.advice.reason), color = palette.textSecondary)
                    Text(coachTr(lang, "Megfelelő előzmény: ${row.advice.historyCount}", "Matching history: ${row.advice.historyCount}", "Passender Verlauf: ${row.advice.historyCount}", "Istoric relevant: ${row.advice.historyCount}"), color = palette.textSecondary)
                    if (row.advice.target.text.isNotBlank()) Text("${coachTr(lang, "Cél", "Target", "Ziel", "Țintă")}: ${row.advice.target.text}", color = palette.textSecondary)
                    if (row.advice.action in setOf("increase", "reduce", "start") && row.advice.weight > 0)
                        Text("${coachTr(lang, "Javasolt súly", "Suggested weight", "Empfohlenes Gewicht", "Greutate recomandată")}: ${row.advice.weight} kg" + when (row.loadType) {
                            "per_hand" -> coachTr(lang, " kézenként", " per hand", " pro Hand", " pe mână")
                            "total" -> coachTr(lang, " összesen", " total", " gesamt", " total")
                            "single_dumbbell" -> coachTr(lang, " egy súlyzóval", " single dumbbell", " mit einer Kurzhantel", " cu o ganteră")
                            else -> ""
                        }, color = palette.textPrimary)
                    if (row.advice.action == "reps" && row.advice.nextReps.isNotEmpty())
                        Text("${coachTr(lang, "Javasolt ismétlések", "Suggested reps", "Empfohlene Wdh.", "Repetări recomandate")}: ${row.advice.nextReps.joinToString(" / ")}", color = palette.textPrimary)
                    row.modePreview?.let { preview ->
                        val style = if (preview.trainingStyle == "hit") coachTr(lang, "HIT-inspirált", "HIT-inspired", "HIT-inspiriert", "Inspirat HIT") else coachTr(lang, "Normál", "Normal", "Normal", "Normal")
                        val mode = when (preview.mode) {
                            "linear" -> coachTr(lang, "Lineáris", "Linear", "Linear", "Liniar")
                            "rir" -> "RIR/Coach"
                            else -> coachTr(lang, "Dupla progresszió", "Double progression", "Doppelte Progression", "Progresie dublă")
                        }
                        Text("$style / $mode • ${preview.workingSets} ${coachTr(lang, "munkasorozat", "working sets", "Arbeitssätze", "serii de lucru")}", color = palette.textSecondary)
                    }
                    if (row.advice.action == "variation") TextButton(onClick = { onPreviewVariation(row.id) },
                        enabled = !state.variationBusy && !state.loading && !state.error) { Text(coachTr(lang, "Változat előnézete", "Preview Variation", "Variante ansehen", "Previzualizează varianta")) }
                }
            }
        }
        if (state.variationFailed) Text(coachTr(lang, "A gyakorlatváltozat nem menthető; frissítsd az adatokat.", "Could not save exercise variation; refresh data.", "Übungsvariante konnte nicht gespeichert werden.", "Varianta nu a putut fi salvată."), color = palette.danger)
        if (state.variationSaved) Text(coachTr(lang, "A gyakorlatváltozat elmentve.", "Exercise variation saved.", "Übungsvariante gespeichert.", "Varianta a fost salvată."), color = palette.success)
    }
}

/** The next real program day is the primary Coach action, not a secondary status tile. */
@Composable
internal fun StudioCoachNextTrainingCard(
    state: CoachUiState,
    onStart: (String, String, String?) -> Unit,
    onWorkout: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language.ifBlank { com.example.localization.LocalTrainPilotStrings.current.language.code }
    val target = state.target
    if (target == null) {
        if (!state.loading && !state.error) {
            Text(coachTr(lang, "Nincs elérhető programnap. Válassz aktív programot.", "No program day available. Select an active program.", "Kein Programmtag verfügbar. Wähle ein aktives Programm.", "Nicio zi de program disponibilă. Alege un program activ."), color = palette.textSecondary)
        }
        return
    }
    // Do not invent an A/B badge from a UUID; prefer the actual day title.
    val letterDay = target.title.trim().takeIf { it.length in 1..2 }
        ?: target.dayId.uppercase().takeIf { it in setOf("A", "B", "C", "D") }
    val shape = RoundedCornerShape(18.dp)
    Surface(color = palette.tintedSurface(0.12f), shape = shape,
        modifier = Modifier.fillMaxWidth().testTag("studio_coach_target")
            .trainPilotSoftGlow(palette.primary, shape, 4.dp)
            .trainPilotBorder(2.dp, palette.primary.copy(alpha = 0.85f), shape)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = palette.tintedSurface(0.20f), shape = RoundedCornerShape(50.dp)) {
                    Text(coachTr(lang, "KÖVETKEZŐ EDZÉSNAP", "NEXT WORKOUT DAY", "NÄCHSTER TRAININGSTAG", "URMĂTOAREA ZI DE ANTRENAMENT"), color = palette.primary,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                if (target.upcoming) Text(coachTr(lang, "TERVEZETT", "PLANNED", "GEPLANT", "PLANIFICAT"), color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(target.programTitle.ifBlank { coachTr(lang, "Következő edzés", "Next workout", "Nächstes Training", "Următorul antrenament") },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("studio_coach_program_name"))
                    Text(if (letterDay == null) target.title else "${coachTr(lang, "Edzésnap", "Workout day", "Trainingstag", "Zi antrenament")}: ${target.title}",
                        style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary,
                        maxLines = 2, modifier = Modifier.testTag("studio_coach_day_name"))
                    target.schedule?.let {
                        Text(it.date, style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary, modifier = Modifier.testTag("studio_coach_next_date"))
                    }
                }
                if (letterDay != null) {
                    Surface(color = palette.tintedSurface(0.16f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.trainPilotBorder(1.25.dp, palette.primary,
                            RoundedCornerShape(16.dp)).testTag("studio_coach_day_badge")) {
                        Box(Modifier.sizeIn(minWidth = 58.dp, minHeight = 58.dp).padding(8.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(letterDay, style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black, color = palette.primary)
                        }
                    }
                }
            }
            Button(enabled = !state.loading && !state.error,
                onClick = { onStart(target.programId, target.dayId, target.schedule?.id) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("studio_coach_start_target")) {
                Text(coachTr(lang, "Edzés indítása", "Start workout", "Training starten", "Începe antrenamentul"),
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Canonical read-only advice is available independently of explicit mode approval. */
@Composable
internal fun StudioWorkoutCoachAdvice(state: WorkoutCoachAdviceState, onReload: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language.code
    val text = nativeResources(LocalContext.current, lang)
    when {
        state.loading && state.row == null -> {
            CircularProgressIndicator(Modifier.size(24.dp).testTag("studio_active_coach_loading"))
            Text(coachTr(lang, "Előzmények betöltése…", "Loading history…", "Verlauf wird geladen…", "Se încarcă istoricul…"), color = palette.textSecondary)
        }
        state.error -> Text(coachTr(lang, "A Coach adatai nem olvashatók. Próbáld újra a frissítést.", "Coach data could not be read. Try refreshing again.", "Coach-Daten konnten nicht gelesen werden. Bitte erneut aktualisieren.", "Datele Coach nu pot fi citite. Încearcă din nou actualizarea."),
            color = palette.danger, modifier = Modifier.testTag("studio_active_coach_error"))
        state.row != null -> {
            val row = state.row
            Text(NativeCoachAdviceText.label(text, row.advice.action), color = palette.primary,
                modifier = Modifier.testTag("studio_active_coach_action"))
            Text(NativeCoachAdviceText.reason(text, row.advice.reason), color = palette.textSecondary,
                modifier = Modifier.testTag("studio_active_coach_reason"))
            Text(coachTr(lang, "Megfelelő előzmény: ${row.advice.historyCount}", "Matching history: ${row.advice.historyCount}", "Passender Verlauf: ${row.advice.historyCount}", "Istoric relevant: ${row.advice.historyCount}"), color = palette.textSecondary)
            if (row.advice.historyCount == 0 && row.advice.reason != "distance" &&
                row.modePreview?.reason !in setOf("invalid-target", "non-strength-activity")) Text(coachTr(lang,
                "Az elmúlt 90 napból nincs megfelelő, összehasonlítható előzmény ehhez a gyakorlathoz.",
                "No matching, comparable history for this exercise in the last 90 days.",
                "Kein passender, vergleichbarer Verlauf für diese Übung in den letzten 90 Tagen.",
                "Nu există istoric comparabil pentru acest exercițiu în ultimele 90 de zile."),
                color = palette.textSecondary, modifier = Modifier.testTag("studio_active_coach_no_history"))
            if (row.advice.target.text.isNotBlank()) Text("${coachTr(lang, "Cél", "Target", "Ziel", "Țintă")}: ${row.advice.target.text}", color = palette.textSecondary)
            if (row.advice.action in setOf("increase", "reduce", "start") && row.advice.weight > 0)
                Text("${coachTr(lang, "Javasolt súly", "Suggested weight", "Empfohlenes Gewicht", "Greutate recomandată")}: ${row.advice.weight} kg" + when (row.loadType) {
                    "per_hand" -> coachTr(lang, " kézenként", " per hand", " pro Hand", " pe mână")
                    "total" -> coachTr(lang, " összesen", " total", " gesamt", " total")
                    "single_dumbbell" -> coachTr(lang, " egy súlyzóval", " single dumbbell", " mit einer Kurzhantel", " cu o ganteră")
                    else -> ""
                }, color = palette.textPrimary, modifier = Modifier.testTag("studio_active_coach_weight"))
            if (row.advice.nextReps.isNotEmpty()) Text("${coachTr(lang, "Javasolt ismétlések", "Suggested reps", "Empfohlene Wdh.", "Repetări recomandate")}: ${row.advice.nextReps.joinToString(" / ")}",
                color = palette.textPrimary, modifier = Modifier.testTag("studio_active_coach_reps"))
        }
        else -> Text(coachTr(lang, "Nincs kiválasztott aktív gyakorlat.", "No active exercise selected.", "Keine aktive Übung ausgewählt.", "Niciun exercițiu activ selectat."), color = palette.textSecondary)
    }
    TextButton(onClick = onReload, enabled = !state.loading,
        modifier = Modifier.testTag("studio_active_coach_refresh")) {
        Text(coachTr(lang, "Coach frissítése", "Refresh Coach", "Coach aktualisieren", "Actualizează Coach"))
    }
}

/** Apply only through the existing snapshot/ownership/settings/history-checked native proposal. */
@Composable
internal fun rememberStudioWorkoutCoachOwner(): androidx.lifecycle.ViewModelStoreOwner {
    // Retain across lazy-item disposal; clear when the exercise screen leaves composition.
    val store = remember { androidx.lifecycle.ViewModelStore() }
    val owner = remember { object : androidx.lifecycle.ViewModelStoreOwner {
        override val viewModelStore = store
    } }
    DisposableEffect(store) { onDispose { store.clear() } }
    return owner
}

@Composable
internal fun StudioWorkoutCoachPanel(exerciseId: String,
    owner: androidx.lifecycle.ViewModelStoreOwner = rememberStudioWorkoutCoachOwner()) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val model: com.repforge.app.ui.workout.ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    val state by model.uiState.collectAsStateWithLifecycle()
    val adviceModel: WorkoutCoachAdviceViewModel = viewModel(viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    LaunchedEffect(exerciseId) { adviceModel.selectExercise(exerciseId) }
    val adviceState by adviceModel.state.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language.code
    if (state.modeReviewOpen) AlertDialog(
        onDismissRequest = model::cancelTrainingModeReview,
        title = { Text(coachTr(lang, "Coach-javaslat jóváhagyása", "Approve Coach Proposal", "Coach-Vorschlag bestätigen", "Aprobă propunerea Coach")) },
        text = { Text(coachTr(lang, "Csak az érintetlen sorozatok súlya változik. RIR, teljesítettség, idő és mérés nem keletkezik.", "Only untouched sets will update weight. RIR, completion, time, and measurements are not fabricated.", "Nur unberührte Sätze ändern das Gewicht. RIR, Abschluss und Zeit werden nicht erzeugt.", "Doar seriile neatinse își modifică greutatea. RIR, finalizarea și timpul nu sunt generate.")) },
        confirmButton = { TextButton(onClick = model::approveTrainingModeWeight, enabled = !state.modeProposalLoading,
            modifier = Modifier.testTag("studio_active_coach_approve")) { Text(coachTr(lang, "Alkalmazás", "Apply", "Anwenden", "Aplică")) } },
        dismissButton = { TextButton(onClick = model::cancelTrainingModeReview) { Text(coachTr(lang, "Mégse", "Cancel", "Abbrechen", "Anulează")) } })
    Surface(color = palette.surfaceCard, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("studio_active_coach_panel")
            .trainPilotBorder(1.dp, palette.sectionBorder, RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TrainPilot Coach", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            StudioWorkoutCoachAdvice(if (adviceState.exerciseId == exerciseId) adviceState else WorkoutCoachAdviceState(exerciseId), adviceModel::reload)
            if (state.explicitTrainingMode) {
                OutlinedButton(onClick = { model.previewTrainingModeWeight(exerciseId) },
                    enabled = !state.modeProposalLoading && !state.isFinishing && !state.isPaused && state.timerOwner == "phone",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio_active_coach_preview")) {
                    Text(if (state.modeProposalLoading) coachTr(lang, "Előkészítés…", "Preparing…", "Vorbereiten…", "Se pregătește…") else coachTr(lang, "Valódi előzmények alapján javaslat", "Proposal from real history", "Vorschlag aus echtem Verlauf", "Propunere din istoricul real"))
                }
                if (state.modeProposalForExercise == exerciseId) {
                    Text(state.modeReason, color = palette.textPrimary)
                    Text(coachTr(lang, "Megfelelő előzmény: ${state.modeHistoryCount}", "Matching history: ${state.modeHistoryCount}", "Passender Verlauf: ${state.modeHistoryCount}", "Istoric relevant: ${state.modeHistoryCount}"), color = palette.textSecondary)
                    if (state.modeSuggestedWeight != null && state.modeBaselineWeight != null) {
                        Text("${state.modeBaselineWeight} → ${state.modeSuggestedWeight} kg", color = palette.textPrimary)
                        Button(onClick = model::reviewTrainingModeWeight,
                            enabled = !state.modeProposalLoading && !state.isPaused && state.timerOwner == "phone",
                            modifier = Modifier.testTag("studio_active_coach_review")) { Text(coachTr(lang, "Javaslat áttekintése", "Review Proposal", "Vorschlag prüfen", "Revizuiește propunerea")) }
                    } else Text(coachTr(lang, "Most nincs jóváhagyható súlymódosítás.", "No weight adjustment to approve right now.", "Derzeit keine Gewichtsänderung verfügbar.", "Nicio modificare de greutate de aprobat acum."), color = palette.textSecondary)
                }
            }
            state.statusMessage?.let { Text(it, color = palette.textSecondary) }
        }
    }
}
