package com.example.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.viewmodel.TrainPilotViewModel
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.updateHealthSyncStatus
import com.example.ui.viewmodel.reloadHealthJournal
import com.repforge.app.ui.health.HealthDashboardViewModel
import com.repforge.app.ui.health.HealthDashboardUiState
import org.json.JSONObject

/** App-root health lifecycle and effects, separate from navigation. */
@Composable
internal fun StudioHealthRootEffects(viewModel: TrainPilotViewModel, uiState: TrainPilotUiState,
    nativeHealth: HealthDashboardViewModel, health: HealthDashboardUiState, activity: Activity?): Map<String, JSONObject> {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, activity) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ ->
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) nativeHealth.appVisible(activity)
            else nativeHealth.appHidden(activity)
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) nativeHealth.appVisible(activity)
        onDispose { lifecycle.removeObserver(observer); nativeHealth.appHidden(activity) }
    }
    // 2805: a saved workout pulls fresh Health data (calories, heart rate) right away, without waiting for the timer.
    var workoutSaveSeen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(uiState.isSavingWorkout, uiState.isWorkoutSessionActive) {
        if (workoutSaveSeen && !uiState.isSavingWorkout && !uiState.isWorkoutSessionActive) nativeHealth.afterWorkoutSaved()
        workoutSaveSeen = uiState.isSavingWorkout
    }
    androidx.compose.runtime.LaunchedEffect(health.healthRefreshing, health.syncMessage, health.syncFailed, health.syncPartial, health.syncOutcome, health.syncCancelled) {
        viewModel.updateHealthSyncStatus(health.healthRefreshing, partial = health.syncPartial,
            failed = health.syncFailed, message = health.syncMessage,
            outcome = if (health.syncCancelled) "CANCELLED" else health.syncOutcome)
    }
    androidx.compose.runtime.LaunchedEffect(health.refreshRevision, health.provider) {
        viewModel.reloadHealthJournal()
    }

    return rememberWorkoutHealthPreviews(
        enabled = uiState.isWorkoutSessionActive && !uiState.isSavingWorkout &&
            com.example.ui.components.isActiveMeasurementScreen(uiState.currentSubScreen) &&
            health.autoOnOpen && !health.healthRefreshing,
        workoutId = uiState.activeWorkoutId, generation = uiState.activeWorkoutGeneration,
        hcEnabled = uiState.permissionHealthConnectEnabled, samsungEnabled = uiState.permissionSamsungHealthEnabled)
}
