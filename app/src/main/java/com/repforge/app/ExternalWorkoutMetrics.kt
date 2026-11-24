package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject

/** Conservative same-origin evidence from the existing journal. No aggregation across providers,
 * no prorating, no guessed session totals, no GPS coordinates. Samples are timestamp-deduplicated.
 */
internal object ExternalWorkoutMetrics {
    internal data class Kind(val type: String, val permission: String, val unit: String, val key: String)
    internal val kinds = listOf(
        Kind("StepsRecord", "READ_STEPS", "steps", "steps"),
        Kind("DistanceRecord", "READ_DISTANCE", "m", "distanceMeters"),
        Kind("ActiveCaloriesBurnedRecord", "READ_ACTIVE_CALORIES_BURNED", "kcal", "activeCalories"),
        Kind("TotalCaloriesBurnedRecord", "READ_TOTAL_CALORIES_BURNED", "kcal", "totalCalories"),
        Kind("HeartRateRecord", "READ_HEART_RATE", "bpm", "HeartRate"),
        Kind("SpeedRecord", "READ_SPEED", "m/s", "Speed")
    )
    private const val SAMPLE_LIMIT = 20_000
    internal const val RECORD_LIMIT = 512

    internal fun values(workout: ExternalWorkout, kind: Kind, rows: List<JSONObject>): Map<String, Double> {
        if (rows.size > RECORD_LIMIT) return emptyMap()
        val relevant = rows.filter {
            it.optString("channel") == workout.identity.provider && it.optString("source") == workout.identity.source &&
                it.optString("type") == kind.type && it.optLong("startMs") < workout.endMs && it.optLong("endMs") > workout.startMs
        }
        if (relevant.isEmpty()) return emptyMap()
        if (kind.key in setOf("HeartRate", "Speed")) {
            val samples = sortedMapOf<Long, Double>()
            val conflicts = mutableSetOf<Long>()
            var seen = 0
            for (row in relevant) {
                val data = row.optJSONObject("data") ?: return emptyMap()
                if (data.optString("unit") != kind.unit) return emptyMap()
                val array = data.optJSONArray("samples") ?: continue
                for (i in 0 until array.length()) {
                    if (++seen > SAMPLE_LIMIT) return emptyMap()
                    val sample = array.optJSONObject(i) ?: continue
                    val t = sample.optLong("timeMs", Long.MIN_VALUE)
                    val v = (sample.opt("value") as? Number)?.toDouble() ?: continue
                    if (t !in workout.startMs until workout.endMs || !v.isFinite() || v < 0 ||
                        kind.key == "HeartRate" && v <= 0) continue
                    if (t in conflicts) continue
                    val prior = samples.putIfAbsent(t, v)
                    if (prior != null && prior != v) { samples.remove(t); conflicts.add(t) }
                }
            }
            if (samples.isEmpty()) return emptyMap()
            val values = samples.values
            return if (kind.key == "HeartRate") mapOf("averageHeartRate" to values.average(),
                "minHeartRate" to values.min(), "maxHeartRate" to values.max(), "heartRateSamples" to values.size.toDouble())
            else mapOf("averageSpeedMps" to values.average(), "maxSpeedMps" to values.max())
        }
        // A boundary-crossing or overlapping aggregate cannot be attributed without guessing.
        var previousEnd = workout.startMs
        var sum = 0.0
        for (row in relevant.sortedBy { it.optLong("startMs") }) {
            val start = row.optLong("startMs")
            val end = row.optLong("endMs")
            if (start < previousEnd || end <= start || end > workout.endMs) return emptyMap()
            val data = row.optJSONObject("data") ?: return emptyMap()
            if (data.optString("unit") != kind.unit) return emptyMap()
            val value = (data.opt("value") as? Number)?.toDouble() ?: return emptyMap()
            if (!value.isFinite() || value < 0) return emptyMap()
            sum += value
            previousEnd = end
        }
        return if (sum.isFinite()) mapOf(kind.key to sum) else emptyMap()
    }
}

internal fun HealthJournalStore.externalWorkoutMetrics(
    workout: ExternalWorkout, generation: Long, grants: Set<String>
): JSONObject? = synchronized(HealthJournalStore.LOCK) {
    if (workout.identity.provider == HealthJournalStore.SAMSUNG) {
        val values = JSONObject()
        workout.sessionCalories?.let { values.put("workoutCalories", it) }
        return@synchronized JSONObject().put("values", values).put("partial", true)
            .put("method", "samsung_exercise_session")
    }
    if (workout.identity.provider != HealthJournalStore.HC || "READ_EXERCISE" !in grants) return@synchronized null
    val db = readableDatabase
    writable(db, generation)
    val args = arrayOf(workout.identity.provider, workout.identity.source, workout.endMs.toString(), workout.startMs.toString())
    val overlap = db.rawQuery("""SELECT 1 FROM records WHERE channel=? AND source=? AND start_ms<? AND end_ms>?
        AND type='ExerciseSessionRecord' AND record_id<>? LIMIT 1""", args + workout.identity.recordId)
        .use { it.moveToFirst() }
    val values = JSONObject()
    if (!overlap) for (kind in ExternalWorkoutMetrics.kinds) {
        if (kind.permission !in grants) continue
        val rows = db.rawQuery("""SELECT json FROM records WHERE channel=? AND source=? AND start_ms<? AND end_ms>?
            AND type=? ORDER BY start_ms,record_id LIMIT ${ExternalWorkoutMetrics.RECORD_LIMIT + 1}""", args + kind.type)
            .use { cursor ->
                buildList { while (cursor.moveToNext()) add(JSONObject(cursor.getString(0))) }
            }
        ExternalWorkoutMetrics.values(workout, kind, rows).forEach { (key, value) -> values.put(key, value) }
    }
    JSONObject().put("values", values).put("partial", true).put("overlappingSession", overlap)
        .put("method", "same_source_cached_evidence")
}
