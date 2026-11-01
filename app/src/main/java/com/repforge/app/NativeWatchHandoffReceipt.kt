package com.repforge.app

import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/** UI admission over the native journal, not a replacement ownership state machine. */
internal object NativeWatchHandoffReceipt {
    fun confirmed(frame: JSONObject?, journal: JSONObject, workoutId: String, owner: String): Boolean {
        if (owner !in setOf("phone", "watch") || frame == null || !frame.optBoolean("active") || frame.optString("workoutId") != workoutId ||
            frame.optString("timerOwner") != owner || frame.optInt("ownershipProtocol193") != 1 ||
            WorkoutOwnership193.pending(frame)) return false
        val h = frame.optJSONObject("handoff193") ?: return false
        val request = journal.optJSONObject("request") ?: return false
        val saved = journal.optJSONObject("snapshot") ?: return false
        val savedHandoff = saved.optJSONObject("handoff193") ?: return false
        return h.optString("state") == "committed" && h.optString("target") == owner &&
            h.optString("id").isNotBlank() && h.optString("id") == request.optString("id") &&
            request.optString("workoutId") == workoutId && saved.optString("workoutId") == workoutId &&
            saved.optString("timerOwner") == owner && savedHandoff.optString("id") == h.optString("id") &&
            h.optLong("epoch") == frame.optLong("ownerEpoch193") &&
            saved.optLong("ownerEpoch193") == frame.optLong("ownerEpoch193") &&
            journal.optString("stage") == if (owner == "watch") "settled" else "installed"
    }

    fun pending(frame: JSONObject?, journal: JSONObject): Boolean {
        if (frame == null || !frame.optBoolean("active")) return false
        if (WorkoutOwnership193.pending(frame) || journal.optString("stage") == "unreadable") return true
        return journal.optJSONObject("request")?.optString("workoutId") == frame.optString("workoutId") &&
            journal.optString("stage") in setOf("requested", "incoming", "offered", "accepted", "committed")
    }
}
