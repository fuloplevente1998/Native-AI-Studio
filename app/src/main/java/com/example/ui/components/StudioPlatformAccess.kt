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
        model.updatePlatformAccess(StudioPlatformAccess.snapshot(context), nodes.map { it.displayName })
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
    var healthRequestPending by remember { mutableStateOf(false) }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (healthRequestPending) {
            healthRequestPending = false
            if (results.isEmpty() || results.values.none { it }) {
                model.showBannerMessage("A Health Connect nem adott hozzáférést. Ellenőrizd a rendszerengedélyeket.")
            }
        }
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
                "hc" -> {
                    val permissions = StudioPlatformAccess.healthPermissions(context)
                    check(permissions.isNotEmpty()) { "A Health Connect ezen az eszközön nem érhető el." }
                    val missing = permissions.filter {
                        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        healthRequestPending = true
                        runtime.launch(missing.toTypedArray())
                    } else {
                        settings.launch(Intent(android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
                            .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName))
                    }
                }
                "samsung" -> scope.launch {
                    val result = activity?.let { SamsungHealthRepository.requestReadPermissions(it) }
                    result?.exceptionOrNull()?.let { model.showBannerMessage(it.message ?: "Samsung Health engedélykérés sikertelen.") }
                    if (activity == null) model.showBannerMessage("Az engedélykéréshez látható Activity szükséges.")
                    refreshSamsung()
                }
            }
        } catch (error: Exception) { model.showBannerMessage(error.message ?: "A rendszer engedélykérője nem érhető el.") }
    }
}
