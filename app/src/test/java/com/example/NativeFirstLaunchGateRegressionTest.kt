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
            NativeFirstLaunchGate(com.example.ui.viewmodel.TrainPilotViewModel()) { Text("Home",Modifier.testTag("gated_home")) }
        } }
        awaitTag("first_run_permissions")
        compose.onNodeWithTag("first_run_permissions_continue").assertIsNotEnabled()
        compose.onNodeWithTag("first_run_permissions_skip").performClick()
        awaitTag("onboarding_welcome")
        assertEquals(false, com.repforge.app.ui.onboarding.firstRunPermissionsPending(context))
        compose.onNodeWithTag("gated_home").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_language_EN").performScrollTo().performClick()
        compose.waitUntil(30_000) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            runBlocking { store.current().language == "en" } &&
                compose.onAllNodes(hasTestTag("onboarding_skip") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("onboarding_skip").performScrollTo().performClick()
        awaitTag("gated_home")
        assertEquals("skipped",runBlocking { store.current().onboarding128 })
        assertEquals("en", runBlocking { store.current().language })
    }
    @Test fun migratedUserDoesNotReceiveFreshInstallOnboarding() {
        val store=TrainPilotSettingsStore(context)
        runBlocking {
            store.importPortableSettings(org.json.JSONObject().put("settings",
                org.json.JSONObject().put("onboarding128", "complete")))
            store.initializeOnboardingForMigration(true)
            store.markMigrationComplete()
            assertEquals("complete", store.current().onboarding128)
        }
        compose.setContent { TrainPilotTheme {
            NativeFirstLaunchGate(com.example.ui.viewmodel.TrainPilotViewModel()) { Text("Home",Modifier.testTag("gated_home")) }
        } }
        awaitTag("gated_home")
        compose.onNodeWithTag("onboarding_welcome").assertDoesNotExist()
        compose.onNodeWithTag("first_run_permissions").assertDoesNotExist()
    }

    @Test fun setupPassesOnlyTheExplicitlySelectedFeatures() {
        var requested: Set<String>? = null
        compose.setContent { TrainPilotTheme {
            com.repforge.app.ui.onboarding.FirstRunPermissionsScreen(
                "hu", listOf(
                    com.repforge.app.ui.onboarding.FirstRunPermissionOption("health_connect", "Health Connect"),
                    com.repforge.app.ui.onboarding.FirstRunPermissionOption("notifications", "Értesítések")
                ), busy = false, error = false, onContinue = { requested = it }, onSkip = {}
            )
        } }
        compose.onNodeWithTag("first_run_permissions_continue").assertIsNotEnabled()
        compose.onNodeWithTag("first_run_permission_health_connect").performClick()
        compose.onNodeWithTag("first_run_permissions_continue").performClick()
        compose.runOnIdle { assertEquals(setOf("health_connect"), requested) }
    }
}
