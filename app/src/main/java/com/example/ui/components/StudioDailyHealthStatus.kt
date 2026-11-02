package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val LocalSyncDailyHealth = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Same current-day source and sync state on Home and Health. No workout-window fallback. */
@Composable
internal fun StudioDailyHealthStatus(state: TrainPilotUiState) {
    val palette = LocalTrainPilotPalette.current
    val request = LocalSyncDailyHealth.current
    val current = state.dailyHealthDate == LocalDate.now().toString()
    val fields = if (current) state.dailyHealthAvailableFields else emptySet()
    val last = runCatching { DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
        .withZone(ZoneId.systemDefault()).format(Instant.parse(state.dailyHealthLastSyncIso)) }.getOrDefault("Még nincs igazolt szinkron")
    Column(Modifier.fillMaxWidth().testTag("studio_daily_health_status"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Mai adatok • ${state.dailyHealthSourceLabel}", color = palette.textPrimary,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { request(if (state.primaryHealthSourceSamsung) HealthJournalStore.SAMSUNG else HealthJournalStore.HC) },
                enabled = !state.healthSyncBusy, modifier = Modifier.testTag("daily_health_sync")) {
                Text(if (state.healthSyncBusy) "Szinkron…" else "Frissítés")
            }
        }
        Text("Utolsó sikeres szinkron: $last", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Text(if (fields.isEmpty()) "Mai napi mérés még nincs lekérve. A Frissítés ellenőrzi a hozzáférést és lekéri a napi adatokat."
            else "Napi lépés: ${if ("steps" in fields) state.dailySteps.toString() else "—"} • " +
                "Aktív energia: ${if ("activeCalories" in fields) "${state.activeCaloriesKcal} kcal" else "—"}",
            modifier = Modifier.testTag("daily_health_values"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        if ("totalCalories" in fields) Text("Összes energia (nyugalmival): ${state.totalCaloriesKcal} kcal",
            style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        if (state.healthSyncMessage.isNotBlank()) Text(state.healthSyncMessage,
            modifier = Modifier.testTag("daily_health_sync_status"), style = MaterialTheme.typography.bodySmall,
            color = if (state.healthSyncFailed) palette.danger else palette.textSecondary)
    }
}
