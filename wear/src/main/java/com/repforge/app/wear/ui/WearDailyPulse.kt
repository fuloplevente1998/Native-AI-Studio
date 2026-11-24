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

/**
 * Foreground-only passive callback, using the existing heart permission, not the exercise engine.
 * 2808: it also carries the opted-in daily totals (ACTIVITY_RECOGNITION), so the open app shows
 * current steps instead of a batched/old total, and a pause/resume keeps the last pulse sample.
 */
@Composable
internal fun rememberWearDailyPulse(enabled: Boolean): WearDailyPulse? {
    val context = LocalContext.current.applicationContext
    val permission = if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS
    val allowed = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    val daily = enabled && WearDailyHealthStore.enabled(context) && WearDailyHealthStore.permitted(context)
    var pulse by remember { mutableStateOf<WearDailyPulse?>(null) }
    DisposableEffect(enabled, allowed, daily) {
        if (!allowed) pulse = null
        if (!enabled || (!allowed && !daily)) onDispose {}
        else {
            val client = HealthServices.getClient(context).passiveMonitoringClient
            var alive = true
            val callback = object : PassiveListenerCallback {
                override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                    if (!alive) return
                    val now = System.currentTimeMillis()
                    if (daily) WearDailyHealthStore.accept(context, dataPoints, now)
                    if (!allowed || context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return
                    val boot = Instant.ofEpochMilli(now - SystemClock.elapsedRealtime())
                    val latest = dataPoints.getData(DataType.HEART_RATE_BPM).mapNotNull {
                        val at = it.getTimeInstant(boot).toEpochMilli()
                        if (it.value.isFinite() && it.value in 20.0..250.0 && now - at in -WEAR_PULSE_SKEW_MS..300000L)
                            WearDailyPulse(it.value.toInt(), minOf(at, now)) else null
                    }.maxByOrNull { it.at }
                    if (latest != null && latest.at >= (pulse?.at ?: 0L)) pulse = latest
                }
                override fun onPermissionLost() { if (alive) pulse = null }
            }
            WearHealthFutures.awaitValue(client.getCapabilitiesAsync(), context.mainExecutor, { alive }, {}) { caps ->
                val supported = caps.supportedDataTypesPassiveMonitoring
                val types = buildSet<DataType<*, *>> {
                    if (allowed && DataType.HEART_RATE_BPM in supported) add(DataType.HEART_RATE_BPM)
                    if (daily) addAll(WearDailyHealthStore.dailyTypes.filter { it in supported })
                }
                if (types.isNotEmpty()) runCatching { client.setPassiveListenerCallback(
                    PassiveListenerConfig.builder().setDataTypes(types).build(), context.mainExecutor, callback) }
            }
            onDispose {
                alive = false
                runCatching { client.clearPassiveListenerCallbackAsync() }
                // Keep the background daily subscription in place after the foreground callback ends.
                if (daily) Thread { runCatching { WearDailyHealthStore.register(context) } }.start()
            }
        }
    }
    return if (enabled && allowed) pulse else null
}
