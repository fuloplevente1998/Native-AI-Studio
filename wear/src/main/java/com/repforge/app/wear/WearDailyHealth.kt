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
import com.repforge.shared.SleepNightTotal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.json.JSONObject

internal data class WearDailyHealth(val steps: Long? = null, val calories: Double? = null, val distanceMeters: Double? = null, val sleepIntervalMinutes: Long? = null,
    /** Time of the cumulative daily step total; lets the UI tell an old total from a current one. */
    val stepsAt: Long? = null)

/** 2808: a same-day total older than this is shown as earlier data, never as the current progress. */
internal const val WEAR_DAILY_STEPS_FRESH_MS = 3L * 60L * 60L * 1000L
/** Cross-process clock rounding tolerance for Health Services interval ends. */
internal const val WEAR_DAILY_SKEW_MS = 2L * 60L * 1000L

internal fun wearDailyStepsFresh(stepsAt: Long?, now: Long): Boolean =
    stepsAt != null && stepsAt > 0 && now - stepsAt in 0L..WEAR_DAILY_STEPS_FRESH_MS

/** A sample end slightly ahead of this process clock is "now"; a real future timestamp is rejected. */
internal fun wearDailySampleTime(end: Long, now: Long): Long? =
    if (end <= 0L || end - now > WEAR_DAILY_SKEW_MS) null else minOf(end, now)

internal fun wearDailyStepPercent(steps: Long?, goal: Int?): Int? =
    if (steps == null || steps < 0 || goal == null || goal <= 0) null
    else (steps.toDouble() * 100 / goal).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()

internal fun wearDailyAccept(previousAt: Long, sampleAt: Long, now: Long, zone: ZoneId): Boolean =
    sampleAt > 0 && sampleAt >= previousAt && sampleAt <= now &&
        Instant.ofEpochMilli(sampleAt).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

/** Only a completed, ordered sleep transition can produce an interval, never a daily total. */
internal fun wearObservedSleepMinutes(start: Long, end: Long): Long? =
    if (start > 0 && end > start && end - start <= 24 * 60 * 60 * 1000L) (end - start) / 60000L else null

/** #367/9: aggregated sleep total of the phone's selected provider, for the local day it ends on. */
data class WatchPhoneSleepSummary(val date: String, val minutes: Long, val provider: String,
    /** Phone clock time the total was computed at (`computedAt`); null when the phone did not send it. */
    val computedAt: Long? = null) {
    companion object {
        fun parse(source: JSONObject?): WatchPhoneSleepSummary? {
            source ?: return null
            val date = source.optString("date").takeIf { runCatching { LocalDate.parse(it) }.isSuccess } ?: return null
            val minutes = source.optLong("minutes", -1L).takeIf { it in 1L..(24L * 60L) } ?: return null
            val provider = source.optString("provider").takeIf { it == "samsung_health" || it == "health_connect" } ?: return null
            val computedAt = source.optLong("computedAt", 0L).takeIf { it > 0L }
            return WatchPhoneSleepSummary(date, minutes, provider, computedAt)
        }
    }
}

/**
 * #367/9: the phone only publishes the home snapshot on events (app start, workout finish, edits),
 * not periodically, and a night can still be partial right after waking. A phone total older than
 * this stops overriding the watch's own measurement (same bound as the daily step total).
 */
internal const val WEAR_PHONE_SLEEP_FRESH_MS = WEAR_DAILY_STEPS_FRESH_MS
/** Phone/watch clock difference tolerated for a `computedAt` slightly ahead of the watch clock. */
internal const val WEAR_PHONE_SLEEP_SKEW_MS = 10L * 60L * 1000L

internal fun wearPhoneSleepFresh(phone: WatchPhoneSleepSummary, now: Long): Boolean {
    val computedAt = phone.computedAt ?: return false
    return now - computedAt in -WEAR_PHONE_SLEEP_SKEW_MS..WEAR_PHONE_SLEEP_FRESH_MS
}

internal enum class WearSleepOrigin { PHONE_SAMSUNG, PHONE_HEALTH_CONNECT, WATCH_OBSERVED }
internal data class WearSleepDisplay(val minutes: Long, val origin: WearSleepOrigin)

/**
 * The phone total (same source/window as the phone Today tile) wins for today while it is fresh
 * ([wearPhoneSleepFresh]); a stale total or one without `computedAt` may still be a partial night,
 * so the watch's own observed intervals of the same wake-up day (summed by the same shared union)
 * are used instead. A summary of another day is never shown as today's sleep; nothing → null
 * ("–"), never 0.
 */
