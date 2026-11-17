package com.repforge.app

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Retries the bounded durable inbox through the existing dispatchers, without a second workout engine. */
internal object WearPendingCommandRecovery {
    private const val IDENTITY = "_phonePendingIdentity255"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val drainMutex = Mutex()
    private var scheduled = false
    private var retryAttempt = 0

    internal enum class Admission { READY, WAIT, STALE }
    internal class EditorRejected(val result: JSONObject) : IllegalStateException(result.optString("reason", "Recording failed"))

    internal fun transient(result: JSONObject): Boolean = result.optString("reason") in setOf(
        "executionNotPrepared", "restoreInProgress", "controllerNotArmed", "timerStillActive",
        "timedResultsAwaitingReconciliation", "gpsMeasurementStillActive", "setStopwatchStillActive")

    /** Read only committed journal proof; legacy adaptation uses the original wire fingerprint,
     * so this lookup needs no live frame, generation, revision, or newly inferred measurements. */
    internal fun terminalReceiptStatus(context: Context, command: JSONObject): String {
        val journalCommand = when {
            PhoneWorkoutExecution.compatible(command) -> command
            command.optString("action") == "completeSet" &&
                listOf("workoutId", "exerciseId", "setId").all { command.optString(it).isNotEmpty() } ->
                JSONObject(command.toString()).put("action", "completeMeasuredSet")
                    .put("legacyBilateral250", JSONObject(command.toString()))
            else -> null
        }
        val proof = journalCommand?.let { PhoneWorkoutExecution.store(context).receipt(it) }
        return when (proof?.optString("status")) {
            "duplicate" -> "applied"
            "rejected" -> "error"
            else -> "superseded"
        }
    }

    private fun supported(command: JSONObject): Boolean = PhoneWorkoutExecution.compatible(command) ||
        command.optString("action") in setOf("completeSet", "updateSet", "automaticControl")

    /** Capture once, at ingress, never infer a new generation when replaying an old legacy command. */
    fun capture(context: Context, command: JSONObject, incoming: Boolean = false) {
        if (!supported(command)) return
        if (incoming) command.remove(IDENTITY) // Local metadata is never accepted from the wire.
        if (command.has(IDENTITY)) return
        val pending = WearCommandQueueStore.pending(context)
        for (i in 0 until pending.length()) {
            val original = pending.optJSONObject(i) ?: continue
            if (original.optString("commandId") == command.optString("commandId")) {
                // A watch resend is still the same old request; do not rebind its legacy generation.
                command.put(IDENTITY, JSONObject((original.optJSONObject(IDENTITY) ?: JSONObject()).toString()))
                return
            }
        }
        val identity = JSONObject()
        ActiveWorkoutStore.current(context)?.let { frame ->
            for (key in listOf("workoutId", "generation", "pauseRevision", "ownerEpoch193", "timerOwner"))
                if (frame.has(key)) identity.put(key, frame.get(key))
        }
        command.put(IDENTITY, identity)
    }

