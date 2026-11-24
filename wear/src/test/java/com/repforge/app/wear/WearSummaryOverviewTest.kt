package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearSummaryOverviewTest {
    private fun set(reps: String, weight: String, done: Boolean = true) = WearSet(1, reps, weight, done, 0, 0, 0.0)
    private fun exercise(name: String, unit: String, sets: List<WearSet>) =
        WearExercise(name, name, "weight", unit, "reps", sets)
    private fun workout(exercises: List<WearExercise>) = WearWorkout(0, "p", "Program", "d", "", "2026-11-24T10:00:00Z", "w", 0, 90, 0L, exercises, dayName = "A")
    @Test fun onlyCompletedWeightedRepetitionsContribute() {
        val overview = wearSummaryOverview(workout(listOf(
            exercise("Bench", "ism.", listOf(set("8", "57,5"), set("10", "100", false))),
            exercise("Hold", "mp", listOf(set("60", "10"))),
            exercise("Bodyweight", "ism.", listOf(set("12", "")))
        )))
        assertEquals("A", overview.name)
        assertEquals(3, overview.sets)
        assertEquals(460.0, overview.volumeKg!!, 0.001)
        assertNull(overview.exercises[1].volumeKg)
        assertNull(overview.exercises[2].volumeKg)
    }
    @Test fun missingOrInvalidVolumeDoesNotAppearAsZero() {
        val overview = wearSummaryOverview(workout(listOf(exercise("A", "ism.", listOf(set("8", "NaN"), set("8", "20", false))))))
        assertNull(overview.volumeKg)
        assertEquals("–", wearSummaryVolume(overview.volumeKg))
    }
}
