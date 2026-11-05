package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.R
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.ui.text.nativeResources

/** Optional, compact feedback; expand only the exercise being answered. */
@Composable
internal fun ExerciseFeedbackControl(effort: String?, note: String, language: String,
    onEffort: (String?) -> Unit, onNote: (String) -> Unit, title: String = "",
    enabled: Boolean = true, effortLocked: Boolean = false, tag: String) {
    val text = nativeResources(LocalContext.current, language)
    val labels = listOf(R.string.journal_effort_easy, R.string.journal_effort_light,
        R.string.journal_effort_good, R.string.journal_effort_challenging,
        R.string.journal_effort_hard, R.string.journal_effort_pain)
    var expanded by rememberSaveable(tag) { mutableStateOf(false) }
    val selected = StudioWorkoutFeedback.efforts.indexOf(if (effort == "good") "right" else effort)
    val colors = LocalTrainPilotColors.current
    Surface(Modifier.fillMaxWidth().testTag(tag), color = colors.card,
        shape = RoundedCornerShape(12.dp), border = TrainPilotFrames.control(colors.line)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                val heading = title.ifBlank { text.getString(R.string.journal_feedback) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(heading, style = MaterialTheme.typography.labelMedium, color = colors.muted)
                    Text(if (selected >= 0) text.getString(labels[selected]) else "—",
                        style = MaterialTheme.typography.bodyMedium, color = colors.accentText)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                    tint = colors.accentText, modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                if (effortLocked) Text(text.getString(R.string.journal_feedback_locked), style = MaterialTheme.typography.bodySmall)
                com.repforge.app.ui.journal.JournalEffortDropdown(
                    selected = effort, choices = labels.mapIndexed { index, label ->
                        StudioWorkoutFeedback.efforts[index] to text.getString(label) },
                    prompt = text.getString(R.string.journal_feedback), enabled = enabled && !effortLocked,
                    onSelect = { onEffort(it) }, tag = "${tag}_selector", optionTagPrefix = tag)
                TextButton(onClick = { onEffort(null) }, enabled = enabled && !effortLocked && effort != null,
                    modifier = Modifier.testTag("${tag}_clear")) {
                    Text(when (language) { "en" -> "Clear answer"; "de" -> "Antwort löschen"; "ro" -> "Șterge răspunsul"; else -> "Válasz törlése" })
                }
                OutlinedTextField(note, { onNote(it.take(3000)) }, enabled = enabled,
                    label = { Text(when (language) { "en" -> "Exercise note"; "de" -> "Übungsnotiz"; "ro" -> "Notiță exercițiu"; else -> "Gyakorlatjegyzet" }) },
                    shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium,
                    colors = com.repforge.app.ui.journal.journalEditorFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("${tag}_note"), maxLines = 4)
            }
        }
    }
}
