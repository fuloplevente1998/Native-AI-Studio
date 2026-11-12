package com.example

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.StudioRestTimer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test fun backgroundElapsedTimeReconcilesAccuratelyAfterShortAndLongBackgroundIntervals() {
        val t0Realtime = 10_000L
        val t0Epoch = 1_700_000_000_000L
        val started = StudioRestTimer.start(
            state = TrainPilotUiState(defaultRestSec = 90),
            durationSec = 90,
            nowRealtimeMs = t0Realtime,
            nowEpochMs = t0Epoch
        )
        assertTrue(started.restTimerRunning)
        assertEquals(90, started.restTimerRemainingSec)
        assertEquals(t0Realtime + 90_000L, started.restTimerEndRealtimeMs)
        assertEquals(t0Epoch + 90_000L, started.restTimerEndEpochMs)

        // 1. Short ~2-second background period must advance the countdown by 2s (not freeze!)
        val after2s = StudioRestTimer.adjusted(
            state = started,
            deltaSec = -1,
            nowRealtimeMs = t0Realtime + 2_100L,
            nowEpochMs = t0Epoch + 2_100L
        )
        assertTrue(after2s.restTimerRunning)
        assertEquals(88, after2s.restTimerRemainingSec)

        // 2. Longer 45-second background period must reconcile to 43s remaining
        val after47s = StudioRestTimer.adjusted(
            state = after2s,
            deltaSec = -1,
            nowRealtimeMs = t0Realtime + 47_100L,
            nowEpochMs = t0Epoch + 47_100L
        )
        assertTrue(after47s.restTimerRunning)
        assertEquals(43, after47s.restTimerRemainingSec)

        // 3. Background period past the deadline finishes the countdown cleanly without negative numbers
        val expiredInBg = StudioRestTimer.adjusted(
            state = after47s,
            deltaSec = -1,
            nowRealtimeMs = t0Realtime + 95_000L,
            nowEpochMs = t0Epoch + 95_000L
        )
        assertEquals(0, expiredInBg.restTimerRemainingSec)
        assertFalse(expiredInBg.restTimerRunning)
        assertEquals(0L, expiredInBg.restTimerEndRealtimeMs)
        assertEquals(0L, expiredInBg.restTimerEndEpochMs)
    }

    @Test fun remoteWatchRestEndAtStartsAndUpdatesPhoneRestTimerInBothPhoneAndWatchOwnedModes() {
        val nowRealtime = 50_000L
        val nowEpoch = 1_700_000_050_000L
        val mirrored = StudioRestTimer.fromRemoteRestEndAt(
            state = TrainPilotUiState(restTimerRemainingSec = 90, restTimerRunning = false),
            restEndAtEpochMs = nowEpoch + 75_000L,
            nowRealtimeMs = nowRealtime,
            nowEpochMs = nowEpoch
        )
        assertTrue(mirrored.restTimerRunning)
        assertEquals(75, mirrored.restTimerRemainingSec)
        assertEquals(nowRealtime + 75_000L, mirrored.restTimerEndRealtimeMs)
        assertEquals(nowEpoch + 75_000L, mirrored.restTimerEndEpochMs)

        val watchState = TrainPilotUiState(
            isWorkoutSessionActive = true,
            activeWorkoutId = "watch-rest",
            activeWorkoutGeneration = 1,
            phoneControlsWorkout = false
        )
        val watchFrame = JSONObject()
            .put("active", true)
            .put("workoutId", "watch-rest")
            .put("generation", 1)
            .put("timerOwner", "watch")
        assertTrue(StudioRestTimer.mirrorAllowed(watchState, watchFrame))
    }

    @Test fun repeatedSmallSchedulerDelaysDoNotMoveTheRestDeadline() {
        val start = 10_000L
        var state = StudioRestTimer.start(TrainPilotUiState(), 90, start, 1_000_000L)
        val deadline = state.restTimerEndRealtimeMs
        repeat(65) { i ->
            state = StudioRestTimer.adjusted(state, -1, start + (i + 1) * 1400L, 1_000_000L + (i + 1) * 1400L)
            if (state.restTimerRunning) assertEquals(deadline, state.restTimerEndRealtimeMs)
        }
        assertEquals(0, state.restTimerRemainingSec)
        assertFalse(state.restTimerRunning)
    }

    @Test fun pauseAndManualAdjustmentReconcileTimeBeforeChangingTheTimer() {
        val started = StudioRestTimer.start(TrainPilotUiState(), 90, 10_000L, 1_000_000L)
        val paused = StudioRestTimer.toggled(started, 40_000L, 1_030_000L)
        assertEquals(60, paused.restTimerRemainingSec)
        assertFalse(paused.restTimerRunning)
        val adjusted = StudioRestTimer.adjusted(started, 15, 40_000L, 1_030_000L)
        assertEquals(75, adjusted.restTimerRemainingSec)
        assertEquals(115_000L, adjusted.restTimerEndRealtimeMs)
    }
}
