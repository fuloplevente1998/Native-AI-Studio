package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import com.example.ui.screens.CloudSyncStatus
import com.example.ui.screens.cloudServiceStatus
import com.repforge.app.GoogleAutoSyncWorker
import com.repforge.app.NativeSyncSchedule
import com.repforge.app.ui.health.NativeHealthAutoRefreshGate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 2805: opt-in automatic Google sync and a user-chosen, reboot-proof Health sync cadence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Collector2805RegressionTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun clean() {
        context.getSharedPreferences("trainpilot-sync-schedule", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun cadenceDefaultsAndOnlyOfferedValuesAreSaved() {
        assertEquals(5, NativeSyncSchedule.foregroundMinutes(context))
        assertEquals(15, NativeSyncSchedule.backgroundMinutes(context))
        assertFalse("not an offered choice", NativeSyncSchedule.setForegroundMinutes(context, 1))
        assertFalse(NativeSyncSchedule.setBackgroundMinutes(context, 5))
        val revision = NativeSyncSchedule.foregroundRevision.value
        assertTrue(NativeSyncSchedule.setForegroundMinutes(context, 2))
        assertTrue("a saved change re-times the running loop", NativeSyncSchedule.foregroundRevision.value != revision)
        assertTrue(NativeSyncSchedule.setBackgroundMinutes(context, 60))
        assertEquals(2, NativeSyncSchedule.foregroundMinutes(context))
        assertEquals(60, NativeSyncSchedule.backgroundMinutes(context))
    }

    @Test fun googleAutomationIsOptInWifiOnlyAndKeepsFailuresUntilSuccess() {
        val initial = NativeSyncSchedule.googleAuto(context)
        assertFalse(initial.any)
        assertTrue(initial.wifiOnly)
        assertEquals(NetworkType.UNMETERED, GoogleAutoSyncWorker.constraints(initial).requiredNetworkType)
        assertTrue(GoogleAutoSyncWorker.constraints(initial).requiresBatteryNotLow())
        assertTrue(NativeSyncSchedule.setGoogleAuto(context, initial.copy(drive = true, wifiOnly = false)))
        val saved = NativeSyncSchedule.googleAuto(context)
        assertTrue(saved.drive && !saved.calendar && saved.any)
        assertEquals(NetworkType.CONNECTED, GoogleAutoSyncWorker.constraints(saved).requiredNetworkType)

        NativeSyncSchedule.recordAutoResult(context, "drive", "Google error")
        assertEquals("Google error", NativeSyncSchedule.autoFailure(context, "drive"))
        assertEquals("", NativeSyncSchedule.autoFailure(context, "calendar"))
        NativeSyncSchedule.recordAutoResult(context, "drive", null)
        assertEquals("", NativeSyncSchedule.autoFailure(context, "drive"))
    }

    @Test fun lateOnlyForAnAutomatedServiceThatAlreadyRan() {
        val now = java.time.Instant.parse("2026-11-24T12:00:00Z")
        assertEquals("never run is not late, even right after enabling", CloudSyncStatus.NEUTRAL, cloudServiceStatus(true, "", false, true, now))
        assertEquals(CloudSyncStatus.LATE, cloudServiceStatus(true, "2026-11-20T08:00:00Z", false, true, now))
        assertEquals(CloudSyncStatus.OK, cloudServiceStatus(true, "2026-11-20T08:00:00Z", false, false, now))
        assertEquals("an unattended failure is shown as an error", CloudSyncStatus.ERROR, cloudServiceStatus(true, "2026-11-24T08:00:00Z", true, true, now))
    }

    @Test fun refreshGateFollowsTheChosenIntervalAndThrottlesResume() {
        val gate = NativeHealthAutoRefreshGate()
        val snap = NativeHealthAutoRefreshGate.Snapshot("hc", "2026-11-24", 1L, 1L)
        assertTrue("first resume always reads", gate.autoOlderThan(1_000L, NativeSyncSchedule.RESUME_MIN_GAP_MS))
        val t = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 1_000L, true, true, true, snap)!!
        gate.finish(t)
        assertFalse("resume within 2 minutes does not read again", gate.autoOlderThan(60_000L, NativeSyncSchedule.RESUME_MIN_GAP_MS))
        assertTrue(gate.autoOlderThan(1_000L + 120_000L, NativeSyncSchedule.RESUME_MIN_GAP_MS))
        assertNull("default 5 min interval blocks a 3 min auto read",
            gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 181_000L, true, true, true, snap))
        gate.intervalMs = 2 * 60_000L
        assertNotNull("2 min cadence lets it through",
            gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 181_000L, true, true, true, snap))
    }

    @Test fun backgroundHealthJobSurvivesRebootAndFollowsTheChoice() {
        val job = java.io.File("src/main/java/com/repforge/app/HealthBackgroundServices.kt").readText()
        assertTrue(job.contains(".setPersisted(true)"))
        assertFalse(job.contains(".setPersisted(false)"))
        assertTrue(job.contains("NativeSyncSchedule.backgroundMinutes(context)"))
        assertTrue("an old non-persisted job is replaced", job.contains("pending.isPersisted && pending.intervalMillis == intervalMs"))
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    }
}
