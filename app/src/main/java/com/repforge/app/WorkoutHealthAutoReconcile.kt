package com.repforge.app

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

/** Auto reads are limited to recently saved native workouts, never an external import or
 * an entire historical journal. Saved evidence remains the durable retry criterion. */
internal object WorkoutHealthAutoPolicy {
    const val LOOKBACK_MS = 48L * 60 * 60 * 1000
    const val RETRY_MS = 15L * 60 * 1000
    const val SETTLE_MS = 15L * 60 * 1000

    fun due(previous: JSONObject?, finishedMs: Long, nowMs: Long): Boolean {
        if (finishedMs <= 0 || nowMs < finishedMs || nowMs - finishedMs > LOOKBACK_MS) return false
        if (previous == null) return true
        val active = NativeWorkoutHealth.metrics(previous, false)["activeCalories"]
        if (active == null || previous.optBoolean("partial")) return true
        val synced = runCatching { Instant.parse(previous.getString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
        // A successful immediate read gets one later read for delayed provider publication.
        return nowMs >= finishedMs + SETTLE_MS && synced < finishedMs + SETTLE_MS
    }

    fun accept(previous: JSONObject?, next: JSONObject): Boolean {
        val measured = NativeWorkoutHealth.metrics(next, false)
            .filterKeys { it != "exerciseMinutes" && it != "exerciseSessionCount" }
        if (measured.isEmpty()) return false
        val prior = previous?.let { NativeWorkoutHealth.metrics(it, false) }.orEmpty()
            .filterKeys { it != "exerciseMinutes" && it != "exerciseSessionCount" }
        // An empty/partial provider response must not erase a previously measured field.
        // Do not splice different read windows or manufacture aggregates.
        if (!measured.keys.containsAll(prior.keys)) return false
        val failed = next.optJSONArray("failedFields")
        if (failed != null && (0 until failed.length()).any { failed.optString(it) in prior.keys }) return false
        if (prior.isNotEmpty() && previous?.optBoolean("partial") != true && next.optBoolean("partial")) return false
        return true
    }
}

/** Called only inside the existing foreground HealthDashboard provider job (serialized).
 * No background work, permission prompts, extra sync loop or SDK calls under Room locks. */
internal object WorkoutHealthAutoReconcile {
    private val attempts = LinkedHashMap<String, Long>()
    @Synchronized private fun admit(key: String, elapsed: Long): Boolean {
        val last = attempts[key]
        if (last != null && elapsed >= last && elapsed - last < WorkoutHealthAutoPolicy.RETRY_MS) return false
        attempts[key] = elapsed
        while (attempts.size > 128) attempts.remove(attempts.keys.first())
        return true
    }

    suspend fun recent(context: Context, activity: Activity, providers: Set<String>, batchGuard: () -> Unit) {
        val now = System.currentTimeMillis()
        val db = TrainPilotDatabase.getInstance(context)
        val rows = withContext(Dispatchers.IO) {
            batchGuard()
            db.workoutHistoryDao().recentNativeWorkoutsForHealth(now - WorkoutHealthAutoPolicy.LOOKBACK_MS, now, 12)
        }
        val reader = NativeWorkoutHealth(context, db)
        var budget = 4
        for (row in rows) for (provider in listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC)) {
            if (budget == 0) return
            if (provider !in providers) continue
            val feature = if (provider == HealthJournalStore.HC) NativePrivacyControls.HEALTH_CONNECT else NativePrivacyControls.SAMSUNG_HEALTH
            val guard = {
                batchGuard()
                NativePrivacyControls.requireEnabled(context, feature)
                check(HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen", true))
                Unit
            }
            val latest = withContext(Dispatchers.IO) {
                batchGuard()
                db.workoutHistoryDao().getWorkoutWithDetailsById(row.id)?.workout
            } ?: continue
            if (latest.started != row.started || latest.finished != row.finished ||
                latest.activeIntervalsJson != row.activeIntervalsJson || latest.originDevice !in setOf("phone", "watch")) continue
            val snapshot = NativeWorkoutHealthSource.snapshot(latest.health240Json?.let { JSONObject(it) }, provider)
            if (!WorkoutHealthAutoPolicy.due(snapshot, latest.finishedEpochMs, now)) continue
            val permitted = withContext(Dispatchers.IO) {
                batchGuard()
                NativePrivacyControls.enabled(context, feature) && when (provider) {
                    HealthJournalStore.HC -> HealthConnectRepository.isSupported(context) &&
                        HealthConnectRepository.currentlyGrantedReadKeys(context).isNotEmpty()
                    else -> SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable() &&
                        runCatching { SamsungHealthRepository.readGrantedKeys(context, guard).isNotEmpty() }
                            .getOrElse { if (it is CancellationException) throw it else false }
                }
            }
            if (!permitted || !admit("${latest.id}|${latest.finished}|$provider", SystemClock.elapsedRealtime())) continue
            budget--
            NativeHealthForegroundAdmission.requireMainAdmission(context)
            reader.refresh(activity, latest, provider, automatic = true, requireAutoGuard = guard)
            // A provider exception is a Result failure, never a failed workout or a synthetic zero.
            // Check the original batch identity before proceeding to a different provider.
            withContext(Dispatchers.IO) { batchGuard() }
        }
    }
}
