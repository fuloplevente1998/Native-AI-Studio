package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.LoggedExerciseDetail
import com.example.model.LoggedWorkoutEntry
import com.example.ui.components.ActiveWorkoutBottomBar
import com.example.ui.components.StudioActiveExerciseTitleRow
import com.example.ui.components.lastLoggedSets
import com.example.ui.components.lastSetLabel
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class Collector2775RegressionTest {
    @get:Rule val compose = createComposeRule()

    private val rdl = ExerciseItem("rdl", "Román felhúzás kézisúlyzókkal", nameEn = "Dumbbell Romanian deadlift")
    private fun set(n: Int, kg: Float, reps: Int, done: Boolean = true) = ExerciseSetItem(n, kg, reps, 2, done)
    private fun workout(id: String, start: String, vararg details: LoggedExerciseDetail) =
        LoggedWorkoutEntry(id = id, titleHu = "Otthoni A/B", savedStartIso = start, exerciseDetails = details.toList())

    @Test fun lastTimeUsesTheNewestSavedWorkoutWithCompletedSetsAndNeverTheActiveOne() {
        val older = workout("w1", "2026-11-10T10:00", LoggedExerciseDetail(rdl.nameHu, "", listOf(set(1, 5f, 10), set(2, 5f, 10))))
        val newer = workout("w2", "2026-11-17T10:00", LoggedExerciseDetail(rdl.nameHu, "", listOf(set(1, 5.5f, 12), set(2, 5.5f, 12, done = false))))
        val active = workout("active", "2026-11-19T10:00", LoggedExerciseDetail(rdl.nameHu, "", listOf(set(1, 9f, 9))))
        val notStarted = workout("w3", "2026-11-18T10:00", LoggedExerciseDetail(rdl.nameHu, "", listOf(set(1, 7f, 7, done = false))))
        val result = lastLoggedSets(listOf(older, active, notStarted, newer), rdl, "active")
        assertEquals(listOf(1), result.map { it.setNumber }) // uncompleted sets are not "last time" values
        assertEquals(5.5f, result.single().weightKg)
        assertEquals("legutóbb: 5,5 × 12", lastSetLabel(result.single(), AppLanguage.HU))
        assertEquals("last time: × 15", lastSetLabel(set(1, 0f, 15), AppLanguage.EN))
    }

    @Test fun noHistoryMeansNoLastTimeValues() {
        assertTrue(lastLoggedSets(emptyList(), rdl, "active").isEmpty())
        val other = workout("w1", "2026-11-10T10:00", LoggedExerciseDetail("Guggolás", "", listOf(set(1, 20f, 8))))
        assertTrue(lastLoggedSets(listOf(other), rdl, "active").isEmpty())
        assertTrue(lastLoggedSets(listOf(other), null, "active").isEmpty())
    }

    @Test fun sharedTitleRowOpensTheLibraryAndTheDetails() {
        var library = 0
        val details = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            StudioActiveExerciseTitleRow(AppLanguage.HU, "Oldalsó plank", "Kétoldali stopper • 1. sorozat • 25 mp / oldal",
                ExerciseItem("side-plank", "Oldalsó plank", isTimed = true), "side-plank",
                onOpenLibrary = { library++ }, onOpenDetail = { details += it })
        } }
        compose.onNodeWithTag("active_exercise_title").performClick()
        compose.onNodeWithTag("active_exercise_kind_icon").performClick()
        compose.runOnIdle {
            assertEquals(1, library)
            assertEquals(listOf("side-plank"), details)
        }
        // One name, one subtitle: no duplicated "Oldalsó plank • Kétoldali stopper" title.
        compose.onAllNodesWithText("Oldalsó plank", substring = true, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun finishButtonUsesADrawnIconAndNoEmojiSquare() {
        compose.setContent { TrainPilotTheme {
            ActiveWorkoutBottomBar(language = AppLanguage.HU, enabled = true, hasNext = true,
                completedSets = 1, totalSets = 4, elapsedLabel = "00:32",
                onPrevious = {}, onNext = {}, onFinish = {}, onDiscard = {}, tagPrefix = "active")
        } }
        compose.onNodeWithTag("active_finish_icon", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("⏹", substring = true).assertCountEquals(0)
        compose.onNodeWithTag("active_finish_menu_btn").assertHasClickAction()
    }
}
