package com.example.ui.components

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.coach.*
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.trainPilotBorder
import androidx.compose.foundation.shape.RoundedCornerShape

/** Studio presentation of the canonical Coach; no local advice formula or silent prescription edits. */
@Composable
internal fun StudioCoachRecommendations(
    state: CoachUiState,
    onStart: (String, String, String?) -> Unit,
    onCalendar: () -> Unit,
    onExercise: (String) -> Unit,
    onPreviewVariation: (String) -> Unit,
    onCloseVariation: () -> Unit,
    onSaveVariation: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val text = nativeResources(LocalContext.current, state.language)
    state.variationPreview?.let { preview ->
        AlertDialog(onDismissRequest = onCloseVariation,
            title = { Text("Gyakorlatváltozat előnézete") },
            text = { Text(NativeCoachAdviceText.variation(text, preview.next)) },
            confirmButton = { TextButton(onClick = onSaveVariation, enabled = !state.variationBusy,
                modifier = Modifier.testTag("studio_coach_variation_confirm")) { Text("Változat mentése") } },
            dismissButton = { TextButton(onClick = onCloseVariation, enabled = !state.variationBusy) { Text("Mégse") } })
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))
        if (state.error) Text("A Coach adatai nem frissíthetők. A korábbi kijelzés nem friss mérés.", color = palette.danger)
        state.target?.let { target ->
            Surface(color = palette.surfaceCard, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("studio_coach_target")
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Következő edzés", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text(target.title, color = palette.textPrimary)
                    target.schedule?.let { Text(it.date, color = palette.textSecondary) }
                    Button(enabled = !state.loading && !state.error,
                        onClick = { if (target.upcoming) onCalendar() else onStart(target.programId, target.dayId, target.schedule?.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio_coach_start_target")) {
                        Text(if (target.upcoming) "Megnyitás a naptárban" else "A javasolt edzés indítása")
                    }
                }
            }
        }
        if (!state.loading && state.target == null && !state.error)
            Text("Nincs elérhető programnap. Válassz aktív programot.", color = palette.textSecondary)
        state.exercises.forEach { row ->
            Surface(color = palette.surfaceCard, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("studio_coach_advice_${row.id}")
                    .trainPilotBorder(0.75.dp, palette.cardBorder, RoundedCornerShape(14.dp))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary,
                        modifier = Modifier.clickable { onExercise(row.id) })
                    Text(NativeCoachAdviceText.label(text, row.advice.action), color = palette.primary)
                    Text(NativeCoachAdviceText.reason(text, row.advice.reason), color = palette.textSecondary)
                    Text("Megfelelő előzmény: ${row.advice.historyCount}", color = palette.textSecondary)
                    if (row.advice.target.text.isNotBlank()) Text("Cél: ${row.advice.target.text}", color = palette.textSecondary)
                    if (row.advice.action in setOf("increase", "reduce", "start") && row.advice.weight > 0)
                        Text("Javasolt súly: ${row.advice.weight} kg" + when (row.loadType) {
                            "per_hand" -> " kézenként"; "total" -> " összesen"; "single_dumbbell" -> " egy súlyzóval"; else -> ""
                        }, color = palette.textPrimary)
                    if (row.advice.action == "reps" && row.advice.nextReps.isNotEmpty())
                        Text("Javasolt ismétlések: ${row.advice.nextReps.joinToString(" / ")}", color = palette.textPrimary)
                    row.modePreview?.let { preview ->
                        val style = if (preview.trainingStyle == "hit") "HIT-inspirált" else "Normál"
                        val mode = when (preview.mode) { "linear" -> "Lineáris"; "rir" -> "RIR/Coach"; else -> "Dupla progresszió" }
                        Text("$style / $mode • ${preview.workingSets} munkasorozat", color = palette.textSecondary)
                    }
                    if (row.advice.action == "variation") TextButton(onClick = { onPreviewVariation(row.id) },
                        enabled = !state.variationBusy && !state.loading && !state.error) { Text("Változat előnézete") }
                }
            }
        }
        if (state.variationFailed) Text("A gyakorlatváltozat nem menthető; frissítsd az adatokat.", color = palette.danger)
        if (state.variationSaved) Text("A gyakorlatváltozat elmentve.", color = palette.success)
    }
}

/** Apply only through the existing snapshot/ownership/settings/history-checked native proposal. */
@Composable
internal fun StudioWorkoutCoachPanel(exerciseId: String) {
    // The native VM has ticking observers: dispose it with this panel, not at Activity destruction.
    val app = LocalContext.current.applicationContext as android.app.Application
    val store = remember { androidx.lifecycle.ViewModelStore() }
    val owner = remember { object : androidx.lifecycle.ViewModelStoreOwner {
        override val viewModelStore = store
    } }
    DisposableEffect(store) { onDispose { store.clear() } }
    val model: com.repforge.app.ui.workout.ActiveWorkoutViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    val state by model.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    if (state.modeReviewOpen) AlertDialog(
        onDismissRequest = model::cancelTrainingModeReview,
        title = { Text("Coach-javaslat jóváhagyása") },
        text = { Text("Csak az érintetlen sorozatok súlya változik. RIR, teljesítettség, idő és mérés nem keletkezik.") },
        confirmButton = { TextButton(onClick = model::approveTrainingModeWeight, enabled = !state.modeProposalLoading,
            modifier = Modifier.testTag("studio_active_coach_approve")) { Text("Alkalmazás") } },
        dismissButton = { TextButton(onClick = model::cancelTrainingModeReview) { Text("Mégse") } })
    Surface(color = palette.surfaceCard, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("studio_active_coach_panel")
            .trainPilotBorder(1.dp, palette.sectionBorder, RoundedCornerShape(18.dp))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TrainPilot Coach", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            if (!state.explicitTrainingMode) {
                Text("A tartós progressziós módot az edzés előtt választhatod ki a Beállításokban. Az előző alkalom értékei ettől függetlenül betöltődnek.", color = palette.textSecondary)
            } else {
                OutlinedButton(onClick = { model.previewTrainingModeWeight(exerciseId) },
                    enabled = !state.modeProposalLoading && !state.isFinishing && !state.isPaused && state.timerOwner == "phone",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio_active_coach_preview")) {
                    Text(if (state.modeProposalLoading) "Előkészítés…" else "Valódi előzmények alapján javaslat")
                }
                if (state.modeProposalForExercise == exerciseId) {
                    Text(state.modeReason, color = palette.textPrimary)
                    Text("Megfelelő előzmény: ${state.modeHistoryCount}", color = palette.textSecondary)
                    if (state.modeSuggestedWeight != null && state.modeBaselineWeight != null) {
                        Text("${state.modeBaselineWeight} → ${state.modeSuggestedWeight} kg", color = palette.textPrimary)
                        Button(onClick = model::reviewTrainingModeWeight,
                            enabled = !state.modeProposalLoading && !state.isPaused && state.timerOwner == "phone",
                            modifier = Modifier.testTag("studio_active_coach_review")) { Text("Javaslat áttekintése") }
                    } else Text("Most nincs jóváhagyható súlymódosítás.", color = palette.textSecondary)
                }
            }
            state.statusMessage?.let { Text(it, color = palette.textSecondary) }
        }
    }
}
