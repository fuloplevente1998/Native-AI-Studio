package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.workoutsOn
import com.example.ui.viewmodel.TrainPilotUiState
import java.time.LocalDate
import kotlin.math.roundToInt
import com.repforge.app.healthreadings.RecoverySource

/** The same six measured/projected values on Home and Health; no second calculation path. */
@Composable
internal fun StudioTodayStatusGrid(state: TrainPilotUiState, compact: Boolean = false,
    onSelect: (String) -> Unit) {
    fun tr(hu: String, en: String, de: String, ro: String) = when (state.language) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val pulse = state.todayPulseReading
    val recovery = state.todayRecoveryReading
    val pulseName = when {
        pulse?.averageBpm != null -> tr("Napi átlagpulzus", "Daily Avg HR", "Ø Tagespuls", "Puls mediu zilnic")
        pulse?.lastBpm != null -> tr("Utolsó mérés", "Last Reading", "Letzte Messung", "Ultima măsurătoare")
        pulse?.restingBpm != null -> tr("Nyugalmi pulzus", "Resting HR", "Ruhepuls", "Puls repaus")
        else -> tr("Pulzus", "Heart Rate", "Puls", "Puls")
    }
    val pulseValue = when {
        pulse?.averageBpm != null -> "${pulse.averageBpm.roundToInt()} bpm"
        pulse?.lastBpm != null -> "${pulse.lastBpm} bpm"
        pulse?.restingBpm != null -> "${pulse.restingBpm.roundToInt()} bpm"
        else -> "—"
    }
    val recoveryName = when (recovery?.source) {
        RecoverySource.SAMSUNG_ENERGY -> tr("Samsung Energia", "Samsung Energy", "Samsung Energie", "Energie Samsung")
        RecoverySource.TRAINPILOT_ESTIMATE -> tr("TP regeneráció", "TP Recovery", "TP Erholung", "Recuperare TP")
        else -> tr("Regeneráció", "Recovery", "Erholung", "Recuperare")
    }
    val values = listOf(
        Triple("sleep", tr("Alvás", "Sleep", "Schlaf", "Somn"), state.sleepHoursDecimal),
        Triple("calories", if (compact) tr("Aktív kcal", "Active kcal", "Aktive kcal", "kcal active") else tr("Aktív kalória", "Active Calories", "Aktive Kalorien", "Calorii active"), if ("activeCalories" in state.dailyHealthAvailableFields) "${state.activeCaloriesKcal} kcal" else "—"),
        Triple("pulse", pulseName, pulseValue),
        Triple("steps", tr("Lépések", "Steps", "Schritte", "Pași"), if ("steps" in state.dailyHealthAvailableFields) "${state.dailySteps}" else "—"),
        Triple("workout", tr("Mai edzés", "Today's Workout", "Heutiges Training", "Antrenament azi"), "${state.loggedWorkouts.workoutsOn(LocalDate.now()).size}"),
        Triple("recovery", recoveryName, recovery?.displayScoreHu ?: "—")
    )
    val icons = listOf(Icons.Default.Bedtime, Icons.Default.LocalFireDepartment, Icons.Default.Favorite,
        Icons.AutoMirrored.Filled.DirectionsWalk, Icons.Default.FitnessCenter, Icons.Default.Eco)
    Column(Modifier.fillMaxWidth().testTag(if (compact) "home_today_status_grid" else "health_today_status_grid"),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        for (row in 0..1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
            for (column in 0..2) {
                val i = row * 3 + column
                val (key, title, value) = values[i]
                MainTodayStatusTile(icons[i], title, value, { onSelect(key) },
                    modifier = Modifier.weight(1f).heightIn(min = if (compact) 64.dp else 76.dp)
                        .testTag("${if (compact) "home" else "health"}_today_$key"), compact = compact)
            }
        }
    }
}
