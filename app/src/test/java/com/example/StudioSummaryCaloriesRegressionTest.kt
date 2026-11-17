package com.example

import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeSavedWearHealth
import com.repforge.app.NativeWorkoutActiveWindows
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.NativeWorkoutHealthSummary
import com.repforge.app.StudioWorkoutSummaryPresentation
import com.repforge.app.data.db.WorkoutHistoryEntity
import kotlinx.coroutines.runBlocking
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
class StudioSummaryCaloriesRegressionTest {
    private val start = Instant.parse("2026-11-17T10:00:00Z")
    private val end = Instant.parse("2026-11-17T10:30:00Z")
    private fun frame() = JSONObject().put("workoutId", "session").put("generation", 7)
        .put("active", true).put("started", start.toString()).put("finished", end.toString())
        .put("workoutPaused", true).put("pauseRevision", 4).put("ownerEpoch193", 2)
        .put("activeIntervals", JSONArray().put(JSONObject().put("start", start.toString()).put("end", "2026-11-17T10:10:00Z"))
            .put(JSONObject().put("start", "2026-11-17T10:20:00Z").put("end", end.toString())))
    private fun live(kcal: Double? = 123.0) = JSONObject().put("schema", 1).put("revision", 2)
        .put("watchId", "watch").put("updatedAt", end.toEpochMilli()).put("workoutId", "session")
        .apply { if (kcal != null) put("totalCalories", kcal) }
    private fun providerResponse(workout: JSONObject, provider: String, kcal: Double? = 40.0): JSONObject = runBlocking {
        NativeWorkoutHealthSource.read(workout, provider,
            healthConnect = { w -> row(w, kcal) }, samsung = { w -> row(w, kcal) })
    }
    private fun row(window: NativeWorkoutActiveWindows.Window, kcal: Double?) = JSONObject()
        .put("start", window.start.toString()).put("end", window.end.toString())
        .apply { if (kcal != null) put("activeCalories", kcal).put("totalCalories", 999.0) }
    private fun calorie(frame: JSONObject, live: JSONObject? = null, provider: String = HealthJournalStore.HC,
        health: StudioWorkoutSummaryPresentation.HealthSnapshot? = null) =
        StudioWorkoutSummaryPresentation.calories(frame, live, end, provider, health)

    @Test fun savedWearWithoutRedundantWorkoutIdMatchesTheJournalAndPulseOnlyLiveCannotHideIt() {
        val frame = frame().put("wear", JSONObject().put("totalCalories", 184.0))
        assertNotNull(NativeSavedWearHealth.fromRaw(frame))
        assertEquals(184.0, calorie(frame)!!, 0.0)
        assertEquals(184.0, calorie(frame, live(null))!!, 0.0)
        assertEquals(184.0, NativeWorkoutHealthSummary.read(journal(frame)).energy.values.values.single(), 0.0)
        assertEquals(123.0, calorie(frame, live())!!, 0.0)
    }

