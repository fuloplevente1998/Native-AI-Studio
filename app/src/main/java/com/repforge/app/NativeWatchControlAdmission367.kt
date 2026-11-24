package com.repforge.app

import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/** A watch-owner command is a durable absolute snapshot, not a relative phone-side timer edit. */
internal object NativeWatchControlAdmission367 {
    fun snapshot(current: JSONObject?, command: JSONObject): JSONObject? {
        val incoming = command.optJSONObject("finalSnapshot") ?: return null
        val live = current ?: return null
        val id = command.optString("commandId")
        val sequence = command.optLong("sequence")
        if (command.optString("action") != "watchControl367" || id.isBlank() || sequence <= 0 ||
            !live.optBoolean("active") || !WorkoutOwnership193.owns(live, "watch") ||
            live.optString("workoutId") != command.optString("workoutId") ||
            incoming.optString("workoutId") != live.optString("workoutId") ||
            !WorkoutOwnership193.owns(incoming, "watch") ||
            incoming.optLong("ownerEpoch193") != live.optLong("ownerEpoch193") ||
            live.optLong("ownerEpoch193") != command.optLong("ownerEpoch193", -1) ||
            live.optLong("generation") != command.optLong("generation", -1) ||
            incoming.optLong("generation") != live.optLong("generation") ||
            live.optBoolean("finalizationPending")) return null
        val stamp = live.optJSONObject("lastControl367")
        val proof = java.security.MessageDigest.getInstance("SHA-256")
            .digest(incoming.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
        if (stamp?.optString("commandId") == id && stamp.optLong("sequence") == sequence)
            return live.takeIf { stamp.optString("kind") == command.optString("kind") && stamp.optString("proof") == proof }
        val basePause = command.optLong("pauseRevision", -1)
        if (basePause < 0 || live.optLong("pauseRevision") > basePause ||
            live.optLong("pauseRevision") == basePause && live.optBoolean("workoutPaused")) return null
        when (command.optString("kind")) {
            "pause" -> if (!incoming.optBoolean("workoutPaused") ||
                incoming.optLong("pauseRevision") <= basePause || incoming.optLong("restEndAt") != 0L) return null
            "rest" -> {
                if (incoming.optBoolean("workoutPaused") || incoming.optLong("pauseRevision") != basePause ||
                    incoming.optJSONObject("automation")?.optBoolean("enabled") == true) return null
                val at = command.optLong("createdAt")
                val old = command.optLong("expectedDeadline")
                val delta = command.optInt("delta")
                if (at <= 0 || old <= at || old - at > 600000 || delta !in listOf(-15, 15)) return null
                val seconds = (((old - at + 999) / 1000) + delta).coerceIn(0, 600)
                val expected = if (seconds == 0L) 0L else at + seconds * 1000
                if (incoming.optLong("restEndAt", -1) != expected) return null
                val exercise = incoming.optJSONArray("exercises")?.optJSONObject(incoming.optInt("currentExercise", -1))
                    ?: return null
                val sets = exercise.optJSONArray("sets") ?: return null
                if (command.optString("exerciseId").isBlank() || command.optString("setId").isBlank() ||
                    exercise.optString("id") != command.optString("exerciseId") ||
                    (0 until sets.length()).none { sets.optJSONObject(it)?.optString("setId") == command.optString("setId") }) return null
            }
            else -> return null
        }
        val next = NativeWatchSnapshotAdmission.snapshot(live, command) ?: return null
        // Persist proof alongside the accepted state before issuing the existing receipt.
        return next.put("lastControl367", JSONObject().put("commandId", id).put("sequence", sequence)
            .put("kind", command.optString("kind")).put("proof", proof))
    }
}
