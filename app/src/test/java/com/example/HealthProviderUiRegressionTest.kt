package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.ui.components.LocalSyncDailyHealth
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

    @Test fun healthTodayShowsSixTilesWithoutSummaryAndPreservesSelectedProviderSyncDetails() {
        var requestedProvider = ""
        val partialMessage = "Samsung Health: részleges hozzáférés"
        var state by mutableStateOf(TrainPilotUiState(
            expandedHealthConnect = true,
            primaryHealthSourceSamsung = true,
            dailyHealthSourceLabel = "Samsung Health",
            dailyHealthDate = LocalDate.now().toString(),
            dailyHealthAvailableFields = setOf("steps", "activeCalories"),
            dailySteps = 7699,
            activeCaloriesKcal = 329,
            dailyHealthLastSyncIso = "2026-11-02T22:07:00Z",
            healthSyncPartial = true,
            healthSyncMessage = partialMessage
        ))
        compose.setContent {
            CompositionLocalProvider(LocalSyncDailyHealth provides { requestedProvider = it }) {
                TrainPilotTheme {
                    HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                        {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
                }
            }
        }
        val list = compose.onNodeWithTag("health_tab_screen")
        compose.onNodeWithTag("studio_daily_health_status").assertDoesNotExist()
        compose.onNodeWithTag("daily_health_values").assertDoesNotExist()
        compose.onNodeWithText("Mai adatok • Samsung Health").assertDoesNotExist()
        listOf("Alvás", "Aktív kalória", "Pulzus", "Lépések", "Mai edzés", "Regeneráció")
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("7699").assertExists()
        compose.onNodeWithText("329 kcal").assertExists()
        compose.onNodeWithTag("health_today_header_sync").performClick()
        assertEquals(HealthJournalStore.SAMSUNG, requestedProvider)

        list.performScrollToNode(hasTestTag("health_selected_provider_last_sync"))
        compose.onNodeWithTag("health_selected_provider_last_sync")
            .assertTextContains("Kiválasztott forrás: Samsung Health • Utolsó sikeres szinkron:", substring = true)
        compose.onNodeWithTag("health_provider_samsung").assertExists()
        list.performScrollToNode(hasTestTag("health_sync_result"))
        compose.onNodeWithTag("health_sync_result").assertTextEquals(partialMessage)

        compose.runOnIdle { state = state.copy(healthSyncFailed = true, healthSyncPartial = false,
            healthSyncMessage = "Samsung Health: PERMISSION_DENIED") }
        compose.onNodeWithTag("health_sync_result").assertTextEquals("Samsung Health: PERMISSION_DENIED")
    }
    @Test fun selectingCurrentSourceIsIdempotentAndSyncResultPersistsWithoutFakeGrantCounts() {
        val selected = mutableListOf<Boolean>()
        var state by mutableStateOf(TrainPilotUiState(expandedHealthConnect = true,
            primaryHealthSourceSamsung = true, samsungGrantedCount = 0,
            healthSyncFailed = true, healthSyncMessage = "Samsung Health: PERMISSION_DENIED"))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, { _, _ -> }, {}, {}, {}, {},
                onSelectHealthSource = { selected.add(it); state = state.copy(primaryHealthSourceSamsung = it) })
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_provider_samsung"))
        compose.onNodeWithTag("health_provider_samsung").performClick()
        compose.onNodeWithTag("health_provider_samsung").performClick()
        assertEquals(listOf(true, true), selected)
        compose.onNodeWithText("● Közvetlen (1/2)").assertDoesNotExist()
        compose.onNodeWithTag("health_provider_connect").performClick()
        assertEquals(false, selected.last())
        list.performScrollToNode(hasTestTag("health_sync_result"))
        compose.onNodeWithTag("health_sync_result").assertTextEquals("Samsung Health: PERMISSION_DENIED")
        compose.runOnIdle { state = state.copy(healthSyncFailed = false, healthSyncMessage = "Health Connect: sikeres szinkron") }
        compose.onNodeWithTag("health_sync_result").assertTextEquals("Health Connect: sikeres szinkron")
    }
}
