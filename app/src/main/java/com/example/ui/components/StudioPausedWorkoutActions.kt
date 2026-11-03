package com.example.ui.components

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

/** Deletion is explicit, confirmed, and bound to the workout shown when the dialog opened. */
@Composable
internal fun StudioPausedWorkoutActions(workoutId: String, phoneControls: Boolean, busy: Boolean,
    continueLabel: String, onContinue: () -> Unit, onDiscard: (String) -> Unit) {
    val palette = LocalTrainPilotPalette.current
    var pendingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(workoutId) { if (pendingId != workoutId) pendingId = null }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onContinue, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 50.dp)
            .testTag("home_continue_workout_button")) { Text(continueLabel) }
        TrainPilotOutlinedButton(onClick = { pendingId = workoutId },
            enabled = !busy && phoneControls && workoutId.isNotBlank(), shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 50.dp).testTag("home_discard_workout_button")) { Text("Elvetés") }
    }
    if (pendingId != null && pendingId == workoutId) {
        AlertDialog(onDismissRequest = { pendingId = null }, title = { Text("Edzés elvetése?") },
            text = { Text("A félbehagyott edzés nem kerül a naplóba. A korábban mentett edzések megmaradnak.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingId
                    pendingId = null
                    if (id == workoutId && phoneControls && !busy && !id.isNullOrBlank()) onDiscard(id)
                }, enabled = phoneControls && !busy, modifier = Modifier.testTag("home_confirm_discard")) {
                    Text("Elvetés", color = palette.danger)
                }
            }, dismissButton = { TextButton(onClick = { pendingId = null }, modifier = Modifier.testTag("home_cancel_discard")) { Text("Mégse") } })
    }
}
