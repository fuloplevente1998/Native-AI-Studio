package com.example

import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.ui.components.secondaryWorkoutDayTitle
import com.example.ui.components.strengthColumnLabels
import org.junit.Assert.*
import org.junit.Test

class WorkoutPresentationLabelsTest {
    @Test fun repeatedDayCodesAreHiddenWithoutLosingUserTitles() {
        assertNull(secondaryWorkoutDayTitle("A nap", "A"))
        assertNull(secondaryWorkoutDayTitle("B", "B nap"))
        assertNull(secondaryWorkoutDayTitle("A day", "A"))
        assertNull(secondaryWorkoutDayTitle("Tag B", "B"))
        assertEquals("Felsőtest – Erő", secondaryWorkoutDayTitle("A nap", "Felsőtest – Erő"))
        assertEquals("B", secondaryWorkoutDayTitle("A nap", "B"))
    }
    @Test fun loadMetadataDeterminesHandOrTotalWeightWhileLegLabelIsNotRepeated() {
        val bulgarian = ExerciseItem("bulgarian", "Bolgár guggolás", loadType = "per_hand", repUnit = "/láb")
        assertEquals("kg/kar", strengthColumnLabels(bulgarian, AppLanguage.HU).weight)
        assertEquals("ism.", strengthColumnLabels(bulgarian, AppLanguage.HU).repetitions)
        assertEquals("kg", strengthColumnLabels(bulgarian.copy(loadType = "total"), AppLanguage.HU).weight)
        assertEquals("kg", strengthColumnLabels(bulgarian.copy(loadType = "single_dumbbell"), AppLanguage.HU).weight)
        assertEquals("kg/hand", strengthColumnLabels(bulgarian, AppLanguage.EN).weight)
        assertEquals("rep.", strengthColumnLabels(bulgarian, AppLanguage.RO).repetitions)
    }
    @Test fun bodyweightAndMeasuredTimeKeepTheirOwnDisplayMeaning() {
        val body = ExerciseItem("plank", "Plank", loadType = "bodyweight", measurementType = "time", repUnit = "mp")
        assertEquals("Saját súly", strengthColumnLabels(body, AppLanguage.HU).weight)
        assertEquals("mp", strengthColumnLabels(body, AppLanguage.HU).repetitions)
        assertEquals("sec", strengthColumnLabels(body, AppLanguage.EN).repetitions)
    }
}
