package com.example

import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.TrainPilotSheetRoutes
import com.example.ui.components.sheetHostState
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** #269: sheet routes render over the screen they were opened from; full surfaces stay pages. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheetRouteHostRegressionTest {
    @Test fun sheetRoutesAndFullPagesAreSeparated() {
        for (sheet in listOf(SubScreen.WATCH_AND_SYNC, SubScreen.PERMISSIONS_SCREEN, SubScreen.BACKUP_RESTORE,
                SubScreen.EXERCISE_DETAIL, SubScreen.LOG_WORKOUT_DETAIL)) assertTrue(sheet.name, sheet in TrainPilotSheetRoutes)
        for (page in listOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL,
                SubScreen.ACTIVE_OUTDOOR_GPS, SubScreen.WORKOUT_SUMMARY, SubScreen.LOG_ROUTE_MAP, SubScreen.PRIVATE_PHOTOS,
                SubScreen.EXERCISE_LIBRARY, SubScreen.SETTINGS_MAIN, SubScreen.NONE)) assertFalse(page.name, page in TrainPilotSheetRoutes)
    }

    @Test fun settingsStaysUnderneathItsDeviceSheets() {
        val settings = NavRoute(MainTab.HOME, SubScreen.SETTINGS_MAIN)
        val state = TrainPilotUiState(currentTab = MainTab.HOME, currentSubScreen = SubScreen.WATCH_AND_SYNC,
            backStack = listOf(NavRoute(MainTab.HOME), settings))
        val host = state.sheetHostState()
        assertEquals(SubScreen.SETTINGS_MAIN, host.currentSubScreen)
        assertEquals(MainTab.HOME, host.currentTab)
        assertEquals(state.backStack, host.backStack)
    }

    @Test fun nestedSheetsUseTheLatestFullScreenAndPagesAreUnchanged() {
        val library = NavRoute(MainTab.WORKOUT, SubScreen.EXERCISE_LIBRARY)
        val nested = TrainPilotUiState(currentTab = MainTab.WORKOUT, currentSubScreen = SubScreen.EXERCISE_DETAIL,
            backStack = listOf(library, NavRoute(MainTab.WORKOUT, SubScreen.LOG_WORKOUT_DETAIL)))
        assertEquals(SubScreen.EXERCISE_LIBRARY, nested.sheetHostState().currentSubScreen)
        val fromTab = TrainPilotUiState(currentTab = MainTab.PROGRESS, currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL)
        assertEquals(SubScreen.NONE, fromTab.sheetHostState().currentSubScreen)
        assertEquals(MainTab.PROGRESS, fromTab.sheetHostState().currentTab)
        val page = TrainPilotUiState(currentSubScreen = SubScreen.EXERCISE_LIBRARY)
        assertEquals(page, page.sheetHostState())
    }
}
