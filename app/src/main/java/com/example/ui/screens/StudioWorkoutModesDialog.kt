package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.components.TrainPilotAlertDialog
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * One Studio presentation for both Settings (global) and Workout (active program).
 * All mode values are the canonical #197 IDs, never translated labels.
 * The preview and its snapshot-guarded approval are owned by TrainPilotViewModel.
 */
private fun workoutModeText(language: AppLanguage, hu: String, en: String, de: String, ro: String): String =
    when (language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }

private fun styleName(language: AppLanguage, code: String) =
    if (code == "hit") workoutModeText(language, "HIT-inspirált", "HIT-inspired", "HIT-inspiriert", "Inspirat de HIT")
    else workoutModeText(language, "Normál", "Normal", "Normal", "Normal")

private fun progressionName(language: AppLanguage, code: String) = when (code) {
    "linear" -> workoutModeText(language, "Lineáris", "Linear", "Linear", "Liniară")
    "rir" -> workoutModeText(language, "RIR / Coach", "RIR / Coach", "RIR / Coach", "RIR / Coach")
    else -> workoutModeText(language, "Dupla progresszió", "Double progression", "Doppelte Progression", "Progresie dublă")
}

@Composable
private fun ModeOption(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val p = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) p.primary else p.surfaceElevated,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold,
            color = if (selected) p.onPrimary else p.textPrimary,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp))
    }
}

