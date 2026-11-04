package com.example.ui.components

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.journal.JournalViewModel

@Composable
internal fun StudioLoggedWorkoutDeleteActions(workoutId: String, onDeleted: (String) -> Unit,
    model: JournalViewModel = viewModel(key = "studio-workout-health")) {
    val state by model.uiState.collectAsStateWithLifecycle()
    TrainPilotDeleteButton(onClick = model::requestDeleteWorkout,
        enabled = !state.deleting && state.detail?.workout?.id == workoutId && state.selectedWorkoutId == workoutId,
        modifier = Modifier.fillMaxWidth().testTag("journal_delete_workout"))
    StudioWorkoutDeleteConfirmation(workoutId, onDeleted, model)
}

/** One confirmation and the same durable native operation for both list and details. */
@Composable
internal fun StudioWorkoutDeleteConfirmation(workoutId: String?, onDeleted: (String) -> Unit,
    model: JournalViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val pending = state.workoutToDelete?.takeIf { workoutId == null || it.workout.id == workoutId }
    if (pending != null) AlertDialog(
        onDismissRequest = model::cancelDeleteWorkout,
        title = { Text(word("Edzés törlése?", "Delete workout?", "Training löschen?", "Ștergi antrenamentul?")) },
        text = { Column {
            Text(word(
                "A mentett edzés és a kizárólag hozzá tartozó helyi fotók és útvonalak végleg törlődnek.",
                "The saved workout and its local photos and routes will be permanently deleted.",
                "Das gespeicherte Training sowie zugehörige lokale Fotos und Routen werden dauerhaft gelöscht.",
                "Antrenamentul salvat și fotografiile și traseele locale aferente vor fi șterse definitiv."
            ))
            if (state.operationFailed) Text(word(
                "A törlés nem fejeződött be. Próbáld újra; ha az edzés közben változott, nyisd meg újra.",
                "Deletion did not complete. Try again; if the workout changed in the meantime, reopen it.",
                "Das Löschen wurde nicht abgeschlossen. Versuche es erneut; falls das Training geändert wurde, öffne es neu.",
                "Ștergerea nu s-a finalizat. Încearcă din nou; dacă antrenamentul s-a modificat între timp, redeschide-l."
            ), color = palette.danger)
        } },
        confirmButton = { TextButton(enabled = !state.deleting,
            onClick = { model.confirmDeleteWorkout(onDeleted) }, modifier = Modifier.testTag("journal_confirm_delete")) {
            Text(if (state.deleting) word("Törlés…", "Deleting…", "Löschen…", "Se șterge…") else word("Törlés", "Delete", "Löschen", "Șterge"), color = palette.danger)
        } },
        dismissButton = { TextButton(enabled = !state.deleting, onClick = model::cancelDeleteWorkout,
            modifier = Modifier.testTag("journal_cancel_delete")) { Text(word("Mégse", "Cancel", "Abbrechen", "Anulează")) } }
    )
}
