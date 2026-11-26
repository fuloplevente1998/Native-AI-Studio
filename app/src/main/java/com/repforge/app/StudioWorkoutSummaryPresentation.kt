package com.repforge.app

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/** Read-only session summary; no daily totals, estimates or second workout clock. */
internal object StudioWorkoutSummaryPresentation {
    data class HealthSnapshot internal constructor(val provider: String, internal val frameJson: String,
        internal val workoutJson: String, internal val documentJson: String)

    fun current(frame: JSONObject?, workoutId: String, generation: Long): JSONObject? = frame?.takeIf {
        it.optString("workoutId") == workoutId &&
            (generation <= 0 || it.optLong("generation") == generation)
    }

    fun selectedProvider(context: Context): String = NativeWorkoutHealthSource.requireProvider(
        HealthJournalStore.get(context).preferences().optString("provider", HealthJournalStore.HC))

    /** A response belongs to the captured session and exact canonical active windows. */
    internal fun healthSnapshot(frame: JSONObject, provider: String, response: JSONObject, end: Instant): HealthSnapshot {
        NativeWorkoutHealthSource.requireProvider(provider)
        check(response.optString("provider", provider) == provider) { "Health provider changed" }
        check(response.optString("workoutId").let { it.isBlank() || it == frame.optString("workoutId") }) { "Health workout changed" }
        val query = NativeLiveWorkoutHealth.window(frame, end)
            .put("id", frame.optString("id").ifBlank { frame.getString("workoutId") })
            .put("syncId", frame.getString("workoutId")).put("workoutId", frame.getString("workoutId"))
            .put("nativeHealthWindowStale", frame.optBoolean("nativeHealthWindowStale"))
        val health = JSONObject(response.toString()).put("workoutId", frame.getString("workoutId"))
        // Canonical projection validates the outer and each actual measurement window.
        val document = NativeWorkoutHealthSource.attach(null, provider, health)
        check(!NativeWorkoutHealthSummary.project(query, null, document).rejectedEvidence) { "Health window mismatch" }
        return HealthSnapshot(provider, frame.toString(), query.toString(), document.toString())
    }

    /** Same foreground SDK adapters as the journal, without persisting or altering the workout. */
    suspend fun readHealth(context: Context, activity: Activity, provider: String,
        expectedFrame: JSONObject, end: Instant): HealthSnapshot =
        readHealthWithReader(context, activity, provider, expectedFrame, end, null)

