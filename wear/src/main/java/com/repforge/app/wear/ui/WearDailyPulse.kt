package com.repforge.app.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import java.time.Instant

internal data class WearDailyPulse(val bpm: Int, val at: Long)

/** Foreground-only passive pulse, using the existing heart permission, not the exercise engine. */
@Composable
internal fun rememberWearDailyPulse(enabled: Boolean): WearDailyPulse? {
    val context = LocalContext.current.applicationContext
    val permission = if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS
    val allowed = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    var pulse by remember { mutableStateOf<WearDailyPulse?>(null) }
    DisposableEffect(enabled, allowed) {
        pulse = null
        if (!enabled || !allowed) onDispose {}
        else {
            val client = HealthServices.getClient(context).passiveMonitoringClient
            var alive = true
            val callback = object : PassiveListenerCallback {
                override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                    if (!alive || context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return
                    val now = System.currentTimeMillis()
                    val boot = Instant.ofEpochMilli(now - SystemClock.elapsedRealtime())
                    val latest = dataPoints.getData(DataType.HEART_RATE_BPM).mapNotNull {
                        val at = it.getTimeInstant(boot).toEpochMilli()
                        if (it.value.isFinite() && it.value in 20.0..250.0 && now - at in 0L..300000L)
                            WearDailyPulse(it.value.toInt(), at) else null
                    }.maxByOrNull { it.at }
                    if (latest != null && latest.at >= (pulse?.at ?: 0L)) pulse = latest
                }
                override fun onPermissionLost() { if (alive) pulse = null }
            }
            WearHealthFutures.awaitValue(client.getCapabilitiesAsync(), context.mainExecutor, { alive }, {}) { caps ->
                if (DataType.HEART_RATE_BPM in caps.supportedDataTypesPassiveMonitoring) {
                    runCatching { client.setPassiveListenerCallback(
                        PassiveListenerConfig.builder().setDataTypes(setOf(DataType.HEART_RATE_BPM)).build(), context.mainExecutor, callback) }
                }
            }
            onDispose {
                alive = false
                pulse = null
                runCatching { client.clearPassiveListenerCallbackAsync() }
            }
        }
    }
    return if (enabled && allowed) pulse else null
}
