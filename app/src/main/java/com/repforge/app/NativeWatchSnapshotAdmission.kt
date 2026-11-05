package com.repforge.app

import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/** A delayed owner snapshot may confirm delivery, never replace a newer epoch, pause, or edit. */
internal object NativeWatchSnapshotAdmission {
    fun snapshot(current: JSONObject?, command: JSONObject): JSONObject? {
        val incoming = command.optJSONObject("finalSnapshot") ?: return null
        val id = command.optString("workoutId")
        val sequence = command.optLong("sequence")
        if (id.isBlank() || sequence <= 0 || incoming.optString("workoutId") != id ||
            !WorkoutOwnership193.owns(incoming, "watch") ||
            incoming.optLong("ownerEpoch193") != command.optLong("ownerEpoch193", -1)) return null
        if (current != null) {
            if (!current.optBoolean("active", true) && current.optString("workoutId") == id) return null
            if (current.optBoolean("active", true) && (current.optString("workoutId") != id ||
                WorkoutOwnership193.stale(current, incoming) || WorkoutOwnership193.pending(current) ||
                incoming.optLong("ownerEpoch193") != current.optLong("ownerEpoch193") ||
                incoming.optLong("pauseRevision") < current.optLong("pauseRevision") ||
                incoming.optLong("pauseRevision") == current.optLong("pauseRevision") &&
                    incoming.optBoolean("workoutPaused") != current.optBoolean("workoutPaused") ||
                sequence <= maxOf(current.optLong("localSequence"), current.optLong("watchSequence")))) return null
        }
        return JSONObject(incoming.toString()).put("active", true)
            .put("localSequence", maxOf(incoming.optLong("localSequence"), sequence))
    }
}
