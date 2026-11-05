package com.repforge.app

import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Read-only display of the existing canonical activity windows; never a second clock. */
internal object StudioWorkoutSessionPresentation {
    fun elapsedSeconds(frame: JSONObject, now: Instant): Long? = runCatching {
        val copy = JSONObject(frame.toString())
        if (copy.optBoolean("workoutPaused")) {
            // A paused session must never grow, even if an incomplete legacy window is open.
            val intervals = copy.optJSONArray("activeIntervals") ?: return null
            for (index in 0 until intervals.length()) intervals.getJSONObject(index).let {
                if (it.isNull("end") || it.optString("end").isBlank()) it.put("end", it.getString("start"))
            }
        }
        NativeWorkoutActiveWindows.read(copy, now).sumOf { Duration.between(it.start, it.end).toMillis() } / 1000
    }.getOrNull()

    fun dayLabel(frame: JSONObject): String? = if (frame.optBoolean("quickWorkout") || frame.optString("type") == "quick") null
        else listOf("dayName", "dayId").firstNotNullOfOrNull { key ->
            frame.optString(key).takeIf { !frame.isNull(key) && it.isNotBlank() && it != "session" }
        }

    fun durationLabel(seconds: Long): String = if (seconds >= 3600)
        "%d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}
