package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** Watch-owned controls use the existing snapshot and durable command outbox. No second timer. */
internal object WearWatchControls367 {
    private fun allowed(current: WearWorkout, expected: WearPauseTarget367): Boolean =
        current.timerOwner == "watch" && current.controlIntent367 == null &&
            current.resumeIntent193 == null && wearPauseTarget367(current) == expected

    private fun intent(next: WearWorkout, target: WearPauseTarget367, kind: String, at: Long,
                       expectedDeadline: Long = 0, delta: Int = 0,
                       exerciseId: String = "", setId: String = ""): WearWorkout {
        val command = JSONObject().put("schema", 1).put("commandId", UUID.randomUUID().toString())
            .put("action", "watchControl367").put("kind", kind).put("workoutId", next.workoutId)
            .put("ownerEpoch193", target.ownerEpoch).put("generation", target.generation)
            .put("pauseRevision", target.pauseRevision).put("baseRevision", target.baseRevision)
            .put("watchSequence", next.localSequence).put("createdAt", at)
            .put("expectedDeadline", expectedDeadline).put("delta", delta)
            .put("exerciseId", exerciseId).put("setId", setId)
            .put("finalSnapshot", WorkoutSnapshotStore.encode(next))
        return next.copy(controlIntent367 = command.toString())
    }

    fun adjust(context: Context, target: WearRestTarget367, delta: Int): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return null
            val now = System.currentTimeMillis()
            if (!allowed(current, target.workout) || wearRestTarget367(current, now) != target) return null
            val deadline = wearRestDeadline367(current.restEndAt, now, delta) ?: return null
            val next = intent(current.copy(restEndAt = deadline), target.workout, "rest", now,
                target.expectedDeadline, delta, target.exerciseId, target.setId)
            WorkoutSnapshotStore.saveWorkout(context, next)
            recover(context)
            return WorkoutSnapshotStore.load(context)
        }}
    }

    fun pause(context: Context, target: WearPauseTarget367): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return null
            if (!allowed(current, target)) return null
            WearOwnership193.freeze(context, current.workoutId) { frozen ->
                intent(frozen, target, "pause", System.currentTimeMillis())
            } ?: return null
            recover(context)
            return WorkoutSnapshotStore.load(context)
        }}
    }

    /** Persist the mutation + command before enqueue; replay the exact same command after process death.
     * The outbox supplies sequence and receipt deduplication; do not regenerate a relative adjustment. */
    fun recover(context: Context) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return
            val raw = current.controlIntent367 ?: return
            val command = runCatching { JSONObject(raw) }.getOrNull() ?: return
            val next = command.optJSONObject("finalSnapshot") ?: return
            if (current.timerOwner != "watch" || current.workoutId != command.optString("workoutId") ||
                current.ownerEpoch193 != command.optLong("ownerEpoch193") ||
                current.pauseRevision != next.optLong("pauseRevision")) return
            val seq = WearCommandOutbox.enqueue(context, command)
            if (seq > 0) WorkoutSnapshotStore.saveWorkout(context, current.copy(
                localSequence = maxOf(current.localSequence, seq), controlIntent367 = null))
        }}
    }
}

internal fun wearRestDeadline367(deadline: Long, now: Long, delta: Int): Long? {
    if (now <= 0 || deadline <= now || deadline - now > WEAR_REST_MAX367 * 1000L) return null
    val remaining = ((deadline - now + 999L) / 1000L).toInt()
    val adjusted = wearRestAdjustedSeconds367(remaining, delta) ?: return null
    return if (adjusted == 0) 0L else now + adjusted * 1000L
}
