package com.example.ui.screens

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.GlassMaterialRole

import com.example.ui.components.trainPilotAnimatedExpansion
import com.example.ui.components.OperationFeedbackKind
import com.example.ui.components.OperationFeedbackLine

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.TrainPilotSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import com.example.ui.components.TrainPilotCheckbox as Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.settings.NativeGoogleMaintenanceConfirmation
import com.repforge.app.ui.settings.NativeGoogleMaintenanceControls
import com.repforge.app.ui.settings.NativeGoogleMaintenanceFeedback
import com.repforge.app.ui.settings.SettingsViewModel

@Composable
fun StudioGoogleCloudSyncCard(
    onShowBanner: (String) -> Unit,
    onCloudSyncCompleted: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    compact: Boolean = false,
    onSyncAllCloud: (() -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    val lang = strings.language
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val activity = LocalActivity.current
    var cloudExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showDisconnectSheet by remember { mutableStateOf(false) }
    if (showDisconnectSheet) GoogleDisconnectSheet(
        language = lang,
        onDismiss = { showDisconnectSheet = false },
        onConfirm = { revoke, deleteData ->
            showDisconnectSheet = false
            activity?.let { settingsViewModel.disconnectGoogleWith(it, revoke, deleteData) }
        }
    )

    val feedback = settingsState.googleActionFeedback
    LaunchedEffect(settingsState.statusMessage, compact) {
        val msg = settingsState.statusMessage
        if (!compact && !msg.isNullOrBlank() && msg != feedback?.message) {
            onShowBanner(msg)
        }
    }

    LaunchedEffect(gs.lastDriveSyncIso, gs.lastCalendarSyncIso, settingsState.maintenanceCompleted) {
        if (gs.lastDriveSyncIso.isNotBlank() || gs.lastCalendarSyncIso.isNotBlank() || settingsState.maintenanceCompleted > 0) {
            onCloudSyncCompleted()
        }
    }

    // #352: one status for the header, judged per service (Drive / Calendar) and never from a stale date of
    // a manual-only sync. 2805: "late" applies only to a service whose daily automatic sync is switched on.
    val context = LocalContext.current
    var autoSync by remember { mutableStateOf(com.repforge.app.NativeSyncSchedule.googleAuto(context)) }
    val autoFailures = remember(gs.lastDriveSyncIso, gs.lastCalendarSyncIso, gs.isBusy, cloudExpanded) {
        com.repforge.app.NativeSyncSchedule.autoFailure(context, "drive") to com.repforge.app.NativeSyncSchedule.autoFailure(context, "calendar")
    }
    fun saveAutoSync(next: com.repforge.app.NativeSyncSchedule.GoogleAuto) {
        // Durable choice first; the job follows the saved value, never an unsaved UI state.
        if (com.repforge.app.NativeSyncSchedule.setGoogleAuto(context, next)) {
            autoSync = next
            com.repforge.app.GoogleAutoSyncWorker.schedule(context)
        }
    }
    val connected = gs.connectedEmail.isNotEmpty()
    var showActionResult by remember(feedback) { mutableStateOf(true) }
    LaunchedEffect(feedback) {
        if (feedback?.failed == false) {
            kotlinx.coroutines.delay(3500)
            showActionResult = false
        }
    }
    val driveFailMessage = feedback?.takeIf { it.failed && it.action == "drive" }?.message ?: autoFailures.first.takeIf { it.isNotBlank() }
    val calendarFailMessage = feedback?.takeIf { it.failed && it.action == "calendar" }?.message ?: autoFailures.second.takeIf { it.isNotBlank() }
    val driveStatus = cloudServiceStatus(connected, gs.lastDriveSyncIso, driveFailMessage != null, expectedAuto = autoSync.drive)
    val calendarStatus = cloudServiceStatus(connected, gs.lastCalendarSyncIso, calendarFailMessage != null, expectedAuto = autoSync.calendar)
    val headerStatus = if (connected && !gs.errorMessage.isNullOrBlank()) CloudSyncStatus.ERROR
        else cloudSyncHeaderStatus(listOf(driveStatus, calendarStatus))
    val busyAny = gs.isBusy || settingsState.replacementBusy || settingsState.maintenanceBusy
    val syncCloudNow: () -> Unit = {
        if (onSyncAllCloud != null) onSyncAllCloud()
        else activity?.let { settingsViewModel.syncAllCloud(it) }
    }
    fun statusLabel(st: CloudSyncStatus) = when (st) {
        CloudSyncStatus.OK -> tr(lang, "Szinkron rendben", "Sync OK", "Sync in Ordnung", "Sincronizare în regulă")
        CloudSyncStatus.LATE -> tr(lang, "Több mint 3 napja nem volt sikeres szinkron", "No successful sync for more than 3 days", "Seit über 3 Tagen kein erfolgreicher Sync", "Nicio sincronizare reușită de peste 3 zile")
        CloudSyncStatus.ERROR -> tr(lang, "Hiba – újrakapcsolás szükséges", "Error – reconnect needed", "Fehler – erneut verbinden", "Eroare – reconectare necesară")
        CloudSyncStatus.NEUTRAL -> if (connected) tr(lang, "Még nincs szinkron", "No sync yet", "Noch kein Sync", "Încă nicio sincronizare")
            else tr(lang, "Nincs összekötve", "Not connected", "Nicht verbunden", "Neconectat")
    }
    @Composable fun StatusDot(st: CloudSyncStatus, modifier: Modifier = Modifier) {
        val (bg, fg, glyph) = when (st) {
            CloudSyncStatus.OK -> Triple(palette.success.copy(alpha = 0.18f), palette.success, "✓")
            CloudSyncStatus.LATE -> Triple(palette.warning.copy(alpha = 0.18f), palette.warning, "⏱")
            CloudSyncStatus.ERROR -> Triple(palette.danger.copy(alpha = 0.18f), palette.danger, "!")
            CloudSyncStatus.NEUTRAL -> Triple(palette.surfaceElevated, palette.textSecondary, "–")
        }
        Box(modifier.size(28.dp).clip(CircleShape).background(bg).semantics { contentDescription = statusLabel(st) },
            contentAlignment = Alignment.Center) {
            if (gs.isBusy) CircularProgressIndicator(Modifier.size(16.dp), color = palette.primary, strokeWidth = 2.dp)
            else Text(glyph, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
    @Composable fun ServiceBox(title: String, st: CloudSyncStatus, lastIso: String, extra: String, tag: String, failMessage: String?) {
        Surface(shape = RoundedCornerShape(12.dp), color = palette.glassTile(palette.surfaceElevated, elevated = true),
            modifier = Modifier.fillMaxWidth().trainPilotGlassRim(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Tile).testTag(tag)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Text(when (st) {
                        CloudSyncStatus.OK -> "✓ " + tr(lang, "Rendben", "OK", "In Ordnung", "În regulă")
                        CloudSyncStatus.LATE -> "⏱ " + tr(lang, "Késik", "Late", "Verspätet", "Întârziat")
                        CloudSyncStatus.ERROR -> "! " + tr(lang, "Hiba", "Error", "Fehler", "Eroare")
                        CloudSyncStatus.NEUTRAL -> "– " + tr(lang, "Még nem futott", "Not run yet", "Noch nicht gelaufen", "Nu a rulat încă")
                    }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = when (st) { CloudSyncStatus.OK -> palette.success; CloudSyncStatus.LATE -> palette.warning
                            CloudSyncStatus.ERROR -> palette.danger; CloudSyncStatus.NEUTRAL -> palette.textSecondary })
                }
                if (lastIso.isNotBlank()) Text("${tr(lang, "Utolsó siker", "Last success", "Letzter Erfolg", "Ultimul succes")}: ${lastIso.take(19).replace('T', ' ')}$extra",
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                if (st == CloudSyncStatus.ERROR) (failMessage ?: gs.errorMessage)?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = palette.danger)
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.glassCardFill(palette.surfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotGlassMaterial(palette, RoundedCornerShape(20.dp))
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            .trainPilotAnimatedExpansion()
            .testTag("studio_google_cloud_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // The refresh action is a sibling, never part of the expandable tap target.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // #352: one row – icon, title, a small status mark and the arrow; the whole row toggles.
                Row(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("settings_google_expand").trainPilotClickable(RoundedCornerShape(10.dp)) { cloudExpanded = !cloudExpanded }
                    .semantics { stateDescription = if (cloudExpanded) tr(lang, "Lenyitva", "Expanded", "Aufgeklappt", "Extins")
                        else tr(lang, "Összecsukva", "Collapsed", "Zugeklappt", "Restrâns") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!compact) Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(palette.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (connected) Icons.Default.CloudDone else Icons.Default.CloudSync, null, tint = palette.primary, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = if (compact) "Google Drive" else tr(lang, "Google Cloud Szinkron", "Google Cloud Sync", "Google Cloud-Synchronisierung", "Sincronizare Google Cloud"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                StatusDot(headerStatus, Modifier.testTag("settings_google_status_${headerStatus.name.lowercase()}"))
                Icon(if (cloudExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
            }

                if (!compact && !cloudExpanded && connected) {
                    val enabled = !busyAny && activity != null
                    val syncLabel = tr(lang, "Google Cloud szinkronizálása", "Sync Google Cloud",
                        "Google Cloud synchronisieren", "Sincronizează Google Cloud")
                    Box(
                        modifier = Modifier.size(48.dp)
                            .trainPilotClickable(CircleShape, enabled = enabled, onClick = syncCloudNow)
                            .testTag("settings_google_quick_sync")
                            .semantics { contentDescription = syncLabel },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier.size(36.dp)
                                .trainPilotGlassRim(palette, CircleShape, GlassMaterialRole.Button)
                                .trainPilotBorder(1.dp, palette.cardBorder, CircleShape)
                                .testTag("settings_google_quick_sync_icon")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (gs.isBusy) CircularProgressIndicator(Modifier.size(18.dp), color = palette.primary, strokeWidth = 2.dp)
                                else Icon(Icons.Default.Sync, null, tint = if (enabled) palette.primary else palette.textSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (cloudExpanded) {
                Column(Modifier.testTag("settings_google_expanded"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (connected) {
                Text(
                    text = "${tr(lang, "Fiók", "Account", "Konto", "Cont")}: " +
                        (if (gs.connectedName.isNotBlank()) "${gs.connectedName} (${gs.connectedEmail})" else gs.connectedEmail),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.textPrimary
                )
                ServiceBox(tr(lang, "Google Drive mentés", "Google Drive backup", "Google Drive-Sicherung", "Backup Google Drive"),
                    driveStatus, gs.lastDriveSyncIso, " • " + tr(lang, "utolsó 3 mentés marad", "last 3 backups kept", "letzte 3 Sicherungen bleiben", "rămân ultimele 3 backup-uri"), "settings_google_drive_status", driveFailMessage)
                val countSuffix = gs.lastCalendarSyncCount?.let { cnt ->
                    " • " + tr(lang, "$cnt esemény", "$cnt events", "$cnt Termine", "$cnt evenimente")
                }.orEmpty() + " • " + tr(lang, "következő 30 nap", "next 30 days", "nächste 30 Tage", "următoarele 30 de zile")
                ServiceBox(tr(lang, "Google Naptár", "Google Calendar", "Google Kalender", "Google Calendar"),
                    calendarStatus, gs.lastCalendarSyncIso, countSuffix, "settings_google_last_calendar_sync", calendarFailMessage)

                if (headerStatus == CloudSyncStatus.ERROR) TrainPilotOutlinedButton(
                    onClick = { activity?.let { settingsViewModel.connectGoogle(it) } },
                    enabled = !busyAny && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.danger.copy(alpha = 0.16f)),
                    border = BorderStroke(1.dp, palette.danger),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_reconnect_btn")
                ) {
                    Text(tr(lang, "Fiók újrakapcsolása", "Reconnect account", "Konto neu verbinden", "Reconectează contul"),
                        color = palette.danger, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }

                // 2806: simplified – one automatic switch (Drive + Calendar), Wi-Fi option, one sync button, one disconnect.
                @Composable fun AutoRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onChange(!checked) }
                        .semantics { stateDescription = if (checked) tr(lang, "Be", "On", "An", "Pornit") else tr(lang, "Ki", "Off", "Aus", "Oprit") }
                        .testTag(tag), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = checked, onCheckedChange = onChange)
                    }
                }
                AutoRow(tr(lang, "Automatikus napi szinkron", "Automatic daily sync", "Automatische tägliche Synchronisierung", "Sincronizare zilnică automată"),
                    autoSync.any, "settings_google_auto_sync") { saveAutoSync(autoSync.copy(drive = it, calendar = it)) }
                if (autoSync.any) AutoRow(tr(lang, "Csak Wi-Fi-n", "Wi-Fi only", "Nur über WLAN", "Doar pe Wi-Fi"),
                    autoSync.wifiOnly, "settings_google_auto_wifi") { saveAutoSync(autoSync.copy(wifiOnly = it)) }
                com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton(
                    onClick = syncCloudNow,
                    enabled = !busyAny && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_sync_now_btn")
                ) {
                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(lang, "Szinkron most", "Sync now", "Jetzt synchronisieren", "Sincronizează acum"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                TrainPilotOutlinedButton(
                    onClick = { showDisconnectSheet = true },
                    enabled = !busyAny,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                    border = BorderStroke(1.dp, palette.danger),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_disconnect_btn")
                ) {
                    Text(tr(lang, "Google-fiók leválasztása", "Disconnect Google account", "Google-Konto trennen", "Deconectează contul Google"),
                        color = palette.danger, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = tr(
                        lang,
                        "Drive-mentés és Naptár szinkron", "Drive backup and Calendar sync",
                        "Drive-Sicherung und Kalendersynchronisierung", "Backup Drive și sincronizare Calendar"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton(
                    onClick = { activity?.let { settingsViewModel.connectGoogle(it) } },
                    enabled = !busyAny && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_login_btn")
                ) {
                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(lang, "Bejelentkezés Google-fiókkal", "Sign in with Google", "Mit Google anmelden", "Conectare cu Google"), fontWeight = FontWeight.Bold)
                }
            }

            if (!compact) Text(driveHealthBackupSummary(lang), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            if (!compact && gs.isBusy && gs.stageMessage.isNotBlank()) {
                Surface(shape = RoundedCornerShape(10.dp), color = palette.primary.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_sync_loading")) {
                    OperationFeedbackLine(OperationFeedbackKind.RUNNING, gs.stageMessage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            if (!compact && !gs.isBusy && feedback == null && !gs.errorMessage.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(10.dp), color = palette.danger.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_sync_error")) {
                    OperationFeedbackLine(OperationFeedbackKind.FAILED, gs.errorMessage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
                }
            }

            if (!compact && !gs.isBusy && showActionResult && feedback != null &&
                !(cloudExpanded && connected && feedback.failed && feedback.action in listOf("drive", "calendar"))
            ) {
                OperationFeedbackLine(
                    if (feedback.failed) OperationFeedbackKind.FAILED else OperationFeedbackKind.SUCCESS,
                    feedback.message, Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            NativeGoogleMaintenanceFeedback(
                language = settingsState.settings.language,
                outcome = settingsState.maintenanceOutcome,
                count = settingsState.maintenanceCompleted
            )

            if (compact && !gs.isBusy && gs.errorMessage.isNullOrBlank() && gs.lastDriveSyncIso.isNotBlank()) {
                Text(
                    text = "${tr(lang, "Utolsó mentés", "Last backup", "Letzte Sicherung", "Ultimul backup")}: ${gs.lastDriveSyncIso.take(19).replace('T', ' ')}",
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("backup_drive_last_sync")
                )
            }
        }
    }
}

/** #352: health of one Google service. NEUTRAL = not connected or never run; LATE only for an expected automatic sync. */
internal enum class CloudSyncStatus { OK, LATE, ERROR, NEUTRAL }

internal fun cloudServiceStatus(connected: Boolean, lastSuccessIso: String, lastAttemptFailed: Boolean, expectedAuto: Boolean,
    now: java.time.Instant = java.time.Instant.now()): CloudSyncStatus {
    if (!connected) return CloudSyncStatus.NEUTRAL
    if (lastAttemptFailed) return CloudSyncStatus.ERROR
    val last = runCatching { java.time.Instant.parse(lastSuccessIso) }.getOrNull()
        ?: runCatching { java.time.LocalDateTime.parse(lastSuccessIso.take(19).replace(' ', 'T')).atZone(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull()
    // A service that has never completed is "not run yet", also right after switching automation on.
    if (last == null) return CloudSyncStatus.NEUTRAL
    // A manual-only (or switched-off) sync never turns yellow just because its date is old.
    return if (expectedAuto && java.time.Duration.between(last, now).toHours() > 72) CloudSyncStatus.LATE else CloudSyncStatus.OK
}

/** The header shows the most serious relevant state: error > late > ok > neutral. */
internal fun cloudSyncHeaderStatus(services: List<CloudSyncStatus>): CloudSyncStatus = when {
    CloudSyncStatus.ERROR in services -> CloudSyncStatus.ERROR
    CloudSyncStatus.LATE in services -> CloudSyncStatus.LATE
    CloudSyncStatus.OK in services -> CloudSyncStatus.OK
    else -> CloudSyncStatus.NEUTRAL
}

internal fun driveHealthBackupSummary(language: AppLanguage) = tr(language,
    "Egészségadatok engedélyezésekor a Drive az edzések egészségösszesítőit menti. A napi lépések a helyi JSON/ZIP mentésbe kerülhetnek.",
    "When enabled, Drive includes workout health summaries. Daily steps can be included in local JSON/ZIP backups.",
    "Bei Zustimmung sichert Drive Trainings-Gesundheitsdaten. Tagesschritte können lokal als JSON/ZIP gesichert werden.",
    "La activare, Drive include rezumatele de sănătate ale antrenamentelor. Pașii zilnici pot fi salvați local în JSON/ZIP.")

/** 2806: the single disconnect sheet – two optional, unticked extras; local data is never touched. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun GoogleDisconnectSheet(language: AppLanguage, onDismiss: () -> Unit, onConfirm: (revoke: Boolean, deleteData: Boolean) -> Unit) {
    val palette = LocalTrainPilotPalette.current
    var revoke by remember { mutableStateOf(false) }
    var deleteData by remember { mutableStateOf(false) }
    com.example.ui.components.TrainPilotModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surfaceCard
    ) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp).testTag("settings_google_disconnect_sheet"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr(language, "Google-fiók leválasztása", "Disconnect Google account", "Google-Konto trennen", "Deconectează contul Google"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text(tr(language, "A mentett edzéseid a telefonon megmaradnak.", "Your saved workouts stay on this phone.",
                "Deine gespeicherten Trainings bleiben auf dem Telefon.", "Antrenamentele salvate rămân pe telefon."),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            @Composable fun Option(title: String, detail: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).trainPilotClickable(RoundedCornerShape(10.dp)) { onChange(!checked) }
                    .testTag(tag), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(checkedColor = palette.primary))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
            }
            Option(tr(language, "Google-hozzáférés visszavonása", "Revoke Google access", "Google-Zugriff widerrufen", "Revocă accesul Google"),
                tr(language, "A Google-nál is megszűnik a TrainPilot engedélye.", "TrainPilot's permission is removed at Google too.",
                    "Die TrainPilot-Berechtigung wird auch bei Google entfernt.", "Permisiunea TrainPilot este eliminată și la Google."),
                revoke, "settings_google_disconnect_revoke") { revoke = it }
            Option(tr(language, "TrainPilot adatainak törlése a Google-fiókból", "Delete TrainPilot data from the Google account",
                    "TrainPilot-Daten aus dem Google-Konto löschen", "Șterge datele TrainPilot din contul Google"),
                tr(language, "Drive-mentések és a TrainPilot edzésnaptár törlése. Nem visszavonható.",
                    "Deletes Drive backups and the TrainPilot workout calendar. Cannot be undone.",
                    "Löscht Drive-Sicherungen und den TrainPilot-Trainingskalender. Nicht umkehrbar.",
                    "Șterge backup-urile Drive și calendarul TrainPilot. Ireversibil."),
                deleteData, "settings_google_disconnect_delete") { deleteData = it }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainPilotOutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                    border = com.repforge.app.ui.theme.trainPilotFrameStroke(BorderStroke(1.dp, palette.cardBorder)),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_google_disconnect_cancel")) {
                    Text(tr(language, "Mégse", "Cancel", "Abbrechen", "Anulează"), color = palette.textPrimary, fontWeight = FontWeight.Bold)
                }
                TrainPilotOutlinedButton(onClick = { onConfirm(revoke, deleteData) }, shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.danger),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("settings_google_disconnect_confirm")) {
                    Text(tr(language, "Leválasztás", "Disconnect", "Trennen", "Deconectează"), color = palette.danger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
