package com.example.ui.screens

import androidx.compose.ui.semantics.selected

import androidx.compose.ui.semantics.semantics

import androidx.compose.foundation.BorderStroke

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassTile

import com.example.ui.theme.GlassMaterialRole

import com.example.ui.theme.glassControlFill

import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.ui.components.TrainPilotSwitch as Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.ui.coach.NativeCoachAdviceText
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.StudioProfileOptionLabels.progressionName
import com.example.localization.StudioProfileOptionLabels.trainingStyleName
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

/** Look up the exact persisted program ID; never display internal IDs as names. */
internal fun studioModeActiveProgramLabel(state: TrainPilotUiState): String {
    val lang = state.language
    val program = state.programs.firstOrNull { it.id == state.activeTrainingModeProgramId }
    return when {
        program != null -> program.localizedName(lang).ifBlank {
            workoutModeText(lang, "Névtelen program", "Unnamed program", "Unbenanntes Programm", "Program fără nume")
        }
        state.programs.isEmpty() -> workoutModeText(lang, "Programok betöltése…", "Loading programs…",
            "Programme werden geladen…", "Se încarcă programele…")
        state.activeTrainingModeProgramId.isBlank() -> workoutModeText(lang, "Nincs aktív program",
            "No active program", "Kein aktives Programm", "Niciun program activ")
        else -> workoutModeText(lang, "Az aktív program nem található", "Active program not found",
            "Aktives Programm nicht gefunden", "Programul activ nu a fost găsit")
    }
}

@Composable
private fun ModeSeparator(tag: String) {
    val p = LocalTrainPilotPalette.current
    HorizontalDivider(modifier = Modifier.fillMaxWidth().testTag(tag), thickness = 1.dp,
        color = p.primary.copy(alpha = 0.18f))
}

