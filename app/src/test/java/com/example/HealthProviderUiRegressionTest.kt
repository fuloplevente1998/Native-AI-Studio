package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.HealthTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
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
