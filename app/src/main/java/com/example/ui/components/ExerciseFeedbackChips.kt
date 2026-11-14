package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.ui.theme.trainPilotBorder

internal fun feedbackText(language: String, hu: String, en: String, de: String, ro: String): String =
    when (language.lowercase().take(2)) { "en" -> en; "de" -> de; "ro" -> ro; else -> hu }

/** Five difficulty levels and a separate pain warning, using the existing six persisted values. */
internal fun feedbackChoiceLabel(effort: String?, language: String): String = when (effort) {
    "easy" -> "😄 " + feedbackText(language, "Túl könnyű", "Too easy", "Zu leicht", "Prea ușor")
    "light" -> "🙂 " + feedbackText(language, "Könnyű", "Easy", "Leicht", "Ușor")
    "right", "good" -> "💪 " + feedbackText(language, "Pont jó", "Just right", "Genau richtig", "Tocmai bun")
    "challenging" -> "😤 " + feedbackText(language, "Nehéz", "Hard", "Schwer", "Greu")
    "hard" -> "🥵 " + feedbackText(language, "Túl nehéz", "Too hard", "Zu schwer", "Prea greu")
    "pain" -> "⚠️ " + feedbackText(language, "Fájdalom", "Pain", "Schmerz", "Durere")
    else -> feedbackText(language, "Nincs megadva", "Not provided", "Nicht angegeben", "Nespecificat")
}

/** Direct choices wrap with available width/font size; no dropdown or preselected answer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseFeedbackChips(effort: String?, note: String, language: String,
    onEffort: (String?) -> Unit, onNote: (String) -> Unit, title: String = "",
    enabled: Boolean = true, tag: String, noteTitle: String? = null) {
    val palette = LocalTrainPilotPalette.current
    var showNote by rememberSaveable(tag) { mutableStateOf(note.isNotBlank()) }
    val selected = if (effort == "good") "right" else effort
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
        FlowRow(Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StudioWorkoutFeedback.efforts.forEach { key ->
                val chosen = selected == key
                val accent = if (key == "pain") com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline else palette.primary
                Surface(shape = RoundedCornerShape(12.dp),
                    color = if (chosen) accent.copy(alpha = 0.2f) else if (key == "pain") accent.copy(alpha = 0.04f) else palette.surfaceElevated,
                    modifier = Modifier.heightIn(min = 48.dp)
                        .trainPilotBorder(1.dp, if (chosen) accent else if (key == "pain") accent.copy(alpha = 0.45f) else palette.cardBorder, RoundedCornerShape(12.dp))
                        .selectable(chosen, enabled = enabled, role = Role.RadioButton, onClick = { onEffort(key) })
                        .testTag("${tag}_$key")) {
                    Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(feedbackChoiceLabel(key, language), color = if (chosen) accent else palette.textPrimary,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { showNote = !showNote }, enabled = enabled,
                modifier = Modifier.testTag("${tag}_toggle_note")) {
                Text(feedbackText(language, "Megjegyzés", "Note", "Notiz", "Notiță"))
            }
            if (effort != null) com.repforge.app.ui.theme.TrainPilotNegativeButton(onClick = { onEffort(null) }, enabled = enabled,
                modifier = Modifier.testTag("${tag}_clear")) {
                Text(feedbackText(language, "Válasz törlése", "Clear answer", "Antwort löschen", "Șterge răspunsul"))
            }
        }
        if (showNote) OutlinedTextField(note, { onNote(it.take(3000)) }, enabled = enabled,
            label = { Text(noteTitle ?: feedbackText(language, "Gyakorlatjegyzet", "Exercise note", "Übungsnotiz", "Notiță exercițiu")) },
            modifier = Modifier.fillMaxWidth().testTag("${tag}_note"), maxLines = 3,
            shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium,
            colors = com.repforge.app.ui.journal.journalEditorFieldColors())
    }
}

/** Summary reviews the existing answer. Editing is explicit and keeps its current selection. */
@Composable
internal fun StudioExerciseFeedbackReview(effort: String?, note: String, language: String,
    title: String, enabled: Boolean, onEffort: (String?) -> Unit, onNote: (String) -> Unit, tag: String) {
    var editing by rememberSaveable(tag) { mutableStateOf(false) }
    val palette = LocalTrainPilotPalette.current
    Surface(Modifier.fillMaxWidth().testTag(tag), color = palette.surfaceCard,
        shape = RoundedCornerShape(14.dp), border = com.repforge.app.ui.theme.TrainPilotFrames.control(
            if (effort == "pain") com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline.copy(alpha = 0.45f) else palette.cardBorder)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
            Text(feedbackChoiceLabel(effort, language), color = if (effort == "pain") palette.danger else palette.primary,
                modifier = Modifier.testTag("${tag}_answer"), style = MaterialTheme.typography.bodyMedium)
            if (note.isNotBlank() && !editing) Text(note, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { editing = !editing }, enabled = enabled, modifier = Modifier.testTag("${tag}_edit")) {
                Text(if (editing) feedbackText(language, "Kész", "Done", "Fertig", "Gata")
                    else if (effort != null || note.isNotBlank()) feedbackText(language, "Módosítás", "Edit", "Bearbeiten", "Modifică")
                    else feedbackText(language, "Pótlás", "Add feedback", "Ergänzen", "Completează"))
            }
            if (editing) ExerciseFeedbackChips(effort, note, language, onEffort, onNote,
                enabled = enabled, tag = "${tag}_editor")
        }
    }
}

/** Overall feeling is optional and separate from the exercise answers. */
@Composable
internal fun StudioWorkoutOverallFeedback(effort: String?, note: String, language: String,
    enabled: Boolean, onEffort: (String?) -> Unit, onNote: (String) -> Unit, tag: String = "workout_overall_feedback") {
    var expanded by rememberSaveable(tag) { mutableStateOf(false) }
    val palette = LocalTrainPilotPalette.current
    val optionalWord = feedbackText(language, "Opcionális", "Optional", "Optional", "Opțional")
    val secondaryText = if (effort != null) "$optionalWord • ${feedbackChoiceLabel(effort, language)}" else optionalWord
    Surface(Modifier.fillMaxWidth().testTag(tag), color = palette.surfaceCard, shape = RoundedCornerShape(14.dp),
        border = com.repforge.app.ui.theme.TrainPilotFrames.control(
            if (effort == "pain") com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline.copy(alpha = 0.45f) else palette.cardBorder)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp).testTag("${tag}_toggle")
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = feedbackText(
                            language,
                            "Edzés összesített visszajelzése",
                            "Overall workout feedback",
                            "Gesamtfeedback zum Training",
                            "Feedback general antrenament"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                Icon(if (expanded) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore,
                    contentDescription = null, tint = palette.primary)
            }
            if (expanded) {
                Text(feedbackText(language, "Az egész edzésről szól; a gyakorlatok válaszait nem módosítja.",
                    "For the whole workout; it does not change exercise answers.",
                    "Für das gesamte Training; Übungsantworten bleiben erhalten.",
                    "Pentru întregul antrenament; nu modifică răspunsurile exercițiilor."),
                    color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                ExerciseFeedbackChips(effort, note, language, onEffort, onNote, enabled = enabled,
                    tag = "${tag}_choices", noteTitle = feedbackText(language, "Edzésjegyzet", "Workout note", "Trainingsnotiz", "Notiță antrenament"))
            } else if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
    }
}