@Composable
private fun ModeOption(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val p = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) p.glassControlFill(p.primary, GlassMaterialRole.Selection) else p.glassTile(p.surfaceElevated),
        border = if (p.isLiquidGlass && selected) BorderStroke(1.dp, p.primary) else null,
        modifier = Modifier.fillMaxWidth().trainPilotGlassRim(p, RoundedCornerShape(12.dp))
            .semantics { this.selected = selected }
            .trainPilotClickable(RoundedCornerShape(12.dp), onClick = onClick).testTag(tag)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold,
            color = if (selected && !p.isLiquidGlass) p.onPrimary else p.textPrimary,
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
    onUndo: () -> Unit,
    embedded: Boolean = false
) {
    val p = LocalTrainPilotPalette.current
    val lang = state.language
    val context = LocalContext.current
    val localized = remember(context, lang) { nativeResources(context, lang.name.lowercase()) }
    val isProgram = scope == "program"
    val initialStyle = if (isProgram) state.programTrainingStyleCode else state.trainingStyleCode
    val initialProgression = if (isProgram) state.programProgressionModeCode else state.progressionModeCode
    var style by remember(scope, initialStyle) { mutableStateOf(initialStyle) }
    var progression by remember(scope, initialProgression) { mutableStateOf(initialProgression) }
    val review = state.progressionModeReview
    val busy = state.progressionModeBusy
    val active = state.isWorkoutSessionActive
    val unit = workoutModeText(lang, "mp", "sec", "Sek.", "sec")

    WorkoutModesContainer(
        embedded = embedded,
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
                modifier = if (embedded) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isProgram) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(workoutModeText(lang, "Aktív program", "Active program",
                            "Aktives Programm", "Program activ"), color = p.textSecondary)
                        Text(studioModeActiveProgramLabel(state), color = p.textPrimary,
                            modifier = Modifier.fillMaxWidth().testTag("studio_modes_active_program_name"))
                    }
                }
                if (review != null) {
                    Text(workoutModeText(lang, "Módváltás előnézete", "Mode change preview",
                        "Vorschau des Moduswechsels", "Previzualizare schimbare mod"),
                        fontWeight = FontWeight.Bold, color = p.textPrimary)
                    Text(
                        workoutModeText(lang, "Korábbi: ", "Before: ", "Vorher: ", "Înainte: ") +
                            trainingStyleName(lang, review.beforeStyle) + " / " + progressionName(lang, review.beforeProgression),
                        color = p.textSecondary)
                    Text(
                        workoutModeText(lang, "Új: ", "After: ", "Nachher: ", "După: ") +
                            trainingStyleName(lang, review.afterStyle) + " / " + progressionName(lang, review.afterProgression),
                        color = p.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        workoutModeText(lang, "Program-összehasonlítás: ",
                            "Program comparison: ", "Programmvergleich: ", "Comparație program: ") +
                            review.programName,
                        color = p.textPrimary, fontWeight = FontWeight.Bold)
                    Text(workoutModeText(lang,
                        "A súlyjavaslat csak indokolt edzéselőzményeknél változik. A jóváhagyás a progressziós szabályt menti, a korábban rögzített súlyokat nem írja át.",
                        "Suggested weights change only when workout history supports them. Approval saves progression rules; existing weights are not overwritten.",
                        "Gewichtsvorschläge ändern sich nur bei passenden Trainingsdaten. Die Bestätigung speichert die Progressionsregeln, ohne bestehende Gewichte zu überschreiben.",
                        "Greutățile propuse se modifică doar cu date de antrenament relevante. Confirmarea salvează regulile, fără a suprascrie greutățile existente."),
                        color = p.textSecondary, modifier = Modifier.testTag("studio_modes_weight_explanation"))
                    for ((index, row) in review.rows.withIndex()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = p.glassTile(p.surfaceElevated),
                            modifier = Modifier.fillMaxWidth().testTag("studio_modes_comparison_$index")
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(row.day, color = p.textSecondary)
                                Text(row.exercise, color = p.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    workoutModeText(lang, "Sorozat: ", "Sets: ", "Sätze: ", "Serii: ") +
                                        "${row.beforeSets ?: "—"} → ${row.afterSets ?: "—"}",
                                    color = p.textPrimary)
                                Text(
                                    workoutModeText(lang, "Ismétlés: ", "Reps: ", "Wiederholungen: ", "Repetări: ") +
                                        "${row.beforeReps} → ${row.afterReps}",
                                    color = p.textPrimary)
                                Text(
                                    workoutModeText(lang, "Súly (kg): ", "Weight (kg): ", "Gewicht (kg): ", "Greutate (kg): ") +
                                        "${row.beforeWeight ?: "—"} → ${row.afterWeight ?: "—"}",
                                    color = p.textPrimary)
                                Text(
                                    workoutModeText(lang, "Pihenő: ", "Rest: ", "Pause: ", "Pauză: ") +
                                        "${row.restSeconds ?: "—"} $unit · RIR: ${row.rir}",
                                    color = p.textSecondary)
                                Text(
                                    workoutModeText(lang, "Javaslat indoka: ", "Recommendation: ",
                                        "Begründung: ", "Motivul recomandării: ") +
                                        NativeCoachAdviceText.reason(localized, row.reason),
                                    color = p.textSecondary,
                                    modifier = Modifier.testTag("studio_modes_comparison_reason_$index"))
                            }
                        }
                    }
                    if (review.inheritGlobal) Text(
                        workoutModeText(lang, "A program a globális beállítást fogja örökölni.",
                            "The program will inherit the global setting.",
                            "Das Programm übernimmt die globale Einstellung.",
                            "Programul va moșteni setarea globală."),
                        color = p.textSecondary)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Alapértelmezett pihenőidő", "Default rest timer",
                            "Standard-Pausenzeit", "Timp implicit de pauză"), color = p.textPrimary)
                        Surface(shape = RoundedCornerShape(10.dp), color = p.glassTile(p.surfaceElevated),
                            modifier = Modifier.trainPilotClickable(RoundedCornerShape(10.dp), onClick = onCycleRest).testTag("studio_modes_rest")) {
                            Text("${state.defaultRestSec} $unit ↻", color = p.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    ModeSeparator("studio_modes_separator_rest_sound")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Hangjelzés pihenő végén", "Sound alert after rest",
                            "Ton am Pausenende", "Sunet la sfârșitul pauzei"), color = p.textPrimary,
                            modifier = Modifier.weight(1f))
                        Switch(checked = state.soundAlertEnabled, onCheckedChange = onToggleSound,
                            modifier = Modifier.testTag("studio_modes_sound"))
                    }
                    ModeSeparator("studio_modes_separator_sound_vibration")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workoutModeText(lang, "Rezgés pihenő végén", "Vibration after rest",
                            "Vibration am Pausenende", "Vibrație la sfârșitul pauzei"),
                            color = p.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = state.vibrationEnabled, onCheckedChange = onToggleVibration,
                            modifier = Modifier.testTag("studio_modes_vibration"))
                    }
                    ModeSeparator("studio_modes_separator_vibration_style")
                    Text(workoutModeText(lang, "Edzésstílus", "Training style", "Trainingsstil",
                        "Stil de antrenament"), color = p.textPrimary, fontWeight = FontWeight.Bold)
                    ModeOption(trainingStyleName(lang, "normal"), style == "normal", "studio_modes_style_normal") { style = "normal" }
                    ModeOption(trainingStyleName(lang, "hit"), style == "hit", "studio_modes_style_hit") { style = "hit" }
                    Text(workoutModeText(lang,
                        "A HIT-inspirált stílus alacsonyabb munkasorozat-számot javasol; nem ír elő bukásig edzést.",
                        "HIT-inspired mode proposes fewer working sets; it does not require training to failure.",
                        "HIT empfiehlt weniger Arbeitssätze; Training bis zum Muskelversagen ist nicht verpflichtend.",
                        "HIT propune mai puține serii; nu necesită epuizare musculară."),
                        color = p.textSecondary)
                    ModeSeparator("studio_modes_separator_style_progression")
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
                    if (canUndo) com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = onUndo, enabled = !busy && !active) {
                        Text(workoutModeText(lang, "Utolsó módváltás visszavonása", "Undo last mode change",
                            "Letzten Moduswechsel rückgängig machen", "Anulează ultima schimbare"),
                            color = androidx.compose.material3.LocalContentColor.current)
                    }
                    if (active) Text(
                        workoutModeText(lang, "Aktív edzés közben a módváltás nem menthető.",
                            "Mode changes cannot be saved during an active workout.",
                            "Moduswechsel können während eines Trainings nicht gespeichert werden.",
                            "Modurile nu pot fi schimbate în timpul antrenamentului."),
                        color = p.textSecondary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (review != null) onEditPreview() else onDismiss() }, enabled = !busy,
                modifier = Modifier.testTag("studio_modes_dismiss")) {
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

/** The same editor/actions render inline in Settings and in a dialog for programs. */
@Composable
private fun WorkoutModesContainer(
    embedded: Boolean,
    onDismissRequest: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    titleContentColor: androidx.compose.ui.graphics.Color,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit
) {
    if (embedded) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            text()
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                confirmButton()
                dismissButton()
            }
        }
    } else {
        TrainPilotAlertDialog(onDismissRequest = onDismissRequest, containerColor = containerColor,
            titleContentColor = titleContentColor, title = title, text = text,
            dismissButton = dismissButton, confirmButton = confirmButton)
    }
}
