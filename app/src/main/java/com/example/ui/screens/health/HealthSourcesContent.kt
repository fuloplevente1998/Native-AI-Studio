package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.MockupWatchGraphicBadge
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
internal fun HealthSourcesContent(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSelectHealthSource: (Boolean) -> Unit,
    onTriggerHealthSync: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    closeSheet: () -> Unit,
    syncDetailsExpanded: Boolean,
    onToggleSyncDetails: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("health_provider_connect")) {
            Text("Health Connect", fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text(tr(uiState.language, "Elsődleges adatforrás", "Primary data source",
                "Primäre Datenquelle", "Sursa principală de date"), color = palette.textSecondary,
                style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Samsung Health", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(tr(uiState.language, "Kiegészítő adatforrás", "Supplementary data source",
                    "Ergänzende Datenquelle", "Sursă suplimentară de date"), color = palette.textSecondary,
                    style = MaterialTheme.typography.labelSmall)
            }
            com.example.ui.components.TrainPilotSwitch(uiState.permissionSamsungHealthEnabled,
                onCheckedChange = onSelectHealthSource,
                modifier = Modifier.testTag("health_provider_samsung")
                    .semantics { contentDescription = "Samsung Health" })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.primary.copy(alpha = 0.42f),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .trainPilotClickable(RoundedCornerShape(16.dp), enabled = !uiState.healthSyncBusy) { onTriggerHealthSync() }
                    .testTag("health_sync_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.healthSyncBusy) tr(uiState.language, "Szinkronizálás…", "Syncing…", "Synchronisiere…", "Se sincronizează…") else strings.syncAction,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(16.dp))
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .trainPilotClickable(RoundedCornerShape(16.dp)) { onRequestHealthPermissions() }
                    .testTag("health_open_permissions_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.permissionsAction,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }
        }

        ExternalWorkoutImportPanel(uiState.language, enabled = !uiState.healthSyncBusy, samsungEnabled = uiState.permissionSamsungHealthEnabled)
        if (uiState.showConnectedWatch) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(20.dp))
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .trainPilotClickable(RoundedCornerShape(20.dp)) {
                        closeSheet()
                        onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC)
                    }
                    .testTag("health_open_watch_sync_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupWatchGraphicBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        val wearTitle = tr(uiState.language, "TrainPilot Wear OS – edzésmérések", "TrainPilot Wear OS – workout measurements",
                            "TrainPilot Wear OS – Trainingsmessungen", "TrainPilot Wear OS – măsurători antrenament")
                        val watchTitle = if (uiState.watchAppAvailable) "$wearTitle (${uiState.watchName})" else wearTitle
                        Text(
                            text = watchTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = when {
                                uiState.watchAppAvailable -> tr(uiState.language, "● Óra csatlakoztatva", "● Watch connected", "● Uhr verbunden", "● Ceas conectat")
                                uiState.watchConnected -> tr(uiState.language, "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve", "Wear OS system connection • TrainPilot watch app not installed", "Wear OS-Systemverbindung • TrainPilot-Uhren-App nicht installiert", "Conexiune sistem Wear OS • Aplicația de ceas TrainPilot nu este instalată")
                                else -> tr(uiState.language, "Nincs csatlakoztatott óra", "No watch connected", "Keine Uhr verbunden", "Niciun ceas conectat")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.watchAppAvailable) palette.success else palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        com.repforge.app.NativeHealthProviderBatch.providers.filter {
            it == com.repforge.app.HealthJournalStore.HC || uiState.permissionSamsungHealthEnabled
        }.forEach { provider ->
            val lastSuccess = uiState.healthLastSyncByProvider[provider]
            val time = runCatching {
                java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm")
                    .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(lastSuccess))
            }.getOrDefault(tr(uiState.language, "Még nincs sikeres szinkron", "No successful sync yet", "Noch kein erfolgreicher Sync", "Nicio sincronizare reușită încă"))
            Text("${com.repforge.app.NativeWorkoutHealthSource.label(provider)} • ${tr(uiState.language, "Utolsó sikeres szinkron", "Last successful sync", "Letzter erfolgreicher Sync", "Ultima sincronizare reușită")}: $time",
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.testTag("health_last_sync_$provider"))
        }
        TrainPilotOutlinedButton(onClick = onToggleSyncDetails,
            modifier = Modifier.fillMaxWidth().testTag("health_sync_details_toggle")) {
            Text(tr(uiState.language, "Szinkron részletei", "Sync details", "Sync-Details", "Detalii sincronizare"),
                modifier = Modifier.weight(1f))
            Icon(if (syncDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        com.example.ui.components.TrainPilotAnimatedDisclosure(visible = syncDetailsExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 2805: the user picks the cadence; saved first, then the loop / background job follow it.
            val syncContext = LocalContext.current
            var foregroundMin by remember { mutableStateOf(com.repforge.app.NativeSyncSchedule.foregroundMinutes(syncContext)) }
            var backgroundMin by remember { mutableStateOf(com.repforge.app.NativeSyncSchedule.backgroundMinutes(syncContext)) }
            Text(
                if (uiState.healthAutoOnOpen) tr(uiState.language,
                    "Automatikus frissítés appnyitáskor, edzésmentés után és előtérben $foregroundMin percenként.",
                    "Automatic refresh on app opening, after saving a workout and every $foregroundMin minutes in the foreground.",
                    "Automatische Aktualisierung beim App-Start, nach dem Speichern eines Trainings und im Vordergrund alle $foregroundMin Minuten.",
                    "Actualizare automată la deschiderea aplicației, după salvarea antrenamentului și la fiecare $foregroundMin minute în prim-plan.")
                else tr(uiState.language, "Automatikus frissítés kikapcsolva.", "Automatic refresh is off.", "Automatische Aktualisierung deaktiviert.", "Actualizarea automată este oprită."),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.testTag("health_auto_refresh_status"))
            if (uiState.healthAutoOnOpen) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(uiState.language, "Előtér", "Foreground", "Vordergrund", "Prim-plan"), style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary, modifier = Modifier.weight(1f))
                com.repforge.app.NativeSyncSchedule.FOREGROUND_MINUTES.forEach { m ->
                    com.example.ui.components.TrainPilotFilterChip(selected = foregroundMin == m,
                        onClick = { if (com.repforge.app.NativeSyncSchedule.setForegroundMinutes(syncContext, m)) foregroundMin = m },
                        label = { Text(tr(uiState.language, "$m perc", "$m min", "$m Min.", "$m min")) },
                        modifier = Modifier.testTag("health_foreground_interval_$m"))
                }
            }
            if (uiState.healthBackgroundOptedIn) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(uiState.language, "Háttér", "Background", "Hintergrund", "Fundal"), style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary, modifier = Modifier.weight(1f))
                com.repforge.app.NativeSyncSchedule.BACKGROUND_MINUTES.forEach { m ->
                    com.example.ui.components.TrainPilotFilterChip(selected = backgroundMin == m,
                        onClick = {
                            if (com.repforge.app.NativeSyncSchedule.setBackgroundMinutes(syncContext, m)) {
                                backgroundMin = m
                                runCatching { com.repforge.app.HealthBackgroundJob.schedule(syncContext) }
                            }
                        },
                        label = { Text(tr(uiState.language, "$m perc", "$m min", "$m Min.", "$m min")) },
                        modifier = Modifier.testTag("health_background_interval_$m"))
                }
            }
            Text(
                when {
                    uiState.healthBackgroundOptedIn && uiState.healthBackgroundScheduled -> tr(uiState.language,
                        "Health Connect háttérfrissítés: kb. $backgroundMin percenként, újraindítás után is; az Android késleltetheti.",
                        "Health Connect background refresh: about every $backgroundMin minutes, also after a restart; Android may delay it.",
                        "Health Connect im Hintergrund: etwa alle $backgroundMin Minuten, auch nach einem Neustart; Android kann dies verzögern.",
                        "Health Connect în fundal: aproximativ la fiecare $backgroundMin minute, și după repornire; Android poate întârzia actualizarea.")
                    uiState.healthBackgroundOptedIn -> tr(uiState.language, "Health Connect háttérfrissítés nincs ütemezve; ellenőrizd az engedélyeket.", "Health Connect background refresh is not scheduled; check permissions.", "Health Connect-Hintergrundaktualisierung nicht geplant; Berechtigungen prüfen.", "Actualizarea Health Connect în fundal nu este programată; verifică permisiunile.")
                    else -> tr(uiState.language, "Health Connect háttérfrissítés kikapcsolva.", "Health Connect background refresh is off.", "Health Connect-Hintergrundaktualisierung deaktiviert.", "Actualizarea Health Connect în fundal este oprită.")
                }, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.testTag("health_background_refresh_status"))
            Text("${tr(uiState.language, "Utolsó adatolvasás", "Last data read", "Letzter Datenabruf", "Ultima citire date")}: ${uiState.lastHealthReadLabel}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            Text(
                text = uiState.healthProviderStatus,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
            }
        }
    }
}
