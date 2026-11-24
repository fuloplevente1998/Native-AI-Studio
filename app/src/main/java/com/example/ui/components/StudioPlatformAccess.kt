package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.viewmodel.TrainPilotViewModel
import com.example.ui.viewmodel.reloadHealthJournal
import com.example.ui.viewmodel.showBannerMessage
import com.example.ui.viewmodel.updateHealthSyncStatus
import com.example.ui.viewmodel.updatePlatformAccess
import com.example.ui.viewmodel.updatePrivacyChoice
import com.example.ui.viewmodel.updateSamsungAccess
import com.repforge.app.HealthConnectSyncRecovery
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthRefresh
import com.repforge.app.NativePrivacyControls
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import com.repforge.app.SamsungHealthRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

internal object StudioPlatformAccess {
    fun healthPermissions(context: Context): List<String> {
        if (!HealthConnectRepository.isSupported(context)) return emptyList()
        val declared = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        return (HealthConnectRepository.ALL_RUNTIME_PERMISSIONS.toList() + HealthConnectRepository.WRITE_EXERCISE_PERMISSION)
            .filter { it in declared }
            .filter { permission ->
                // Pre-Android 14 permissions belong to the Health Connect APK, not PackageManager.
                Build.VERSION.SDK_INT < 34 ||
                    runCatching { context.packageManager.getPermissionInfo(permission, 0); true }.getOrDefault(false)
            }
    }
    fun snapshot(context: Context): JSONObject {
        val health = healthPermissions(context).filter { it != HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
        fun granted(permission: String) =
            if (permission.startsWith("android.permission.health."))
                HealthConnectRepository.isAllowed(context, permission.substringAfterLast('.'))
            else context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        val location = context.getSystemService(LocationManager::class.java)
        val locationEnabled = if (Build.VERSION.SDK_INT >= 28) location?.isLocationEnabled == true
            else location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        return JSONObject().put("gps", NativePrivacyControls.enabled(context, NativePrivacyControls.GPS))
            .put("gpsGranted", granted(Manifest.permission.ACCESS_FINE_LOCATION))
            .put("location", locationEnabled)
            .put("notifications", NativePrivacyControls.enabled(context, NativePrivacyControls.NOTIFICATIONS))
            .put("notificationsGranted", (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) && NotificationManagerCompat.from(context).areNotificationsEnabled())
            .put("healthConnect", NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT))
            .put("healthReadGranted", HealthConnectRepository.anyReadAllowed(context))
            .put("samsungHealth", NativePrivacyControls.enabled(context, NativePrivacyControls.SAMSUNG_HEALTH))
            .put("healthGranted", health.count(::granted)).put("healthTotal", health.size)
    }
}

/** The Settings entry point is NOT a data sync. Request missing read grants from the visible Activity. */
internal object StudioHealthPermissionRouting {
    fun sourceManageAction(provider: String): String =
        if (provider == HealthJournalStore.SAMSUNG) "samsung_manage" else "hc_manage"

    fun missingReads(declaredReads: List<String>, granted: (String) -> Boolean): List<String> =
        declaredReads.filterNot(granted)

    // ACTION_MANAGE_HEALTH_PERMISSIONS can require GRANT_RUNTIME_PERMISSIONS on some OS builds.
    // Its privileged controller is not a supported third-party launcher.
    fun settingsIntent(): Intent = HealthConnectRepository.settingsIntent()

    fun fallbackIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
}

private fun privacyMessage(language: String, hu: String, en: String, de: String, ro: String): String =
    when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

