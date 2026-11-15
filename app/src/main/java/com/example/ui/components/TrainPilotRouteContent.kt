package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.navigationRoute

private fun NavRoute.contentKey(): String {
    val detail = when (subScreen) {
        SubScreen.NONE -> if (tab == MainTab.LOG) "1" else if (tab == MainTab.PROGRESS) "${view?.progressMainSection ?: 0}" else ""
        SubScreen.LOG_WORKOUT_DETAIL, SubScreen.LOG_ROUTE_MAP -> view?.selectedLogWorkoutId.orEmpty()
        SubScreen.EXERCISE_DETAIL -> view?.selectedExerciseId.orEmpty()
        SubScreen.PRIVATE_PHOTOS -> view?.privatePhotoWorkoutId.orEmpty()
        SubScreen.PROGRAM_EDITOR -> "${view?.editingProgramId}:${view?.editingDayIndex}"
        else -> ""
    }
    return listOf(tab.name, subScreen.name, detail).joinToString("|") { "${it.length}:$it" }
}

/** Keep the origin's scroll/local view state while a child is open; discard popped drafts. */
@Composable
fun TrainPilotRouteContent(state: TrainPilotUiState, content: @Composable () -> Unit) {
    val holder = rememberSaveableStateHolder()
    val known = remember { mutableSetOf<String>() }
    val route = state.navigationRoute()
    val key = route.contentKey()
    val retained = (state.backStack + route).map { it.contentKey() }.toSet()
    SideEffect { known.add(key) }
    LaunchedEffect(retained) {
        val popped = known.filterNot { it in retained }
        popped.forEach(holder::removeState)
        known.removeAll(popped.toSet())
    }
    // Native execution owns live values and transient set editors; do not cache those drafts.
    if (isActiveMeasurementScreen(state.currentSubScreen) || state.currentSubScreen == SubScreen.WORKOUT_SUMMARY)
        content()
    else holder.SaveableStateProvider(key, content)
}
