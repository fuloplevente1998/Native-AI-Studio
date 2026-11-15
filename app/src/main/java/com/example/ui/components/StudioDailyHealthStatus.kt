package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
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
internal fun StudioDailyHealthStatus(state: TrainPilotUiState, showRefreshButton: Boolean = true, showTotalEnergy: Boolean = true) {
    val palette = LocalTrainPilotPalette.current
    val request = LocalSyncDailyHealth.current
    fun word(hu: String, en: String, de: String, ro: String) = when (state.language) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val current = state.dailyHealthDate == LocalDate.now().toString()
    val fields = if (current) state.dailyHealthAvailableFields else emptySet()
    val last = runCatching { DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
        .withZone(ZoneId.systemDefault()).format(Instant.parse(state.dailyHealthLastSyncIso)) }.getOrDefault(
        word("Még nincs igazolt szinkron", "No verified sync yet", "Noch keine bestätigte Synchronisierung", "Încă nu există o sincronizare verificată")
    )
    Column(Modifier.fillMaxWidth().testTag("studio_daily_health_status"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("${word("Mai adatok", "Today's data", "Heutige Daten", "Datele de azi")} • ${state.dailyHealthSourceLabel}", color = palette.textPrimary,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (showRefreshButton) TextButton(onClick = { request(if (state.primaryHealthSourceSamsung) HealthJournalStore.SAMSUNG else HealthJournalStore.HC) },
                enabled = !state.healthSyncBusy, modifier = Modifier.testTag("daily_health_sync")) {
                Text(if (state.healthSyncBusy) word("Szinkron…", "Syncing…", "Synchronisiere…", "Sincronizare…")
                    else word("Frissítés", "Refresh", "Aktualisieren", "Actualizează"))
            }
        }
        Text("${word("Utolsó sikeres szinkron", "Last successful sync", "Letzte erfolgreiche Synchronisierung", "Ultima sincronizare reușită")}: $last", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Text(if (fields.isEmpty()) word(
                "Mai napi mérés még nincs lekérve. A Frissítés ellenőrzi a hozzáférést és lekéri a napi adatokat.",
                "Today's daily measurements have not been fetched yet. Refresh checks access and fetches daily data.",
                "Heutige Tagesmessungen wurden noch nicht abgerufen. Aktualisieren prüft den Zugriff und lädt die Tagesdaten.",
                "Măsurătorile zilnice de azi nu au fost încă preluate. Actualizarea verifică accesul și preia datele zilnice."
            )
            else "${word("Napi lépés", "Daily steps", "Tagesschritte", "Pași zilnici")}: ${if ("steps" in fields) state.dailySteps.toString() else "—"} • " +
                "${word("Aktív energia", "Active energy", "Aktive Energie", "Energie activă")}: ${if ("activeCalories" in fields) "${state.activeCaloriesKcal} kcal" else "—"}",
            modifier = Modifier.testTag("daily_health_values"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        if (showTotalEnergy && "totalCalories" in fields) Text(
            "${word("Összes energia (nyugalmival)", "Total energy (with resting)", "Gesamtenergie (mit Ruheumsatz)", "Energie totală (cu repaus)")}: ${state.totalCaloriesKcal} kcal",
            style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        if (state.healthSyncMessage.isNotBlank()) Text(state.healthSyncMessage,
            modifier = Modifier.testTag("daily_health_sync_status"), style = MaterialTheme.typography.bodySmall,
            color = if (state.healthSyncFailed) palette.danger else palette.textSecondary)
    }
}