internal fun wearSleepDisplay(phone: WatchPhoneSleepSummary?, watchMinutes: Long?, now: Long, zone: ZoneId): WearSleepDisplay? {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
    if (phone != null && phone.date == today && wearPhoneSleepFresh(phone, now)) return WearSleepDisplay(phone.minutes,
        if (phone.provider == "samsung_health") WearSleepOrigin.PHONE_SAMSUNG else WearSleepOrigin.PHONE_HEALTH_CONNECT)
    return watchMinutes?.takeIf { it > 0 }?.let { WearSleepDisplay(it, WearSleepOrigin.WATCH_OBSERVED) }
}

/**
 * Adds one completed watch-observed asleep interval to the spans of its wake-up day and returns
 * the new spans. Spans of another day are dropped (each night is summed on its own); duplicates
 * and overlaps stay harmless because the total is a union ([SleepNightTotal]).
 */
internal fun wearSleepSpansAfter(storedDay: String?, stored: List<Pair<Long, Long>>, start: Long, end: Long, zone: ZoneId): Pair<String, List<Pair<Long, Long>>> {
    val day = Instant.ofEpochMilli(end).atZone(zone).toLocalDate().toString()
    val kept = if (storedDay == day) stored else emptyList()
    return day to (kept + (start to end)).distinct().takeLast(48)
}

internal fun wearSleepSpansEncode(spans: List<Pair<Long, Long>>): String = spans.joinToString(",") { "${it.first}-${it.second}" }
internal fun wearSleepSpansDecode(raw: String?): List<Pair<Long, Long>> = raw.orEmpty().split(',').mapNotNull { part ->
    val bits = part.split('-')
    val a = bits.getOrNull(0)?.toLongOrNull(); val b = bits.getOrNull(1)?.toLongOrNull()
    if (bits.size == 2 && a != null && b != null && b > a) a to b else null
}

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
        val steps = value("steps")?.toLong()
        return WearDailyHealth(steps, value("calories"), value("distance"), value("sleep")?.toLong(),
            stepsAt = prefs.getLong("stepsAt", 0L).takeIf { steps != null && it > 0L })
    }

    /** Shared by the background service and the foreground callback: cumulative totals, never added. */
    fun accept(context: Context, dataPoints: DataPointContainer, now: Long = System.currentTimeMillis()) {
        val boot = Instant.ofEpochMilli(now - SystemClock.elapsedRealtime())
        dataPoints.getData(DataType.STEPS_DAILY).forEach { point ->
            wearDailySampleTime(point.getEndInstant(boot).toEpochMilli(), now)?.let { write(context, "steps", point.value.toDouble(), it, now) }
        }
        dataPoints.getData(DataType.CALORIES_DAILY).forEach { point ->
            wearDailySampleTime(point.getEndInstant(boot).toEpochMilli(), now)?.let { write(context, "calories", point.value, it, now) }
        }
        dataPoints.getData(DataType.DISTANCE_DAILY).forEach { point ->
            wearDailySampleTime(point.getEndInstant(boot).toEpochMilli(), now)?.let { write(context, "distance", point.value, it, now) }
        }
    }

    internal val dailyTypes: Set<DataType<*, *>> = setOf(DataType.STEPS_DAILY, DataType.CALORIES_DAILY, DataType.DISTANCE_DAILY)

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
                // #367/9: sum every observed interval of the night, not only the last one.
                if (sameBoot && wearObservedSleepMinutes(start, at) != null) {
                    val (day, spans) = wearSleepSpansAfter(prefs.getString("sleepSpansDay", null),
                        wearSleepSpansDecode(prefs.getString("sleepSpans", null)), start, at, ZoneId.systemDefault())
                    edit.putString("sleepSpansDay", day).putString("sleepSpans", wearSleepSpansEncode(spans))
                    SleepNightTotal.unionMinutes(spans)?.let { write(context, "sleep", it.toDouble(), at, now) }
                }
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
        val types = dailyTypes
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
        WearDailyHealthStore.accept(this, dataPoints)
    }
}

/**
 * 2808: Health Services passive registrations do not survive a reboot (and are not renewed by an
 * app update). Before, they were only renewed when the TrainPilot UI was resumed, so the watch
 * kept a same-day morning total (shown as 0% in the evening). Restore the user's opt-in here.
 */
class WearDailyHealthRestoreReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action !in setOf(android.content.Intent.ACTION_BOOT_COMPLETED, android.content.Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val app = context.applicationContext
        if (!WearDailyHealthStore.enabled(app) || !WearDailyHealthStore.permitted(app)) return
        val pending = goAsync()
        Thread {
            try { runCatching { WearDailyHealthStore.register(app) } } finally { pending.finish() }
        }.start()
    }
}
