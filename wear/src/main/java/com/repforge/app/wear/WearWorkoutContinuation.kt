package com.repforge.app.wear

import android.content.Context
import com.repforge.shared.WorkoutContinuation
import org.json.JSONObject

internal object WearWorkoutContinuation {
    fun receive(context: Context, raw: String?, origin: String?): Boolean {
        if (raw == null || raw.length > 4096 || origin.isNullOrBlank()) return false
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current = WorkoutSnapshotStore.load(context) ?: return false
            val handoff = current.handoff193?.let(::JSONObject)
            val phone = when (handoff?.optString("source")) {
                "phone" -> handoff?.optString("sourceNode").orEmpty()
                "watch" -> handoff?.optString("targetNode").orEmpty()
                else -> WatchHomeStore.phoneNode193(context)
            }
            if (phone != origin || WearClosureStore.isClosed(context, current.workoutId) || WearUndo188.blocked(current)) return false
            val command = try { JSONObject(raw) } catch (_: Exception) { return false }
            if (!WorkoutContinuation.accepts(WorkoutSnapshotStore.encode(current), command, System.currentTimeMillis())) return false
            WearOwnership193.resume(context)
            return true
        }}
    }
}
