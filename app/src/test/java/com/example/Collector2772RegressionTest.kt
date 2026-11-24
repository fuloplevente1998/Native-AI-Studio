package com.example

import com.example.ui.viewmodel.TrainPilotViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.viewmodel.openQuickWorkoutSheet
import com.example.ui.viewmodel.selectQuickWorkoutTab
import com.example.ui.viewmodel.toggleQuickWorkoutSheet

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Collector2772RegressionTest {
    @Test fun workoutTilesOpenTheQuickSheetOnTheirTabAndTheQuickButtonKeepsTheLastTab() {
        val model = TrainPilotViewModel()
        model.openQuickWorkoutSheet(1)
        assertTrue(model.uiState.value.showQuickWorkoutSheet)
        assertEquals(1, model.uiState.value.quickWorkoutTab)
        model.selectQuickWorkoutTab(2)
        model.toggleQuickWorkoutSheet(false)
        assertFalse(model.uiState.value.showQuickWorkoutSheet)
        // "Gyors edzés" button reopens on the last used tab; no route change happens.
        model.toggleQuickWorkoutSheet(true)
        assertEquals(2, model.uiState.value.quickWorkoutTab)
        assertEquals(com.example.model.SubScreen.NONE, model.uiState.value.currentSubScreen)
        model.openQuickWorkoutSheet(9)
        assertEquals(2, model.uiState.value.quickWorkoutTab)
    }
}
