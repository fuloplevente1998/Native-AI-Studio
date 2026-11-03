package com.example.ui.components

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
    val pending = state.workoutToDelete?.takeIf { workoutId == null || it.workout.id == workoutId }
    if (pending != null) AlertDialog(
        onDismissRequest = model::cancelDeleteWorkout,
        title = { Text("Edzés törlése?") },
        text = { Column {
            Text("A mentett edzés és a kizárólag hozzá tartozó helyi fotók és útvonalak végleg törlődnek.")
            if (state.operationFailed) Text("A törlés nem fejeződött be. Próbáld újra; ha az edzés közben változott, nyisd meg újra.", color = palette.danger)
        } },
        confirmButton = { TextButton(enabled = !state.deleting,
            onClick = { model.confirmDeleteWorkout(onDeleted) }, modifier = Modifier.testTag("journal_confirm_delete")) {
            Text(if (state.deleting) "Törlés…" else "Törlés", color = palette.danger)
        } },
        dismissButton = { TextButton(enabled = !state.deleting, onClick = model::cancelDeleteWorkout,
            modifier = Modifier.testTag("journal_cancel_delete")) { Text("Mégse") } }
    )
}
