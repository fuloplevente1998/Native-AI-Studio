package com.repforge.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeWorkoutHealthSummary
import com.repforge.app.R
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.trainPilotFrame
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.LocalTrainPilotPalette
import java.text.NumberFormat

/** TrainPilot history has one measurement source; imported workouts keep their own evidence. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun JournalHealthPanel(detail: WorkoutHistoryWithDetails, text: android.content.res.Resources,
    language: String, refreshing: Boolean, error: Int?, enabled: Boolean,
    provider: String = HealthJournalStore.HC, status: String? = null,
    selectProvider: (String) -> Unit = {}, permissions: (@Composable (String) -> Unit)? = null,
    expandedState: MutableState<String?>? = null,
    samsungEnabled: Boolean = com.repforge.app.NativePrivacyControls.enabled(androidx.compose.ui.platform.LocalContext.current, com.repforge.app.NativePrivacyControls.SAMSUNG_HEALTH),
    refresh: () -> Unit) {
    if (detail.workout.originDevice == "external") {
        JournalExternalWorkoutMetrics(detail, text, language)
        return
    }
    val colors = LocalTrainPilotColors.current
    val summary = remember(detail.workout) { NativeWorkoutHealthSummary.readJournal(detail.workout) }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().background(LocalTrainPilotPalette.current.overlayRoleFill(colors.card, GlassMaterialRole.Panel), MaterialTheme.shapes.large)
        .trainPilotFrame(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_health_source_group"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Forrás", "Source", "Quelle", "Sursă"), style = MaterialTheme.typography.titleMedium, color = colors.text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(local("Pulzus, energia", "Heart rate, energy", "Puls, Energie", "Puls, energie"),
                color = colors.text, modifier = Modifier.weight(1f))
            Text("TrainPilot WearOS", color = colors.text, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("journal_wear_source"))
        }
        if (NativeWorkoutHealthSummary.journalValues(summary).isEmpty()) Text(
            local("Nincs óramérés ehhez az edzéshez.", "No watch measurement for this workout.",
                "Keine Uhrmessung für dieses Training.", "Nu există măsurători de pe ceas pentru acest antrenament."),
            color = colors.muted, modifier = Modifier.testTag("journal_wear_source_missing"))
        Text(local("A tárolt egészségadat megmarad, de itt nem jelenik meg. Külső (importált) edzésnél a saját forrása látszik.",
            "Stored health data is retained but not shown here. Imported workouts show their own source.",
            "Gespeicherte Gesundheitsdaten bleiben erhalten, werden hier aber nicht angezeigt. Importierte Trainings zeigen ihre eigene Quelle.",
            "Datele de sănătate stocate sunt păstrate, dar nu apar aici. Antrenamentele importate afișează sursa proprie."),
            style = MaterialTheme.typography.labelSmall, color = colors.muted)
    }
}

@Composable
internal fun JournalHealthMetrics(metrics: Map<String, Double>, source: String, text: android.content.res.Resources, number: NumberFormat, compact: Boolean = false) {
    val colors = LocalTrainPilotColors.current
    for ((key, rawValue) in metrics) {
        val label = when (key) {
            "averageHeartRate" -> R.string.health_metric_averageheartrate
            "minHeartRate" -> R.string.health_metric_minheartrate
            "maxHeartRate" -> R.string.health_metric_maxheartrate
            "heartRateSamples" -> R.string.health_metric_heartratesamples
            "steps" -> R.string.health_metric_steps
            "activeCalories" -> R.string.health_metric_activecalories
            "totalCalories" -> R.string.health_metric_totalcalories
            "workoutCalories" -> R.string.journal_health_workout_energy
            "distanceMeters" -> R.string.journal_health_distance
            "activeDurationSeconds" -> R.string.journal_health_active_duration
            "exerciseMinutes" -> R.string.journal_health_exercise_minutes
            "exerciseSessionCount" -> R.string.journal_health_exercise_count
            "averageSpeedMps" -> R.string.journal_health_average_speed
            "maxSpeedMps" -> R.string.journal_health_max_speed
            else -> continue
        }
        val unit = when (key) {
            "averageHeartRate", "minHeartRate", "maxHeartRate" -> "bpm"
            "activeCalories", "totalCalories", "workoutCalories" -> "kcal"
            "distanceMeters" -> "m"
            "activeDurationSeconds" -> "s"
            "exerciseMinutes" -> "min"
            "averageSpeedMps", "maxSpeedMps" -> "km/h"
            else -> ""
        }
        val value = if (key in setOf("averageSpeedMps", "maxSpeedMps")) rawValue * 3.6 else rawValue
        if (!value.isFinite()) continue
        if (compact) Row(Modifier.fillMaxWidth().testTag("journal_${source}_$key"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text.getString(label), color = colors.text, modifier = Modifier.weight(1f))
            Text("${number.format(value)} $unit".trim(), color = colors.text,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("journal_${source}_${key}_value"))
        } else Column(Modifier.fillMaxWidth().testTag("journal_${source}_$key")) {
            Text(text.getString(label), style = MaterialTheme.typography.labelMedium, color = colors.muted)
            Text("${number.format(value)} $unit".trim(), modifier = Modifier.testTag("journal_${source}_${key}_value"))
        }
    }
}
