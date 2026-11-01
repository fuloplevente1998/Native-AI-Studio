package com.repforge.app

import org.json.JSONObject

/** Explicit Finish may retain actual GPS-service time even when no distance fix was obtained. */
internal object StudioOutdoorFinish {
    const val FIELD = "studioOutdoorFinish"

    fun receipt(frame: JSONObject): JSONObject? {
        val binding = frame.optJSONObject(NativeGpsMeasurement.FIELD) ?: return null
        val capture = binding.optJSONObject("capture") ?: return null
        if (!frame.optBoolean("active") || frame.optString("timerOwner", "phone") != "phone" ||
            frame.optBoolean("finalizationPending") || com.repforge.shared.WorkoutOwnership193.pending(frame) ||
            !binding.optBoolean("applied") || capture.optBoolean("active", true) ||
            capture.optLong("elapsedSeconds") <= 0 || capture.optLong("elapsedSeconds") > 86_400 ||
            binding.optString("workoutId") != frame.optString("workoutId") ||
            binding.optLong("generation") != frame.optLong("generation") ||
            capture.optString("measurementId") != binding.optString("id") ||
            capture.optString("nativeWorkoutId") != frame.optString("workoutId") ||
            capture.optLong("nativeGeneration") != frame.optLong("generation")) return null
        return JSONObject().put("workoutId", frame.getString("workoutId"))
            .put("generation", frame.getLong("generation")).put("measurementId", binding.getString("id"))
            .put("capture", capture.toString())
    }

    fun canSaveTimeOnly(frame: JSONObject): Boolean {
        val expected = receipt(frame) ?: return false
        val saved = frame.optJSONObject(FIELD) ?: return false
        return listOf("workoutId", "generation", "measurementId", "capture").all { expected.get(it) == saved.opt(it) }
    }
}