/** App consent and system permission are distinct; a switch must not open settings when turned off. */
@Composable
internal fun rememberStudioPermissionRequest(context: Context, model: TrainPilotViewModel): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val activity = context as? Activity
    var previousLegacyGrants by remember { mutableStateOf<Set<String>?>(null) }
    suspend fun refresh() {
        val nodes = runCatching { withTimeout(3_000) { Wearable.getNodeClient(context).connectedNodes.await() } }
            .getOrElse { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                emptyList()
            }
        val companion = if (nodes.size == 1) runCatching {
            withTimeout(3_000) { com.repforge.app.NativeWearCompanion.reachable(context, nodes.single().id) }
        }.getOrElse { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            false
        } else false
        if (Build.VERSION.SDK_INT in 28..33) {
            val liveGrants = withContext(Dispatchers.IO) {
                try { HealthConnectRepository.currentlyGrantedReadKeys(context) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { emptySet() }
            }
            if (previousLegacyGrants != liveGrants) {
                previousLegacyGrants = liveGrants
                // A revoked grant hides previously saved measurements immediately; a new
                // grant exposes the journal without requiring a fresh app launch.
                model.reloadHealthJournal()
            }
        }
        model.updatePlatformAccess(StudioPlatformAccess.snapshot(context), nodes.map { it.displayName }, companion)
    }
    suspend fun refreshSamsung() {
        if (!SamsungHealthRepository.isInstalled(context) || !SamsungHealthRepository.isSdkAvailable()) {
            model.updateSamsungAccess(false); return
        }
        val granted = runCatching { SamsungHealthRepository.readGrantScope(context) {
            check(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }.isNotEmpty() }.getOrElse { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            false
        }
        model.updateSamsungAccess(granted)
    }
    var healthConsent by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    var healthActionJob by remember { mutableStateOf<Job?>(null) }
    val uiState by model.uiState.collectAsState()
    val route = uiState.currentTab to uiState.currentSubScreen
    DisposableEffect(route, owner) {
        onDispose { healthActionJob?.cancel(); healthConsent?.cancel(); healthConsent = null }
    }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        healthConsent?.complete(Unit)
        healthConsent = null
        scope.launch { refresh() }
    }
    // Keep Health Connect consent independent of GPS/notification requests.
    val healthPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        val pendingSync = healthConsent
        healthConsent = null
        pendingSync?.complete(Unit)
        scope.launch {
            refresh()
            if (pendingSync == null) {
                val declaredReads = StudioPlatformAccess.healthPermissions(context)
                    .filter { it != HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
                val granted = declaredReads.count {
                    HealthConnectRepository.isAllowed(context, it.substringAfterLast('.'))
                }
                if (NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT) && granted == 0)
                    model.showBannerMessage(privacyMessage(model.uiState.value.language.name,
                        "A Health Connect használatához olvasási engedély kell.",
                        "Health Connect needs read access.", "Health Connect benötigt Lesezugriff.",
                        "Health Connect necesită acces de citire."))
                else model.showBannerMessage("Health Connect: $granted/${declaredReads.size}")
            }
        }
    }
    val healthPermissionsLegacy = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        val pendingSync = healthConsent
        healthConsent = null
        pendingSync?.complete(Unit)
        scope.launch { refresh(); model.reloadHealthJournal() }
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch { refresh(); refreshSamsung() }
    }
    fun openPermissionSettings(intent: Intent, fallback: Intent? = null): Boolean {
        for (candidate in listOfNotNull(intent, fallback)) {
            try {
                settings.launch(candidate)
                return true
            } catch (_: SecurityException) {
            } catch (_: ActivityNotFoundException) {
            }
        }
        return false
    }
    fun permissionNotice(hu: String, en: String, de: String, ro: String) {
        android.widget.Toast.makeText(context, privacyMessage(model.uiState.value.language.name, hu, en, de, ro),
            android.widget.Toast.LENGTH_LONG).show()
    }
    fun openHealthConnectSettings() {
        if (!openPermissionSettings(StudioHealthPermissionRouting.settingsIntent(),
                StudioHealthPermissionRouting.fallbackIntent(context))) {
            permissionNotice(
                "Az Android-beállításokban nyisd meg a Health Connect alkalmazásengedélyeit.",
                "Open Health Connect app permissions from Android settings.",
                "Öffne die Health-Connect-App-Berechtigungen in den Android-Einstellungen.",
                "Deschide permisiunile Health Connect din setările Android.")
        }
    }
    // Called directly in the click handler, not after a suspend/health sync operation.
    fun requestHealthConnectPermissions(manage: Boolean = false) {
        if (manage && !NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT)) {
            openHealthConnectSettings()
            return
        }
        if (!manage && !NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT)) {
            model.showBannerMessage(privacyMessage(model.uiState.value.language.name,
                "A Health Connect ki van kapcsolva.", "Health Connect is turned off.",
                "Health Connect ist ausgeschaltet.", "Health Connect este dezactivat."))
            return
        }
        if (healthActionJob?.isActive == true) {
            model.showBannerMessage("Egészségügyi művelet már folyamatban van.")
            return
        }
        if (Build.VERSION.SDK_INT in 28..33 && !HealthConnectRepository.isSupported(context)) {
            // The provider is a separate Play Store app on Android 9–13.
            val id = "com.google.android.apps.healthdata"
            val play = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$id"))
            val web = Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$id"))
            try { settings.launch(play) }
            catch (_: ActivityNotFoundException) { settings.launch(web) }
            model.showBannerMessage(privacyMessage(model.uiState.value.language.name,
                "Telepítsd vagy frissítsd a Health Connect alkalmazást, majd engedélyezd az adatokat.",
                "Install or update Health Connect, then grant data access.",
                "Installiere oder aktualisiere Health Connect und erteile Datenzugriff.",
                "Instalează sau actualizează Health Connect, apoi acordă acces la date."))
            return
        }
        val declaredReads = StudioPlatformAccess.healthPermissions(context)
            .filter { it != HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
        if (!HealthConnectRepository.isSupported(context) || declaredReads.isEmpty()) {
            model.showBannerMessage("A Health Connect ezen az eszközön nem érhető el.")
            return
        }
        if (Build.VERSION.SDK_INT < 34) {
            scope.launch {
                val granted = runCatching { HealthConnectRepository.currentlyGrantedReadKeys(context) }
                    .getOrElse { model.showBannerMessage("A Health Connect engedélyek nem olvashatók."); return@launch }
                val missing = declaredReads.filter { it.substringAfterLast('.') !in granted }
                if (missing.isNotEmpty()) {
                    try { healthPermissionsLegacy.launch(missing.toSet()) }
                    catch (_: ActivityNotFoundException) { openHealthConnectSettings() }
                    catch (_: SecurityException) { openHealthConnectSettings() }
                } else if (manage) {
                    openHealthConnectSettings()
                } else {
                    refresh()
                }
            }
            return
        }
        val missingReads = StudioHealthPermissionRouting.missingReads(declaredReads) { permission ->
            HealthConnectRepository.isAllowed(context, permission.substringAfterLast('.'))
        }
        if (missingReads.isNotEmpty()) {
            try { healthPermissions.launch(missingReads.toTypedArray()) }
            catch (_: ActivityNotFoundException) { openHealthConnectSettings() }
            catch (_: SecurityException) { openHealthConnectSettings() }
        } else if (manage) {
            openHealthConnectSettings()
        } else {
            scope.launch { refresh() } // Already granted: do not launch the OS settings.
        }
    }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch { refreshSamsung(); model.reloadHealthJournal() }
            var cacheDay = java.time.LocalDate.now()
            while (isActive) {
                refresh()
                val nowDay = java.time.LocalDate.now()
                if (nowDay != cacheDay) { cacheDay = nowDay; model.reloadHealthJournal() }
                delay(5_000)
            }
        }
    }
    fun startHealthAction(provider: String, permissionOnly: Boolean = false) {
        val feature = if (provider == HealthJournalStore.HC) NativePrivacyControls.HEALTH_CONNECT
            else NativePrivacyControls.SAMSUNG_HEALTH
        if (!NativePrivacyControls.enabled(context, feature)) {
            model.showBannerMessage(privacyMessage(model.uiState.value.language.name,
                "Ez az adatforrás ki van kapcsolva.", "This health source is off.",
                "Diese Gesundheitsquelle ist ausgeschaltet.", "Această sursă de sănătate este oprită."))
            return
        }
        if (healthActionJob?.isActive == true) {
            model.showBannerMessage("Egészségügyi művelet már folyamatban van."); return
        }
        healthActionJob = scope.launch {
            val admittedRoute = model.uiState.value.currentTab to model.uiState.value.currentSubScreen
            val guard = {
                NativePrivacyControls.requireEnabled(context, feature)
                check(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { "Látható képernyő szükséges a szinkronhoz." }
                check((model.uiState.value.currentTab to model.uiState.value.currentSubScreen) == admittedRoute) { "A képernyő megváltozott." }
            }
            try {
                val host = checkNotNull(activity) { "A szinkronhoz látható Activity szükséges." }
                model.updateHealthSyncStatus(true, message = "${if (provider == HealthJournalStore.HC) "Health Connect" else "Samsung Health"}: hozzáférés ellenőrzése…")
                runStudioHealthPermissionAction(
                    hasReadPermission = {
                        if (provider == HealthJournalStore.HC)
                            HealthConnectRepository.isSupported(context) &&
                                HealthConnectRepository.currentlyGrantedReadKeys(context).isNotEmpty()
                        else {
                            check(SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()) { "Samsung Health nem érhető el." }
                            val granted = SamsungHealthRepository.readGrantedKeys(context, guard)
                            if (permissionOnly) SamsungHealthRepository.PERMISSION_KEYS.all { it in granted }
                            else granted.isNotEmpty()
                        }
                    },
                    requestPermission = {
                        if (provider == HealthJournalStore.HC) {
                            val permissions = StudioPlatformAccess.healthPermissions(context)
                                .filter { it != HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
                            check(permissions.isNotEmpty()) { "A Health Connect ezen az eszközön nem érhető el." }
                            val pending = CompletableDeferred<Unit>(currentCoroutineContext()[Job])
                            healthConsent = pending
                            val granted = HealthConnectRepository.currentlyGrantedReadKeys(context)
                            val missing = permissions.filter { it.substringAfterLast('.') !in granted }
                            if (missing.isNotEmpty()) {
                                if (Build.VERSION.SDK_INT >= 34) healthPermissions.launch(missing.toTypedArray())
                                else healthPermissionsLegacy.launch(missing.toSet())
                            } else pending.complete(Unit)
                            pending.await()
                        } else SamsungHealthRepository.requestReadPermissions(host).getOrThrow()
                        withTimeout(10_000) {
                            while (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) delay(50)
                        }
                    }, guard = guard,
                    sync = {
                        if (permissionOnly) {
                            model.updateHealthSyncStatus(false, message = "Olvasási hozzáférés igazolva.")
                        } else {
                        guard()
                        NativeHealthForegroundAdmission.requireMainAdmission(context)
                        val report = NativeHealthRefresh.sync(context, host, providerOverride = provider,
                            requireScreenGuard = guard).getOrThrow()
                        val partial = report.optBoolean("partial")
                        model.updateHealthSyncStatus(false, partial = partial,
                            message = "${if (provider == HealthJournalStore.HC) "Health Connect" else "Samsung Health"}: " +
                                if (partial) "részleges szinkron • ${report.optInt("completedDays")} nap frissítve"
                                else if (report.optBoolean("changeTrackingUnavailable")) HealthConnectSyncRecovery.limitedMessage(model.uiState.value.language.name)
                                else "szinkron befejezve")
                        }
                    })
                refresh(); refreshSamsung()
            } catch (cancelled: CancellationException) {
                model.updateHealthSyncStatus(false, failed = true, message = "Az egészségügyi művelet megszakadt.")
                throw cancelled
            } catch (error: Exception) {
                model.updateHealthSyncStatus(false, failed = true, message = HealthConnectSyncRecovery.failureMessage(error, model.uiState.value.language.name))
            } finally {
                if (!permissionOnly) model.reloadHealthJournal()
                healthConsent?.cancel(); healthConsent = null
            }
        }
    }
    return { key ->
        try {
            when (key) {
                "gps_on", "gps_off", "notif_on", "notif_off",
                "hc_on", "hc_off", "samsung_on", "samsung_off" -> {
                    val enabled = key.endsWith("_on")
                    val feature = when {
                        key.startsWith("gps_") -> NativePrivacyControls.GPS
                        key.startsWith("notif_") -> NativePrivacyControls.NOTIFICATIONS
                        key.startsWith("hc_") -> NativePrivacyControls.HEALTH_CONNECT
                        else -> NativePrivacyControls.SAMSUNG_HEALTH
                    }
                    NativePrivacyControls.set(context, feature, enabled)
                    model.updatePrivacyChoice(feature, enabled)
                    if (enabled && feature == NativePrivacyControls.HEALTH_CONNECT) {
                        scope.launch(Dispatchers.IO) {
                            runCatching { HealthBackgroundJob.schedule(context) }
                        }
                    }
                    if (!enabled && feature in setOf(NativePrivacyControls.HEALTH_CONNECT,
                            NativePrivacyControls.SAMSUNG_HEALTH)) {
                        healthActionJob?.cancel()
                        healthConsent?.cancel()
                        healthConsent = null
                    }
                    scope.launch { refresh(); refreshSamsung() }
                    if (enabled) when (feature) {
                        NativePrivacyControls.GPS ->
                            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                                runtime.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        NativePrivacyControls.NOTIFICATIONS ->
                            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                                runtime.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        NativePrivacyControls.HEALTH_CONNECT -> requestHealthConnectPermissions()
                        NativePrivacyControls.SAMSUNG_HEALTH ->
                            startHealthAction(HealthJournalStore.SAMSUNG, permissionOnly = true)
                    }
                }
                // Other entry points request access, but never infer or change the app switch.
                "gps" -> if (NativePrivacyControls.enabled(context, NativePrivacyControls.GPS) &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    runtime.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                "notif" -> if (NativePrivacyControls.enabled(context, NativePrivacyControls.NOTIFICATIONS) &&
                    Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    runtime.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                "central" -> settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                "permissions_manage", "gps_manage" ->
                    settings.launch(StudioHealthPermissionRouting.fallbackIntent(context))
                "notif_manage" -> settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                "hc" -> startHealthAction(HealthJournalStore.HC)
                "samsung" -> startHealthAction(HealthJournalStore.SAMSUNG)
                "hc_permission" -> requestHealthConnectPermissions()
                "samsung_permission" -> startHealthAction(HealthJournalStore.SAMSUNG, permissionOnly = true)
                "hc_manage" -> requestHealthConnectPermissions(manage = true)
                "samsung_manage" -> scope.launch {
                    activity?.let { SamsungHealthRepository.requestReadPermissions(it, force = true) }
                        ?.exceptionOrNull()?.let {
                            model.showBannerMessage(HealthConnectSyncRecovery.failureMessage(it, model.uiState.value.language.name))
                        }
                    refreshSamsung()
                }
            }
        } catch (error: Exception) {
            model.showBannerMessage(HealthConnectSyncRecovery.failureMessage(error, model.uiState.value.language.name))
        }
    }
}
