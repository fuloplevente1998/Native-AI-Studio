package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthProviderSelectionRegressionTest {
    private val start = "2026-11-01T10:00:00Z"
    private val end = "2026-11-01T16:20:00Z"
    private fun workout() = JSONObject().put("started", start).put("finished", end).put("activeIntervals", JSONArray()
        .put(JSONObject().put("start", start).put("end", "2026-11-01T10:25:00Z"))
        .put(JSONObject().put("start", "2026-11-01T16:00:00Z").put("end", end)))
    private fun data(pulse: Int) = JSONObject().put("windowStart", start).put("windowEnd", end).put("averageHeartRate", pulse)

    @Test fun providerSnapshotsKeepLegacyHealthConnectAndNeverSubstituteItForSamsung() {
        val hc = data(111)
        assertNull(NativeWorkoutHealthSource.snapshot(hc, HealthJournalStore.SAMSUNG))
        val combined = NativeWorkoutHealthSource.attach(hc, HealthJournalStore.SAMSUNG, data(144))
        assertEquals(111, combined.getInt("averageHeartRate"))
        assertEquals(144, NativeWorkoutHealthSource.snapshot(combined, HealthJournalStore.SAMSUNG)!!.getInt("averageHeartRate"))
        val refreshed = NativeWorkoutHealthSource.attach(combined, HealthJournalStore.HC, data(122))
        assertEquals(122, NativeWorkoutHealthSource.snapshot(refreshed, HealthJournalStore.HC)!!.getInt("averageHeartRate"))
        assertEquals(144, NativeWorkoutHealthSource.snapshot(refreshed, HealthJournalStore.SAMSUNG)!!.getInt("averageHeartRate"))
        assertEquals(111, hc.getInt("averageHeartRate"))
    }
    @Test fun selectedSamsungQueriesOnlyTwoActiveWindowsAndRetainsPartialErrorProof() = runBlocking {
        var hcReads = 0
        val windows = mutableListOf<NativeWorkoutActiveWindows.Window>()
        val out = NativeWorkoutHealthSource.read(workout(), HealthJournalStore.SAMSUNG,
            healthConnect = { hcReads++; error("Wrong provider") }, samsung = { w ->
                windows.add(w)
                JSONObject().put("start", w.start.toString()).put("end", w.end.toString()).put("steps", 100)
                    .put("failedFields", JSONArray().put("averageHeartRate"))
                    .put("errors", JSONObject().put("averageHeartRate", "PERMISSION_DENIED"))
            })
        assertEquals(0, hcReads)
        assertEquals(2, windows.size)
        assertEquals(45 * 60L, windows.sumOf { java.time.Duration.between(it.start, it.end).seconds })
        assertEquals(200, out.getInt("steps"))
        assertTrue(out.getBoolean("partial"))
        assertEquals("PERMISSION_DENIED", out.getJSONObject("errors").getString("averageHeartRate"))
        assertFalse(out.has("averageHeartRate"))
    }
    @Test fun selectedHealthConnectNeverInvokesSamsungAndSamsungFailureNeverFallsBack() = runBlocking {
        val out = NativeWorkoutHealthSource.read(workout(), HealthJournalStore.HC,
            healthConnect = { w -> JSONObject().put("start", w.start.toString()).put("end", w.end.toString()).put("steps", 20) },
            samsung = { error("Wrong provider") })
        assertEquals(40, out.getInt("steps"))
        try {
            NativeWorkoutHealthSource.read(workout(), HealthJournalStore.SAMSUNG,
                healthConnect = { error("Fallback forbidden") }, samsung = { throw SecurityException("Denied") })
            fail("Failure hidden")
        } catch (_: SecurityException) { }
    }
    @Test fun exactDailySourceDoesNotFallBackAndSyncProofIsProviderScoped() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        HealthJournalStore(context).use { store ->
            store.erase()
            val day = LocalDate.parse("2026-11-01")
            store.samsungDay(day.toString(), JSONObject().put("steps", 222), JSONArray(), store.generation())
            assertEquals(222, NativeHealthProviderData.day(store, HealthJournalStore.SAMSUNG, day)!!.getInt("steps"))
            assertNull(NativeHealthProviderData.day(store, HealthJournalStore.HC, day))
            store.syncMeta(JSONObject().put("lastSyncAt", end).put("lastSyncAt_health_connect", end))
            assertEquals(end, store.nativeSyncLastSuccess(HealthJournalStore.HC))
            assertEquals("—", store.nativeSyncLastSuccess(HealthJournalStore.SAMSUNG))
            store.erase()
            assertEquals("—", store.nativeSyncLastSuccess(HealthJournalStore.HC))
            assertNull(NativeHealthProviderData.day(store, HealthJournalStore.SAMSUNG, day))
        }
    }
    @Test fun liveWindowExcludesLongPauseAndRejectsChangedOwnershipOrFinalization() {
        val frame = workout().put("active", true).put("workoutId", "live").put("generation", 7)
            .put("ownerEpoch193", 3).put("pauseRevision", 2)
        val original = frame.toString()
        assertEquals(45 * 60L, NativeWorkoutActiveWindows.seconds(NativeLiveWorkoutHealth.window(frame, Instant.parse(end))))
        assertTrue(NativeLiveWorkoutHealth.sameSession(frame, JSONObject(original)))
        for (key in listOf("generation", "ownerEpoch193", "pauseRevision"))
            assertFalse(NativeLiveWorkoutHealth.sameSession(frame, JSONObject(original).put(key, 99)))
        assertFalse(NativeLiveWorkoutHealth.sameSession(frame, JSONObject(original).put("started", "2026-11-01T09:00:00Z")))
        assertFalse(NativeLiveWorkoutHealth.sameSession(frame, JSONObject(original).put("finalizationPending", true)))
        assertEquals(original, frame.toString())
    }
}
