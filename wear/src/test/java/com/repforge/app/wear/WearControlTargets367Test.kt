package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearControlTargets367Test {
    private val now = 100000L
    private val exercise = WearExercise("ex", "Bench", "weight", "ism.", "reps",
        listOf(WearSet(1, "8", "50", false, 0, 0, 0.0, setId = "s")))
    private val workout = WearWorkout(3, "p", "Program", "d", "", "2026-11-24T10:00:00Z", "w",
        0, 90, now + 30000, listOf(exercise), ownershipProtocol193 = 1, ownerEpoch193 = 2,
        pauseRevision = 7, generation = 9, timerOwner = "watch")

    @Test fun targetCapturesCurrentDeadlineAndOwnerIdentity() {
        val target = wearRestTarget367(workout, now)!!
        assertEquals(now + 30000, target.expectedDeadline)
        assertEquals("s", target.setId)
        assertEquals(2L, target.workout.ownerEpoch)
        assertEquals(7L, target.workout.pauseRevision)
        assertNotEquals(target, wearRestTarget367(workout.copy(restEndAt = now + 45000), now))
        assertNotEquals(target, wearRestTarget367(workout.copy(ownerEpoch193 = 3), now))
    }
    @Test fun stalePausedAndUnsupportedTargetsCannotAdjust() {
        assertNull(wearRestTarget367(workout.copy(restEndAt = now), now))
        assertNull(wearRestTarget367(workout.copy(workoutPaused = true), now))
        assertNull(wearRestTarget367(workout.copy(ownershipProtocol193 = 0), now))
        assertNull(wearRestTarget367(workout.copy(timerOwner = "unknown"), now))
        assertNull(wearPauseTarget367(workout.copy(handoff193 = """{"state":"prepared"}""")))
    }
    @Test fun phoneRestAdjustmentRules() {
        assertEquals(0, wearRestAdjustedSeconds367(5, -15))
        assertEquals(600, wearRestAdjustedSeconds367(595, 15))
        assertEquals(45, wearRestAdjustedSeconds367(30, 15))
        assertNull(wearRestAdjustedSeconds367(30, 1))
        assertNull(wearRestAdjustedSeconds367(0, 15))
    }
}
