package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotActionAppearance
import com.repforge.app.ui.theme.TrainPilotButton
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

private fun bb(lang: AppLanguage, hu: String, en: String, de: String, ro: String) = when (lang) {
    AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
}

/**
 * Active workout bottom bar (2772, option B): ‹ Previous · ⏹ Finish · Next ›.
 * Finish opens a small sheet: save & finish (summary) / discard (confirmed) / keep training.
 * Next carries no exercise name (the strip above shows it); on the last exercise it reads "Save ›".
 */
@Composable
fun ActiveWorkoutBottomBar(
    language: AppLanguage,
    enabled: Boolean,
    hasNext: Boolean,
    completedSets: Int,
    totalSets: Int,
    elapsedLabel: String,
    tagPrefix: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var showFinish by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TrainPilotOutlinedButton(
            onClick = onPrevious, enabled = enabled,
            colors = WorkoutControlAppearance.secondaryColors(), border = WorkoutControlAppearance.secondaryBorder(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tagPrefix}_prev_exercise_btn")
        ) { Text(bb(language, "‹ Előző", "‹ Previous", "‹ Zurück", "‹ Anterior"), fontWeight = FontWeight.SemiBold, maxLines = 1) }
        // 2775: theme-following secondary button with a drawn stop icon (no emoji square).
        TrainPilotOutlinedButton(
            onClick = { showFinish = true }, enabled = enabled,
            colors = WorkoutControlAppearance.secondaryColors(), border = WorkoutControlAppearance.secondaryBorder(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tagPrefix}_finish_menu_btn")
        ) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Stop, contentDescription = null,
                tint = palette.primary, modifier = Modifier.size(18.dp).testTag("${tagPrefix}_finish_icon"))
            androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
            Text(bb(language, "Befejezés", "Finish", "Beenden", "Finalizare"), fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        TrainPilotButton(
            onClick = onNext, enabled = enabled, shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
            modifier = Modifier.weight(1f).height(48.dp).testTag("${tagPrefix}_next_exercise_btn")
        ) {
            Text(if (hasNext) bb(language, "Következő ›", "Next ›", "Weiter ›", "Următorul ›")
                else bb(language, "Mentés ›", "Save ›", "Speichern ›", "Salvează ›"),
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (showFinish) WorkoutFinishSheet(language, enabled, completedSets, totalSets, elapsedLabel, tagPrefix,
        onFinish = onFinish, onDiscard = onDiscard, onDismiss = { showFinish = false })
}


/** Shared finish sheet: save & finish (summary) / discard after confirmation / keep training. */
@Composable
fun WorkoutFinishSheet(
    language: AppLanguage,
    enabled: Boolean,
    completedSets: Int,
    totalSets: Int,
    elapsedLabel: String,
    tagPrefix: String,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    TrainPilotScreenSheet(onDismiss = onDismiss, testTag = "${tagPrefix}_finish_sheet") {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(bb(language, "Edzés befejezése", "Finish workout", "Training beenden", "Finalizare antrenament"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text((if (elapsedLabel.isBlank()) "" else "$elapsedLabel · ") + bb(language, "$completedSets / $totalSets sorozat kész", "$completedSets / $totalSets sets done",
                "$completedSets / $totalSets Sätze erledigt", "$completedSets / $totalSets serii gata"),
                style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = { onDismiss(); onFinish() }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_finish_save")
            ) { Text(bb(language, "✓ Mentés és befejezés", "✓ Save and finish", "✓ Speichern und beenden", "✓ Salvează și încheie"), fontWeight = FontWeight.Bold) }
            com.repforge.app.ui.theme.TrainPilotActionButton(positive = false,
                onClick = { confirmDiscard = true }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_finish_discard")
            ) { Text(bb(language, "✕ Elvetés", "✕ Discard", "✕ Verwerfen", "✕ Renunță"), fontWeight = FontWeight.Bold) }
            TrainPilotOutlinedButton(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tagPrefix}_finish_continue")
            ) { Text(bb(language, "Folytatom az edzést", "Keep training", "Weiter trainieren", "Continui antrenamentul")) }
        }
    }
    if (confirmDiscard) TrainPilotAlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(bb(language, "Edzés elvetése?", "Discard workout?", "Training verwerfen?", "Renunți la antrenament?")) },
        text = { Text(bb(language, "A mostani edzés nem kerül mentésre. Nem visszavonható.", "This workout will not be saved. This cannot be undone.",
            "Dieses Training wird nicht gespeichert. Das kann nicht rückgängig gemacht werden.", "Antrenamentul nu va fi salvat. Nu se poate anula.")) },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotActionButton(positive = false,
                onClick = { confirmDiscard = false; onDismiss(); onDiscard() },
                modifier = Modifier.testTag("${tagPrefix}_discard_confirm")
            ) { Text(bb(language, "Elvetés", "Discard", "Verwerfen", "Renunță"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TrainPilotOutlinedButton(onClick = { confirmDiscard = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(bb(language, "Mégse", "Cancel", "Abbrechen", "Anulează"))
            }
        }
    )
}
