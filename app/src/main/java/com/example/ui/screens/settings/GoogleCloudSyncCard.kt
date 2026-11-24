package com.example.ui.screens

import com.example.ui.components.trainPilotAnimatedExpansion

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.example.ui.components.trainPilotAnimatedExpansion
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
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.material3.Checkbox
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
    compact: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    val lang = strings.language
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val activity = LocalActivity.current
    var cloudExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showMaintenanceOptions by remember { mutableStateOf(false) }

    val maintenancePreview by settingsViewModel.maintenancePreview.collectAsStateWithLifecycle()
    NativeGoogleMaintenanceConfirmation(
        request = maintenancePreview,
        language = settingsState.settings.language,
        busy = settingsState.maintenanceBusy,
        cancel = settingsViewModel::cancelGoogleMaintenance,
        confirm = { activity?.let(settingsViewModel::confirmGoogleMaintenance) }
    )

    LaunchedEffect(settingsState.statusMessage, compact) {
        val msg = settingsState.statusMessage
        if (!compact && !msg.isNullOrBlank()) {
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
    val feedback = settingsState.googleActionFeedback
    val driveFailMessage = feedback?.takeIf { it.failed && it.action == "drive" }?.message ?: autoFailures.first.takeIf { it.isNotBlank() }
    val calendarFailMessage = feedback?.takeIf { it.failed && it.action == "calendar" }?.message ?: autoFailures.second.takeIf { it.isNotBlank() }
    val driveStatus = cloudServiceStatus(connected, gs.lastDriveSyncIso, driveFailMessage != null, expectedAuto = autoSync.drive)
    val calendarStatus = cloudServiceStatus(connected, gs.lastCalendarSyncIso, calendarFailMessage != null, expectedAuto = autoSync.calendar)
    val headerStatus = if (connected && !gs.errorMessage.isNullOrBlank()) CloudSyncStatus.ERROR
        else cloudSyncHeaderStatus(listOf(driveStatus, calendarStatus))
    val busyAny = gs.isBusy || settingsState.replacementBusy || settingsState.maintenanceBusy
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
        Surface(shape = RoundedCornerShape(12.dp), color = palette.surfaceElevated, modifier = Modifier.fillMaxWidth().testTag(tag)) {
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
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            .trainPilotAnimatedExpansion()
            .testTag("studio_google_cloud_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // #352: one row – icon, title, a small status mark and the arrow; the whole row toggles.
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
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

            if (cloudExpanded) {
                Column(Modifier.testTag("settings_google_expanded"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (connected) {
                Text(
                    text = "${tr(lang, "Fiók", "Account", "Konto", "Cont")}: " +
                        (if (gs.connectedName.isNotBlank()) "${gs.connectedName} (${gs.connectedEmail})" else gs.connectedEmail),
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.textPrimary
                )
                ServiceBox(tr(lang, "Google Drive mentés", "Google Drive backup", "Google Drive-Sicherung", "Backup Google Drive"),
                    driveStatus, gs.lastDriveSyncIso, "", "settings_google_drive_status", driveFailMessage)
                val countSuffix = gs.lastCalendarSyncCount?.let { cnt ->
                    " • " + tr(lang, "$cnt esemény", "$cnt events", "$cnt Termine", "$cnt evenimente")
                }.orEmpty()
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

                // 2805: opt-in daily automatic sync; the manual actions below stay unchanged.
                Text(tr(lang, "Automatikus szinkron", "Automatic sync", "Automatische Synchronisierung", "Sincronizare automată"),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textSecondary,
                    modifier = Modifier.padding(top = 2.dp))
                @Composable fun AutoRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onChange(!checked) }
                        .semantics { stateDescription = if (checked) tr(lang, "Be", "On", "An", "Pornit") else tr(lang, "Ki", "Off", "Aus", "Oprit") }
                        .testTag(tag), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = checked, onCheckedChange = onChange)
                    }
                }
                AutoRow(tr(lang, "Napi Drive-mentés", "Daily Drive backup", "Tägliche Drive-Sicherung", "Backup Drive zilnic"),
                    autoSync.drive, "settings_google_auto_drive") { saveAutoSync(autoSync.copy(drive = it)) }
                AutoRow(tr(lang, "Naptár automatikus frissítése", "Refresh calendar automatically", "Kalender automatisch aktualisieren", "Actualizare automată calendar"),
                    autoSync.calendar, "settings_google_auto_calendar") { saveAutoSync(autoSync.copy(calendar = it)) }
                if (autoSync.any) AutoRow(tr(lang, "Csak Wi-Fi-n", "Wi-Fi only", "Nur über WLAN", "Doar pe Wi-Fi"),
                    autoSync.wifiOnly, "settings_google_auto_wifi") { saveAutoSync(autoSync.copy(wifiOnly = it)) }
                if (autoSync.any) Text(tr(lang,
                    "Naponta egyszer, töltöttségkímélően; az Android választja ki a pontos időt. Aktív edzés közben kimarad.",
                    "Once a day, battery-friendly; Android picks the exact time. Skipped during an active workout.",
                    "Einmal täglich, akkuschonend; Android wählt die genaue Zeit. Während eines Trainings ausgelassen.",
                    "O dată pe zi, economic; Android alege ora exactă. Omisă în timpul unui antrenament activ."),
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)

                // Everyday actions, one under the other (#352).
                Text(tr(lang, "Kézi műveletek", "Manual actions", "Manuelle Aktionen", "Acțiuni manuale"),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textSecondary,
                    modifier = Modifier.padding(top = 2.dp))
                Button(
                    onClick = { activity?.let { settingsViewModel.syncDriveCloud(it) } },
                    enabled = !busyAny && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_drive_sync_btn")
                ) {
                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(lang, "Drive szinkron", "Drive Sync", "Drive-Sync", "Sincronizare Drive"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                // The merge option belongs to the Drive sync above (it is not a separate action).
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { settingsViewModel.setRecoverEarlierBackups(!settingsState.recoverEarlierBackupsChecked) }
                        .testTag("settings_google_recover_earlier"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = settingsState.recoverEarlierBackupsChecked,
                        onCheckedChange = { settingsViewModel.setRecoverEarlierBackups(it) },
                        colors = CheckboxDefaults.colors(checkedColor = palette.primary)
                    )
                    Text(
                        text = tr(lang, "Szinkronnál korábbi mentések összefésülése is", "Also merge earlier backups when syncing", "Beim Sync auch frühere Sicherungen zusammenführen", "La sincronizare îmbină și backup-urile anterioare"),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary
                    )
                }
                TrainPilotOutlinedButton(
                    onClick = { activity?.let { settingsViewModel.syncCalendarCloud(it) } },
                    enabled = !busyAny && activity != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_calendar_sync_btn")
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = palette.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(lang, "Naptár szinkron", "Calendar Sync", "Kalender-Sync", "Sincronizare Calendar"),
                        color = palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                TrainPilotOutlinedButton(
                    onClick = { showMaintenanceOptions = !showMaintenanceOptions },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated),
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_maintenance_toggle")
                ) {
                    Text(tr(lang, "Felhő karbantartás", "Cloud Maintenance", "Cloud-Wartung", "Mentenanță cloud"),
                        color = palette.textPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Icon(if (showMaintenanceOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.textSecondary)
                }

                // Rarely used, destructive actions live here, each with its own confirmation.
                if (showMaintenanceOptions) {
                    Surface(shape = RoundedCornerShape(12.dp), color = palette.surfaceCard,
                        modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))) {
                        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            com.repforge.app.ui.theme.TrainPilotTextButton(
                                onClick = { settingsViewModel.disconnectGoogle() },
                                enabled = !busyAny,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_google_disconnect_btn")
                            ) {
                                Text(tr(lang, "Google-fiók lekapcsolása", "Disconnect Google Account", "Google-Konto trennen", "Deconectează contul Google"),
                                    color = palette.danger, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            }
                            NativeGoogleMaintenanceControls(
                                language = settingsState.settings.language,
                                busy = busyAny,
                                open = settingsViewModel::openGoogleMaintenance
                            )
                        }
                    }
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
                Button(
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
                    Text(gs.stageMessage, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                        color = palette.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            if (!compact && !gs.errorMessage.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(10.dp), color = palette.danger.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("settings_google_sync_error")) {
                    Text(gs.errorMessage, style = MaterialTheme.typography.labelMedium, color = palette.danger,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
                }
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
