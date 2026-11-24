package com.example.ui.screens

import com.repforge.app.ui.theme.trainPilotClickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import com.example.localization.LocalTrainPilotStrings
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementModeSwitcherRow(
    currentSubScreen: SubScreen,
    onSelectMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val modes = listOf(
        SubScreen.ACTIVE_STRENGTH to "1. ${strings.modeStrengthLabel}",
        SubScreen.ACTIVE_TIMED to "2. ${strings.modeTimedLabel}",
        SubScreen.ACTIVE_BILATERAL to "3. ${strings.modeBilateralLabel}",
        SubScreen.ACTIVE_OUTDOOR_GPS to "4. ${strings.modeOutdoorGpsLabel}",
        SubScreen.INTERVAL_SETTINGS to "5. ${strings.modeIntervalsLabel}",
        SubScreen.WORKOUT_SUMMARY to "6. ${strings.modeSummaryLabel}"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, label) ->
            val selected = currentSubScreen == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.primary else palette.surfaceElevated,
                modifier = Modifier
                    .height(48.dp)
                    .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectMode(mode) }
                    .testTag("measurement_mode_${mode.name.lowercase()}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.onPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}


@Composable
internal fun StudioRestCountdown(state: TrainPilotUiState, onAdjust: (Int) -> Unit) {
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val allowed = com.repforge.app.StudioRestTimer.allowed(state, frame) ||
        com.repforge.app.StudioRestTimer.mirrorAllowed(state, frame)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, allowed, state.restTimerRunning, state.restTimerRemainingSec) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                allowed && state.restTimerRunning && state.restTimerRemainingSec > 0
            ) {
                onAdjust(-1)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(
        state.activeWorkoutId,
        state.activeWorkoutGeneration,
        allowed,
        state.restTimerRunning,
        state.restTimerRemainingSec,
        state.restTimerEndRealtimeMs
    ) {
        if (allowed && state.restTimerRunning && state.restTimerRemainingSec > 0) {
            val nowRealtime = android.os.SystemClock.elapsedRealtime()
            if (state.restTimerEndRealtimeMs > 0L &&
                state.restTimerLastTickRealtimeMs > 0L &&
                nowRealtime - state.restTimerLastTickRealtimeMs >= 1500L
            ) {
                onAdjust(-1)
                return@LaunchedEffect
            }
            delay(1000L)
            onAdjust(-1)
        }
    }
}
