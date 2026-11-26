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
    // 2816: a mentett adat a Samsung-kapcsolótól függetlenül látszik (az csak az új közvetlen lekérdezést szabályozza).
    val result = remember(detail.workout) { NativeWorkoutHealthSummary.read(detail.workout) }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    fun sources(metric: NativeWorkoutHealthSummary.Metric) = metric.evidence.map {
        if (NativeWorkoutHealthSummary.samsungOriginViaHealthConnect(it)) local("Samsung Health – Health Connecten át",
            "Samsung Health – via Health Connect", "Samsung Health – über Health Connect", "Samsung Health – prin Health Connect")
        else NativeWorkoutHealthSummary.label(it.provider)
    }.distinct()
    Column(Modifier.fillMaxWidth().background(colors.card, MaterialTheme.shapes.large)
        .trainPilotFrame(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_workout_health_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Edzés pulzus és energia", "Workout heart rate and energy", "Trainingspuls und Energie", "Puls și energie antrenament"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        val displayEnergy = listOf(result.workoutTotal)
        val displayed = listOf("summary_heart" to result.heart) +
            displayEnergy.map { metric -> "summary_energy" to metric }
        for ((tag, metric) in displayed) {
            // Sample counts remain part of the native aggregation and Source details,
            // but are technical provenance, not a headline workout measurement.
            val visibleValues = metric.values.filterKeys { it != "heartRateSamples" }
            JournalHealthMetrics(visibleValues, tag, text, number)
            if (tag == "summary_energy" && visibleValues.isNotEmpty()) {
                // Ismert hiányos lefedettség: az érték marad, „részleges" jelzéssel; ismeretlen: „nem igazolt".
                val note = when {
                    metric.incomplete -> local("Részleges mérés", "Partial measurement", "Teilweise Messung", "Măsurare parțială")
                    metric.hasUnverifiedProviderCoverage -> local("Lefedettség nem igazolt", "Coverage unverified",
                        "Abdeckung ungeprüft", "Acoperire neverificată")
                    else -> null
                }
                Text(listOfNotNull(sources(metric).joinToString(", ").takeIf { it.isNotBlank() }, note).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = colors.muted,
                    modifier = Modifier.testTag("journal_summary_energy_note"))
            }
            if (visibleValues.isEmpty()) Text(text.getString(R.string.journal_no_data), color = colors.muted,
                modifier = Modifier.testTag("journal_${tag}_missing"))
            // Az aktív kcal külön sorban marad; a fő energia mindig a teljes kcal, a kettő nem adódik össze.
            if (tag == "summary_energy") result.activeEnergy.values.takeIf { it.isNotEmpty() }?.let {
                JournalHealthMetrics(it, "summary_active", text, number)
            }
        }
        val providers = displayed.flatMap { sources(it.second) }.distinct()
        val status = if (displayed.any { it.second.incomplete } || result.rejectedEvidence)
            local("Részleges / hiányzó mérés", "Partial / missing measurement",
                "Teilweise / fehlende Messung", "Măsurare parțială / lipsă") else null
        val footer = listOfNotNull(providers.takeIf { it.isNotEmpty() }?.joinToString(", "), status).joinToString(" · ")
        if (footer.isNotBlank()) Text(footer, color = colors.muted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.testTag("journal_summary_sources"))
    }
}
