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
    val result = remember(detail.workout) { NativeWorkoutHealthSummary.read(detail.workout) }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().background(colors.card, MaterialTheme.shapes.large)
        .trainPilotFrame(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_workout_health_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Edzés pulzus és energia", "Workout heart rate and energy", "Trainingspuls und Energie", "Puls și energie antrenament"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        val displayed = listOf(
            Triple("summary_heart", "averageHeartRate", result.heart),
            Triple("summary_energy", "totalCalories", result.workoutTotal)
        ) + listOf(Triple("summary_energy", "activeCalories", result.activeEnergy)).filter { it.third.values.isNotEmpty() }
        for ((tag, key, metric) in displayed) {
            val visibleValues = metric.values.filterKeys { it != "heartRateSamples" }
            JournalHealthMetrics(visibleValues, tag, text, number)
            if (visibleValues.isNotEmpty()) {
                val sources = NativeWorkoutHealthSummary.labels(metric, key, language)
                val coverage = when {
                    metric.incomplete -> local("Részleges mérés", "Partial measurement", "Teilweise Messung", "Măsurare parțială")
                    key != "averageHeartRate" && metric.hasUnverifiedProviderCoverage ->
                        local("Lefedettség nem igazolt", "Coverage unverified", "Abdeckung ungeprüft", "Acoperire neverificată")
                    else -> null
                }
                Text(listOfNotNull(sources.takeIf { it.isNotBlank() }, coverage).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = colors.muted)
            }
            if (visibleValues.isEmpty()) Text(text.getString(R.string.journal_no_data), color = colors.muted,
                modifier = Modifier.testTag("journal_${tag}_missing"))
        }
        val providers = displayed.map { (_, key, metric) -> NativeWorkoutHealthSummary.labels(metric, key, language) }
            .filter { it.isNotBlank() }.distinct()
        val status = if (displayed.any { it.third.incomplete } || result.rejectedEvidence)
            local("Részleges / hiányzó mérés", "Partial / missing measurement",
                "Teilweise / fehlende Messung", "Măsurare parțială / lipsă") else null
        val footer = listOfNotNull(providers.takeIf { it.isNotEmpty() }?.joinToString(", "), status).joinToString(" · ")
        if (footer.isNotBlank()) Text(footer, color = colors.muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag("journal_summary_sources"))
    }
}
