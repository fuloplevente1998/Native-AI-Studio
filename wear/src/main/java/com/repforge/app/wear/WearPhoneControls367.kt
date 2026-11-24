package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** A phone-owned session is never mutated locally. Only a durable, fenced request is queued. */
internal object WearPhoneControls367 {
    fun adjust(context: Context, target: WearRestTarget367, delta: Int): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return null
            val now = System.currentTimeMillis()
            val live = wearRestTarget367(current, now) ?: return null
            if (current.timerOwner != "phone" || live.copy(expectedDeadline=target.expectedDeadline, workout=live.workout.copy(baseRevision=target.workout.baseRevision)) != target ||
                delta !in listOf(-15,15) || (WearCommandOutbox.state(context,current.workoutId)?.pending ?: 0) > 0) return null
            val command = JSONObject().put("schema",1).put("commandId",UUID.randomUUID().toString())
                .put("action","phoneRest367").put("workoutId",current.workoutId)
                .put("generation",current.generation).put("ownerEpoch193",current.ownerEpoch193)
                .put("pauseRevision",current.pauseRevision).put("key",target.timerKey)
                .put("phaseNumber",target.phaseNumber).put("duration",target.duration)
                .put("delta",delta).put("watchSequence",current.localSequence).put("createdAt",now)
            if(target.phaseDurationMs > 0) command.put("phaseDurationMs",target.phaseDurationMs).put("restRevision",target.restRevision)
            if (WearCommandOutbox.enqueue(context,command) <= 0L) return null
            return WorkoutSnapshotStore.load(context)
        }
    }

    fun pause(context: Context, target: WearPauseTarget367, paused: Boolean): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return null
            if (current.timerOwner != "phone" || current.nativeExecutionProtocol != 2 ||
                current.generation <= 0 || current.workoutPaused == paused ||
                wearPauseTarget367(current, allowPaused = true) != target ||
                (WearCommandOutbox.state(context,current.workoutId)?.pending ?: 0) > 0) return null
            val now = System.currentTimeMillis()
            val command = JSONObject().put("schema",1).put("commandId",UUID.randomUUID().toString())
                .put("action","phonePause367").put("workoutId",current.workoutId)
                .put("generation",current.generation).put("ownerEpoch193",current.ownerEpoch193)
                .put("pauseRevision",current.pauseRevision).put("requestedRevision",maxOf(now,current.pauseRevision+1))
                .put("paused",paused).put("watchSequence",current.localSequence).put("createdAt",now)
            if (WearCommandOutbox.enqueue(context,command) <= 0L) return null
            return WorkoutSnapshotStore.load(context)
        }
    }
}
