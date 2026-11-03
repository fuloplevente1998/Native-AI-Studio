package com.example.ui.components

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.TrainingProgramModel
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

/** Same native editor, explicit actions; only confirm invokes a destructive command. */
@Composable
internal fun StudioProgramManagementControls(
    program: TrainingProgramModel, busy: Boolean, hasDay: Boolean,
    onCopy: () -> Unit, onDelete: () -> Unit, onAddDay: (String) -> Unit, onDeleteDay: () -> Unit
) {
    var pending by remember(program.id) { mutableStateOf<String?>(null) }
    var dayName by remember(program.id) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TrainPilotOutlinedButton(onClick = onCopy, enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("studio_program_copy")) { Text("Program másolása saját tervként") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotOutlinedButton(onClick = { pending = "add-day" }, enabled = !busy && program.days.size < 31,
                modifier = Modifier.weight(1f).testTag("studio_program_add_day")) { Text("+ Edzésnap") }
            if (hasDay) TrainPilotDeleteButton(onClick = { pending = "delete-day" },
                enabled = !busy && program.days.size > 1, modifier = Modifier.weight(1f).testTag("studio_program_delete_day"),
                label = "Nap törlése")
        }
        if (program.isCustom) TrainPilotDeleteButton(onClick = { pending = "delete-program" }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("studio_program_delete"), label = "Program törlése")
        else Text("A beépített sablon változtatása saját másolatot hoz létre.", style = MaterialTheme.typography.labelSmall)
    }
    pending?.let { operation ->
        AlertDialog(onDismissRequest = { if (!busy) pending = null }, title = { Text(when (operation) {
            "add-day" -> "Új edzésnap"; "delete-day" -> "Edzésnap törlése?"; else -> "Program törlése?"
        }) }, text = {
            if (operation == "add-day") OutlinedTextField(dayName, { dayName = it.take(120) },
                label = { Text("Edzésnap neve") }, singleLine = true,
                modifier = Modifier.testTag("studio_program_day_name"))
            else Text(if (operation == "delete-day")
                "Az aktuális edzésnap törlődik a programból. A naplóban vagy naptárban használt napot az app megőrzi."
                else "A saját program végleg törlődik a tárból. Aktív, naplóban vagy naptárban használt program nem törölhető.")
        }, confirmButton = { TextButton(enabled = !busy && (operation != "add-day" || dayName.isNotBlank()),
            modifier = Modifier.testTag("studio_program_confirm"), onClick = {
                pending = null
                when (operation) { "add-day" -> { onAddDay(dayName); dayName = "" }; "delete-day" -> onDeleteDay(); else -> onDelete() }
            }) { Text(if (operation == "add-day") "Hozzáadás" else "Törlés") }
        }, dismissButton = { TextButton(onClick = { pending = null }, enabled = !busy) { Text("Mégse") } })
    }
}

@Composable
internal fun StudioRemoveProgramExerciseButton(id: String, name: String, busy: Boolean, onRemove: (String) -> Unit) {
    var confirm by remember(id) { mutableStateOf(false) }
    TrainPilotDeleteButton(onClick = { confirm = true }, enabled = !busy,
        modifier = Modifier.fillMaxWidth().testTag("studio_program_remove_exercise_$id"), label = "Eltávolítás az edzésnapból")
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Gyakorlat eltávolítása?") },
        text = { Text("$name kikerül ebből az edzésnapból. A gyakorlatkönyvtár és a korábbi napló változatlan.") },
        confirmButton = { TextButton(onClick = { confirm = false; onRemove(id) },
            modifier = Modifier.testTag("studio_program_confirm_remove_exercise")) { Text("Eltávolítás") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Mégse") } })
}
