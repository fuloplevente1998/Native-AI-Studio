package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutMetricsTest {
    private val workout = ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.HC, "third.party", "a"),
        1000, 100, 200, 1, null, null, null, null, null, null)
    private fun kind(key: String) = ExternalWorkoutMetrics.kinds.single { it.key == key }
    private fun row(key: String, start: Long, end: Long, data: JSONObject, source: String = "third.party") =
        JSONObject().put("channel", HealthJournalStore.HC).put("source", source).put("type", kind(key).type)
            .put("startMs", start).put("endMs", end).put("data", data.put("unit", kind(key).unit))
    private fun amount(key: String, start: Long, end: Long, value: Double, source: String = "third.party") =
        row(key, start, end, JSONObject().put("value", value), source)

    @Test fun additiveRecordsMustBeContainedAndNonoverlapping() {
        val a = amount("activeCalories", 100, 150, 5.0)
        val b = amount("activeCalories", 150, 200, 6.0)
        assertEquals(mapOf("activeCalories" to 11.0), ExternalWorkoutMetrics.values(workout, kind("activeCalories"),
            listOf(a, b, amount("activeCalories", 100, 200, 99.0, "other"), amount("totalCalories", 100, 200, 30.0))))
        assertTrue(ExternalWorkoutMetrics.values(workout, kind("activeCalories"), listOf(a, amount("activeCalories", 140, 180, 2.0))).isEmpty())
        assertTrue(ExternalWorkoutMetrics.values(workout, kind("activeCalories"), listOf(amount("activeCalories", 90, 120, 2.0))).isEmpty())
    }

    @Test fun sampleConflictsAreExcludedAndMissingValuesStayAbsent() {
        fun s(t: Long, v: Int) = JSONObject().put("timeMs", t).put("value", v)
        val a = row("HeartRate", 90, 220, JSONObject().put("samples", JSONArray().put(s(110, 100)).put(s(120, 130)).put(s(200, 200))))
        val b = row("HeartRate", 100, 200, JSONObject().put("samples", JSONArray().put(s(110, 100)).put(s(120, 150)).put(s(130, 140))))
        val result = ExternalWorkoutMetrics.values(workout, kind("HeartRate"), listOf(a, b))
        assertEquals(120.0, result.getValue("averageHeartRate"), 0.0)
        assertEquals(2.0, result.getValue("heartRateSamples"), 0.0)
        assertEquals(140.0, result.getValue("maxHeartRate"), 0.0)
        assertTrue(ExternalWorkoutMetrics.values(workout, kind("Speed"), emptyList()).isEmpty())
    }
}
