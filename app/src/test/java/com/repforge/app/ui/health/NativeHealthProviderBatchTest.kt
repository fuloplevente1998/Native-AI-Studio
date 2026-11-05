package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthProviderBatch as Batch
import com.repforge.app.NativeHealthSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeHealthProviderBatchTest {
    @Test fun firstProviderFailureStillReadsTheOtherWithoutOverlappingOrChangingSelection() = runBlocking {
        val visited = mutableListOf<String>()
        var inFlight = false
        val report = Batch.run({ true }, {}) { source ->
            assertFalse(inFlight)
            inFlight = true
            visited += source
            kotlinx.coroutines.yield()
            inFlight = false
            if (source == HealthJournalStore.SAMSUNG) Result.failure(java.io.IOException("offline"))
            else Result.success(JSONObject())
        }
        assertEquals(listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC), visited)
        assertTrue(report.partial)
        assertFalse(report.failed)
        assertTrue(report.retryable)
        assertEquals(listOf(Batch.Status.FAILED, Batch.Status.SUCCESS), report.outcomes.map { it.status })
    }

    @Test fun unavailableAndPartialSourcesAreReportedWithoutInventingSuccess() = runBlocking {
        val visited = mutableListOf<String>()
        val report = Batch.run({ it == HealthJournalStore.HC }, {}) { source ->
            visited += source
            Result.success(JSONObject().put("partial", true))
        }
        assertEquals(listOf(HealthJournalStore.HC), visited)
        assertTrue(report.partial)
        assertFalse(report.successful)
        assertFalse(report.retryable)
        assertEquals(listOf(Batch.Status.UNAVAILABLE, Batch.Status.PARTIAL), report.outcomes.map { it.status })
        assertFalse(Batch.run({ false }, {}) { error("unavailable sources must not be read") }.successful)
    }

    @Test fun cancellationAndObsoleteGenerationStopBeforeTheNextProvider() = runBlocking {
        val visited = mutableListOf<String>()
        try {
            Batch.run({ true }, {}) { source ->
                visited += source
                Result.failure(CancellationException("background"))
            }
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
        assertEquals(listOf(HealthJournalStore.SAMSUNG), visited)
        visited.clear()
        var current = true
        try {
            Batch.run({ true }, { check(current) { "restore changed generation" } }) { source ->
                visited += source
                current = false
                Result.success(JSONObject())
            }
            fail("Obsolete generation continued")
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(HealthJournalStore.SAMSUNG), visited)
    }

    @Test fun resumeAndRetriesBypassOnlyTheCooldownAndNeverAdmissionOrTheSingleFlightGate() {
        val gate = NativeHealthAutoRefreshGate()
        val snapshot = NativeHealthAutoRefreshGate.Snapshot(HealthJournalStore.HC, "2026-11-05", 1, 1)
        val auto = NativeHealthAutoRefreshGate.Trigger.AUTO
        val first = gate.begin(auto, 0, true, true, true, snapshot)!!
        assertNull(gate.begin(auto, 1, true, true, true, snapshot, true))
        gate.finish(first)
        assertNull(gate.begin(auto, 299_999, true, true, true, snapshot))
        assertNull(gate.begin(auto, 1, false, true, true, snapshot, true))
        assertNull(gate.begin(auto, 1, true, false, true, snapshot, true))
        assertNull(gate.begin(auto, 1, true, true, false, snapshot, true))
        val retry = gate.begin(auto, 30_000, true, true, true, snapshot, true)!!
        gate.invalidate()
        assertFalse(gate.isCurrent(retry, snapshot, true, true))
        gate.finish(retry)
        assertNotNull(gate.begin(auto, 330_000, true, true, true, snapshot))
    }

    @Test fun transientRetriesAreBoundedAndPermissionsAreNeverRetried() {
        assertEquals(30_000L, NativeHealthAppRefreshPolicy.retryDelay(0, true))
        assertEquals(120_000L, NativeHealthAppRefreshPolicy.retryDelay(1, true))
        assertNull(NativeHealthAppRefreshPolicy.retryDelay(2, true))
        assertNull(NativeHealthAppRefreshPolicy.retryDelay(0, false))
        assertTrue(NativeHealthAppRefreshPolicy.transient(IllegalStateException(java.io.IOException("offline"))))
        assertFalse(NativeHealthAppRefreshPolicy.transient(SecurityException("denied")))
        assertFalse(NativeHealthAppRefreshPolicy.transient(CancellationException("background")))
        assertFalse(NativeHealthAppRefreshPolicy.transient(IllegalStateException("provider changed")))
    }

    @Test fun partialDailyIoFailureCarriesRetryWithoutCommittingASuccessCheckpoint() = runBlocking {
        for (failure in listOf(java.io.IOException("offline"), SecurityException("denied"))) {
            var commits = 0
            val day = java.time.LocalDate.parse("2026-11-05")
            val provider = object : NativeHealthSync.Provider {
                override val changesSupported = false
                override suspend fun createToken() = error("no change feed")
                override suspend fun pollPage(token: String) = error("no change feed")
                override fun invalidToken(error: Throwable) = false
                override suspend fun readDay(value: java.time.LocalDate): Boolean {
                    if (value == day) throw failure
                    return true
                }
                override suspend fun requireIdentity() { }
            }
            val journal = object : NativeHealthSync.Journal {
                override suspend fun checkpoint() = NativeHealthSync.Checkpoint("scope", initialized = true)
                override suspend fun dirtyDays() = emptySet<java.time.LocalDate>()
                override suspend fun attempt(at: java.time.Instant) { }
                override suspend fun commit(expected: NativeHealthSync.Checkpoint, next: NativeHealthSync.Checkpoint,
                    full: Boolean, at: java.time.Instant) { commits++ }
            }
            val report = NativeHealthSync.run(provider, journal, "scope", day, false, {}, {})
            assertEquals(listOf(day), report.failedDays)
            assertEquals(failure is java.io.IOException, report.retryable)
            assertEquals(0, commits)
            val batch = Batch.run({ true }, {}) {
                Result.success(JSONObject().put("partial", true).put("retryable", report.retryable))
            }
            assertEquals(report.retryable, batch.retryable)
        }
    }
}
