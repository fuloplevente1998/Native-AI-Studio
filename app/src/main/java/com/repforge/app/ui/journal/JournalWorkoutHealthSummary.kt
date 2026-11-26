package com.repforge.app.ui.journal
import com.repforge.app.ui.theme.trainPilotFrame

import android.content.res.Resources
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
    Column(Modifier.fillMaxWidth().background(colors.card, MaterialTheme.shapes.large)
        .trainPilotFrame(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_workout_health_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Edzés pulzus és energia", "Workout heart rate and energy", "Trainingspuls und Energie", "Puls și energie antrenament"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        val values = NativeWorkoutHealthSummary.journalValues(result)
        if (values.isEmpty()) {
            Text(local("Nincs óramérés ehhez az edzéshez.", "No watch measurement for this workout.",
                "Keine Uhrmessung für dieses Training.", "Nu există măsurători de pe ceas pentru acest antrenament."),
                color = colors.muted, modifier = Modifier.testTag("journal_wear_measurement_missing"))
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
