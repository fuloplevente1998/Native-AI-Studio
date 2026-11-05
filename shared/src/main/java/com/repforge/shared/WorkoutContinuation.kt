package com.repforge.shared

import org.json.JSONObject

/** Explicit continuation is addressed to the paused owner's exact revision, never a takeover. */
object WorkoutContinuation {
    const val PATH_PREFIX = "/trainpilot/workout-continue/"

    fun request(current: JSONObject, commandId: String, now: Long): JSONObject {
        check(current.optString("workoutId").length in 1..512 && commandId.length in 1..128 && now > 0 &&
            WorkoutOwnership193.owns(current, "watch") && current.optInt("ownershipProtocol193") == 1 &&
            !current.optBoolean("finalizationPending") && current.optBoolean("workoutPaused")) { "Az órás edzés állapota megváltozott." }
        return JSONObject().put("schema", 1).put("action", "continueWorkout").put("commandId", commandId)
            .put("workoutId", current.getString("workoutId")).put("ownerEpoch193", current.optLong("ownerEpoch193"))
            .put("pauseRevision", current.optLong("pauseRevision")).put("createdAt", now)
    }

    fun accepts(current: JSONObject, command: JSONObject, now: Long): Boolean =
        current.optString("workoutId").length in 1..512 && command.optLong("createdAt") > 0 &&
            command.optInt("schema") == 1 && command.optString("action") == "continueWorkout" &&
            command.optString("commandId").length in 1..128 && now - command.optLong("createdAt") in 0L..15_000L &&
            WorkoutOwnership193.owns(current, "watch") && current.optInt("ownershipProtocol193") == 1 &&
            !current.optBoolean("finalizationPending") && current.optBoolean("workoutPaused") &&
            current.optString("workoutId") == command.optString("workoutId") &&
            current.optLong("ownerEpoch193") == command.optLong("ownerEpoch193", -1) &&
            current.optLong("pauseRevision") == command.optLong("pauseRevision", -1)
}
