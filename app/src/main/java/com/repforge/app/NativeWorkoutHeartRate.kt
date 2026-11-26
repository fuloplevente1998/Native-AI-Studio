package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** All displayed pulse statistics use the same measured sample set. */
internal object NativeWorkoutHeartRate {
    private val keys = listOf("averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples")

    data class Sample(val time: Instant, val bpm: Long, val origin: String)

    fun fromSamples(out: JSONObject, start: Instant, end: Instant, samples: Sequence<Sample>): Boolean {
        val points = samples.filter { it.time >= start && it.time < end && it.bpm > 0 && it.origin.isNotBlank() }
            .groupBy { it.origin }
            .mapValues { (_, rows) ->
                rows.groupBy { it.time }.values.mapNotNull { atTime ->
                    atTime.map { it.bpm }.distinct().singleOrNull()
                }
            }
        val best = points.entries.sortedWith(compareByDescending<Map.Entry<String, List<Long>>> { it.value.size }
            .thenBy { it.key }).firstOrNull { it.value.isNotEmpty() } ?: return false
        val values = best.value
        out.put("averageHeartRate", values.average()).put("minHeartRate", values.min())
            .put("maxHeartRate", values.max()).put("heartRateSamples", values.size)
            .put("heartRateOrigin", best.key)
        return true
    }

    /** A Health Connect package can publish a different stream in a later interval. */
    fun preferredOrigin(rows: List<JSONObject>): String? = rows
        .filter { measured(it, "averageHeartRate") && measured(it, "heartRateSamples") && it.optString("heartRateOrigin").isNotBlank() }
        .groupBy { it.getString("heartRateOrigin") }.entries
        .sortedWith(compareByDescending<Map.Entry<String, List<JSONObject>>> { entry ->
            entry.value.sumOf { it.getDouble("heartRateSamples") }
        }.thenBy { it.key }).firstOrNull()?.key

    fun summarize(out: JSONObject, rows: List<JSONObject>) {
        keys.forEach { out.remove(it) }
        out.remove("heartRateOrigin")
        val origin = preferredOrigin(rows)
        val selected = if (origin == null) rows else rows.filter { it.optString("heartRateOrigin") == origin }
        origin?.let { out.put("heartRateOrigin", it) }
        for (key in listOf("minHeartRate", "maxHeartRate")) {
            val values = selected.map { it.optDouble(key) }.filter { it.isFinite() && it > 0 }
            if (values.isNotEmpty()) out.put(key, if (key.startsWith("min")) values.min() else values.max())
        }
        val pulses = selected.filter { measured(it, "heartRateSamples") }
        if (pulses.isNotEmpty() && pulses.all { measured(it, "averageHeartRate") }) {
            val count = pulses.sumOf { it.getDouble("heartRateSamples") }
            out.put("heartRateSamples", count)
                .put("averageHeartRate", pulses.sumOf { it.getDouble("averageHeartRate") * it.getDouble("heartRateSamples") } / count)
        }
    }

    /**
     * Fill missing pulse from the same provider and exact active window independently of energy.
     * This keeps prior observations and their original energy-read timestamp.
     */
    fun addMissing(previous: JSONObject, next: JSONObject): JSONObject? {
        val merged = JSONObject(previous.toString())
        val oldRows = merged.optJSONArray("measurementWindows")
        val newRows = next.optJSONArray("measurementWindows")
        if (oldRows == null) {
            if (!addPulse(merged, next)) return null
        } else {
            if (newRows == null) return null
            val newByWindow = rows(newRows).associateBy(::window)
            var added = false
            val oldWindows = rows(oldRows)
            for (old in oldWindows) {
                val candidate = newByWindow[window(old)] ?: continue
                added = addPulse(old, candidate) || added
            }
            if (!added) return null
            summarize(merged, oldWindows)
        }
        next.optString("syncedAt").takeIf(String::isNotBlank)?.let { merged.put("heartRateSyncedAt", it) }
        return merged
    }

    private fun rows(array: JSONArray) = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    private fun window(row: JSONObject) = Instant.parse(row.getString("start")) to Instant.parse(row.getString("end"))

    private fun measured(row: JSONObject, key: String): Boolean =
        (row.opt(key) as? Number)?.toDouble()?.let { it.isFinite() && it > 0 } == true

    private fun addPulse(previous: JSONObject, next: JSONObject): Boolean {
        if (measured(previous, "averageHeartRate") || !measured(next, "averageHeartRate") ||
            !measured(next, "heartRateSamples")) return false
        for (key in keys + "heartRateOrigin") {
            previous.remove(key)
            next.opt(key)?.takeUnless { it === JSONObject.NULL }?.let { previous.put(key, it) }
        }
        return true
    }
}