    @Test fun selectedProviderUsesOnlyItsExactActiveWindowsBeforeSavingWithJournalParity() {
        val frame = frame().apply { remove("finished") }
        val query = JSONObject(frame.toString()).put("finished", end.toString())
        val response = providerResponse(query, HealthJournalStore.SAMSUNG)
        val health = StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.SAMSUNG, response, end)
        val before = frame.toString()
        // The summary may stay open after reading; its captured read window remains stable.
        val energy = StudioWorkoutSummaryPresentation.energy(frame, null, end.plusSeconds(90), HealthJournalStore.SAMSUNG, health)!!
        assertEquals(mapOf("activeCalories" to 80.0), energy.values)
        assertEquals(listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.SAMSUNG), energy.evidence.map { it.provider })
        assertEquals(1200_000L, energy.evidence.sumOf { it.milliseconds })
        val saved = JSONObject(frame.toString()).put("finished", end.toString())
            .put("health240", JSONObject(health.documentJson))
        val journal = NativeWorkoutHealthSummary.read(journal(saved)).energy
        assertEquals(journal.values, energy.values)
        assertEquals(journal.evidence.map { it.windows }, energy.evidence.map { it.windows })
        assertEquals(journal.incomplete, energy.incomplete)
        assertEquals(before, frame.toString())
    }

    @Test fun providerSelectionNeverFallsBackToAnotherProviderOrDailyTotalsAndNeverAddsActiveToTotal() {
        val frame = frame()
        var document = NativeWorkoutHealthSource.attach(null, HealthJournalStore.HC,
            providerResponse(frame, HealthJournalStore.HC, 20.0))
        document = NativeWorkoutHealthSource.attach(document, HealthJournalStore.SAMSUNG,
            providerResponse(frame, HealthJournalStore.SAMSUNG, 70.0))
        frame.put("health240", document).put("activeCalories", 9000).put("totalCalories", 12000)
        assertEquals(40.0, calorie(frame, provider = HealthJournalStore.HC)!!, 0.0)
        assertEquals(140.0, calorie(frame, provider = HealthJournalStore.SAMSUNG)!!, 0.0)
        document.getJSONObject("providers").remove(HealthJournalStore.SAMSUNG)
        assertNull(calorie(frame, provider = HealthJournalStore.SAMSUNG))
        assertNull(calorie(frame().put("activeCalories", 9000).put("totalCalories", 12000)))
    }

    @Test fun samsungSessionEnergyPreservesItsDefinitionAndJournalProvenance() {
        val frame = frame()
        val response = providerResponse(frame, HealthJournalStore.SAMSUNG)
        val rows = response.getJSONArray("measurementWindows")
        for (i in 0 until rows.length()) rows.getJSONObject(i).apply {
            remove("activeCalories"); remove("totalCalories")
            put("workoutCalories", 35.0).put("workoutEnergyTypes", JSONArray().put("active"))
            put("workoutEnergyRecordIds", JSONArray().put("samsung|session-$i"))
        }
        val health = StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.SAMSUNG, response, end)
        val energy = StudioWorkoutSummaryPresentation.energy(frame, null, end, HealthJournalStore.SAMSUNG, health)!!
        assertEquals(mapOf("workoutCalories" to 70.0), energy.values)
        assertEquals(2, energy.evidence.size)
        assertEquals("samsung|session-0", energy.evidence.first().data.getJSONArray("workoutEnergyRecordIds").getString(0))
        frame.put("health240", JSONObject(health.documentJson))
        assertEquals(NativeWorkoutHealthSummary.read(journal(frame)).energy.values, energy.values)
    }

    @Test fun staleOrWrongWorkoutEvidenceIsRejectedAndCannotHideValidSavedWear() {
        val frame = frame().put("healthSummary", JSONObject().put("workoutId", "session").put("totalCalories", 65))
        assertEquals(65.0, calorie(frame, live().put("workoutId", "another"))!!, 0.0)
        assertEquals(65.0, calorie(frame, live().put("updatedAt", start.minusSeconds(1).toEpochMilli()))!!, 0.0)
        assertEquals(65.0, calorie(frame, live().put("windowStart", start.minusSeconds(1)).put("windowEnd", end))!!, 0.0)
        assertNull(calorie(frame().put("wear", JSONObject().put("workoutId", "another").put("totalCalories", 888))))
        assertNull(calorie(frame.put("nativeHealthWindowStale", true), live()))
        val wrong = providerResponse(frame(), HealthJournalStore.HC).put("workoutId", "another")
        assertNull(calorie(frame().put("health240", NativeWorkoutHealthSource.attach(null, HealthJournalStore.HC, wrong))))
        val stale = providerResponse(frame(), HealthJournalStore.HC).put("start", start.minusSeconds(1).toString())
        assertNull(calorie(frame().put("health240", NativeWorkoutHealthSource.attach(null, HealthJournalStore.HC, stale))))
    }

    @Test fun asyncSnapshotCannotCrossProviderGenerationPauseOrWorkoutChanges() {
        val frame = frame()
        val health = StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.HC,
            providerResponse(frame, HealthJournalStore.HC), end)
        assertEquals(80.0, calorie(frame, health = health)!!, 0.0)
        for ((key, value) in listOf("generation" to 8, "pauseRevision" to 5, "ownerEpoch193" to 3,
            "workoutId" to "other", "finalizationPending" to true, "active" to false, "nativeHealthWindowStale" to true)) {
            assertNull("Snapshot survived $key change", calorie(JSONObject(frame.toString()).put(key, value), health = health))
        }
        val changedWindows = JSONObject(frame.toString()).put("activeIntervals", JSONArray().put(JSONObject()
            .put("start", start.toString()).put("end", "2026-11-17T10:05:00Z")))
        assertNull(calorie(changedWindows, health = health))
        assertNull(calorie(frame, provider = HealthJournalStore.SAMSUNG, health = health))
    }

    @Test fun missingRemainsUnknownAndMeasuredZeroRemainsZero() {
        val frame = frame()
        val missing = StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.HC,
            providerResponse(frame, HealthJournalStore.HC, null), end)
        assertNull(calorie(frame, health = missing))
        val zero = StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.HC,
            providerResponse(frame, HealthJournalStore.HC, 0.0), end)
        assertEquals(0.0, calorie(frame, health = zero)!!, 0.0)
        assertNull(calorie(frame, live(-1.0)))
    }

    @Test fun mislabeledProviderAndForeignSnapshotAreRejectedAtCapture() {
        val frame = frame()
        val response = providerResponse(frame, HealthJournalStore.HC)
        assertThrows(IllegalStateException::class.java) {
            StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.SAMSUNG, response, end)
        }
        assertThrows(IllegalStateException::class.java) {
            StudioWorkoutSummaryPresentation.healthSnapshot(frame, HealthJournalStore.HC,
                JSONObject(response.toString()).put("workoutId", "other"), end)
        }
        val mislabeled = JSONObject().put("providers", JSONObject().put(HealthJournalStore.HC,
            JSONObject(response.toString()).put("provider", HealthJournalStore.SAMSUNG)))
        assertNull(calorie(frame.put("health240", mislabeled)))
    }

    private fun journal(frame: JSONObject) = WorkoutHistoryEntity(id = "session", workoutKey = "session", syncId = "session",
        started = start.toString(), finished = end.toString(), startedEpochMs = start.toEpochMilli(),
        finishedEpochMs = end.toEpochMilli(), durationSeconds = 1200, programId = "", programName = "Workout",
        dayId = "", dayName = "", scheduleId = "", quickWorkout = true, originDevice = "phone",
        ownershipProtocol193 = 0, ownerEpoch193 = 0, effortFeedback = null, feedbackSkipped = false,
        feedbackUpdatedAt = 0, totalVolumeKg = 0.0, completedSetsCount = 0, totalSetsCount = 0,
        totalRepsCount = 0, hasPr226 = false, prRecordsJson226 = "[]",
        wearMetricsJson = NativeSavedWearHealth.fromRaw(frame)?.toString(),
        health240Json = frame.optJSONObject("health240")?.toString(), timeLogs162Json = null,
        activeIntervalsJson = frame.optJSONArray("activeIntervals")?.toString(), editedAt = null, rawJson = frame.toString())
}
