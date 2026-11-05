package com.repforge.app

import android.content.Context
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/** Presentation state for the existing Studio rest strip; never records or advances a set. */
internal object StudioRestTimer {
    fun visible(state: TrainPilotUiState): Boolean = state.restTimerRemainingSec > 0

    fun allowed(state: TrainPilotUiState, frame: JSONObject?): Boolean =
        state.isWorkoutSessionActive && !state.isSavingWorkout && state.phoneControlsWorkout &&
            !state.watchControlPending && state.activeWorkoutId.isNotBlank() && frame != null &&
            frame.optBoolean("active") && frame.optString("workoutId") == state.activeWorkoutId &&
            frame.optLong("generation") == state.activeWorkoutGeneration &&
            frame.optString("timerOwner", "phone") == "phone" && !frame.optBoolean("workoutPaused") &&
            !frame.optBoolean("finalizationPending") && !frame.optBoolean("discardPending") &&
            !WorkoutOwnership193.pending(frame) && !NativePhoneReturnInstallation.requiresInstallation(frame)

    private fun allowed(context: Context?, state: TrainPilotUiState): Boolean = context != null && runCatching {
        val store = PhoneWorkoutExecution.store(context)
        !store.restoreBlocked() && !store.commandsSuspended() &&
            allowed(state, store.snapshot()) && allowed(state, ActiveWorkoutStore.current(context)) &&
            PhoneOwnership193.phoneAllowed(context, state.activeWorkoutId)
    }.getOrDefault(false)

    fun adjust(context: Context?, state: TrainPilotUiState, deltaSec: Int): TrainPilotUiState =
        if (allowed(context, state)) adjusted(state, deltaSec) else state

    fun toggle(context: Context?, state: TrainPilotUiState): TrainPilotUiState =
        if (allowed(context, state)) toggled(state) else state

    fun skip(context: Context?, state: TrainPilotUiState): TrainPilotUiState =
        if (allowed(context, state)) skipped(state) else state

    internal fun adjusted(state: TrainPilotUiState, deltaSec: Int): TrainPilotUiState {
        // -1 is the existing visible-screen tick. A late tick cannot consume a paused rest.
        if (deltaSec == -1 && !state.restTimerRunning) return state
        val remaining = (state.restTimerRemainingSec.toLong() + deltaSec).coerceIn(0L, 600L).toInt()
        return state.copy(restTimerRemainingSec = remaining,
            restTimerRunning = state.restTimerRunning && remaining > 0)
    }

    internal fun toggled(state: TrainPilotUiState): TrainPilotUiState =
        state.copy(restTimerRunning = visible(state) && !state.restTimerRunning)

    internal fun skipped(state: TrainPilotUiState): TrainPilotUiState =
        state.copy(restTimerRemainingSec = 0, restTimerRunning = false)
}
