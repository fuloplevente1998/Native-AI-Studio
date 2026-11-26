package com.example

import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.NativeWorkoutHealthSummary
import com.repforge.app.WorkoutEnergyCoverage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** 2816/A: mentett edzés teljes kcal + pulzus – tolerancia, részleges jelzés, Samsung eredet HC-n át. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutHealthSummary2816Test {
    private val start = Instant.parse("2026-11-01T10:00:00Z")
    private val pause = start.plusSeconds(25 * 60L)
    private val resume = pause.plusSeconds(2 * 3600L)
    private val end = resume.plusSeconds(20 * 60L)
    private fun intervals() = JSONArray()
        .put(JSONObject().put("start", start.toString()).put("end", pause.toString()))
        .put(JSONObject().put("start", resume.toString()).put("end", end.toString()))
    private fun workout() = JSONObject().put("id", "saved").put("started", start.toString())
        .put("finished", end.toString()).put("activeIntervals", intervals())
    private fun row(a: Instant, b: Instant, total: Double?, active: Double? = null) =
        JSONObject().put("start", a.toString()).put("end", b.toString()).also { row ->
            total?.let { row.put("totalCalories", it) }
            active?.let { row.put("activeCalories", it) }
        }
    private fun snapshot(vararg rows: JSONObject) = JSONObject().put("windowStart", start.toString())
        .put("windowEnd", end.toString()).put("activeIntervals", intervals())
        .put("measurementWindows", JSONArray(rows.toList()))
    private fun doc(provider: String, data: JSONObject) = NativeWorkoutHealthSource.attach(null, provider, data)

    @Test fun legacySaveWithMillisecondDriftStillShowsCompleteTotal() {
        // (a) A második ablak mentése 30 s-mal rövidebb: nem tűnhet el a teljes kcal.
        val data = snapshot(row(start, pause, 210.0), row(resume, end.minusSeconds(30), 160.0))
        val total = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, data)).workoutTotal
        assertEquals(mapOf("totalCalories" to 370.0), total.values)
        assertFalse(total.incomplete)
        assertTrue(NativeWorkoutHealthSummary.coversActive(45 * 60_000L - 60_000L, 45 * 60_000L))
        assertFalse(NativeWorkoutHealthSummary.coversActive(25 * 60_000L, 45 * 60_000L))
        assertFalse(NativeWorkoutHealthSummary.coversActive(0L, 0L))
    }

    @Test fun savedSamsungEvidenceIsAlwaysProjectedWithItsLabel() {
        // (b) A mentett, közvetlen Samsung-bizonyíték a kapcsolótól függetlenül vetül ki.
        val data = snapshot(row(start, pause, 200.0).put("averageHeartRate", 130).put("heartRateSamples", 10),
            row(resume, end, 150.0).put("averageHeartRate", 150).put("heartRateSamples", 10))
        val result = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.SAMSUNG, data))
        assertEquals(mapOf("totalCalories" to 350.0), result.workoutTotal.values)
        assertEquals(140.0, result.heart.values.getValue("averageHeartRate"), 0.0)
        assertEquals("Samsung Health", NativeWorkoutHealthSummary.label(result.workoutTotal.evidence.first().provider))
    }

    @Test fun samsungOriginRecordsReadThroughHealthConnectAreNeverDropped() {
        val origin = "com.sec.android.app.shealth"
        val data = snapshot(
            row(start, pause, 200.0).put("averageHeartRate", 130).put("heartRateSamples", 10)
                .put("maxHeartRate", 160).put("heartRateOrigin", origin).put("sources", JSONArray().put(origin)),
            row(resume, end, 150.0).put("averageHeartRate", 150).put("heartRateSamples", 10)
                .put("maxHeartRate", 170).put("heartRateOrigin", origin).put("sources", JSONArray().put(origin)))
        val result = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, data))
        assertEquals(140.0, result.heart.values.getValue("averageHeartRate"), 0.0)
        assertEquals(170.0, result.heart.values.getValue("maxHeartRate"), 0.0)
        assertEquals(mapOf("totalCalories" to 350.0), result.workoutTotal.values)
        assertTrue(result.workoutTotal.evidence.all { NativeWorkoutHealthSummary.samsungOriginViaHealthConnect(it) })
        assertTrue(result.heart.evidence.all { it.provider == HealthJournalStore.HC })
    }

    @Test fun pausedWorkoutUsesOnlyThe45ActiveMinutesTotalAndKeepsActiveSeparate() {
        // (c) 45 perc aktív + 2 óra szünet: csak az aktív ablakok teljes kcal-ja, az aktív külön.
        val data = snapshot(row(start, pause, 210.0, 90.0), row(resume, end, 160.0, 70.0))
        val result = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, data))
        assertEquals(mapOf("totalCalories" to 370.0), result.workoutTotal.values)
        assertEquals(mapOf("activeCalories" to 160.0), result.activeEnergy.values)
        assertEquals(45 * 60_000L, result.workoutTotal.evidence.sumOf { it.milliseconds })
        // A szünetet átfogó (nem aktív ablakra mentett) adat nem kerülhet be.
        val pauseInclusive = snapshot(row(start, end, 999.0))
        assertTrue(NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, pauseInclusive))
            .workoutTotal.values.isEmpty())
    }

    @Test fun knownPartialCoverageIsShownAsPartialFromOneSourceNeverSummed() {
        // (d) Ismert hiányos lefedettség: látszik, „részleges", egyetlen (legnagyobb lefedettségű) forrásból.
        fun partial(a: Instant, b: Instant, kcal: Double) = row(a, b, kcal).also {
            WorkoutEnergyCoverage.record(it, "totalCalories", a, b,
                listOf(WorkoutEnergyCoverage.Span(a, a.plusSeconds(60), "watch")), true, "READ_TOTAL_CALORIES_BURNED", JSONArray())
        }
        val hc = snapshot(partial(start, pause, 40.0), partial(resume, end, 30.0))
        assertEquals(WorkoutEnergyCoverage.Status.PARTIAL_RAW, WorkoutEnergyCoverage.status(hc, "totalCalories"))
        val samsung = snapshot(row(start, pause, 180.0))
        var document = doc(HealthJournalStore.HC, hc)
        document = NativeWorkoutHealthSource.attach(document, HealthJournalStore.SAMSUNG, samsung)
        val total = NativeWorkoutHealthSummary.project(workout(), null, document).workoutTotal
        assertTrue(total.incomplete)
        // Samsung 25 percet fed le, HC csak 2 percet: a Samsung egyedül, nincs összeadás.
        assertEquals(mapOf("totalCalories" to 180.0), total.values)
        assertEquals(setOf(HealthJournalStore.SAMSUNG), total.evidence.map { it.provider }.toSet())
        val onlyHc = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, hc)).workoutTotal
        assertEquals(mapOf("totalCalories" to 70.0), onlyHc.values)
        assertTrue(onlyHc.incomplete)
    }

    @Test fun realAbsenceOrFailedReadStaysNoData() {
        // (e) Valódi hiány → nincs érték (nem 0); hibás olvasás értéke sem jelenik meg.
        val missing = snapshot(row(start, pause, null, 90.0), row(resume, end, null, 70.0))
        val result = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, missing))
        assertTrue(result.workoutTotal.values.isEmpty())
        assertTrue(result.workoutTotal.incomplete)
        assertTrue(result.heart.values.isEmpty())
        assertEquals(mapOf("activeCalories" to 160.0), result.activeEnergy.values)
        val failed = snapshot(row(start, pause, 10.0).put("errors", JSONObject().put("totalCalories", "READ_FAILED")))
        assertTrue(NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, failed))
            .workoutTotal.values.isEmpty())
    }

    @Test fun unweightedPulseWindowsStillShowSameSourceExtremesWithoutAveragingMeans() {
        val data = snapshot(row(start, pause, null).put("averageHeartRate", 120).put("maxHeartRate", 150).put("minHeartRate", 90),
            row(resume, end, null).put("averageHeartRate", 140).put("maxHeartRate", 170).put("minHeartRate", 95))
        val heart = NativeWorkoutHealthSummary.project(workout(), null, doc(HealthJournalStore.HC, data)).heart
        assertFalse(heart.values.containsKey("averageHeartRate"))
        assertEquals(170.0, heart.values.getValue("maxHeartRate"), 0.0)
        assertEquals(90.0, heart.values.getValue("minHeartRate"), 0.0)
        assertTrue(heart.incomplete)
    }
}
