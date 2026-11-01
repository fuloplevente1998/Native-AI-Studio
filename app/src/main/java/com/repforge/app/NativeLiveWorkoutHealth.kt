package com.repforge.app

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.repforge.shared.WorkoutOwnership193
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/** Read-only preview: no timer, owner, sensor, workout-frame or history writes. */
internal object NativeLiveWorkoutHealth {
    fun window(frame: JSONObject, end: Instant): JSONObject {
        check(frame.optBoolean("active") && !frame.optBoolean("finalizationPending") && !WorkoutOwnership193.pending(frame))
        check(frame.has("activeIntervals") || frame.optLong("pauseRevision") == 0L) { "Nincs igazolt aktív időablak." }
        return JSONObject().put("started", frame.getString("started")).put("finished", end.toString()).also {
            frame.optJSONArray("activeIntervals")?.let { intervals -> it.put("activeIntervals", intervals) }
        }
    }
    fun sameSession(expected: JSONObject, current: JSONObject?): Boolean = current != null && current.optBoolean("active") &&
        !current.optBoolean("finalizationPending") && !WorkoutOwnership193.pending(current) &&
        current.optString("workoutId") == expected.optString("workoutId") && current.optLong("generation") == expected.optLong("generation") &&
        current.optLong("ownerEpoch193") == expected.optLong("ownerEpoch193") && current.optLong("pauseRevision") == expected.optLong("pauseRevision") &&
        current.optJSONArray("activeIntervals")?.toString() == expected.optJSONArray("activeIntervals")?.toString()

    suspend fun read(context: Context, activity: Activity, provider: String): JSONObject = withContext(Dispatchers.Main.immediate) {
        NativeWorkoutHealthSource.requireProvider(provider)
        val owner = activity as? LifecycleOwner ?: error("Látható edzésképernyő szükséges.")
        val foreground = AtomicBoolean(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> foreground.set(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        owner.lifecycle.addObserver(observer)
        try {
            val expected = checkNotNull(ActiveWorkoutStore.current(context)) { "Nincs aktív edzés." }
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val query = window(expected, Instant.now())
            val guard = {
                check(foreground.get()) { "Az egészségadat-lekérés csak előtérben folytatható." }
                ticket.requireCurrent(context)
                check(NativeHealthBackupGate.available() && sameSession(expected, ActiveWorkoutStore.current(context))) {
                    "Az edzés vagy az aktív időablak közben megváltozott; kérd le újra."
                }
                Unit
            }
            guard()
            if (provider == HealthJournalStore.HC) check(Build.VERSION.SDK_INT >= 34 && HealthConnectRepository.isSupported(context)) {
                "Health Connect ezen az eszközön nem érhető el."
            }
            if (provider == HealthJournalStore.HC && !HealthConnectRepository.anyReadAllowed(context))
                throw SecurityException("Health Connect olvasási engedély szükséges.")
            val result = NativeWorkoutHealthSource.read(query, provider,
                healthConnect = { w -> HealthConnectRepository.readTrainingWindowSuspend(context, w.start.toString(), w.end.toString(), guard) },
                samsung = { w -> SamsungHealthRepository.readTrainingWindowSuspend(context, w.start.toString(), w.end.toString(), guard) })
            guard()
            result.put("readAt", Instant.now().toString())
        } finally { owner.lifecycle.removeObserver(observer) }
    }
}
