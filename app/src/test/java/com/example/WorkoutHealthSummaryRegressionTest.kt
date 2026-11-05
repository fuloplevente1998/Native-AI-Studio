package com.example

import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeWorkoutActiveWindows
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.NativeWorkoutHealthSummary
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutHealthSummaryRegressionTest {
    private val start = "2026-11-01T10:00:00Z"
    private val middle = "2026-11-01T10:10:00Z"
    private val end = "2026-11-01T10:20:00Z"
    private fun workout() = JSONObject().put("id", "saved").put("started", start).put("finished", end)
    private fun row(a: String = start, b: String = end, bpm: Double = 120.0, kcal: Double = 80.0, count: Int = 2) =
        JSONObject().put("start", a).put("end", b).put("averageHeartRate", bpm)
            .put("heartRateSamples", count).put("activeCalories", kcal)
    private fun snapshot(vararg rows: JSONObject) = JSONObject().put("windowStart", start).put("windowEnd", end)
        .put("measurementWindows", JSONArray(rows.toList()))
    private fun providers(samsung: JSONObject?, hc: JSONObject?): JSONObject {
        var out = JSONObject()
        samsung?.let { out = NativeWorkoutHealthSource.attach(out, HealthJournalStore.SAMSUNG, it) }
        hc?.let { out = NativeWorkoutHealthSource.attach(out, HealthJournalStore.HC, it) }
        return out
    }
    @Test fun mirroredWearSamsungAndHealthConnectCountOnlyOnceAndKeepInputs() {
        val wear = row().put("workoutId", "saved").put("totalCalories", 200)
        val samsung = snapshot(row().put("workoutEnergyRecordIds", JSONArray().put("origin|id")))
        val hc = snapshot(row().put("workoutEnergyRecordIds", JSONArray().put("origin|id")))
        val document = providers(samsung, hc)
        val before = document.toString(); val beforeWear = wear.toString()
        val result = NativeWorkoutHealthSummary.project(workout(), wear, document)
        assertEquals(120.0, result.heart.values.getValue("averageHeartRate"), 0.0)
        assertEquals(2.0, result.heart.values.getValue("heartRateSamples"), 0.0)
        assertEquals(80.0, result.energy.values.getValue("activeCalories"), 0.0)
        assertEquals(listOf("wear"), result.heart.evidence.map { it.provider })
        assertTrue(result.heart.overlapExcluded)
        assertTrue(result.relationships.any { it.confirmedMirror })
        assertEquals(before, document.toString()); assertEquals(beforeWear, wear.toString())
    }
    @Test fun adjacentEqualValuesRemainSeparateAndHeartUsesSampleCounts() {
        val document = providers(snapshot(row(start, middle, 100.0, 80.0, 1)),
            snapshot(row(middle, end, 160.0, 80.0, 3)))
        val result = NativeWorkoutHealthSummary.project(workout(), null, document)
        assertEquals(145.0, result.heart.values.getValue("averageHeartRate"), 0.0)
        assertEquals(4.0, result.heart.values.getValue("heartRateSamples"), 0.0)
        assertEquals(160.0, result.energy.values.getValue("activeCalories"), 0.0)
        assertFalse(result.energy.incomplete); assertFalse(result.energy.overlapExcluded)
        assertEquals(2, result.energy.evidence.size)
    }
    @Test fun partialOverlapNeverProratesAndIndependentKnownWindowsSurvive() {
        val five = "2026-11-01T10:05:00Z"; val fifteen = "2026-11-01T10:15:00Z"
        val result = NativeWorkoutHealthSummary.project(workout(), null, providers(
            snapshot(row(start, middle)), snapshot(row(five, fifteen, kcal = 900.0), row(fifteen, end, kcal = 20.0))))
        assertEquals(100.0, result.energy.values.getValue("activeCalories"), 0.0)
        assertTrue(result.energy.incomplete); assertTrue(result.energy.overlapExcluded)
        assertEquals(2, result.energy.evidence.size)
    }
    @Test fun noUnweightedAverageOfMeansAndNoActivePlusTotal() {
        val a = row(start, middle).apply { remove("heartRateSamples"); remove("activeCalories"); put("totalCalories", 50) }
        val b = row(middle, end).put("activeCalories", 999)
        val result = NativeWorkoutHealthSummary.project(workout(), null, providers(snapshot(a), snapshot(b)))
        assertTrue(result.heart.values.isEmpty())
        assertEquals(mapOf("totalCalories" to 50.0), result.energy.values)
        assertTrue(result.energy.incomplete)
    }
    @Test fun zeroMissingInvalidAndStaleDataAreDistinct() {
        val zero = row(kcal = 0.0).apply { remove("averageHeartRate"); remove("heartRateSamples") }
        val result = NativeWorkoutHealthSummary.project(workout(), null, providers(snapshot(zero), null))
        assertEquals(0.0, result.energy.values.getValue("activeCalories"), 0.0)
        assertTrue(result.heart.values.isEmpty())
        val stale = snapshot(row()).put("windowStart", "2026-10-31T10:00:00Z")
        val missing = NativeWorkoutHealthSummary.project(workout(), null, providers(stale, null))
        assertTrue(missing.energy.values.isEmpty()); assertTrue(missing.rejectedEvidence)
        val invalid = snapshot(row().put("activeCalories", -1).put("averageHeartRate", 0))
        assertTrue(NativeWorkoutHealthSummary.project(workout(), null, providers(invalid, null)).energy.values.isEmpty())
    }
    @Test fun legacyWindowRemainsReadableButCannotCoverAPause() {
        val legacy = row().put("windowStart", start).put("windowEnd", end)
        assertEquals(80.0, NativeWorkoutHealthSummary.project(workout(), null,
            providers(null, legacy)).energy.values.getValue("activeCalories"), 0.0)
        val paused = workout().put("activeIntervals", JSONArray().put(JSONObject().put("start", start).put("end", middle)))
        val result = NativeWorkoutHealthSummary.project(paused, null, providers(null, legacy))
        assertTrue(result.heart.values.isEmpty()); assertTrue(result.rejectedEvidence)
    }
    @Test fun wrongWorkoutAndEditedWearWindowNeverEnterSummary() {
        val other = row().put("workoutId", "other")
        assertTrue(NativeWorkoutHealthSummary.project(workout(), other, null).rejectedEvidence)
        val edited = workout().put("nativeHealthWindowStale", true)
        assertTrue(NativeWorkoutHealthSummary.project(edited, row(), null).energy.values.isEmpty())
    }
    @Test fun mixedOrUnknownSessionEnergyNeverMergesDifferentDefinitions() {
        val mixed = row().apply { remove("activeCalories"); put("workoutCalories", 200)
            put("workoutEnergyTypes", JSONArray().put("active").put("total")) }
        assertTrue(NativeWorkoutHealthSummary.project(workout(), null, providers(snapshot(mixed), null)).energy.values.isEmpty())
        val a = row(start, middle).apply { remove("activeCalories"); put("workoutCalories", 10) }
        val b = row(middle, end).apply { remove("activeCalories"); put("workoutCalories", 30) }
        val result = NativeWorkoutHealthSummary.project(workout(), null, providers(snapshot(a), snapshot(b)))
        assertEquals(10.0, result.energy.values.getValue("workoutCalories"), 0.0)
        assertTrue(result.energy.incomplete)
    }
    @Test fun activeWindowReadKeepsActualPerWindowEvidenceAndProviderErrors() = runBlocking {
        val paused = workout().put("activeIntervals", JSONArray().put(JSONObject().put("start", start).put("end", middle)))
        var reads = 0
        val result = NativeWorkoutActiveWindows.health(paused) { window ->
            reads++; row(window.start.toString(), window.end.toString()).put("partial", true)
                .put("errors", JSONObject().put("heartRate", "READ_FAILED"))
        }
        assertEquals(1, reads)
        assertEquals(middle, result.getJSONArray("measurementWindows").getJSONObject(0).getString("end"))
        assertEquals("READ_FAILED", result.getJSONArray("measurementWindows").getJSONObject(0)
            .getJSONObject("errors").getString("heartRate"))
    }
}
