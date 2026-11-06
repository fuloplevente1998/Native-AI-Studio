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
    onCopy: () -> Unit, onDelete: () -> Unit, onAddDay: (String) -> Unit, onDeleteDay: () -> Unit,
    onSave: () -> Unit = {}
) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    var pending by remember(program.id) { mutableStateOf<String?>(null) }
    var dayName by remember(program.id) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TrainPilotOutlinedButton(onClick = onCopy, enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("studio_program_copy")) {
            Text(word("Program másolása saját tervként", "Copy program as custom plan", "Programm als eigenen Plan kopieren", "Copiază programul ca plan propriu"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotOutlinedButton(onClick = { pending = "add-day" }, enabled = !busy && program.days.size < 31,
                modifier = Modifier.weight(1f).testTag("studio_program_add_day")) {
                Text(word("+ Edzésnap", "+ Workout day", "+ Trainingstag", "+ Zi de antrenament"))
            }
            if (hasDay) TrainPilotDeleteButton(onClick = { pending = "delete-day" },
                enabled = !busy && program.days.size > 1, modifier = Modifier.weight(1f).testTag("studio_program_delete_day"),
                label = word("Nap törlése", "Delete day", "Tag löschen", "Șterge ziua"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.repforge.app.ui.theme.TrainPilotSaveButton(onClick = onSave, enabled = !busy,
                modifier = Modifier.weight(1f).testTag("studio_program_save")) {
                Text(word("Mentés", "Save", "Speichern", "Salvează"))
            }
            if (program.isCustom) TrainPilotDeleteButton(onClick = { pending = "delete-program" }, enabled = !busy,
                modifier = Modifier.weight(1f).testTag("studio_program_delete"),
                label = word("Program törlése", "Delete program", "Programm löschen", "Șterge programul"))
        }
        if (!program.isCustom) Text(word(
            "A beépített sablon változtatása saját másolatot hoz létre.",
            "Editing a built-in template creates a custom copy.",
            "Das Ändern einer Vorlage erstellt eine eigene Kopie.",
            "Modificarea unui șablon predefinit creează o copie proprie."
        ), style = MaterialTheme.typography.labelSmall)
    }
    pending?.let { operation ->
        AlertDialog(onDismissRequest = { if (!busy) pending = null }, title = { Text(when (operation) {
            "add-day" -> word("Új edzésnap", "New workout day", "Neuer Trainingstag", "Zi nouă de antrenament")
            "delete-day" -> word("Edzésnap törlése?", "Delete workout day?", "Trainingstag löschen?", "Ștergi ziua de antrenament?")
            else -> word("Program törlése?", "Delete program?", "Programm löschen?", "Ștergi programul?")
        }) }, text = {
            if (operation == "add-day") OutlinedTextField(dayName, { dayName = it.take(120) },
                label = { Text(word("Edzésnap neve", "Workout day name", "Name des Trainingstags", "Numele zilei de antrenament")) }, singleLine = true,
                modifier = Modifier.testTag("studio_program_day_name"))
            else Text(if (operation == "delete-day")
                word(
                    "Az aktuális edzésnap törlődik a programból. A naplóban vagy naptárban használt napot az app megőrzi.",
                    "The current workout day will be removed from the program. Days referenced in the log or calendar are preserved.",
                    "Der aktuelle Trainingstag wird aus dem Programm entfernt. Im Protokoll oder Kalender verwendete Tage bleiben erhalten.",
                    "Ziua curentă de antrenament va fi eliminată din program. Zilele utilizate în jurnal sau calendar sunt păstrate."
                )
                else word(
                    "A saját program végleg törlődik a tárból. Aktív, naplóban vagy naptárban használt program nem törölhető.",
                    "The custom program will be permanently deleted. Active programs or programs used in the log or calendar cannot be deleted.",
                    "Das eigene Programm wird dauerhaft gelöscht. Aktive oder im Protokoll/Kalender verwendete Programme können nicht gelöscht werden.",
                    "Programul personalizat va fi șters definitiv. Programele active sau utilizate în jurnal/calendar nu pot fi șterse."
                ))
        }, confirmButton = { com.repforge.app.ui.theme.TrainPilotActionButton(positive = operation == "add-day",
            enabled = !busy && (operation != "add-day" || dayName.isNotBlank()),
            modifier = Modifier.testTag("studio_program_confirm"), onClick = {
                pending = null
                when (operation) { "add-day" -> { onAddDay(dayName); dayName = "" }; "delete-day" -> onDeleteDay(); else -> onDelete() }
            }) { Text(if (operation == "add-day") word("Hozzáadás", "Add", "Hinzufügen", "Adaugă") else word("Törlés", "Delete", "Löschen", "Șterge")) }
        }, dismissButton = { TextButton(onClick = { pending = null }, enabled = !busy) { Text(word("Mégse", "Cancel", "Abbrechen", "Anulează")) } })
    }
}

@Composable
internal fun StudioRemoveProgramExerciseButton(id: String, name: String, busy: Boolean, onRemove: (String) -> Unit) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun word(hu: String, en: String, de: String, ro: String) = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    var confirm by remember(id) { mutableStateOf(false) }
    TrainPilotDeleteButton(onClick = { confirm = true }, enabled = !busy,
        modifier = Modifier.fillMaxWidth().testTag("studio_program_remove_exercise_$id"),
        label = word("Eltávolítás az edzésnapból", "Remove from workout day", "Aus dem Trainingstag entfernen", "Elimină din ziua de antrenament"))
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(word("Gyakorlat eltávolítása?", "Remove exercise?", "Übung entfernen?", "Elimini exercițiul?")) },
        text = { Text(word(
            "$name kikerül ebből az edzésnapból. A gyakorlatkönyvtár és a korábbi napló változatlan.",
            "$name will be removed from this workout day. The exercise library and previous workout logs remain unchanged.",
            "$name wird aus diesem Trainingstag entfernt. Übungsbibliothek und bisherige Protokolle bleiben unverändert.",
            "$name va fi eliminat din această zi de antrenament. Biblioteca de exerciții și jurnalul anterior rămân neschimbate."
        )) },
        confirmButton = { com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = { confirm = false; onRemove(id) },
            modifier = Modifier.testTag("studio_program_confirm_remove_exercise")) { Text(word("Eltávolítás", "Remove", "Entfernen", "Elimină")) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(word("Mégse", "Cancel", "Abbrechen", "Anulează")) } })
}
