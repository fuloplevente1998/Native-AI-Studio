package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.HealthTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class HealthProviderUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun healthTodayAndBottomSyncShareCallbackAndKeepEachProvidersSuccessProof() {
        var syncRequests = 0
        val partialMessage = "Samsung Health: részleges hozzáférés"
        var state by mutableStateOf(TrainPilotUiState(
            permissionAccess = com.example.ui.viewmodel.StudioPermissionAccess(samsungHealthEnabled = true),
            dailyHealthSourceLabel = "Samsung Health",
            dailyHealthDate = LocalDate.now().toString(),
            dailyHealthAvailableFields = setOf("steps", "activeCalories"),
            dailySteps = 7699,
            activeCaloriesKcal = 329,
            dailyHealthLastSyncIso = "2026-11-02T22:07:00Z",
            healthLastSyncByProvider = mapOf(HealthJournalStore.SAMSUNG to "2026-11-02T22:07:00Z"),
            healthSyncPartial = true,
            healthSyncMessage = partialMessage
        ))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                { if (it == "connect") state = state.copy(expandedHealthConnect = !state.expandedHealthConnect) },
                {}, {}, { _, _ -> }, {}, { syncRequests++ }, {}, {})
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        compose.onNodeWithTag("studio_daily_health_status").assertDoesNotExist()
        compose.onNodeWithTag("daily_health_values").assertDoesNotExist()
        compose.onNodeWithText("Mai adatok • Samsung Health").assertDoesNotExist()
        listOf("Alvás", "Aktív kalória", "Pulzus", "Lépések", "Mai edzés", "Regeneráció")
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("7699").assertExists()
        compose.onNodeWithText("329 kcal").assertExists()
        compose.onNodeWithTag("health_today_header_sync").performClick()
        assertEquals(1, syncRequests)

        list.performScrollToNode(hasText("Források, óra és szinkron"))
        compose.onNodeWithText("Források, óra és szinkron").performClick()
        compose.onNodeWithTag("health_last_sync_${HealthJournalStore.SAMSUNG}")
            .performScrollTo().assertTextContains("Samsung Health • Utolsó sikeres szinkron:", substring = true)
        compose.onNodeWithTag("health_last_sync_${HealthJournalStore.HC}")
            .assertTextContains("Még nincs sikeres szinkron", substring = true)
        compose.onNodeWithTag("health_sync_button").performScrollTo().performClick()
        assertEquals(2, syncRequests)
        compose.runOnIdle { state = state.copy(healthSyncBusy = true) }
        compose.onNodeWithTag("health_sync_button").assertIsNotEnabled()
        assertEquals(2, syncRequests)
        compose.onNodeWithTag("health_provider_samsung").assertExists()
        compose.onNodeWithTag("health_sync_result").assertIsDisplayed().assertTextEquals(partialMessage)

        compose.runOnIdle { state = state.copy(healthSyncFailed = true, healthSyncPartial = false,
            healthSyncMessage = "Samsung Health: PERMISSION_DENIED") }
        compose.onNodeWithTag("health_sync_result").assertIsDisplayed().assertTextEquals("Samsung Health: PERMISSION_DENIED")
        compose.onNodeWithTag("health_connect_close").performClick()
        list.performScrollToNode(hasTestTag("health_today_header_sync"))
        compose.onNodeWithTag("health_today_header_sync").assertIsNotEnabled()
    }
    @Test fun samsungSwitchDoesNotChangeThePrimaryHealthConnectSource() {
        val selected = mutableListOf<Boolean>()
        var state by mutableStateOf(TrainPilotUiState(expandedHealthConnect = true,
            samsungGrantedCount = 0,
            healthSyncFailed = true, healthSyncMessage = "Samsung Health: PERMISSION_DENIED"))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, { _, _ -> }, {}, {}, {}, {},
                onSelectHealthSource = { selected.add(it); state = state.copy(
                    permissionAccess = state.permissionAccess.copy(samsungHealthEnabled = it)) })
        } }
        val supplement = compose.onNodeWithTag("health_provider_samsung")
        supplement.performClick().assertIsOn()
        supplement.performClick().assertIsOff()
        assertEquals(listOf(true, false), selected)
        compose.onNodeWithText("Elsődleges adatforrás").assertExists()
        compose.onNodeWithTag("health_provider_connect").assertHasNoClickAction()
        compose.onNodeWithTag("health_sync_result").assertTextEquals("Samsung Health: PERMISSION_DENIED")
        compose.runOnIdle { state = state.copy(healthSyncFailed = false, healthSyncMessage = "Health Connect: sikeres szinkron") }
        compose.onNodeWithTag("health_sync_result").assertTextEquals("Health Connect: sikeres szinkron")
    }
}
