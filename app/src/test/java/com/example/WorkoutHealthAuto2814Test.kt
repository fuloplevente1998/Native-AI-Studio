package com.example

import com.repforge.app.WorkoutHealthAutoPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutHealthAuto2814Test {
    private val end = Instant.parse("2026-11-26T09:00:00Z").toEpochMilli()
    private fun energy(value: Double, at: Long = end) = JSONObject()
        .put("activeCalories", value).put("syncedAt", Instant.ofEpochMilli(at).toString())

    @Test fun recentMissingAndEarlyReadAreRetriedButSettledReadIsNot() {
        val settle = WorkoutHealthAutoPolicy.SETTLE_MS
        assertTrue(WorkoutHealthAutoPolicy.due(null, end, end))
        assertFalse(WorkoutHealthAutoPolicy.due(null, end, end - 1))
        assertFalse(WorkoutHealthAutoPolicy.due(null, end, end + WorkoutHealthAutoPolicy.LOOKBACK_MS + 1))
        assertFalse(WorkoutHealthAutoPolicy.due(energy(0.0), end, end + 1000))
        assertTrue(WorkoutHealthAutoPolicy.due(energy(0.0), end, end + settle))
        assertFalse(WorkoutHealthAutoPolicy.due(energy(0.0, end + settle), end, end + settle + 1))
        assertTrue(WorkoutHealthAutoPolicy.due(JSONObject(), end, end + settle))
    }

    @Test fun emptyPartialAndMissingMetricResponsesCannotEraseSavedEnergy() {
        val saved = energy(125.0).put("averageHeartRate", 140)
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, JSONObject()))
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, JSONObject().put("exerciseSessionCount", 0)))
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, JSONObject().put("averageHeartRate", 130)))
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, energy(140.0).put("averageHeartRate", 140).put("partial", true)))
        assertTrue(WorkoutHealthAutoPolicy.accept(saved, energy(145.0).put("averageHeartRate", 140)))
        assertTrue(WorkoutHealthAutoPolicy.accept(null, energy(0.0)))
        assertEquals(125.0, saved.getDouble("activeCalories"), 0.0)
    }
}
