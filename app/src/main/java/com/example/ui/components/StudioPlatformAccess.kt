package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthRefresh
import com.repforge.app.NativeDataReplacement
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import com.repforge.app.SamsungHealthRepository
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

internal object StudioPlatformAccess {
    fun healthPermissions(context: Context): List<String> {
        if (Build.VERSION.SDK_INT < 34 || !HealthConnectRepository.isSupported(context)) return emptyList()
        val declared = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        return (HealthConnectRepository.ALL_RUNTIME_PERMISSIONS.toList() + HealthConnectRepository.WRITE_EXERCISE_PERMISSION).filter { it in declared }.filter { permission ->
            runCatching { context.packageManager.getPermissionInfo(permission, 0); true }.getOrDefault(false)
        }
    }
    fun snapshot(context: Context): JSONObject {
        val health = healthPermissions(context)
        fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        val location = context.getSystemService(LocationManager::class.java)
        val locationEnabled = if (Build.VERSION.SDK_INT >= 28) location?.isLocationEnabled == true
            else location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        return JSONObject().put("gps", granted(Manifest.permission.ACCESS_FINE_LOCATION))
            .put("location", locationEnabled)
            .put("notifications", (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)) && NotificationManagerCompat.from(context).areNotificationsEnabled())
            .put("healthGranted", health.count(::granted)).put("healthTotal", health.size)
    }
}

/** Request OS/provider permission, then read back system state. Never optimistically toggle a grant. */
@Composable
internal fun rememberStudioPermissionRequest(context: Context, model: TrainPilotViewModel): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val activity = context as? Activity
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
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch { refresh(); refreshSamsung() }
    }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch { refreshSamsung() }
            while (isActive) { refresh(); delay(5_000) }
        }
    }
    fun startHealthAction(provider: String) {
        if (healthActionJob?.isActive == true) {
            model.showBannerMessage("Egészségügyi művelet már folyamatban van."); return
        }
        healthActionJob = scope.launch {
            val admittedRoute = model.uiState.value.currentTab to model.uiState.value.currentSubScreen
            val guard = {
                check(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { "Látható képernyő szükséges a szinkronhoz." }
                check((model.uiState.value.currentTab to model.uiState.value.currentSubScreen) == admittedRoute) { "A képernyő megváltozott." }
            }
            try {
                val host = checkNotNull(activity) { "A szinkronhoz látható Activity szükséges." }
                model.updateHealthSyncStatus(true, message = "${if (provider == HealthJournalStore.HC) "Health Connect" else "Samsung Health"}: hozzáférés ellenőrzése…")
                runStudioHealthPermissionAction(
                    hasReadPermission = {
                        if (provider == HealthJournalStore.HC)
                            HealthConnectRepository.isSupported(context) && HealthConnectRepository.anyReadAllowed(context)
                        else {
                            check(SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()) { "Samsung Health nem érhető el." }
                            SamsungHealthRepository.readGrantedKeys(context, guard).isNotEmpty()
                        }
                    },
                    requestPermission = {
                        if (provider == HealthJournalStore.HC) {
                            val permissions = StudioPlatformAccess.healthPermissions(context)
                                .filter { it != HealthConnectRepository.WRITE_EXERCISE_PERMISSION }
                            check(permissions.isNotEmpty()) { "A Health Connect ezen az eszközön nem érhető el." }
                            val pending = CompletableDeferred<Unit>(currentCoroutineContext()[Job])
                            healthConsent = pending
                            runtime.launch(permissions.toTypedArray())
                            pending.await()
                        } else SamsungHealthRepository.requestReadPermissions(host).getOrThrow()
                        withTimeout(10_000) {
                            while (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) delay(50)
                        }
                    }, guard = guard,
                    sync = {
                        guard()
                        NativeHealthForegroundAdmission.requireMainAdmission(context)
                        withContext(Dispatchers.IO) {
                            NativeDataReplacement.mutate(context) {
                                guard()
                                HealthJournalStore.get(context).setPreferences(JSONObject().put("provider", provider))
                            }
                        }
                        val report = NativeHealthRefresh.sync(context, host, requireScreenGuard = guard).getOrThrow()
                        val partial = report.optBoolean("partial")
                        model.updateHealthSyncStatus(false, partial = partial,
                            message = "${if (provider == HealthJournalStore.HC) "Health Connect" else "Samsung Health"}: " +
                                if (partial) "részleges szinkron • ${report.optInt("completedDays")} nap frissítve" else "szinkron befejezve")
                        model.reloadHealthJournal()
                    })
                refresh(); refreshSamsung()
            } catch (cancelled: CancellationException) {
                model.updateHealthSyncStatus(false, failed = true, message = "Az egészségügyi művelet megszakadt.")
                throw cancelled
            } catch (error: Exception) {
                model.updateHealthSyncStatus(false, failed = true, message = error.message ?: "Az egészségügyi szinkron nem sikerült.")
            } finally { healthConsent?.cancel(); healthConsent = null }
        }
    }
    return { key ->
        try {
            when (key) {
                "gps" -> if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    runtime.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    else settings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
                "central" -> settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                "notif" -> if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    runtime.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    else settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                "hc" -> startHealthAction(HealthJournalStore.HC)
                "samsung" -> startHealthAction(HealthJournalStore.SAMSUNG)
                "hc_manage" -> settings.launch(Intent(android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
                    .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName))
                "samsung_manage" -> scope.launch {
                    activity?.let { SamsungHealthRepository.requestReadPermissions(it, force = true) }
                        ?.exceptionOrNull()?.let { model.showBannerMessage(it.message ?: "Samsung Health engedélykérés sikertelen.") }
                    refreshSamsung()
                }
            }
        } catch (error: Exception) { model.showBannerMessage(error.message ?: "A rendszer engedélykérője nem érhető el.") }
    }
}
