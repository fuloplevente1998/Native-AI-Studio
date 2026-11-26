package com.example.ui.components

import com.example.model.SubScreen
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.restoreNavigation

/**
 * Sub screens that open as a bottom sheet over the screen they were opened from (#269).
 * The screen itself is unchanged; only its container differs. Full working surfaces (active
 * workouts, GPS map, summary, editors) stay separate pages.
 */
internal val TrainPilotSheetRoutes: Set<SubScreen> = setOf(
    SubScreen.WATCH_AND_SYNC,
    SubScreen.PERMISSIONS_SCREEN,
    SubScreen.BACKUP_RESTORE,
    SubScreen.EXERCISE_DETAIL,
    // 2773: the exercise library opens as a sheet from every entry (active workout swap,
    // programs list, program editor), like the Quick workout tabs.
    SubScreen.EXERCISE_LIBRARY,
    SubScreen.LOG_WORKOUT_DETAIL,
    SubScreen.PRIVATE_PHOTOS
)

/** The route rendered underneath an open sheet route: the latest non-sheet entry of the back stack. */
internal fun TrainPilotUiState.sheetHostState(): TrainPilotUiState {
    if (currentSubScreen !in TrainPilotSheetRoutes) return this
    val host = backStack.lastOrNull { it.subScreen !in TrainPilotSheetRoutes } ?: NavRoute(currentTab)
    return restoreNavigation(host)
}

