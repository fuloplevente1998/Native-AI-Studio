package com.repforge.app.ui.health

import com.repforge.app.HealthConnectSyncRecovery
import com.repforge.app.NativeHealthSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ExecutionException

class HealthConnectSyncRecoveryRegressionTest {
    private val today = LocalDate.parse("2026-11-17")
    private fun brokenSchema() = ExecutionException(IllegalStateException(
        "android.database.sqlite.SQLiteException: table change_log_request_table has no column named row_offset"))

    private class Journal(val old: NativeHealthSync.Checkpoint) : NativeHealthSync.Journal {
        var commits = 0
        override suspend fun checkpoint() = old
        override suspend fun dirtyDays() = setOf(LocalDate.parse("2026-10-01"))
        override suspend fun attempt(at: Instant) {}
        override suspend fun commit(expected: NativeHealthSync.Checkpoint, next: NativeHealthSync.Checkpoint,
            full: Boolean, at: Instant) { commits++ }
        var dataSynced = 0
        override suspend fun dataSynced(at: Instant) { dataSynced++ }
    }
    private inner class Provider(val failureAt: String, val failure: Exception = brokenSchema()) : NativeHealthSync.Provider {
        override val changesSupported = true
        val reads = mutableListOf<LocalDate>()
        var polls = 0
        var creates = 0
        var identityChecks = 0
        override suspend fun createToken(): String {
            creates++
            if (failureAt == "create") throw failure
            return "new-private-token"
        }
        override suspend fun pollPage(token: String): NativeHealthSync.ChangePage {
            polls++
            if (failureAt == "poll" || failureAt == "catchup") throw failure
            return NativeHealthSync.ChangePage("next-private-token", false, emptySet())
        }
        override fun invalidToken(error: Throwable) = false
        override fun changeLogUnavailable(error: Throwable) = HealthConnectSyncRecovery.schemaFailure(error)
        override suspend fun readDay(day: LocalDate): Boolean { reads += day; return day != failedDay }
        override suspend fun requireIdentity() { identityChecks++ }
        var failedDay: LocalDate? = null
    }

    @Test fun tokenCreationAndInitialPollAndCatchupFailuresReadBoundedDaysWithoutAcknowledgingChanges() = runBlocking {
        for (stage in listOf("create", "poll", "catchup")) {
            val old = NativeHealthSync.Checkpoint("scope", "original-private-token", true, 42)
            val journal = Journal(old)
            val provider = Provider(stage)
            val report = NativeHealthSync.run(provider, journal, "scope", today, stage != "poll", {}, {})
            assertEquals((NativeHealthSync.recentDays(today, 30) + today.withMonth(10).withDayOfMonth(1)).toSet(), provider.reads.toSet())
            assertEquals(31, provider.reads.size)
            assertTrue(report.changeTrackingUnavailable)
            assertFalse(report.successful)
            assertTrue(report.failedDays.isEmpty())
            assertEquals(0, journal.commits)
            assertSame(old, journal.checkpoint())
            assertEquals("original-private-token", journal.old.token)
            assertTrue(provider.identityChecks >= provider.reads.size + 1)
            assertEquals(if (stage == "create") 0 else 1, provider.polls)
            assertEquals(if (stage == "poll") 0 else 1, provider.creates)
        }
    }

    @Test fun failedDailyReadRemainsPartialDuringFallback() = runBlocking {
        val provider = Provider("create").also { it.failedDay = today }
        val journal = Journal(NativeHealthSync.Checkpoint())
        val report = NativeHealthSync.run(provider, journal, "scope", today, false, {}, {})
        assertEquals(listOf(today), report.failedDays)
        assertTrue(report.changeTrackingUnavailable)
        assertEquals(0, journal.commits)
        assertFalse(journal.old.initialized)
    }

    @Test fun permissionCancellationAndUnrelatedFailuresNeverBecomeFallbackReads() = runBlocking {
        for (failure in listOf(SecurityException("denied"), CancellationException("background"),
            IllegalStateException("other SQLite error"))) {
            val provider = Provider("create", failure)
            val journal = Journal(NativeHealthSync.Checkpoint())
            try {
                NativeHealthSync.run(provider, journal, "scope", today, false, {}, {})
                fail("Expected original failure")
            } catch (actual: Exception) { assertSame(failure, actual) }
            assertTrue(provider.reads.isEmpty())
            assertEquals(0, journal.commits)
        }
    }

    @Test fun healthyChangeFeedCanCommitNormallyAfterAnEarlierDegradedAttempt() = runBlocking {
        val old = NativeHealthSync.Checkpoint("scope", "original-private-token", true, 42)
        val journal = Journal(old)
        NativeHealthSync.run(Provider("poll"), journal, "scope", today, false, {}, {})
        assertEquals(0, journal.commits)
        val report = NativeHealthSync.run(Provider("healthy"), journal, "scope", today, false, {}, {})
        assertTrue(report.successful)
        assertFalse(report.changeTrackingUnavailable)
        assertEquals(1, journal.commits)
    }

    @Test fun foregroundLossAfterSchemaFailurePreventsFallbackPublication() = runBlocking {
        val provider = Provider("create")
        val journal = Journal(NativeHealthSync.Checkpoint())
        val cancelled = CancellationException("foreground lost")
        try {
            NativeHealthSync.run(provider, journal, "scope", today, false,
                { if (provider.creates > 0) throw cancelled }, {})
            fail("Expected foreground cancellation")
        } catch (actual: CancellationException) { assertSame(cancelled, actual) }
        assertTrue(provider.reads.isEmpty())
        assertEquals(0, journal.commits)
    }

    @Test fun exactSchemaSignatureIsRequiredAndWrappedSecurityDoesNotQualify() {
        assertTrue(HealthConnectSyncRecovery.schemaFailure(brokenSchema()))
        assertFalse(HealthConnectSyncRecovery.schemaFailure(IllegalStateException("row_offset")))
        assertFalse(HealthConnectSyncRecovery.schemaFailure(SecurityException(brokenSchema())))
        for (language in listOf("hu", "en", "de", "ro")) {
            assertFalse(HealthConnectSyncRecovery.failureMessage(brokenSchema(), language).contains("SQLite"))
            assertFalse(HealthConnectSyncRecovery.limitedMessage(language).contains("row_offset"))
        }
    }
}
