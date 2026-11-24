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
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

internal data class WearDailyHealth(val steps: Long? = null, val calories: Double? = null, val distanceMeters: Double? = null, val sleepIntervalMinutes: Long? = null)

internal fun wearDailyStepPercent(steps: Long?, goal: Int?): Int? =
    if (steps == null || steps < 0 || goal == null || goal <= 0) null
    else (steps.toDouble() * 100 / goal).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()

internal fun wearDailyAccept(previousAt: Long, sampleAt: Long, now: Long, zone: ZoneId): Boolean =
    sampleAt > 0 && sampleAt >= previousAt && sampleAt <= now &&
        Instant.ofEpochMilli(sampleAt).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

/** Only a completed, ordered sleep transition can produce an interval, never a daily total. */
internal fun wearObservedSleepMinutes(start: Long, end: Long): Long? =
    if (start > 0 && end > start && end - start <= 24 * 60 * 60 * 1000L) (end - start) / 60000L else null

internal object WearDailyHealthStore {
    private val registrationLock = Any()
    fun prefs(context: Context) = context.getSharedPreferences("wear_daily_health_367", Context.MODE_PRIVATE)
    fun permitted(context: Context) = context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    @Synchronized fun enable(context: Context, enabled: Boolean) {
        val edit = prefs(context).edit()
        if (!enabled) edit.clear()
        if (enabled && !enabled(context)) edit.putLong("enabledAt", System.currentTimeMillis())
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
        return WearDailyHealth(value("steps")?.toLong(), value("calories"), value("distance"), value("sleep")?.toLong())
    }

    @Synchronized fun activity(context: Context, state: UserActivityState, at: Long, now: Long) {
        if (!enabled(context) || !permitted(context)) return
        val prefs = prefs(context)
        if (at < prefs.getLong("enabledAt", now) || at <= prefs.getLong("activityAt", 0) || at > now) return
        val boot = now - SystemClock.elapsedRealtime()
        val start = prefs.getLong("sleepStart", 0)
        val sameBoot = kotlin.math.abs(boot - prefs.getLong("sleepBoot", 0)) < 60000L
        val edit = prefs.edit().putLong("activityAt", at)
        when (state) {
            UserActivityState.USER_ACTIVITY_ASLEEP -> {
                if (start == 0L || !sameBoot) edit.putLong("sleepStart", at).putLong("sleepBoot", boot)
            }
            UserActivityState.USER_ACTIVITY_PASSIVE, UserActivityState.USER_ACTIVITY_EXERCISE -> {
                if (sameBoot) wearObservedSleepMinutes(start, at)?.let { write(context, "sleep", it.toDouble(), at, now) }
                edit.remove("sleepStart").remove("sleepBoot")
            }
            else -> edit.remove("sleepStart").remove("sleepBoot")
        }
        edit.apply()
    }

    /** Called off the main thread. Separate from the active ExerciseClient and its permissions. */
    fun register(context: Context): Boolean = synchronized(registrationLock) {
        val client = HealthServices.getClient(context).passiveMonitoringClient
        if (!enabled(context) || !permitted(context)) {
            client.clearPassiveListenerServiceAsync().get(20, TimeUnit.SECONDS)
            return false
        }
        val capabilities = client.getCapabilitiesAsync().get(20, TimeUnit.SECONDS)
        val types = setOf(DataType.STEPS_DAILY, DataType.CALORIES_DAILY, DataType.DISTANCE_DAILY)
            .filter { it in capabilities.supportedDataTypesPassiveMonitoring }.toSet()
        val sleepSupported = UserActivityState.USER_ACTIVITY_ASLEEP in capabilities.supportedUserActivityStates
        if (types.isEmpty() && !sleepSupported) return false
        if (!enabled(context) || !permitted(context)) return false
        client.setPassiveListenerServiceAsync(WearDailyHealthService::class.java,
            PassiveListenerConfig.builder().setDataTypes(types).setShouldUserActivityInfoBeRequested(sleepSupported).build())
            .get(20, TimeUnit.SECONDS)
        // Ask for buffered totals once when opening, not on every UI tick.
        runCatching { client.flushAsync().get(10, TimeUnit.SECONDS) }
        true
    }
}

/** Receives watch-originated daily totals even outside a workout. No phone upload or exercise mutation. */
class WearDailyHealthService : PassiveListenerService() {
    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        WearDailyHealthStore.activity(this, info.userActivityState, info.stateChangeTime.toEpochMilli(), System.currentTimeMillis())
    }

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
