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
        assertFalse(WorkoutHealthAutoPolicy.due(energy(0.0, end + settle).put("totalCalories", 0), end, end + settle + 1))
        assertTrue(WorkoutHealthAutoPolicy.due(energy(0.0, end + settle), end, end + settle + 1))
        assertTrue(WorkoutHealthAutoPolicy.due(JSONObject(), end, end + settle))
    }

    @Test fun workoutReadRunsBeforeDailyAndDoesNotSuppressItOnIndependentFailure() = kotlinx.coroutines.runBlocking {
        val order = mutableListOf<String>()
        com.repforge.app.WorkoutHealthAutoReconcile.attemptBeforeDaily(true) {
            order += "workout"
            throw java.io.IOException("Provider not ready")
        }
        order += "daily"
        assertEquals(listOf("workout", "daily"), order)
        com.repforge.app.WorkoutHealthAutoReconcile.attemptBeforeDaily(false) { fail("Disabled read") }
        var cancelled = false
        try {
            com.repforge.app.WorkoutHealthAutoReconcile.attemptBeforeDaily(true) {
                throw kotlinx.coroutines.CancellationException("Foreground ended")
            }
        } catch (_: kotlinx.coroutines.CancellationException) { cancelled = true }
        assertTrue(cancelled)
    }

    @Test fun latePartialEnergyCanBeAddedWithoutLosingExistingWindowEvidence() {
        fun row(a: String, b: String) = JSONObject().put("start", a).put("end", b)
        fun snapshot(vararg rows: JSONObject) = JSONObject().put("measurementWindows", org.json.JSONArray(rows.toList()))
        val a = "2026-11-26T08:00:00Z"; val b = "2026-11-26T08:10:00Z"; val c = "2026-11-26T08:20:00Z"; val d = "2026-11-26T08:30:00Z"
        val saved = snapshot(row(a,b).put("averageHeartRate", 130), row(c,d).put("steps", 42))
        val improved = snapshot(row(a,b).put("averageHeartRate", 130).put("activeCalories", 25),
            row(c,d).put("steps", 42)).put("partial", true)
        assertTrue(WorkoutHealthAutoPolicy.accept(saved, improved))
        val lost = snapshot(row(a,b).put("activeCalories", 25), row(c,d).put("steps", 42)).put("partial", true)
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, lost))
        val changed = snapshot(row(a,c).put("averageHeartRate", 130).put("activeCalories", 25), row(c,d).put("steps", 42))
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, changed))
        val failed = snapshot(row(a,b).put("averageHeartRate", 130).put("activeCalories", 25)
            .put("errors", JSONObject().put("averageHeartRate", "READ_FAILED")), row(c,d).put("steps", 42))
        assertFalse(WorkoutHealthAutoPolicy.accept(saved, failed))
        assertTrue(WorkoutHealthAutoPolicy.accept(null, snapshot(row(a,b).put("activeCalories", 25))))
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
