package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
@OptIn(ExperimentalLayoutApi::class)
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
        shape = RoundedCornerShape(16.dp), border = TrainPilotFrames.stroke(0.75.dp, colors.line)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                val heading = title.ifBlank { text.getString(R.string.journal_feedback) }
                Text("$heading · ${if (selected >= 0) text.getString(labels[selected]) else "—"} ${if (expanded) "⌃" else "⌄"}")
            }
            if (expanded) {
                if (effortLocked) Text(text.getString(R.string.journal_feedback_locked), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEachIndexed { index, label ->
                        FilterChip(selected = selected == index, onClick = { onEffort(StudioWorkoutFeedback.efforts[index]) },
                            enabled = enabled && !effortLocked, label = { Text(text.getString(label)) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_$index"))
                    }
                    TextButton(onClick = { onEffort(null) }, enabled = enabled && !effortLocked && effort != null,
                        modifier = Modifier.testTag("${tag}_clear")) {
                        Text(when (language) { "en" -> "Clear answer"; "de" -> "Antwort löschen"; "ro" -> "Șterge răspunsul"; else -> "Válasz törlése" })
                    }
                }
                OutlinedTextField(note, { onNote(it.take(3000)) }, enabled = enabled,
                    label = { Text(when (language) { "en" -> "Exercise note"; "de" -> "Übungsnotiz"; "ro" -> "Notiță exercițiu"; else -> "Gyakorlatjegyzet" }) },
                    modifier = Modifier.fillMaxWidth().testTag("${tag}_note"), maxLines = 4)
            }
        }
    }
}
