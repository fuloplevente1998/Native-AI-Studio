package com.repforge.app.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

internal data class WearDailyHealth(val steps: Long? = null, val calories: Double? = null, val distanceMeters: Double? = null)

internal fun wearDailyStepPercent(steps: Long?, goal: Int?): Int? =
    if (steps == null || steps < 0 || goal == null || goal <= 0) null
    else (steps.toDouble() * 100 / goal).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()

internal fun wearDailyAccept(previousAt: Long, sampleAt: Long, now: Long, zone: ZoneId): Boolean =
    sampleAt > 0 && sampleAt >= previousAt && sampleAt <= now &&
        Instant.ofEpochMilli(sampleAt).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

internal object WearDailyHealthStore {
    fun prefs(context: Context) = context.getSharedPreferences("wear_daily_health_367", Context.MODE_PRIVATE)
    fun permitted(context: Context) = context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    @Synchronized fun enable(context: Context, enabled: Boolean) {
        val edit = prefs(context).edit()
        if (!enabled) edit.clear()
        edit.putBoolean("enabled", enabled).commit()
    }
    @Synchronized fun write(context: Context, key: String, value: Double, at: Long, now: Long = System.currentTimeMillis()) {
        if (!enabled(context) || !permitted(context) || !value.isFinite() || value < 0) return
        val prefs = prefs(context)
        if (!wearDailyAccept(prefs.getLong(key + "At", 0), at, now, ZoneId.systemDefault())) return
        // Daily metrics are cumulative snapshots, not increments. Never add batches.
        prefs.edit().putString(key, value.toString()).putLong(key + "At", at).apply()
    }
    fun read(context: Context, now: Long = System.currentTimeMillis()): WearDailyHealth {
        if (!enabled(context) || !permitted(context)) return WearDailyHealth()
        val prefs = prefs(context)
        fun value(key: String): Double? = if (wearDailyAccept(0, prefs.getLong(key + "At", 0), now, ZoneId.systemDefault()))
            prefs.getString(key, null)?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } else null
        return WearDailyHealth(value("steps")?.toLong(), value("calories"), value("distance"))
    }

    /** Called off the main thread. Separate from the active ExerciseClient and its permissions. */
    fun register(context: Context): Boolean {
        val client = HealthServices.getClient(context).passiveMonitoringClient
        if (!enabled(context) || !permitted(context)) {
            client.clearPassiveListenerServiceAsync().get(20, TimeUnit.SECONDS)
            return false
        }
        val supported = client.getCapabilitiesAsync().get(20, TimeUnit.SECONDS).supportedDataTypesPassiveMonitoring
        val types = setOf(DataType.STEPS_DAILY, DataType.CALORIES_DAILY, DataType.DISTANCE_DAILY).filter { it in supported }.toSet()
        if (types.isEmpty()) return false
        client.setPassiveListenerServiceAsync(WearDailyHealthService::class.java,
            PassiveListenerConfig.builder().setDataTypes(types).build()).get(20, TimeUnit.SECONDS)
        return true
    }
}

/** Receives watch-originated daily totals even outside a workout. No phone upload or exercise mutation. */
class WearDailyHealthService : PassiveListenerService() {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        val now = System.currentTimeMillis()
        val boot = Instant.ofEpochMilli(now - SystemClock.elapsedRealtime())
        dataPoints.getData(DataType.STEPS_DAILY).forEach {
            WearDailyHealthStore.write(this, "steps", it.value.toDouble(), it.getEndInstant(boot).toEpochMilli(), now)
        }
        dataPoints.getData(DataType.CALORIES_DAILY).forEach {
            WearDailyHealthStore.write(this, "calories", it.value, it.getEndInstant(boot).toEpochMilli(), now)
        }
        dataPoints.getData(DataType.DISTANCE_DAILY).forEach {
            WearDailyHealthStore.write(this, "distance", it.value, it.getEndInstant(boot).toEpochMilli(), now)
        }
    }
}
