package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.navigationRoute

/** New main-tab visits get a fresh local scroll key; child-route transitions never do. */
internal fun advanceMainTabScrollEpochs(
    previousTab: MainTab, previousSubScreen: SubScreen,
    targetTab: MainTab, targetSubScreen: SubScreen, epochs: IntArray
): IntArray {
    if (previousSubScreen != SubScreen.NONE || targetSubScreen != SubScreen.NONE || previousTab == targetTab)
        return epochs
    return epochs.copyOf().apply { this[targetTab.ordinal]++ }
}

private fun NavRoute.contentKey(epochs: IntArray): String {
    val detail = when (subScreen) {
        SubScreen.NONE -> if (tab == MainTab.LOG) "1" else if (tab == MainTab.PROGRESS) "${view?.progressMainSection ?: 0}" else ""
        SubScreen.LOG_WORKOUT_DETAIL, SubScreen.LOG_ROUTE_MAP -> view?.selectedLogWorkoutId.orEmpty()
        SubScreen.EXERCISE_DETAIL -> view?.selectedExerciseId.orEmpty()
        SubScreen.PRIVATE_PHOTOS -> view?.privatePhotoWorkoutId.orEmpty()
        SubScreen.PROGRAM_EDITOR -> "${view?.editingProgramId}:${view?.editingDayIndex}"
        else -> ""
    }
    return listOf(tab.name, subScreen.name, detail,
        if (subScreen == SubScreen.NONE) epochs[tab.ordinal].toString() else "").joinToString("|") { "${it.length}:$it" }
}

/** Keep the origin's scroll/local view state while a child is open; discard popped drafts. */
@Composable
fun TrainPilotRouteContent(state: TrainPilotUiState, content: @Composable () -> Unit) {
    val holder = rememberSaveableStateHolder()
    val known = remember { mutableSetOf<String>() }
    val route = state.navigationRoute()
    // UI-only saveable keys, not persisted settings or navigation focus. The same route
    // survives a detail/sheet/dialógus round trip; only a different main tab gets a new key.
    var previousTab by rememberSaveable { mutableStateOf(route.tab.name) }
    var previousSubScreen by rememberSaveable { mutableStateOf(route.subScreen.name) }
    var epochs by rememberSaveable { mutableStateOf(IntArray(MainTab.entries.size)) }
    val nextEpochs = advanceMainTabScrollEpochs(MainTab.valueOf(previousTab), SubScreen.valueOf(previousSubScreen),
        route.tab, route.subScreen, epochs)
    val key = route.contentKey(nextEpochs)
    val retained = (state.backStack + route).map { it.contentKey(nextEpochs) }.toSet()
    SideEffect {
        known.add(key)
        epochs = nextEpochs
        previousTab = route.tab.name
        previousSubScreen = route.subScreen.name
    }
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
