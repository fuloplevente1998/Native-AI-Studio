package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.example.model.ExerciseItem
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class Collector2776RegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun workoutTilesShowTheirWholeSubtitleAndBadgeOnANarrowPhone() {
        val exercises = (1..140).map { ExerciseItem("e$it", "Gyakorlat $it", isTimed = it <= 30) }
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.WorkoutTabScreen(
                state = com.example.ui.viewmodel.TrainPilotUiState(exercises = exercises), onOpenSettings = {},
                onOpenQuickWorkout = {}, onOpenExerciseLibrary = {}, onOpenOutdoorWorkout = {}, onOpenProgramsList = {},
                onOpenProgramEditor = { _, _ -> }, onStartWorkoutDay = {}, onOpenCalendar = {}, onOpenCoach = {},
                onOpenProgressionModal = {}, onOpenTimed = {}, onOpenExerciseCatalog = {})
        } }
        val expected = mapOf(
            "workout_tab_exercise_picker_subtitle" to "súly × ismétlés",
            "workout_tab_exercise_picker_badge" to "140 gyak.",
            "workout_tab_timed_card_subtitle" to "idő / tartás",
            "workout_tab_timed_card_badge" to "30 gyak.",
            "workout_tab_outdoor_card_badge" to "GPS élő")
        expected.forEach { (tag, text) ->
            val node = compose.onNodeWithTag(tag, useUnmergedTree = true)
            node.assertTextEquals(text)
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val info = "lines=${layout.lineCount} size=${layout.size} w=${layout.didOverflowWidth} h=${layout.didOverflowHeight} " +
                "constraints=${layout.layoutInput.constraints} density=${layout.layoutInput.density.density}"
            assertFalse("$tag is clipped: $info", layout.hasVisualOverflow)
            assertEquals("$tag is ellipsized", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        }
    }
}
