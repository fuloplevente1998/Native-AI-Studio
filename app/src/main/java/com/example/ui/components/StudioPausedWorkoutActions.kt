package com.example.ui.components

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

/** Deletion is explicit, confirmed, and bound to the workout shown when the dialog opened. */
@Composable
internal fun StudioPausedWorkoutActions(workoutId: String, phoneControls: Boolean, busy: Boolean,
    continueLabel: String, onContinue: () -> Unit, onDiscard: (String) -> Unit) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    var pendingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(workoutId) { if (pendingId != workoutId) pendingId = null }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onContinue, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 50.dp)
            .testTag("home_continue_workout_button")) { Text(continueLabel) }
        com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = { pendingId = workoutId },
            enabled = !busy && phoneControls && workoutId.isNotBlank(), shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 50.dp).testTag("home_discard_workout_button")) {
            Text(word("Elvetés", "Discard", "Verwerfen", "Renunță"))
        }
    }
    if (pendingId != null && pendingId == workoutId) {
        AlertDialog(onDismissRequest = { pendingId = null },
            title = { Text(word("Edzés elvetése?", "Discard workout?", "Training verwerfen?", "Renunți la antrenament?")) },
            text = { Text(word(
                "A félbehagyott edzés nem kerül a naplóba. A korábban mentett edzések megmaradnak.",
                "The unfinished workout will not be saved to the log. Previously saved workouts remain intact.",
                "Das abgebrochene Training wird nicht im Protokoll gespeichert. Früher gespeicherte Trainings bleiben erhalten.",
                "Antrenamentul neterminat nu va fi salvat în jurnal. Antrenamentele salvate anterior rămân intacte."
            )) },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = {
                    val id = pendingId
                    pendingId = null
                    if (id == workoutId && phoneControls && !busy && !id.isNullOrBlank()) onDiscard(id)
                }, enabled = phoneControls && !busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("home_confirm_discard")) {
                    Text(word("Elvetés", "Discard", "Verwerfen", "Renunță"))
                }
            }, dismissButton = {
                TextButton(onClick = { pendingId = null }, modifier = Modifier.testTag("home_cancel_discard")) {
                    Text(word("Mégse", "Cancel", "Abbrechen", "Anulează"))
                }
            })
    }
}
