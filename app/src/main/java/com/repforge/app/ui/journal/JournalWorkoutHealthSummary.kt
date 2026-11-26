package com.repforge.app.ui.journal
import com.repforge.app.ui.theme.trainPilotFrame
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.LocalTrainPilotPalette

import android.content.res.Resources
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.NativeWorkoutHealthSummary
import com.repforge.app.R
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun JournalWorkoutHealthSummary(detail: WorkoutHistoryWithDetails, text: Resources, language: String) {
    if (detail.workout.originDevice == "external") {
        JournalExternalWorkoutMetrics(detail, text, language, showSource = false)
        return
    }
    val colors = LocalTrainPilotColors.current
    val result = remember(detail.workout) { NativeWorkoutHealthSummary.readJournal(detail.workout) }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().background(LocalTrainPilotPalette.current.overlayRoleFill(colors.card, GlassMaterialRole.Panel), MaterialTheme.shapes.large)
        .trainPilotFrame(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_workout_health_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Edzés pulzus és energia", "Workout heart rate and energy", "Trainingspuls und Energie", "Puls și energie antrenament"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        val values = NativeWorkoutHealthSummary.journalValues(result)
        if (values.isEmpty()) {
            Text(local("Nincs óramérés ehhez az edzéshez.", "No watch measurement for this workout.",
                "Keine Uhrmessung für dieses Training.", "Nu există măsurători de pe ceas pentru acest antrenament."),
                color = colors.muted, modifier = Modifier.testTag("journal_wear_measurement_missing"))
            JournalWearInstallTip(detail.workout.id, text)
        } else {
            JournalHealthMetrics(values.filterKeys { it.endsWith("HeartRate") }, "summary_heart", text, number, compact = true)
            JournalHealthMetrics(values.filterKeys { it.endsWith("Calories") }, "summary_energy", text, number, compact = true)
            val measured = listOf(result.heart, result.activeEnergy, result.workoutTotal).filter { it.values.isNotEmpty() }
            val partial = measured.any { it.incomplete } || values.size < 4
            Text(local("Forrás: TrainPilot WearOS", "Source: TrainPilot WearOS", "Quelle: TrainPilot WearOS",
                "Sursă: TrainPilot WearOS") + if (partial) " · " +
                local("Részleges mérés", "Partial measurement", "Teilweise Messung", "Măsurare parțială") else "",
                style = MaterialTheme.typography.labelSmall, color = colors.muted,
                modifier = Modifier.testTag("journal_summary_sources"))
        }
    }
}

/** Presentation-only dismissal, separate from health evidence, Room and DataStore. */
private const val WEAR_TIP_PREFS = "journal-wear-tip-2817"
internal fun wearInstallIntent(web: Boolean = false): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(
    if (web) "https://play.google.com/store/apps/details?id=com.TrainPilot.com"
    else "market://details?id=com.TrainPilot.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun JournalWearInstallTip(workoutId: String, text: Resources) {
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val prefs = remember(context) { context.applicationContext.getSharedPreferences(WEAR_TIP_PREFS, Context.MODE_PRIVATE) }
    var dismissed by remember(workoutId, prefs) { mutableStateOf(prefs.getBoolean(workoutId, false)) }
    if (dismissed) return
    Column(Modifier.fillMaxWidth()
        .background(LocalTrainPilotPalette.current.overlayRoleFill(colors.card2.copy(alpha = .35f), GlassMaterialRole.Panel), MaterialTheme.shapes.medium)
        .trainPilotFrame(TrainPilotFrames.control(colors.line), MaterialTheme.shapes.medium)
        .padding(12.dp).testTag("journal_wear_install_tip"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text.getString(R.string.journal_wear_install_tip), color = colors.text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotOutlinedButton(onClick = {
                val opened = runCatching { context.startActivity(wearInstallIntent()) }.isSuccess ||
                    runCatching { context.startActivity(wearInstallIntent(web = true)) }.isSuccess
                if (!opened) Toast.makeText(context, text.getString(R.string.journal_wear_install_unavailable),
                    Toast.LENGTH_LONG).show()
            }, modifier = Modifier.weight(1.5f).heightIn(min = 48.dp).testTag("journal_wear_install")) {
                Text(text.getString(R.string.journal_wear_install))
            }
            TrainPilotOutlinedButton(onClick = {
                prefs.edit().putBoolean(workoutId, true).apply()
                dismissed = true
            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("journal_wear_tip_later")) {
                Text(text.getString(R.string.journal_wear_tip_later))
            }
        }
    }
}