    /** Adapter seam retains every production admission and completion fence. */
    internal suspend fun readHealthWithReader(context: Context, activity: Activity, provider: String,
        expectedFrame: JSONObject, end: Instant,
        readWindow: (suspend (NativeWorkoutActiveWindows.Window) -> JSONObject)?): HealthSnapshot = withContext(Dispatchers.Main.immediate) {
        NativeWorkoutHealthSource.requireProvider(provider)
        val expected = JSONObject(expectedFrame.toString())
        val query = NativeLiveWorkoutHealth.window(expected, end)
        val owner = activity as? LifecycleOwner ?: error("Foreground lifecycle required")
        val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ ->
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) foreground.set(false)
        }
        owner.lifecycle.addObserver(observer)
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val operation = currentCoroutineContext()
            val guard = {
                operation.ensureActive()
                ticket.requireCurrent(context)
                check(foreground.get() && NativeHealthBackupGate.available() && selectedProvider(context) == provider &&
                    NativeLiveWorkoutHealth.sameSession(expected, ActiveWorkoutStore.current(context))) {
                    "Workout, provider or foreground changed; refresh again"
                }
                Unit
            }
            guard()
            if (provider == HealthJournalStore.HC) {
                check(HealthConnectRepository.isSupported(context)) { "Health Connect unavailable" }
                if (HealthConnectRepository.currentlyGrantedReadKeys(context).isEmpty())
                    throw SecurityException("Health Connect read permission required")
            }
            val reader: suspend (NativeWorkoutActiveWindows.Window) -> JSONObject = { window ->
                guard()
                val result = if (readWindow != null) readWindow(window)
                    else if (provider == HealthJournalStore.HC && Build.VERSION.SDK_INT >= 34)
                        HealthConnectRepository.readTrainingWindowSuspend(
                            context, window.start.toString(), window.end.toString(), guard)
                    else if (provider == HealthJournalStore.HC)
                        HealthConnectLegacyBridge.readTrainingWindow(context, window.start, window.end, guard)
                    else SamsungHealthRepository.readTrainingWindowSuspend(context, window.start.toString(), window.end.toString(), guard)
                guard()
                result
            }
            val response = NativeWorkoutHealthSource.read(query, provider, healthConnect = reader, samsung = reader)
            guard()
            healthSnapshot(expected, provider, response, end)
        } finally { owner.lifecycle.removeObserver(observer) }
    }

    fun calories(frame: JSONObject?, live: JSONObject?, now: Instant, provider: String = HealthJournalStore.HC,
        health: HealthSnapshot? = null): Double? = energy(frame, live, now, provider, health)
        ?.values?.values?.singleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

    /** The selected phone provider and admissible Wear measurements use the journal's source policy. */
    fun energy(frame: JSONObject?, live: JSONObject?, now: Instant, provider: String = HealthJournalStore.HC,
        health: HealthSnapshot? = null): NativeWorkoutHealthSummary.Metric? =
        projection(frame, live, now, provider, health)?.energy

    /** Same typed projection as saved workout detail, scoped to the chosen provider and Wear. */
    fun projection(frame: JSONObject?, live: JSONObject?, now: Instant, provider: String = HealthJournalStore.HC,
        health: HealthSnapshot? = null, expectedNode: String? = null): NativeWorkoutHealthSummary.Result? {
        frame ?: return null
        NativeWorkoutHealthSource.requireProvider(provider)
        val id = frame.optString("workoutId").takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val fresh = health?.takeIf {
                val expected = JSONObject(it.frameJson)
                it.provider == provider && NativeLiveWorkoutHealth.sameSession(expected, frame) &&
                    expected.optString("finished") == frame.optString("finished") &&
                    expected.optBoolean("nativeHealthWindowStale") == frame.optBoolean("nativeHealthWindowStale")
            }
            val projection = fresh?.let { JSONObject(it.workoutJson) } ?: JSONObject(frame.toString())
                .put("id", frame.optString("id").ifBlank { id }).put("syncId", id)
            if (projection.optString("finished").isBlank()) projection.put("finished", now.toString())
            val liveWear = WearWorkoutEnergyEvidence.live(frame,live,expectedNode,now)
            val savedWear = NativeSavedWearHealth.fromRaw(frame)?.takeIf {
                it.optString("workoutId").let { wearId -> wearId.isBlank() || wearId == id ||
                    wearId in listOf(frame.optString("id"), frame.optString("syncId"), frame.optString("workoutKey")) }
            }
            // A pulse-only or rejected live packet must not hide a saved measured energy value.
            val admittedLive = liveWear?.takeIf {
                !it.optBoolean("livePreview2815") ||
                    WearWorkoutEnergyEvidence.accept(savedWear,it,setOf(id,frame.optString("id"),frame.optString("syncId"),frame.optString("workoutKey")).filter(String::isNotBlank).toSet())
            }
            val wear = listOfNotNull(admittedLive, savedWear).firstOrNull {
                NativeWorkoutHealthSummary.project(projection, it, null).energy.values.isNotEmpty()
            }
            val document = fresh?.let { JSONObject(it.documentJson) } ?: frame.optJSONObject("health240")
            val selected = NativeWorkoutHealthSource.snapshot(document, provider)?.let {
                JSONObject().put("providers", JSONObject().put(provider, it))
            }
            NativeWorkoutHealthSummary.project(projection, wear, selected)
        }.getOrNull()
    }
}
