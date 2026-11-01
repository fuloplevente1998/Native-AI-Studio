package com.example

import com.example.model.ExerciseSetItem
import com.example.model.StudioWorkoutJournalDisplay
import org.junit.Assert.*
import org.junit.Test

class StudioWorkoutJournalDisplayTest {
    private fun set(weight: Float = 0f, reps: Int = 17, distance: Double? = null) =
        ExerciseSetItem(setNumber = 1, weightKg = weight, reps = reps, rir = 0,
            completed = true, distanceMeters = distance)

    @Test fun cardioNeverShowsKilogramsOrRepetitions() {
        val result = StudioWorkoutJournalDisplay.setLabel(set(), "distance", "km", true)
        assertEquals("Nincs rögzített távolság", result)
        assertFalse(result.contains("kg"))
        assertFalse(result.contains("17"))
    }

    @Test fun gpsDistanceUsesRecordedMeters() {
        assertEquals("1,23 km", StudioWorkoutJournalDisplay.setLabel(set(distance = 1234.0), "distance", "km", true))
    }

    @Test fun strengthStillShowsRealLoadAndRepetitions() {
        assertEquals("42 kg × 8", StudioWorkoutJournalDisplay.setLabel(set(weight = 42f, reps = 8), "reps", "rep", false))
    }

    @Test fun bodyweightAndTimeHaveTheirOwnUnits() {
        assertEquals("17 ismétlés (saját testsúly)", StudioWorkoutJournalDisplay.setLabel(set(), "reps", "rep", false))
        assertEquals("17 mp", StudioWorkoutJournalDisplay.setLabel(set(), "time", "sec", false))
    }
}
