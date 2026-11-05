package com.example

import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.StudioRestTimer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StudioRestTimerRegressionTest {
    @Test fun expirationHidesTheStripAndCannotRestartAnEmptyRest() {
        val active = TrainPilotUiState(restTimerRemainingSec = 1, restTimerRunning = true)
        val expired = StudioRestTimer.adjusted(active, -1)
        assertEquals(0, expired.restTimerRemainingSec)
        assertFalse(expired.restTimerRunning)
        assertFalse(StudioRestTimer.visible(expired))
        assertFalse(StudioRestTimer.toggled(expired).restTimerRunning)
        assertEquals(expired, StudioRestTimer.adjusted(expired, -1))
    }

    @Test fun pausedRestKeepsItsSecondsButManualAdjustmentAndSkipStillWork() {
        val paused = TrainPilotUiState(restTimerRemainingSec = 8, restTimerRunning = false)
        assertEquals(paused, StudioRestTimer.adjusted(paused, -1))
        val adjusted = StudioRestTimer.adjusted(paused, 15)
        assertEquals(23, adjusted.restTimerRemainingSec)
        assertFalse(adjusted.restTimerRunning)
        assertTrue(StudioRestTimer.toggled(adjusted).restTimerRunning)
        assertFalse(StudioRestTimer.visible(StudioRestTimer.skipped(adjusted)))
        assertFalse(StudioRestTimer.adjusted(paused.copy(restTimerRunning = true), -15).restTimerRunning)
    }

    @Test fun stalePausedForeignAndFinalizingSessionsCannotTick() {
        val state = TrainPilotUiState(isWorkoutSessionActive = true, activeWorkoutId = "rest-test",
            activeWorkoutGeneration = 8, phoneControlsWorkout = true)
        fun frame() = JSONObject().put("active", true).put("workoutId", "rest-test")
            .put("generation", 8).put("timerOwner", "phone")
        assertTrue(StudioRestTimer.allowed(state, frame()))
        for (invalid in listOf(frame().put("generation", 7), frame().put("workoutId", "old"),
            frame().put("workoutPaused", true), frame().put("timerOwner", "watch"),
            frame().put("finalizationPending", true), frame().put("discardPending", true),
            frame().put("handoff193", JSONObject().put("state", "offered")))) {
            assertFalse(StudioRestTimer.allowed(state, invalid))
        }
        assertFalse(StudioRestTimer.allowed(state.copy(watchControlPending = true), frame()))
        assertFalse(StudioRestTimer.allowed(state.copy(isSavingWorkout = true), frame()))
    }
}