    /** Called on Main while holding the same dataset lease as the actual mutation. */
    internal fun admission(context: Context, command: JSONObject): Admission {
        val store = PhoneWorkoutExecution.store(context)
        // An unfinished replacement is fail-closed, even if it has temporarily removed the frame.
        if (NativeDataReplacement.blocked(context) || store.restoreBlocked() || store.commandsSuspended()) return Admission.WAIT
        if (!PhoneOwnership193.commandAllowed(context, command)) return Admission.STALE
        val current = ActiveWorkoutStore.current(context) ?: return Admission.STALE
        val frame = if (current.optInt("nativeEditorProjection") == 1 && current.optString("timerOwner") == "phone")
            store.snapshot() ?: return Admission.STALE else current
        if (!current.optBoolean("active") || !frame.optBoolean("active") ||
            current.optBoolean("finalizationPending") || frame.optBoolean("finalizationPending") ||
            current.optString("workoutId") != command.optString("workoutId") ||
            frame.optString("workoutId") != command.optString("workoutId")) return Admission.STALE
        val identity = command.optJSONObject(IDENTITY)
        if (identity?.has("workoutId") == true && identity.optString("workoutId") != command.optString("workoutId")) return Admission.STALE
        for (key in listOf("generation", "pauseRevision", "ownerEpoch193")) {
            val expected = if (command.has(key)) command.optLong(key) else identity?.takeIf { it.has(key) }?.optLong(key)
            // A legacy command with no generation proof cannot be rebound to the current dataset.
            if (key == "generation" && (expected == null || expected <= 0L)) return Admission.STALE
            if (expected != null && (expected != frame.optLong(key) || expected != current.optLong(key))) return Admission.STALE
        }
        if (identity != null && identity.has("timerOwner") && identity.optString("timerOwner") != frame.optString("timerOwner")) return Admission.STALE
        if (frame.optBoolean("workoutPaused") || current.optBoolean("workoutPaused") ||
            com.repforge.shared.WorkoutOwnership193.pending(frame)) return Admission.WAIT
        if (command.optString("action") in setOf("updateSet", "completeSet") && !PhoneWorkoutExecution.compatible(command)) {
            val exercise = frame.optJSONArray("exercises")?.optJSONObject(command.optInt("exerciseIndex", -1)) ?: return Admission.STALE
            val set = exercise.optJSONArray("sets")?.optJSONObject(command.optInt("setIndex", -1)) ?: return Admission.STALE
            if (exercise.optString("id") != command.optString("exerciseId") || set.optString("setId") != command.optString("setId")) return Admission.STALE
            if (command.optString("action") == "updateSet" &&
                (command.optString("field") !in setOf("weight", "reps", "leftSeconds", "rightSeconds") ||
                    set.optBoolean("done") && command.optLong("sequence") > maxOf(frame.optLong("watchSequence"), frame.optLong("localSequence"))))
                return Admission.STALE
            if (command.optString("action") == "updateSet" && command.has("executionRevision") &&
                command.optLong("sequence") > maxOf(frame.optLong("watchSequence"), frame.optLong("localSequence")) &&
                command.optLong("executionRevision") != frame.optLong("executionRevision") &&
                !PhoneWorkoutRecording.matchesWatchEditChain(frame, command)) return Admission.STALE
        }
        if (command.optString("action") == "automaticControl" && !PhoneWorkoutExecution.compatible(command)) {
            val timer = IntervalTimerService.snapshot(context)
            val automation = command.optJSONObject("automation") ?: return Admission.STALE
            if (command.optString("value") !in setOf("pause", "resume", "skipRest")) return Admission.STALE
            // A replaced/finished timer is terminal. An interrupted timer with the same key awaits explicit local recovery.
            if (timer.optString("key").isNotEmpty() && timer.optString("key") != automation.optString("key")) return Admission.STALE
            if (timer.optBoolean("stopped") || timer.optString("phase") == "complete") return Admission.STALE
        }
        return Admission.READY
    }

    /** Coalesced retries use backoff, no wake lock, and stop when the supported durable inbox is empty. */
    fun schedule(context: Context) {
        if (!hasPending(context)) return
        val waitMillis = synchronized(this) {
            if (scheduled) return
            scheduled = true
            when (retryAttempt++) { 0 -> 1_000L; 1 -> 5_000L; 2 -> 15_000L; else -> 60_000L }
        }
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            scope.launch {
                try { drain(app) }
                finally {
                    val remaining = hasPending(app)
                    synchronized(this@WearPendingCommandRecovery) {
                        scheduled = false
                        if (!remaining) retryAttempt = 0
                    }
                    schedule(app)
                }
            }
        }, waitMillis)
    }

    private fun hasPending(context: Context): Boolean {
        val pending = WearCommandQueueStore.pending(context)
        return (0 until pending.length()).any { pending.optJSONObject(it)?.let(::supported) == true }
    }

    /** One serial pass over at most the existing 64 persisted entries, also used by lifecycle regression tests. */
    internal suspend fun drain(context: Context) = drainMutex.withLock {
        val pending = WearCommandQueueStore.pending(context)
        for (i in 0 until pending.length()) {
            val command = pending.optJSONObject(i) ?: continue
            if (!supported(command)) continue
            val uri = command.optString("_uri").takeIf { it.isNotBlank() }?.let(Uri::parse)
            try {
                NativeDataReplacement.mutate<Unit>(context) {
                    val ready = withContext(Dispatchers.Main.immediate) {
                        if (WearCommandQueueStore.seen(context, command.optString("commandId"))) {
                            WearCommandReceipts.replay(context, command, uri)
                            false
                        } else when (admission(context, command)) {
                            Admission.WAIT -> false
                            Admission.STALE -> {
                                // A crash after the engine transaction but before inbox ACK must still
                                // acknowledge that durable application, even if the workout has since closed.
                                WearCommandReceipts.complete(context, command, terminalReceiptStatus(context, command), uri)
                                false
                            }
                            Admission.READY -> true
                        }
                    }
                    if (ready) {
                        if (PhoneWorkoutExecution.compatible(command)) withContext(Dispatchers.Main.immediate) {
                            val result = WearCommandListenerService.dispatchNativeCommand(context, command, uri)
                            if (result.optString("status") == "rejected" && !transient(result))
                                WearCommandReceipts.complete(context, command, "error", uri)
                        } else if (command.optString("action") == "automaticControl") withContext(Dispatchers.Main.immediate) {
                            WearCommandListenerService.dispatchAutomaticCommand(context, command, uri)
                        } else WearSyncRepository.processIncomingWatchCommand(context, command, uri)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The exact command and original URI remain durable; transport or storage failure is not success.
            }
        }
    }
}
