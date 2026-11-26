package com.example

import com.repforge.app.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutEnergyCoverage2815Test {
    private val start = Instant.parse("2026-01-01T22:00:00Z")
    private val end = start.plusSeconds(3600)
    private fun span(a: Long,b: Long,origin: String = "watch") =
        WorkoutEnergyCoverage.Span(start.plusSeconds(a),start.plusSeconds(b),origin)
    private fun record(value: Double, spans: List<WorkoutEnergyCoverage.Span>, permitted: Boolean = true,
        warnings: JSONArray = JSONArray()): JSONObject = JSONObject().put("totalCalories",value).also {
        WorkoutEnergyCoverage.record(it,"totalCalories",start,end,spans,permitted,"READ_TOTAL_CALORIES_BURNED",warnings)
    }

    @Test fun aggregateWindowDoesNotProveCoverageAndOverlapIsNotCountedTwice() {
        val data = record(40.0,listOf(span(-30,60),span(0,120,"mirror"),span(120,180)))
        assertEquals(WorkoutEnergyCoverage.Status.PARTIAL_RAW,WorkoutEnergyCoverage.status(data,"totalCalories"))
        assertEquals(180000L,WorkoutEnergyCoverage.knownMillis(data,"totalCalories"))
        assertEquals(40.0,data.getDouble("totalCalories"),0.0)
        val origins = data.getJSONObject("energyCoverage").getJSONObject("totalCalories").getJSONObject("origins")
        assertEquals(120000L,origins.getJSONObject("watch").getLong("coveredMs"))
        assertEquals(120000L,origins.getJSONObject("mirror").getLong("coveredMs"))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(data,"activeCalories"))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(record(40.0,emptyList(),false),"totalCalories"))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(
            record(40.0,emptyList(),warnings=JSONArray().put("READ_TOTAL_CALORIES_BURNED: failure")),"totalCalories"))
    }

    @Test fun knownPartialRawCoverageLosesToUnverifiedCoverageForTheSameDefinition() {
        val workout = JSONObject().put("id","w").put("started",start.toString()).put("finished",end.toString())
        fun saved(data: JSONObject) = data.put("windowStart",start.toString()).put("windowEnd",end.toString())
        val partial = saved(record(40.0,listOf(span(0,180))))
        val full = saved(record(260.0,listOf(span(0,3600))))
        var document = NativeWorkoutHealthSource.attach(null,HealthJournalStore.SAMSUNG,partial)
        document = NativeWorkoutHealthSource.attach(document,HealthJournalStore.HC,full)
        val result = NativeWorkoutHealthSummary.project(workout,null,document)
        assertEquals(mapOf("totalCalories" to 260.0),result.totalEnergy.values)
        assertEquals(HealthJournalStore.HC,result.totalEnergy.evidence.single().provider)
        assertTrue(result.totalEnergy.overlapExcluded)
        assertTrue(result.activeEnergy.values.isEmpty())
    }

    @Test fun fullRawUnionDoesNotPromoteTheHealthConnectAggregate() {
        val workout = JSONObject().put("id", "w").put("started", start.toString()).put("finished", end.toString())
        fun saved(data: JSONObject) = data.put("windowStart", start.toString()).put("windowEnd", end.toString())
        val key = "totalCalories"
        val hc = saved(record(260.0, listOf(span(0, 1800, "phone"), span(1800, 3600, "watch"))))
        val samsung = saved(JSONObject().put(key, 270.0))
        WorkoutEnergyCoverage.recordDirectAggregate(samsung, key, start, end, permitted = true)
        val document = NativeWorkoutHealthSource.attach(
            NativeWorkoutHealthSource.attach(null, HealthJournalStore.SAMSUNG, samsung), HealthJournalStore.HC, hc)
        val result = NativeWorkoutHealthSummary.project(workout, null, document)

        assertEquals(WorkoutEnergyCoverage.Status.FULL_RAW, WorkoutEnergyCoverage.status(hc, key))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN, WorkoutEnergyCoverage.aggregateStatus(hc, key))
        for (metric in listOf(result.energy, result.totalEnergy)) {
            assertEquals(mapOf(key to 270.0), metric.values)
            assertEquals(HealthJournalStore.SAMSUNG, metric.evidence.single().provider)
            assertTrue(metric.hasUnverifiedProviderCoverage)
        }
    }

    @Test fun samsungDirectAggregateDistinguishesZeroEmptyDeniedAndFailureWithoutInventingCoverage() {
        fun direct(value: Double?, permitted: Boolean = true, failed: Boolean = false): JSONObject {
            val out = JSONObject().put("activeCalories",value ?: JSONObject.NULL)
            if (failed) out.put("errors",JSONObject().put("activeCalories","FAILED"))
            WorkoutEnergyCoverage.recordDirectAggregate(out,"activeCalories",start,end,permitted)
            return out
        }
        val zero = direct(0.0)
        assertEquals(0.0,zero.getDouble("activeCalories"),0.0)
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(zero,"activeCalories"))
        assertNull(WorkoutEnergyCoverage.knownMillis(zero,"activeCalories"))
        assertEquals(WorkoutEnergyCoverage.Status.MISSING,WorkoutEnergyCoverage.status(direct(null),"activeCalories"))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(direct(null,false),"activeCalories"))
        assertEquals(WorkoutEnergyCoverage.Status.UNKNOWN,WorkoutEnergyCoverage.status(direct(null,failed = true),"activeCalories"))
    }

    @Test fun pausedWindowsKeepSeparateCoverageAndLateDataCannotShrinkKnownCoverage() {
        fun row(seconds: Long) = record(40.0,listOf(span(0,seconds))).put("start",start.toString()).put("end",end.toString())
        fun snapshot(row: JSONObject) = JSONObject().put("measurementWindows",JSONArray().put(row))
        val old = snapshot(row(180))
        assertEquals(WorkoutEnergyCoverage.Status.PARTIAL_RAW,WorkoutEnergyCoverage.status(old,"totalCalories"))
        assertFalse(WorkoutHealthAutoPolicy.accept(old,snapshot(row(60))))
        assertTrue(WorkoutHealthAutoPolicy.accept(old,snapshot(row(3600))))
    }
}
