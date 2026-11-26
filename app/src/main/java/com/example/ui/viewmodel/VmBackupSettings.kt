package com.example.ui.viewmodel

import com.example.model.AppLanguage
import com.example.ui.theme.TrainPilotPalettes
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import com.repforge.app.ActiveWorkoutStore
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.model.watchinfo.WatchInfoCardData
import com.repforge.app.BackupRestoreManager
import android.net.Uri

fun TrainPilotViewModel.exportBackup(context: Context, uri: Uri, format: String) {
    val busy = studioModesText("Mentés készül…", "Creating backup…", "Sicherung wird erstellt…", "Se creează backup-ul…")
    val failed = studioModesText("A mentés nem sikerült.", "Backup failed.", "Sicherung fehlgeschlagen.", "Backup-ul a eșuat.")
    runLocalBackup(busy, failed) {
        check(calendarWriteError == null) { requireNotNull(calendarWriteError) }
        check(plannerSettingsWriteError == null) { requireNotNull(plannerSettingsWriteError) }
        val manager = BackupRestoreManager(context)
        if (format.equals("ZIP", ignoreCase = true)) manager.exportZipArchiveToUri(uri).getOrThrow()
        else manager.exportJsonBackupToUri(uri).getOrThrow()
        studioModesText("A mentés elkészült.", "Backup saved.", "Sicherung gespeichert.", "Backup-ul a fost salvat.")
    }
}

fun TrainPilotViewModel.importBackup(context: Context, uri: Uri) {
    val busy = studioModesText("Visszaállítás folyamatban…", "Restoring backup…", "Sicherung wird eingespielt…", "Se restaurează backup-ul…")
    val failed = studioModesText("A visszaállítás nem sikerült.", "Restore failed.", "Wiederherstellung fehlgeschlagen.", "Restaurarea a eșuat.")
    runLocalBackup(busy, failed) {
        BackupRestoreManager(context).importBackupFromUri(uri, recoverInterrupted = false).getOrThrow()
        withContext(Dispatchers.Main.immediate) { refreshAfterCloudSync(context) }
        studioModesText("A visszaállítás sikerült.", "Backup restored.", "Sicherung wiederhergestellt.", "Backup-ul a fost restaurat.")
    }
}

internal fun TrainPilotViewModel.runLocalBackup(busy: String, failed: String, operation: suspend () -> String) {
    val scope = nativeScope ?: return
    if (_uiState.value.localBackupStatus?.busy == true) return
    _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(busy, busy = true)) }
    scope.launch(Dispatchers.IO) {
        try {
            calendarWriteJob?.join()
            plannerSettingsWriteJob?.join()
            val message = operation()
            _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(message)) }
        } catch (cancelled: CancellationException) {
            _uiState.update { it.copy(localBackupStatus = null) }
            throw cancelled
        } catch (error: Exception) {
            val detail = error.localizedMessage?.takeIf { it.isNotBlank() }
            _uiState.update { it.copy(localBackupStatus = LocalBackupStatus(
                listOfNotNull(failed, detail).joinToString("\n"), failed = true)) }
        }
    }
}

fun TrainPilotViewModel.refreshAfterCloudSync(context: Context) {
    val scope = nativeScope ?: return
    val db = nativeDatabase ?: return
    val store = nativeSettingsStore ?: return
    reloadNativeEnvironment(context, store, db, scope)
}

fun TrainPilotViewModel.selectBackupFormat(format: String) {
    _uiState.update { it.copy(selectedBackupFormat = format) }
}

