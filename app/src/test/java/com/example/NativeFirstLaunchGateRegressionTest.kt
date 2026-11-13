package com.example

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.onboarding.NativeFirstLaunchGate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NativeFirstLaunchGateRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun isolateSettings() = runBlocking {
        // DataStore's delegate may survive Robolectric application recreation.
        // Reset only this test sandbox, through the existing settings API.
        TrainPilotSettingsStore(context).resetAllSettingsForEraseLocal()
    }
    private fun awaitTag(tag: String) {
        compose.waitUntil(30_000) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun firstRunShowsWelcomeAndOnlyACommittedSkipOpensHome() {
        val store = TrainPilotSettingsStore(context)
        runBlocking {
            store.initializeOnboardingForMigration(false)
            store.markMigrationComplete()
            assertEquals("welcome", store.current().onboarding128)
        }
        compose.setContent { TrainPilotTheme {
            NativeFirstLaunchGate { Text("Home",Modifier.testTag("gated_home")) }
        } }
        awaitTag("onboarding_welcome")
        compose.onNodeWithTag("gated_home").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_skip").performClick()
        awaitTag("gated_home")
        assertEquals("skipped",runBlocking { store.current().onboarding128 })
    }
    @Test fun migratedUserDoesNotReceiveFreshInstallOnboarding() {
        val store=TrainPilotSettingsStore(context)
        runBlocking {
            store.importPortableSettings(org.json.JSONObject().put("onboarding128", "complete"))
            store.initializeOnboardingForMigration(true)
            store.markMigrationComplete()
            assertEquals("complete", store.current().onboarding128)
        }
        compose.setContent { TrainPilotTheme {
            NativeFirstLaunchGate { Text("Home",Modifier.testTag("gated_home")) }
        } }
        awaitTag("gated_home")
        compose.onNodeWithTag("onboarding_welcome").assertDoesNotExist()
    }
}
