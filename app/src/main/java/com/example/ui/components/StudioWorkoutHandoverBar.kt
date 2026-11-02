package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

internal fun isActiveMeasurementScreen(screen: SubScreen): Boolean = screen in setOf(
    SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS)

/** Placed in Scaffold.topBar: scroll-independent and reserves space instead of covering controls. */
@Composable
internal fun StudioWorkoutHandoverBar(state: TrainPilotUiState, strings: LocalizedStrings, onToggle: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).testTag("workout_sticky_handover")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (!state.isWorkoutSessionActive) "Nincs aktív natív edzés" else if (state.watchControlPending) "Átadás visszaigazolására vár…"
                else if (state.phoneControlsWorkout) "Telefon vezérli" else "Óra vezérli / telefonos visszavétel szükséges",
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            OutlinedButton(onClick = onToggle,
                enabled = state.isWorkoutSessionActive && !state.watchControlPending && !state.isSavingWorkout && (!state.phoneControlsWorkout || state.watchAppAvailable),
                modifier = Modifier.heightIn(min = 48.dp).testTag("workout_handover_action")) {
                Text(if (state.watchControlPending) "ACK-ra vár…"
                    else if (state.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
        if (!state.watchAppAvailable && state.phoneControlsWorkout)
            Text("Nincs igazolt TrainPilot Wear OS kapcsolat", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        state.statusBannerMessage?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("workout_inline_status"))
        }
    }
}
