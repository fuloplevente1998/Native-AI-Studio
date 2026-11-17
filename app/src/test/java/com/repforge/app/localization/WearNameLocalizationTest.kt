package com.repforge.app.localization

import com.repforge.app.data.db.ExerciseLibraryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WearNameLocalizationTest {
    private val catalog = ExerciseLibraryEntity(
        id = "squat", nameHu = "Guggolás", nameEn = "Squat", nameDe = "Kniebeuge", nameRo = "Genuflexiune",
        loadType = "weight", repUnit = "reps", measurementType = "reps", defaultSets = 3,
        defaultReps = "10", defaultWeight = 0.0, defaultRestSeconds = 60, primaryMuscle = "legs",
        muscleGroupsJson = "[]", notesHu = "", notesEn = "", notesDe = "", notesRo = "",
        videoUrl = null, videoProvider = null, custom = false, setRir229 = false, rawJson = "{}"
    )

    @Test fun resolvesFourLanguagesAndFallbacks() {
        val resolve = { lang: String -> WearNameLocalization.name(lang, "HU", "EN", "DE", "RO") }
        assertEquals("HU", resolve("hu"))
        assertEquals("EN", resolve("EN-gb"))
        assertEquals("DE", resolve("de_DE"))
        assertEquals("RO", resolve("ro"))
        assertEquals("EN", WearNameLocalization.name("de", "HU", "EN", "", "RO"))
        assertEquals("HU", WearNameLocalization.name("de", "HU", "", "", ""))
        assertEquals("HU", resolve("unknown"))
    }

    @Test fun historicalBuiltinsTranslateOnlyWhenSavedNamesStillMatch() {
        assertEquals("Kniebeuge", WearNameLocalization.historicalExercise("de", "Guggolás", "Squat", catalog))
        assertEquals("Genuflexiune", WearNameLocalization.historicalExercise("ro", "Guggolás", "Squat", catalog))
        assertEquals("My squat", WearNameLocalization.historicalExercise("de", "My squat", "Squat", catalog))
        assertEquals("Guggolás", WearNameLocalization.historicalExercise("de", "Guggolás", "Squat", catalog.copy(custom = true)))
        assertEquals("Archived name", WearNameLocalization.historicalExercise("en", "Archived name", "", null))
    }
}
