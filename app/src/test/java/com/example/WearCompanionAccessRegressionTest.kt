package com.example

import com.example.ui.viewmodel.TrainPilotViewModel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearCompanionAccessRegressionTest {
    @Test fun systemConnectionDoesNotImplyInstalledTrainPilotCompanion() {
        val model = TrainPilotViewModel()
        model.updatePlatformAccess(JSONObject(), listOf("Galaxy Watch7"))
        assertTrue(model.uiState.value.watchConnected)
        assertFalse(model.uiState.value.watchAppAvailable)
        model.updatePlatformAccess(JSONObject(), listOf("Galaxy Watch7"), companionAvailable = true)
        assertTrue(model.uiState.value.watchAppAvailable)
        model.updatePlatformAccess(JSONObject(), emptyList(), companionAvailable = true)
        assertFalse(model.uiState.value.watchConnected)
        assertFalse(model.uiState.value.watchAppAvailable)
    }

    @Test fun ambiguousMultipleWatchesCannotEnableCompanionHandoff() {
        val model = TrainPilotViewModel()
        model.updatePlatformAccess(JSONObject(), listOf("Watch 1", "Watch 2"), companionAvailable = true)
        assertFalse(model.uiState.value.watchAppAvailable)
    }
}
