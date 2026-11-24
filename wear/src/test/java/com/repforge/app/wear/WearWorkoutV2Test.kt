package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearWorkoutV2Test {
    private val set = MiniJournalSet(2, "8", 57.5, null, null, null)
    private val exercise = MiniJournalExercise("Bench press", listOf(set))
    private fun workout(id: String, started: String, exercises: List<MiniJournalExercise>) =
        MiniJournalWorkout(id, started, started, "A", false, 60, exercises, null, null)

    @Test fun previousSetUsesLatestMatchingJournalSetOnly() {
        val old = workout("old", "2026-11-20T10:00:00Z", listOf(exercise))
        val current = workout("current", "2026-11-24T10:00:00Z", listOf(exercise.copy(sets = listOf(set.copy(weight = 70.0)))))
        assertEquals("57.5 kg × 8", wearPreviousSetV2(listOf(current, old), "current", "Bench press", 2))
        assertNull(wearPreviousSetV2(listOf(old), "current", "Other exercise", 2))
        assertNull(wearPreviousSetV2(listOf(old), "current", "Bench press", 1))
        assertNull(wearPreviousSetV2(listOf(old.copy(exercises = listOf(exercise, exercise))), "current", "Bench press", 2))
    }
}
