package com.example.ui.components

import com.example.model.SubScreen
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.restoreNavigation

/**
 * Sub screens that open as a bottom sheet over the screen they were opened from (#269).
 * The screen itself is unchanged; only its container differs. Full working surfaces (active
 * workouts, GPS map, summary, private photo gallery, editors) stay separate pages.
 */
internal val TrainPilotSheetRoutes: Set<SubScreen> = setOf(
    SubScreen.WATCH_AND_SYNC,
    SubScreen.PERMISSIONS_SCREEN,
    SubScreen.BACKUP_RESTORE,
    SubScreen.EXERCISE_DETAIL,
    SubScreen.LOG_WORKOUT_DETAIL
)

/** The route rendered underneath an open sheet route: the latest non-sheet entry of the back stack. */
internal fun TrainPilotUiState.sheetHostState(): TrainPilotUiState {
    if (currentSubScreen !in TrainPilotSheetRoutes) return this
    val host = backStack.lastOrNull { it.subScreen !in TrainPilotSheetRoutes } ?: NavRoute(currentTab)
    return restoreNavigation(host)
}