fun TrainPilotViewModel.toggleIncludeHealthInBackup(include: Boolean) {
    val ctx = nativeContext ?: return
    val store = nativeSettingsStore ?: return
    val scope = nativeScope ?: return
    if (_uiState.value.backupHealthConsentSaving) return
    _uiState.update { it.copy(backupHealthConsentSaving = true) }
    scope.launch(Dispatchers.IO) {
        try {
            NativeDataReplacement.mutate(ctx) { store.setHealthBackupConsent(include) }
            _uiState.update { it.copy(includeHealthInBackup = include) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { _uiState.update { it.copy(statusBannerMessage = "A mentési hozzájárulás nem menthető. Próbáld újra.") } }
        finally { _uiState.update { it.copy(backupHealthConsentSaving = false) } }
    }
}

fun TrainPilotViewModel.updatePlatformAccess(access: JSONObject, watchNames: List<String>, companionAvailable: Boolean = false) {
    val frame = nativeContext?.let { ActiveWorkoutStore.current(it) }
    val journal = watchJournal()
    val pending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, journal)
    val confirmed = frame?.let { com.repforge.app.NativeWatchHandoffReceipt.confirmed(
        it, journal, it.optString("workoutId"), it.optString("timerOwner")) } == true
    val companionReady = companionAvailable && watchNames.size == 1
    _uiState.update {
        val oldCard = it.watchInfoCardData
        val sameNode = companionReady && oldCard.name == watchNames.singleOrNull() && oldCard.appAvailable
        val nextCard = if (sameNode) oldCard.copy(connected = true, appAvailable = true)
            else WatchInfoCardData(
                connected = watchNames.isNotEmpty(),
                appAvailable = companionReady,
                name = watchNames.singleOrNull()
            )
        it.copy(permissionAccess = it.permissionAccess.copy(
            gpsEnabled = access.optBoolean("gps"),
            gpsGranted = access.optBoolean("gpsGranted"),
            centralLocationEnabled = access.optBoolean("location"),
            notificationsEnabled = access.optBoolean("notifications"),
            notificationsGranted = access.optBoolean("notificationsGranted"),
            healthConnectEnabled = access.optBoolean("healthConnect"),
            healthConnectGranted = access.optBoolean("healthReadGranted"),
            samsungHealthEnabled = access.optBoolean("samsungHealth"),
            trainPilotWearEnabled = access.optBoolean("trainPilotWear")),
            healthGrantedCount = access.optInt("healthGranted"),
            healthPermissionCount = access.optInt("healthTotal"),
            watchControlPending = pending,
            phoneControlsWorkout = (frame == null || frame.optString("timerOwner", "phone") == "phone") &&
                !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame),
            watchAppAvailable = companionReady,
            watchConnected = watchNames.isNotEmpty(),
            watchName = if (companionReady) watchNames.single() else "Nincs csatlakoztatott Wear OS óra",
            watchInfoCardData = nextCard,
            watchBatteryPercent = if (sameNode) it.watchBatteryPercent else -1,
            lastWatchSyncLabel = if (!companionReady) "Nincs órakapcsolat"
                else if (confirmed) "Tulajdonosi ACK igazolva"
                else if (it.lastWatchSyncLabel in setOf("—", "Nincs órakapcsolat", "Nincs óra kapcsolat"))
                    "Még nincs igazolt adatszinkron" else it.lastWatchSyncLabel)
    }
}

fun TrainPilotViewModel.updatePrivacyChoice(feature: String, enabled: Boolean) {
    // Only called after the persistent preference commit succeeds. Unlike OS grants,
    // this is the state the switch represents and should update without a Wear query.
    _uiState.update { state ->
        when (feature) {
            com.repforge.app.NativePrivacyControls.GPS -> state.copy(permissionAccess = state.permissionAccess.copy(gpsEnabled = enabled))
            com.repforge.app.NativePrivacyControls.NOTIFICATIONS -> state.copy(permissionAccess = state.permissionAccess.copy(notificationsEnabled = enabled))
            com.repforge.app.NativePrivacyControls.HEALTH_CONNECT -> state.copy(permissionAccess = state.permissionAccess.copy(healthConnectEnabled = enabled))
            com.repforge.app.NativePrivacyControls.SAMSUNG_HEALTH -> state.copy(permissionAccess = state.permissionAccess.copy(samsungHealthEnabled = enabled))
            com.repforge.app.NativePrivacyControls.TRAINPILOT_WEAR -> state.copy(
                permissionAccess = state.permissionAccess.copy(trainPilotWearEnabled = enabled))
            else -> state
        }
    }
}

fun TrainPilotViewModel.updateSamsungAccess(granted: Boolean) { _uiState.update { it.copy(permissionAccess = it.permissionAccess.copy(samsungHealthGranted = granted)) } }

fun TrainPilotViewModel.showBannerMessage(message: String) {
    _uiState.update { it.copy(statusBannerMessage = message) }
}

fun TrainPilotViewModel.clearBannerMessage() {
    _uiState.update { it.copy(statusBannerMessage = null) }
}

fun TrainPilotViewModel.syncNativeSettings(
    languageCode: String,
    themeKey: String,
    restSec: Int = 90,
    sound: Boolean = true,
    vibration: Boolean = true
) {
    val lang = when (languageCode.lowercase()) {
        "en" -> AppLanguage.EN
        "de" -> AppLanguage.DE
        "ro" -> AppLanguage.RO
        else -> AppLanguage.HU
    }
    val palette = com.example.ui.theme.TrainPilotPalettes.getByKey(themeKey)
    _uiState.update { current ->
        current.copy(
            language = lang,
            selectedPaletteId = palette?.id ?: TrainPilotPalettes.getById(1).id,
            defaultRestSec = if (restSec > 0) restSec else current.defaultRestSec,
            soundAlertEnabled = sound,
            vibrationEnabled = vibration
        )
    }
}
