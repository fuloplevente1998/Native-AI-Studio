package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.ui.theme.TrainPilotButton
import com.repforge.app.ui.theme.TrainPilotTextButton
import com.repforge.app.ui.theme.NativeThemeTokens

/** Shared editor for the active exercise and its completion dialog. The host owns scrolling. */
@Composable
internal fun WorkoutFeedbackPopup(
    exerciseName: String, effort: String?, note: String, language: String,
    enabled: Boolean, onEffort: (String?) -> Unit, onNote: (String) -> Unit, tag: String,
    modifier: Modifier = Modifier
) {
    val p = LocalTrainPilotPalette.current
    val selected = if (effort == "good") "right" else effort
    Column(modifier.fillMaxWidth().testTag(tag),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(exerciseName, color = p.primary, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text(feedbackText(language, "Milyen volt a gyakorlat?", "How was the exercise?", "Wie war die Übung?", "Cum a fost exercițiul?"),
            color = p.textPrimary, style = MaterialTheme.typography.bodyMedium)
        BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
            val fontScale = LocalDensity.current.fontScale
            val columns = when {
                maxWidth >= 300.dp && fontScale <= 1.3f -> 3
                maxWidth >= 200.dp && fontScale <= 1.7f -> 2
                else -> 1
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioWorkoutFeedback.efforts.chunked(columns).forEach { keys ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        keys.forEach { key ->
                            val chosen = selected == key
                            val paint = workoutFeedbackPaint(p, key, chosen)
                            val shape = RoundedCornerShape(12.dp)
                            Surface(shape = shape, color = paint.fill, border = BorderStroke(1.dp, paint.border),
                                modifier = Modifier.weight(1f).heightIn(min = 54.dp)
                                    .trainPilotGlassRim(p, shape, if (chosen) GlassMaterialRole.Selection else GlassMaterialRole.Button)
                                    .trainPilotSelectable(chosen, shape, enabled = enabled, role = Role.RadioButton, onClick = { onEffort(key) })
                                    .testTag("${tag}_$key")) {
                                Row(Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Text(feedbackChoiceLabel(key, language), color = paint.text,
                                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                                    if (chosen) Icon(Icons.Default.Check, contentDescription = null,
                                        tint = paint.text, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        repeat(columns - keys.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(feedbackText(language, "Megjegyzés (opcionális)", "Note (optional)", "Notiz (optional)", "Notiță (opțional)"),
                color = p.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (effort != null) TrainPilotTextButton(onClick = { onEffort(null) }, enabled = enabled,
                modifier = Modifier.testTag("${tag}_clear")) {
                Text(feedbackText(language, "Válasz törlése", "Clear answer", "Antwort löschen", "Șterge răspunsul"),
                    color = p.danger, style = MaterialTheme.typography.labelMedium)
            }
        }
        TrainPilotTextField(value = note, onValueChange = { onNote(it.take(3000)) }, enabled = enabled,
            placeholder = { Text(feedbackText(language, "Rövid megjegyzés…", "Short note…", "Kurze Notiz…", "Notiță scurtă…")) },
            trailingIcon = if (note.isNotBlank()) {
                { TrainPilotIconButton(onClick = { onNote("") }, enabled = enabled) {
                    Icon(Icons.Default.Close, feedbackText(language, "Megjegyzés törlése", "Clear note", "Notiz löschen", "Șterge notița"))
                } }
            } else null,
            modifier = Modifier.fillMaxWidth().testTag("${tag}_note"), maxLines = 3,
            textStyle = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun WorkoutFeedbackAction(text: String, primary: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(14.dp)
    val fill = if (primary) {
        if (p.isLiquidGlass) p.glassStateTile(p.primary, p.primary, strong = true) else p.primary
    } else p.glassTile(p.surfaceElevated, elevated = true)
    val textColor = if (primary && !p.isLiquidGlass) NativeThemeTokens.onFill(p.primary) else p.textPrimary
    TrainPilotButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = shape, elevation = null, colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = textColor),
        border = BorderStroke(1.dp, if (primary) p.primary else p.cardBorder),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
