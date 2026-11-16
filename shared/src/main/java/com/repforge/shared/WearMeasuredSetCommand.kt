package com.repforge.shared

import org.json.JSONObject

/** The Wear transport boundary for an explicit bilateral OK and its inverse. No clock is started here. */
object WearMeasuredSetCommand {
    fun bilateral(unit: String, measurement: String, exerciseId: String = "") =
        measurement != "distance" && WorkoutStatsCalculator.isBilateralUnit(unit, exerciseId)

    private fun phoneExecution(workout: JSONObject) = workout.optInt("nativeExecutionProtocol") == 2 &&
        workout.optString("timerOwner") == "phone" && workout.optLong("generation") > 0 &&
        workout.optLong("executionRevision") > 0

    /** Fields come from the explicit OK, so earlier rotary edits need not arrive first. */
    fun completion(workout: JSONObject, command: JSONObject): JSONObject {
        if (!phoneExecution(workout) || command.optString("action") != "completeSet") return command
        val exercises = workout.optJSONArray("exercises") ?: return command
        val exercise = (0 until exercises.length()).map { exercises.getJSONObject(it) }
            .singleOrNull { it.optString("id") == command.optString("exerciseId") } ?: return command
        if (!bilateral(exercise.optString("repUnit"), exercise.optString("measurementType"), exercise.optString("id"))) return command
        return JSONObject(command.toString()).put("action", "completeMeasuredSet")
            .put("nativeExecutionProtocol", 2).put("generation", command.opt("generation") ?: workout.getLong("generation"))
            .put("executionRevision", command.opt("executionRevision") ?: workout.getLong("executionRevision"))
            .put("ownerEpoch193", command.opt("ownerEpoch193") ?: workout.optLong("ownerEpoch193"))
            .put("pauseRevision", command.opt("pauseRevision") ?: workout.optLong("pauseRevision"))
            .put("wearBilateralValues", JSONObject().put("leftSeconds", command.opt("leftSeconds") ?: JSONObject.NULL)
                .put("rightSeconds", command.opt("rightSeconds") ?: JSONObject.NULL).put("weight", command.opt("weight") ?: JSONObject.NULL))
    }

    fun undo(workout: JSONObject, record: JSONObject, commandId: String, now: Long, automation: JSONObject?): JSONObject {
        val command = JSONObject(record.toString()).put("schema", 1).put("commandId", commandId).put("action", "undoSet")
            .put("createdAt", now).put("watchSequence", workout.optLong("localSequence")).put("baseRevision", workout.optLong("revision"))
            .put("generation", workout.optLong("generation")).put("executionRevision", workout.optLong("executionRevision"))
            .put("ownerEpoch193", workout.optLong("ownerEpoch193")).put("pauseRevision", workout.optLong("pauseRevision"))
            .put("automation", automation)
        workout.optJSONObject("undo188")?.let { command.put("undoDependency188", it.optString("commandId")) }
        // Older local bilateral records have native188=false. The current native owner still routes
        // their inverse to the durable engine, which alone decides whether that record exists.
        if (phoneExecution(workout)) command.put("nativeExecutionProtocol", 2)
        return command
    }
}