@Composable
internal fun StudioWorkoutModesDialog(
    state: TrainPilotUiState,
    scope: String,
    onDismiss: () -> Unit,
    onCycleRest: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onPreview: (String, String, Boolean) -> Unit,
    onApprove: () -> Unit,
    onEditPreview: () -> Unit,
    onUndo: () -> Unit
) {
    val p = LocalTrainPilotPalette.current
    val lang = state.language
    val isProgram = scope == "program"
    val initialStyle = if (isProgram) state.programTrainingStyleCode else state.trainingStyleCode
    val initialProgression = if (isProgram) state.programProgressionModeCode else state.progressionModeCode
    var style by remember(scope, initialStyle) { mutableStateOf(initialStyle) }
    var progression by remember(scope, initialProgression) { mutableStateOf(initialProgression) }
    val review = state.progressionModeReview
    val busy = state.progressionModeBusy
    val active = state.isWorkoutSessionActive
    val unit = workoutModeText(lang, "mp", "sec", "Sek.", "sec")

    TrainPilotAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = p.surfaceCard,
        titleContentColor = p.textPrimary,
        title = {
            Text(workoutModeText(lang, "Edzés beállításai és progresszió",
                "Workout settings & progression", "Trainingseinstellungen & Progression",
                "Setări antrenament și progresie"), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isProgram) Text(
                    workoutModeText(lang, "Aktív program: ", "Active program: ", "Aktives Programm: ", "Program activ: ") +
                        state.activeTrainingModeProgramId,
                    color = p.textSecondary)
                if (review != null) {
                    Text(workoutModeText(lang, "Módváltás előnézete", "Mode change preview",
                        "Vorschau des Moduswechsels", "Previzualizare schimbare mod"),
                        fontWeight = FontWeight.Bold, color = p.textPrimary)
                    Text(
                        workoutModeText(lang, "Korábbi: ", "Before: ", "Vorher: ", "Înainte: ") +
                            styleName(lang, review.beforeStyle) + " / " + progressionName(lang, review.beforeProgression),
                        color = p.textSecondary)
                    Text(
                        workoutModeText(lang, "Új: ", "After: ", "Nachher: ", "După: ") +
                            styleName(lang, review.afterStyle) + " / " + progressionName(lang, review.afterProgression),
                        color = p.textPrimary, fontWeight = FontWeight.SemiBold)
                    if (review.inheritGlobal) Text(
                        workoutModeText(lang, "A program a globális beállítást fogja örökölni.",
                            "The program will inherit the global setting.",
                            "Das Programm übernimmt die globale Einstellung.",
                            "Programul va moșteni setarea globală."),
                        color = p.textSecondary)
                    Text(
                        workoutModeText(lang,
                            "A változtatás nem ír át korábbi naplót, programreceptet vagy aktív edzést. Az új súlyjavaslat külön jóváhagyást igényel.",
                            "This does not rewrite past sessions, program prescriptions or an active workout. New weight proposals require separate approval.",
                            "Vergangene Trainings, Programmrezepte und aktive Trainings bleiben unverändert. Gewichtsvorschläge brauchen eine eigene Freigabe.",
                            "Nu modifică antrenamentele trecute, rețetele sau sesiunile active. Propunerile de greutate cer confirmare separată."),
                        color = p.textSecondary)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Alapértelmezett pihenőidő", "Default rest timer",
                            "Standard-Pausenzeit", "Timp implicit de pauză"), color = p.textPrimary)
                        Surface(shape = RoundedCornerShape(10.dp), color = p.surfaceElevated,
                            modifier = Modifier.clickable(onClick = onCycleRest).testTag("studio_modes_rest")) {
                            Text("${state.defaultRestSec} $unit ↻", color = p.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Hangjelzés pihenő végén", "Sound alert after rest",
                            "Ton am Pausenende", "Sunet la sfârșitul pauzei"), color = p.textPrimary,
                            modifier = Modifier.weight(1f))
                        Switch(checked = state.soundAlertEnabled, onCheckedChange = onToggleSound,
                            modifier = Modifier.testTag("studio_modes_sound"))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Rezgés pihenő végén", "Vibration after rest",
                            "Vibration am Pausenende", "Vibrație la sfârșitul pauzei"),
                            color = p.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = state.vibrationEnabled, onCheckedChange = onToggleVibration,
                            modifier = Modifier.testTag("studio_modes_vibration"))
                    }
                    Text(workoutModeText(lang, "Edzésstílus", "Training style", "Trainingsstil",
                        "Stil de antrenament"), color = p.textPrimary, fontWeight = FontWeight.Bold)
                    ModeOption(styleName(lang, "normal"), style == "normal", "studio_modes_style_normal") { style = "normal" }
                    ModeOption(styleName(lang, "hit"), style == "hit", "studio_modes_style_hit") { style = "hit" }
                    Text(workoutModeText(lang,
                        "A HIT-inspirált stílus alacsonyabb munkasorozat-számot javasol; nem ír elő bukásig edzést.",
                        "HIT-inspired mode proposes fewer working sets; it does not require training to failure.",
                        "HIT empfiehlt weniger Arbeitssätze; Training bis zum Muskelversagen ist nicht verpflichtend.",
                        "HIT propune mai puține serii; nu necesită epuizare musculară."),
                        color = p.textSecondary)
                    Text(workoutModeText(lang, "Progressziós szabály", "Progression rule", "Progressionsregel",
                        "Regulă de progresie"), color = p.textPrimary, fontWeight = FontWeight.Bold)
                    for (code in listOf("double", "linear", "rir")) {
                        ModeOption(progressionName(lang, code), progression == code,
                            "studio_modes_progression_$code") { progression = code }
                    }
                    if (isProgram) {
                        Text(workoutModeText(lang, "Érvényes forrás: ", "Effective source: ",
                            "Gültige Quelle: ", "Sursă efectivă: ") +
                            when (state.programTrainingModeSource) {
                                "program" -> workoutModeText(lang, "program", "program", "Programm", "program")
                                "global" -> workoutModeText(lang, "globális", "global", "global", "global")
                                else -> workoutModeText(lang, "korábbi működés", "legacy", "bisherig", "anterior")
                            }, color = p.textSecondary)
                        if (state.programTrainingModeSource == "program") {
                            TextButton(onClick = { onPreview(style, progression, true) },
                                enabled = !busy && !active,
                                modifier = Modifier.testTag("studio_modes_inherit")) {
                                Text(workoutModeText(lang, "Globális mód öröklése",
                                    "Inherit global mode", "Globalen Modus übernehmen",
                                    "Moștenește modul global"), color = p.primary)
                            }
                        }
                    }
                    val canUndo = if (isProgram) state.programTrainingModeUndo else state.globalTrainingModeUndo
                    if (canUndo) TextButton(onClick = onUndo, enabled = !busy && !active) {
                        Text(workoutModeText(lang, "Utolsó módváltás visszavonása", "Undo last mode change",
                            "Letzten Moduswechsel rückgängig machen", "Anulează ultima schimbare"),
                            color = p.primary)
                    }
                    if (active) Text(
                        workoutModeText(lang, "Aktív edzés közben a módváltás nem menthető.",
                            "Mode changes cannot be saved during an active workout.",
                            "Moduswechsel können während eines Trainings nicht gespeichert werden.",
                            "Modurile nu pot fi schimbate în timpul antrenamentului."),
                        color = p.textSecondary)
                    Text(workoutModeText(lang,
                        "A pihenőidő, a hang és a rezgés módosítása azonnal mentődik.",
                        "Rest, sound and vibration changes are saved immediately.",
                        "Pause, Ton und Vibration werden sofort gespeichert.",
                        "Pauza, sunetul și vibrațiile se salvează imediat."),
                        color = p.textSecondary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (review != null) onEditPreview() else onDismiss() }, enabled = !busy) {
                Text(if (review == null) workoutModeText(lang, "Mégse", "Cancel", "Abbrechen", "Anulează")
                    else workoutModeText(lang, "Vissza", "Back", "Zurück", "Înapoi"), color = p.textSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (review != null) onApprove() else onPreview(style, progression, false) },
                enabled = !busy && (review != null || !active),
                colors = ButtonDefaults.buttonColors(containerColor = p.primary, contentColor = p.onPrimary),
                modifier = Modifier.testTag(if (review != null) "studio_modes_approve" else "studio_modes_preview")
            ) {
                Text(if (review != null)
                    workoutModeText(lang, "Jóváhagyom", "Approve", "Bestätigen", "Confirmă")
                    else workoutModeText(lang, "Előnézet", "Preview", "Vorschau", "Previzualizare"),
                    fontWeight = FontWeight.Bold)
            }
        }
    )
}
