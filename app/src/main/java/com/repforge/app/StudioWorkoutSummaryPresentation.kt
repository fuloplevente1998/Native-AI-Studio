package com.repforge.app

import org.json.JSONObject
import java.time.Instant

/** Read-only session summary; no daily totals, estimates or second workout clock. */
internal object StudioWorkoutSummaryPresentation {
    fun current(frame: JSONObject?, workoutId: String, generation: Long): JSONObject? = frame?.takeIf {
        it.optString("workoutId") == workoutId &&
            (generation <= 0 || it.optLong("generation") == generation)
    }

    fun calories(frame: JSONObject?, live: JSONObject?, now: Instant): Double? {
        frame ?: return null
        val id = frame.optString("workoutId").takeIf { it.isNotBlank() } ?: return null
        val measured = live?.takeIf { WearLiveHealthStore.valid(it) && it.optString("workoutId") == id }
            ?: NativeSavedWearHealth.fromRaw(frame)?.takeIf { it.optString("workoutId") == id }
        val projection = JSONObject(frame.toString()).put("id", id)
        if (projection.optString("finished").isBlank()) projection.put("finished", now.toString())
        return runCatching {
            NativeWorkoutHealthSummary.project(projection, measured, frame.optJSONObject("health240"))
                .energy.values.values.singleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
        }.getOrNull()
    }
}
