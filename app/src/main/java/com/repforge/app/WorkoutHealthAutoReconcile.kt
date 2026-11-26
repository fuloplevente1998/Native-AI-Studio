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
        val metrics = NativeWorkoutHealth.metrics(previous, false)
        if (metrics["activeCalories"] == null || previous.optBoolean("partial")) return true
        if (previous.optString("provider") == HealthJournalStore.HC &&
            listOf("activeCalories","totalCalories").any { WorkoutEnergyCoverage.status(previous,it) != WorkoutEnergyCoverage.Status.FULL_RAW })
            return true
        val synced = runCatching { Instant.parse(previous.getString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
        // Missing total energy may be published later even if active energy already arrived.
        // After the initial settle period it remains retryable within the existing lookback.
        return nowMs >= finishedMs + SETTLE_MS &&
            (synced < finishedMs + SETTLE_MS || metrics["totalCalories"] == null)
    }

    private val energyKeys = setOf("activeCalories", "totalCalories", "workoutCalories")
    private fun measured(data: JSONObject) = NativeWorkoutHealth.metrics(data, false)
        .filterKeys { it != "exerciseMinutes" && it != "exerciseSessionCount" }

    private fun failed(data: JSONObject, keys: Set<String>): Boolean {
        val fields = data.optJSONArray("failedFields")
        return (0 until (fields?.length() ?: 0)).any { fields?.optString(it) in keys } ||
            keys.any { data.optJSONObject("errors")?.optString(it).orEmpty().isNotBlank() }
    }

    private fun windows(data: JSONObject?): Map<Pair<Instant, Instant>, JSONObject>? {
        val rows = data?.optJSONArray("measurementWindows") ?: return null
        val result = linkedMapOf<Pair<Instant, Instant>, JSONObject>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val window = Instant.parse(row.getString("start")) to Instant.parse(row.getString("end"))
            require(window.second > window.first && result.put(window, row) == null)
        }
        return result
    }

    fun accept(previous: JSONObject?, next: JSONObject): Boolean = runCatching {
        val current = measured(next)
        val prior = previous?.let(::measured).orEmpty()
        val oldWindows = windows(previous)
        val newWindows = windows(next)
        if (current.isEmpty() && newWindows.orEmpty().values.none { measured(it).isNotEmpty() }) return false
        // A refresh must keep every existing measured field, including measurements that
        // exist in only one active interval and therefore have no full-workout aggregate.
        if (!current.keys.containsAll(prior.keys) || failed(next, prior.keys)) return false
        var gainsEnergy = (current.keys - prior.keys).any { it in energyKeys }
        for ((window, oldRow) in oldWindows.orEmpty()) {
            val oldFields = measured(oldRow).keys
            if (oldFields.isEmpty()) continue
            val newRow = newWindows?.get(window) ?: return false
            val newFields = measured(newRow).keys
            if (!newFields.containsAll(oldFields) || failed(newRow, oldFields)) return false
            for (key in oldFields.intersect(energyKeys)) {
                val oldMs = WorkoutEnergyCoverage.knownMillis(oldRow,key)
                val newMs = WorkoutEnergyCoverage.knownMillis(newRow,key)
                if (oldMs != null && (newMs == null || newMs < oldMs)) return false
            }
            gainsEnergy = gainsEnergy || (newFields - oldFields).any { it in energyKeys }
        }
        if (newWindows != null) for ((window, row) in newWindows) {
            if (oldWindows?.containsKey(window) != true && measured(row).keys.any { it in energyKeys }) gainsEnergy = true
        }
        // A partial result may add energy to an earlier complete pulse-only read, but
        // only when it retains all earlier evidence. Never splice different snapshots.
        val hadEvidence = prior.isNotEmpty() || oldWindows.orEmpty().values.any { measured(it).isNotEmpty() }
        if (hadEvidence && previous?.optBoolean("partial") != true && next.optBoolean("partial") && !gainsEnergy)
            return false
        true
    }.getOrDefault(false)
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

    /** Run the bounded workout read before daily sync. Its independent failure must not
     * suppress daily publication; cancellation must still stop the original SDK flight. */
    suspend fun attemptBeforeDaily(enabled: Boolean, reconcile: suspend () -> Unit) {
        if (!enabled) return
        try { reconcile() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* The next eligible foreground sync can retry. */ }
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
                ?.takeIf { NativeWorkoutHealth.matchesWindow(it, latest) }
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
