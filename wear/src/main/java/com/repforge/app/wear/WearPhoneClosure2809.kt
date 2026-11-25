package com.repforge.app.wear

import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/**
 * 2809: phone closures the watch must apply even when the Data Layer skipped a state.
 *
 * The Data Layer keeps only the newest item per path, so a terminal snapshot of workout X can be
 * replaced by a later state (another workout, or its terminal) before the watch sees it. Before
 * 2809 an epoch-fenced phone mirror of X then stayed on the watch forever: every later snapshot
 * was "stale" for it, the phone could not discard it (it no longer had X), and each time the
 * watch app was opened (for example after an update) it looked like a workout started by itself.
 */
internal object WearPhoneClosure2809 {
    /**
     * A phone-owned mirror is superseded when the phone's canonical store (one monotonic revision
     * counter for all workouts) has moved past it to a state that is not this workout. The phone
     * holds exactly one current snapshot, so this workout is no longer active there. Watch-owned
     * workouts, pending handoffs and durable local control intents are never dropped this way.
     */
    fun mirrorSuperseded(current: WearWorkout, incoming: JSONObject): Boolean =
        current.workoutId.isNotBlank() && incoming.optString("workoutId") != current.workoutId &&
            current.timerOwner == "phone" && current.automation?.owner != "watch" &&
            current.revision > 0L && incoming.optLong("revision", 0L) > current.revision &&
            !WearOwnership193.pending(current) && current.controlIntent367 == null && current.resumeIntent193 == null

    /** The explicit, confirmed phone discard of exactly this workout (the phone tombstoned it). */
    fun phoneDiscard(current: WearWorkout?, terminal: JSONObject): Boolean =
        current != null && WorkoutOwnership193.phoneDiscardTargets(terminal, current.workoutId)

    /**
     * The watch's acknowledgement, sent through the durable outbox. The phone already holds the
     * tombstone, so it answers with the existing "discarded" receipt and never re-applies it.
     */
    fun acknowledgement(current: WearWorkout, commandId: String, now: Long): JSONObject = JSONObject()
        .put("schema", 1).put("commandId", commandId).put("workoutId", current.workoutId)
        .put("baseRevision", current.revision).put("watchSequence", current.localSequence)
        .put("action", "discardWorkout").put("confirmed", true)
        .put(WorkoutOwnership193.PHONE_DISCARD_2809, true)
        .put("finishedAt", java.time.Instant.ofEpochMilli(now).toString()).put("createdAt", now)
        .put("ownerEpoch193", current.ownerEpoch193)
        .put("finalSnapshot", WorkoutSnapshotStore.encode(current))
}

/**
 * 2809: automatic (non-explicit) recording start. A watch-owned workout is the watch's own state.
 * A phone-owned copy restored from disk after an update/reboot/process start must first be
 * confirmed by the phone's current Data Layer state, which may already have closed it.
 */
internal fun wearMayAutoRecord2809(workout: WearWorkout, phoneStateApplied: Boolean, explicitPhoneStart: Boolean): Boolean =
    workout.timerOwner != "phone" || phoneStateApplied || explicitPhoneStart
