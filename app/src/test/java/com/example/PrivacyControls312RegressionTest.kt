package com.example

import android.app.Application
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.StudioPlatformAccess
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativePrivacyControls
import com.repforge.app.HealthConnectRepository
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.repforge.app.exportSnapshot
import com.repforge.app.healthDay

/** #312/13: a system permission is not an app opt-in; OFF never erases health history. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacyControls312RegressionTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test fun fourOptInsSurviveARecreatedContextAndAreNotSystemPermissionAliases() {
        val features = listOf(NativePrivacyControls.GPS, NativePrivacyControls.NOTIFICATIONS,
            NativePrivacyControls.HEALTH_CONNECT, NativePrivacyControls.SAMSUNG_HEALTH)
        try {
            features.forEach { NativePrivacyControls.set(app, it, false) }
            // New ContextWrapper has no Compose/ViewModel state, yet sees the durable values.
            val recreatedContext = ContextWrapper(app)
            features.forEach { assertFalse(NativePrivacyControls.enabled(recreatedContext, it)) }
            val snapshot = StudioPlatformAccess.snapshot(recreatedContext)
            assertFalse(snapshot.getBoolean("gps"))
            assertFalse(snapshot.getBoolean("notifications"))
            assertFalse(snapshot.getBoolean("healthConnect"))
            assertFalse(snapshot.getBoolean("samsungHealth"))
            features.forEach { NativePrivacyControls.set(app, it, true) }
            features.forEach { assertTrue(NativePrivacyControls.enabled(recreatedContext, it)) }
        } finally {
            features.forEach { NativePrivacyControls.set(app, it, true) }
        }
    }

    @Test fun directIntegrationsRequireOptInAndRetainTheExplicitChoice() {
        assertTrue(NativePrivacyControls.enabled(app, NativePrivacyControls.HEALTH_CONNECT))
        for (feature in listOf(NativePrivacyControls.SAMSUNG_HEALTH, NativePrivacyControls.TRAINPILOT_WEAR)) {
            assertFalse(NativePrivacyControls.enabled(app, feature))
            NativePrivacyControls.set(app, feature, true)
            assertTrue(NativePrivacyControls.enabled(ContextWrapper(app), feature))
            NativePrivacyControls.set(app, feature, false)
            assertFalse(NativePrivacyControls.enabled(ContextWrapper(app), feature))
        }
    }

    @Test fun disablingHealthProvidersLeavesTheAlreadyStoredJournalUntouched() {
        val journal = HealthJournalStore.get(app)
        val day = "2026-11-18"
        journal.healthDay(day, JSONObject().put("source", "@aggregate")
            .put("steps", 37).put("activityOrigin", "recorded"), journal.generation())
        val before = journal.exportSnapshot().getJSONArray("days").toString()
        try {
            NativePrivacyControls.set(app, NativePrivacyControls.HEALTH_CONNECT, false)
            NativePrivacyControls.set(app, NativePrivacyControls.SAMSUNG_HEALTH, false)
            assertEquals(before, journal.exportSnapshot().getJSONArray("days").toString())
        } finally {
            NativePrivacyControls.set(app, NativePrivacyControls.HEALTH_CONNECT, true)
            NativePrivacyControls.set(app, NativePrivacyControls.SAMSUNG_HEALTH, true)
        }
    }

    @Test fun workoutWriteOptInCannotBypassTheDisabledProviderAndSurvivesItsToggle() = runBlocking {
        val feature = NativePrivacyControls.HEALTH_CONNECT
        val settings = TrainPilotSettingsStore(app, publishTheme = {})
        val oldWrite = settings.current().autoWriteWorkoutToHealthConnect
        try {
            settings.setAutoWriteWorkoutToHealthConnect(true)
            NativePrivacyControls.set(app, feature, false)
            val status = StudioPlatformAccess.snapshot(app)
            assertFalse(status.getBoolean("healthConnect"))
            assertNotNull(status.get("healthReadGranted"))
            assertNotNull(status.get("healthGranted"))
            assertNotNull(status.get("healthTotal"))
            assertTrue(settings.current().autoWriteWorkoutToHealthConnect)
            val failure = runCatching {
                HealthConnectRepository.writeWorkoutSuspend(app, "2026-11-18T09:00:00Z",
                    "2026-11-18T10:00:00Z", "Test workout")
            }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
            NativePrivacyControls.set(app, feature, true)
            assertTrue(settings.current().autoWriteWorkoutToHealthConnect)
        } finally {
            NativePrivacyControls.set(app, feature, true)
            settings.setAutoWriteWorkoutToHealthConnect(oldWrite)
        }
    }
}
