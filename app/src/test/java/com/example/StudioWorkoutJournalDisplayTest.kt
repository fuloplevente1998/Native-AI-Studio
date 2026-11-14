package com.example

import com.example.model.AppLanguage
import com.example.model.LoggedWorkoutEntry
import com.example.model.ExerciseSetItem
import com.example.model.StudioWorkoutJournalDisplay
import org.junit.Assert.*
import org.junit.Test

class StudioWorkoutJournalDisplayTest {
    @Test fun savedDayLabelsNeverInferMissingHistoryOrRewriteCustomTitles() {
        val old = LoggedWorkoutEntry("old", titleHu = "Otthoni A/B – Alap")
        assertEquals("Otthoni A/B – Alap", old.localizedTitle(AppLanguage.HU))
        assertEquals("Otthoni A/B – Alap • B nap", old.copy(programId = "custom", dayId = "b")
            .localizedTitle(AppLanguage.HU))
        val builtin = old.copy(programId = "home-basic", dayId = "b")
        assertEquals("Otthoni A/B • B nap", builtin.localizedTitle(AppLanguage.HU))
        assertEquals("Home A/B • Day B", builtin.localizedTitle(AppLanguage.EN))
        assertEquals("Zuhause A/B • Tag B", builtin.localizedTitle(AppLanguage.DE))
        assertEquals("Acasă A/B • Ziua B", builtin.localizedTitle(AppLanguage.RO))
        assertEquals("Saját név • B nap", builtin.copy(titleHu = "Saját név").localizedTitle(AppLanguage.HU))
        assertEquals("Gyors edzés", LoggedWorkoutEntry("quick", titleHu = "Gyors edzés").localizedTitle(AppLanguage.HU))
    }

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
