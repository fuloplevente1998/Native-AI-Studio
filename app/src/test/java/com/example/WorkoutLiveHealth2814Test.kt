package com.example

import com.example.ui.components.WorkoutLiveHealthPolicy
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.NativeWorkoutHealthSummary
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutLiveHealth2814Test {
    private val now = 120000L
    private fun frame() = JSONObject().put("active", true).put("workoutId", "w")
        .put("generation", 2).put("ownerEpoch193", 3).put("started", "1970-01-01T00:01:00Z")
    private fun packet() = JSONObject().put("schema", 1).put("revision", 1).put("watchId", "id")
        .put("workoutId", "w").put("ownerEpoch351", 3).put("sourceNode351", "node")
        .put("state", "active").put("updatedAt", now)
        .put("summary", JSONObject().put("heartRate", 130).put("totalCalories", 42).put("steps", 0))

    @Test fun nestedWatchDataIsSessionBoundAndTotalIsNotActive() {
        val metrics = WorkoutLiveHealthPolicy.watchMetrics(frame(), packet(), "node", now)
        assertEquals(130.0, metrics.getValue("heartRate"), 0.0)
        assertEquals(42.0, metrics.getValue("totalCalories"), 0.0)
        assertEquals(0.0, metrics.getValue("steps"), 0.0)
        assertFalse(metrics.containsKey("activeCalories"))
        assertTrue(WorkoutLiveHealthPolicy.watchMetrics(frame(), packet(), "other", now).isEmpty())
        assertTrue(WorkoutLiveHealthPolicy.watchMetrics(frame(), packet(), "node", now + 46000).isEmpty())
        assertTrue(WorkoutLiveHealthPolicy.watchMetrics(frame().put("workoutPaused", true), packet(), "node", now).isEmpty())
        assertTrue(WorkoutLiveHealthPolicy.watchMetrics(frame().put("ownerEpoch193", 4), packet(), "node", now).isEmpty())
    }

    @Test fun activeEnergyIsVisibleAlongsideWearTotalWithoutAddingMirrors() {
        val start = "2026-11-26T08:00:00Z"; val end = "2026-11-26T09:00:00Z"
        val workout = JSONObject().put("id", "w").put("started", start).put("finished", end)
        val wear = JSONObject().put("workoutId", "w").put("totalCalories", 200)
        val source = JSONObject().put("windowStart", start).put("windowEnd", end).put("activeCalories", 125)
        var document = NativeWorkoutHealthSource.attach(null, HealthJournalStore.SAMSUNG, source)
        document = NativeWorkoutHealthSource.attach(document, HealthJournalStore.HC, source)
        val result = NativeWorkoutHealthSummary.project(workout, wear, document)
        assertEquals(mapOf("activeCalories" to 125.0), result.activeEnergy.values)
        assertEquals(mapOf("totalCalories" to 200.0), result.totalEnergy.values)
        assertTrue(result.activeEnergy.overlapExcluded)
        assertEquals(1, result.activeEnergy.evidence.size)
    }
}
