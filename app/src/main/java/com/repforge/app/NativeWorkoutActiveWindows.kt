package com.repforge.app

import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Canonical half-open active windows, shared by history duration and provider reads. */
internal object NativeWorkoutActiveWindows {
    data class Window(val start: Instant, val end: Instant)

    fun read(workout: JSONObject, finish: Instant = Instant.parse(workout.getString("finished"))): List<Window> {
        val start = Instant.parse(workout.getString("started"))
        require(!finish.isBefore(start))
        val source = workout.optJSONArray("activeIntervals") ?: return listOf(Window(start, finish))
        val sorted = (0 until source.length()).mapNotNull { index ->
            val row = source.getJSONObject(index)
            val from = maxOf(start, Instant.parse(row.getString("start")))
            val to = minOf(finish, if (row.isNull("end") || row.optString("end").isBlank()) finish
                else Instant.parse(row.getString("end")))
            if (to.isAfter(from)) Window(from, to) else null
        }.sortedBy { it.start }
        val merged = mutableListOf<Window>()
        for (window in sorted) {
            val last = merged.lastOrNull()
            if (last != null && !window.start.isAfter(last.end))
                merged[merged.lastIndex] = Window(last.start, maxOf(last.end, window.end))
            else merged.add(window)
        }
        return merged
    }

    fun seconds(workout: JSONObject): Long = read(workout).sumOf { Duration.between(it.start, it.end).toMillis() } / 1000

    /** Never query across a pause. Unknown/incomplete aggregates remain unavailable. */
    suspend fun health(workout: JSONObject, reader: suspend (Window) -> JSONObject): JSONObject {
        val windows = read(workout)
        val rows = windows.map { window -> reader(window).also {
            check(Instant.parse(it.getString("start")) == window.start &&
                Instant.parse(it.getString("end")) == window.end) { "Health window mismatch" }
        } }
        val out = JSONObject().put("start", workout.getString("started")).put("end", workout.getString("finished"))
            .put("activeIntervals", org.json.JSONArray(windows.map {
                JSONObject().put("start", it.start.toString()).put("end", it.end.toString())
            })).put("partial", rows.any { it.optBoolean("partial") })
        fun value(row: JSONObject, key: String) = (row.opt(key) as? Number)?.toDouble()
            ?.takeIf { it.isFinite() && it >= 0 }
        for (key in listOf("steps", "activeCalories", "totalCalories", "workoutCalories", "distanceMeters", "exerciseMinutes")) {
            val values = rows.mapNotNull { value(it, key) }
            if (rows.isNotEmpty() && values.size == rows.size) out.put(key, values.sum())
        }
        for (key in listOf("minHeartRate", "maxHeartRate", "maxSpeedMps")) {
            val values = rows.mapNotNull { value(it, key) }.filter { it > 0 }
            if (values.isNotEmpty()) out.put(key, if (key.startsWith("min")) values.min() else values.max())
        }
        val pulses = rows.filter { value(it, "heartRateSamples")?.let { n -> n > 0 } == true }
        if (pulses.isNotEmpty() && pulses.all { value(it, "averageHeartRate")?.let { n -> n > 0 } == true }) {
            val count = pulses.sumOf { value(it, "heartRateSamples")!! }
            out.put("heartRateSamples", count).put("averageHeartRate",
                pulses.sumOf { value(it, "averageHeartRate")!! * value(it, "heartRateSamples")!! } / count)
        }
        for (key in listOf("sources", "warnings")) {
            val values = rows.flatMap { row -> row.optJSONArray(key)?.let { a ->
                (0 until a.length()).map { a.get(it).toString() }
            } ?: emptyList() }.distinct()
            out.put(key, org.json.JSONArray(values))
        }
        return out
    }
}
