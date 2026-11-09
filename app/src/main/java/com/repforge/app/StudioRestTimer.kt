package com.repforge.app

import android.content.Context
import android.os.SystemClock
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject
import kotlin.math.ceil

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

    fun mirrorAllowed(state: TrainPilotUiState, frame: JSONObject?): Boolean =
        state.isWorkoutSessionActive && !state.isSavingWorkout &&
            state.activeWorkoutId.isNotBlank() && frame != null &&
            frame.optBoolean("active") && frame.optString("workoutId") == state.activeWorkoutId &&
            frame.optLong("generation") == state.activeWorkoutGeneration &&
            frame.optString("timerOwner") == "watch" && !frame.optBoolean("workoutPaused") &&
            !frame.optBoolean("finalizationPending") && !frame.optBoolean("discardPending")

    private fun allowed(context: Context?, state: TrainPilotUiState): Boolean = context != null && runCatching {
        val store = PhoneWorkoutExecution.store(context)
        val current = ActiveWorkoutStore.current(context)
        (!store.restoreBlocked() && !store.commandsSuspended() &&
            allowed(state, store.snapshot()) && allowed(state, current) &&
            PhoneOwnership193.phoneAllowed(context, state.activeWorkoutId)) ||
            mirrorAllowed(state, current)
    }.getOrDefault(false)

    fun adjust(context: Context?, state: TrainPilotUiState, deltaSec: Int): TrainPilotUiState =
        if (allowed(context, state)) adjusted(state, deltaSec) else state

    fun toggle(context: Context?, state: TrainPilotUiState): TrainPilotUiState =
        if (allowed(context, state)) toggled(state) else state

    fun skip(context: Context?, state: TrainPilotUiState): TrainPilotUiState =
        if (allowed(context, state)) skipped(state) else state

    internal fun start(
        state: TrainPilotUiState,
        durationSec: Int = state.defaultRestSec,
        nowRealtimeMs: Long = safeElapsedRealtime(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): TrainPilotUiState {
        val clamped = durationSec.coerceIn(0, 600)
        if (clamped <= 0) return skipped(state)
        return state.copy(
            restTimerRemainingSec = clamped,
            restTimerRunning = true,
            restTimerEndRealtimeMs = if (nowRealtimeMs > 0L) nowRealtimeMs + clamped * 1000L else 0L,
            restTimerEndEpochMs = if (nowEpochMs > 0L) nowEpochMs + clamped * 1000L else 0L,
            restTimerLastTickRealtimeMs = nowRealtimeMs
        )
    }

    internal fun reconcile(
        state: TrainPilotUiState,
        nowRealtimeMs: Long = safeElapsedRealtime(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): TrainPilotUiState {
        if (!state.restTimerRunning) return state
        val remaining = when {
            state.restTimerEndRealtimeMs > 0L && nowRealtimeMs > 0L ->
                ceil((state.restTimerEndRealtimeMs - nowRealtimeMs).coerceAtLeast(0L) / 1000.0).toInt().coerceIn(0, 600)
            state.restTimerEndEpochMs > 0L && nowEpochMs > 0L ->
                ceil((state.restTimerEndEpochMs - nowEpochMs).coerceAtLeast(0L) / 1000.0).toInt().coerceIn(0, 600)
            else -> state.restTimerRemainingSec.coerceIn(0, 600)
        }
        if (remaining <= 0) {
            return state.copy(
                restTimerRemainingSec = 0,
                restTimerRunning = false,
                restTimerEndRealtimeMs = 0L,
                restTimerEndEpochMs = 0L,
                restTimerLastTickRealtimeMs = 0L
            )
        }
        return state.copy(
            restTimerRemainingSec = remaining,
            restTimerRunning = true,
            restTimerLastTickRealtimeMs = if (nowRealtimeMs > 0L) nowRealtimeMs else state.restTimerLastTickRealtimeMs
        )
    }

    internal fun fromRemoteRestEndAt(
        state: TrainPilotUiState,
        restEndAtEpochMs: Long,
        nowRealtimeMs: Long = safeElapsedRealtime(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): TrainPilotUiState {
        if (restEndAtEpochMs <= nowEpochMs) {
            return skipped(state)
        }
        val remaining = ceil((restEndAtEpochMs - nowEpochMs).coerceAtLeast(0L) / 1000.0).toInt().coerceIn(0, 600)
        if (remaining <= 0) return skipped(state)
        val remainingMs = restEndAtEpochMs - nowEpochMs
        return state.copy(
            restTimerRemainingSec = remaining,
            restTimerRunning = true,
            restTimerEndRealtimeMs = if (nowRealtimeMs > 0L) nowRealtimeMs + remainingMs else 0L,
            restTimerEndEpochMs = restEndAtEpochMs,
            restTimerLastTickRealtimeMs = nowRealtimeMs
        )
    }

    internal fun adjusted(
        state: TrainPilotUiState,
        deltaSec: Int,
        nowRealtimeMs: Long = safeElapsedRealtime(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): TrainPilotUiState {
        // -1 is the existing visible-screen tick. A late tick cannot consume a paused rest.
        if (deltaSec == -1 && !state.restTimerRunning) return state
        if (deltaSec == -1 && state.restTimerRunning &&
            state.restTimerEndRealtimeMs > 0L && state.restTimerLastTickRealtimeMs > 0L &&
            nowRealtimeMs - state.restTimerLastTickRealtimeMs >= 1500L
        ) {
            return reconcile(state, nowRealtimeMs, nowEpochMs)
        }
        val remaining = (state.restTimerRemainingSec.toLong() + deltaSec).coerceIn(0L, 600L).toInt()
        val running = state.restTimerRunning && remaining > 0
        val hasDeadlines = state.restTimerEndRealtimeMs > 0L || state.restTimerEndEpochMs > 0L || deltaSec != -1
        return state.copy(
            restTimerRemainingSec = remaining,
            restTimerRunning = running,
            restTimerEndRealtimeMs = if (running && hasDeadlines && nowRealtimeMs > 0L) nowRealtimeMs + remaining * 1000L else 0L,
            restTimerEndEpochMs = if (running && hasDeadlines && nowEpochMs > 0L) nowEpochMs + remaining * 1000L else 0L,
            restTimerLastTickRealtimeMs = if (running && hasDeadlines && nowRealtimeMs > 0L) nowRealtimeMs else 0L
        )
    }

    internal fun toggled(
        state: TrainPilotUiState,
        nowRealtimeMs: Long = safeElapsedRealtime(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): TrainPilotUiState {
        val nextRunning = visible(state) && !state.restTimerRunning
        return state.copy(
            restTimerRunning = nextRunning,
            restTimerEndRealtimeMs = if (nextRunning && nowRealtimeMs > 0L) nowRealtimeMs + state.restTimerRemainingSec * 1000L else 0L,
            restTimerEndEpochMs = if (nextRunning && nowEpochMs > 0L) nowEpochMs + state.restTimerRemainingSec * 1000L else 0L,
            restTimerLastTickRealtimeMs = if (nextRunning && nowRealtimeMs > 0L) nowRealtimeMs else 0L
        )
    }

    internal fun skipped(state: TrainPilotUiState): TrainPilotUiState =
        state.copy(
            restTimerRemainingSec = 0,
            restTimerRunning = false,
            restTimerEndRealtimeMs = 0L,
            restTimerEndEpochMs = 0L,
            restTimerLastTickRealtimeMs = 0L
        )

    private fun safeElapsedRealtime(): Long = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
}

