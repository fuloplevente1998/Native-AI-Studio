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

/** The same six measured/projected values on Home and Health; no second calculation path. */
@Composable
internal fun StudioTodayStatusGrid(state: TrainPilotUiState, compact: Boolean = false,
    onSelect: (String) -> Unit) {
    val values = listOf(
        Triple("sleep", "Alvás", state.sleepHoursDecimal),
        Triple("calories", if (compact) "Aktív kcal" else "Aktív kalória", if ("activeCalories" in state.dailyHealthAvailableFields) "${state.activeCaloriesKcal} kcal" else "—"),
        Triple("pulse", "Pulzus", if (state.currentHrBpm > 0) "${state.currentHrBpm} bpm" else "—"),
        Triple("steps", "Lépések", if ("steps" in state.dailyHealthAvailableFields) "${state.dailySteps}" else "—"),
        Triple("workout", "Mai edzés", "${state.loggedWorkouts.workoutsOn(LocalDate.now()).size}"),
        Triple("recovery", "Regeneráció", if (state.coachReadinessScore > 0) "${state.coachReadinessScore}/100" else "—")
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
