package com.example.ui.screens

import com.example.ui.theme.trainPilotScreenBackdrop
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.ui.components.SubScreenTopBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.settings.SettingsViewModel

/**
 * 4/4 #3 MENTÉS ÉS VISSZAÁLLÍTÁS (Mockup 4/4 #3)
 */
@Composable
fun BackupRestoreScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFormat: (String) -> Unit,
    onToggleIncludeHealth: (Boolean) -> Unit,
    onShowBanner: (String) -> Unit,
    onExportBackup: ((android.net.Uri) -> Unit)? = null,
    onImportBackup: ((android.net.Uri) -> Unit)? = null,
    onCloudSyncCompleted: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val gs = settingsState.googleSyncState
    val activity = LocalActivity.current
    val lang = uiState.language
    val drive = uiState.selectedBackupFormat == "DRIVE"
    val inSheet = com.example.ui.components.LocalTrainPilotInSheet.current
    val localStatus = uiState.localBackupStatus
    var pickerMessage by remember { mutableStateOf<String?>(null) }
    val driveFeedback = settingsState.googleActionFeedback?.takeIf { it.action == "drive" || it.action == "connect" }
    val driveBlock = when {
        gs.isBusy -> gs.stageMessage.ifBlank { tr(lang, "Google-szinkron folyamatban…", "Google sync in progress…", "Google-Sync läuft…", "Sincronizarea Google este în curs…") }
        settingsState.replacementBusy || localStatus?.busy == true ->
            tr(lang, "Mentés vagy visszaállítás folyamatban.", "Backup or restore in progress.", "Sicherung oder Wiederherstellung läuft.", "Backup-ul sau restaurarea este în curs.")
        settingsState.maintenanceBusy -> tr(lang, "Karbantartás folyamatban.", "Maintenance in progress.", "Wartung läuft.", "Mentenanță în curs.")
        activity == null -> tr(lang, "Nyisd meg újra ezt a képernyőt.", "Reopen this screen.", "Öffne diese Seite erneut.", "Redeschide acest ecran.")
        uiState.isWorkoutSessionActive && gs.connectedEmail.isNotBlank() ->
            tr(lang, "A Drive-szinkron előtt fejezd be az aktív edzést.", "Finish the active workout before syncing Drive.",
                "Beende das laufende Training vor dem Drive-Sync.", "Încheie antrenamentul activ înainte de sincronizarea Drive.")
        else -> null
    }
    val status = when {
        drive && driveBlock != null -> com.example.ui.viewmodel.LocalBackupStatus(driveBlock, busy = gs.isBusy || settingsState.maintenanceBusy)
        pickerMessage != null -> com.example.ui.viewmodel.LocalBackupStatus(pickerMessage.orEmpty())
        drive && driveFeedback != null -> com.example.ui.viewmodel.LocalBackupStatus(driveFeedback.message, failed = driveFeedback.failed)
        else -> localStatus
    }
    val mimeType = if (uiState.selectedBackupFormat == "ZIP") "application/zip" else "application/json"
    var showHealthInfo by remember { mutableStateOf(false) }
    val defaultFileName = if (uiState.selectedBackupFormat == "ZIP") "trainpilot-backup.zip" else "trainpilot-backup.json"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri != null) {
            settingsViewModel.clearGoogleActionFeedback()
            if (onExportBackup != null) {
                onExportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Mentés készítése...", "Creating backup...", "Sicherung wird erstellt...", "Se creează backup-ul..."))
            }
        } else {
            pickerMessage = tr(lang, "A mentés megszakítva.", "Backup canceled.", "Sicherung abgebrochen.", "Backup anulat.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.clearGoogleActionFeedback()
            if (onImportBackup != null) {
                onImportBackup(uri)
            } else {
                onShowBanner(tr(uiState.language, "Visszaállítás fájlból...", "Restoring from file...", "Wiederherstellung aus Datei...", "Se restaurează din fișier..."))
            }
        } else {
            pickerMessage = tr(lang, "A fájlimport megszakítva.", "File import canceled.", "Dateiimport abgebrochen.", "Import de fișier anulat.")
        }
    }

    if (showHealthInfo) {
        com.example.ui.components.TrainPilotFormSheet(scrollText = false,
            testTag = "backup_health_info_sheet",
            onDismissRequest = { showHealthInfo = false },
            title = { Text(tr(uiState.language, "Egészségadatok", "Health data", "Gesundheitsdaten", "Date de sănătate")) },
            text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = tr(uiState.language,
                "Alapból kikapcsolva. JSON/ZIP: tárolt egészségnapló és edzésösszesítések. Drive: csak az edzéshez mentett egészségadatok; a teljes napi egészségnapló nem kerül Drive-ra. Kikapcsolás nem törli a helyi adatokat vagy a korábbi mentéseket.",
                "Off by default. JSON/ZIP: stored health journal and workout summaries. Drive: workout-attached health summaries only; the full daily health journal is not uploaded to Drive. Turning off does not delete local data or existing backups.",
                "Standardmäßig aus. JSON/ZIP: gespeichertes Gesundheitstagebuch und Trainingszusammenfassungen. Drive: nur trainingsbezogene Gesundheitsdaten; das vollständige Tagestagebuch wird nicht hochgeladen. Das Ausschalten löscht weder lokale Daten noch vorhandene Sicherungen.",
                "Dezactivat implicit. JSON/ZIP: jurnalul de sănătate stocat și rezumatele antrenamentelor. Drive: doar datele de sănătate atașate antrenamentelor; jurnalul zilnic complet nu este încărcat. Dezactivarea nu șterge datele locale sau backup-urile existente.")) },
            confirmButton = { com.repforge.app.ui.theme.TrainPilotTextButton(onClick = { showHealthInfo = false }) { Text("OK") } }
        )
    }

    Column(Modifier.then(com.example.ui.components.sheetAwareFillMaxSize()).trainPilotScreenBackdrop(palette)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth().weight(1f, fill = !inSheet)
                // In the sheet the JSON / ZIP / DRIVE tabs share one stable height (the tallest, Drive),
                // so switching tabs never makes the sheet jump.
                .then(if (inSheet)
                    Modifier.heightIn(min = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.78f)
                    else Modifier)
                // 2809/1: the outer column already paints the (glass) backdrop.
                .then(if (palette.isLiquidGlass) Modifier else Modifier.background(palette.background))
                .testTag("backup_restore_screen"),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            item {
                SubScreenTopBar(
                    title = strings.backupRestoreTitle,
                    onBack = onBack,
                    onOpenSettings = onOpenSettings
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JSON", "ZIP", "DRIVE").forEach { fmt ->
                        val sel = uiState.selectedBackupFormat == fmt
                        val interaction = remember(fmt) { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val shape = RoundedCornerShape(10.dp)
                        val background = if (sel) palette.primary else palette.surfaceCard
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("backup_format_$fmt")
                                .selectable(selected = sel, role = androidx.compose.ui.semantics.Role.Tab,
                                    interactionSource = interaction, indication = null, onClick = { onSelectFormat(fmt) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = shape,
                                color = lerp(background, palette.primary, if (pressed) 0.12f else 0f),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(shape)
                                    .indication(interaction, ripple())
                                    .trainPilotBorder(1.dp, if (sel) palette.primary else palette.cardBorder, shape)
                                    .testTag("backup_format_${fmt}_visual")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(fmt, fontWeight = FontWeight.Bold, color = if (sel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
            if (drive) item {
                StudioGoogleCloudSyncCard(
                    onShowBanner = onShowBanner,
                    onCloudSyncCompleted = onCloudSyncCompleted,
                    settingsViewModel = settingsViewModel,
                    compact = true
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr(uiState.language, "Mentés tartalma", "Backup contents", "Sicherungsinhalt", "Conținut backup"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(tr(uiState.language, "✓ Edzések és rekordok", "✓ Workouts and records", "✓ Trainings und Rekorde", "✓ Antrenamente și recorduri"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Text(tr(uiState.language, "✓ Programok és gyakorlatok", "✓ Programs and exercises", "✓ Programme und Übungen", "✓ Programe și exerciții"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Text(tr(lang, "✓ Célok, naptárterv és beállítások", "✓ Goals, calendar plan and settings", "✓ Ziele, Kalenderplan und Einstellungen", "✓ Obiective, calendar și setări"), color = palette.success, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (drive) tr(lang, "Edzések egészségadatai", "Workout health data", "Training-Gesundheitsdaten", "Date de sănătate la antrenament")
                                else tr(lang, "Egészségadatok", "Health data", "Gesundheitsdaten", "Date de sănătate"), color = palette.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showHealthInfo = true }, modifier = Modifier.testTag("backup_health_info")) {
                                Icon(Icons.Default.Info, contentDescription = tr(uiState.language, "Egészségadatok információ", "Health data information", "Informationen zu Gesundheitsdaten", "Informații despre datele de sănătate"), tint = palette.textSecondary)
                            }
                            Switch(checked = uiState.includeHealthInBackup, enabled = !uiState.backupHealthConsentSaving, onCheckedChange = onToggleIncludeHealth, modifier = Modifier.heightIn(min = 48.dp).testTag("backup_health_switch"))
                        }
                        if (drive) Text(tr(lang, "Napi lépések: helyi JSON/ZIP.", "Daily steps: local JSON/ZIP.",
                            "Tagesschritte: lokal als JSON/ZIP.", "Pași zilnici: JSON/ZIP local."), color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("backup_drive_health_scope"))
                    }
                }
            }
            item {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        pickerMessage = null
                        if (drive) {
                            if (gs.connectedEmail.isEmpty()) {
                                activity?.let { settingsViewModel.connectGoogle(it) }
                            } else {
                                activity?.let { settingsViewModel.syncDriveCloud(it) }
                            }
                        } else {
                            exportLauncher.launch(defaultFileName)
                        }
                    },
                    enabled = if (drive) driveBlock == null else localStatus?.busy != true && !gs.isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("backup_export_btn")
                ) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            drive && gs.connectedEmail.isEmpty() -> tr(lang, "Bejelentkezés Google-fiókkal", "Sign in with Google", "Mit Google anmelden", "Conectare cu Google")
                            drive -> tr(lang, "Drive-mentés és szinkron", "Drive backup and sync", "Drive-Sicherung und Sync", "Backup și sincronizare Drive")
                            else -> "${tr(lang, "Mentés", "Backup", "Sichern", "Salvează")} (${uiState.selectedBackupFormat})"
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (drive && gs.connectedEmail.isNotBlank()) {
                    Text(tr(lang, "A szinkron a felhőből is visszaállít és összefésül.",
                        "Sync also restores and merges data from Drive.",
                        "Der Sync stellt Daten aus Drive wieder her und führt sie zusammen.",
                        "Sincronizarea restaurează și îmbină datele din Drive."),
                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TrainPilotOutlinedButton(
                    onClick = {
                        pickerMessage = null
                        importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = localStatus?.busy != true && !gs.isBusy && !settingsState.replacementBusy &&
                        !settingsState.maintenanceBusy && !uiState.isWorkoutSessionActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("import_repforge_legacy_btn")
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(lang, "Helyi JSON/ZIP importálása", "Import local JSON/ZIP", "Lokale JSON-/ZIP-Datei importieren", "Importă JSON/ZIP local"),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Text(
                    if (uiState.isWorkoutSessionActive) tr(lang, "Az importálás előtt fejezd be az aktív edzést.",
                        "Finish the active workout before importing.", "Beende das laufende Training vor dem Import.",
                        "Încheie antrenamentul activ înainte de import.")
                    else tr(lang, "Korábbi mentések is visszaállíthatók.", "Earlier backups can also be restored.",
                        "Auch ältere Sicherungen lassen sich wiederherstellen.", "Poți restaura și backup-uri mai vechi."),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (status != null) {
            val color = when {
                status.failed -> palette.danger
                status.busy -> palette.primary
                pickerMessage == null && (!drive || driveBlock == null) &&
                    (status === localStatus || driveFeedback != null) -> palette.success
                else -> palette.textSecondary
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                modifier = Modifier.fillMaxWidth().padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 6.dp)
                    .testTag("backup_operation_status").semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(status.message, color = color, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()).padding(10.dp))
            }
        }
    }
}
