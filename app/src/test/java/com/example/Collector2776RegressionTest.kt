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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
// Real text measurement: the legacy shadow returns placeholder layouts (e.g. 15 px wide, "overflowing").
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
            // hasVisualOverflow is not usable for wrap-content text (paragraph width vs. size); check each line instead.
            assertFalse("$tag is cut vertically: $info", layout.didOverflowHeight)
            for (line in 0 until layout.lineCount) assertTrue("$tag line $line is wider than its box: $info",
                layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f)
            assertEquals("$tag is ellipsized", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        }
    }

    private fun rec(id: String, type: String, start: Long, source: String = "watch", data: String = "{\"value\":1}") =
        com.repforge.app.ui.health.HealthRecordRow(id, "hc", source, type, start, start + 60_000, start, data)

    @Test fun oneLinePerTimeWindowMergesOnlyDifferentTypesOfTheSameSource() {
        val t = 1_000_000L
        val groups = com.repforge.app.ui.health.groupHealthRecordRows(listOf(
            rec("s1", "StepsRecord", t), rec("c1", "ActiveCaloriesBurnedRecord", t), rec("d1", "DistanceRecord", t),
            rec("s2", "StepsRecord", t), // same type again: never merged, so no value is hidden
            rec("p1", "StepsRecord", t, source = "phone"), // other source: own line
            rec("s3", "StepsRecord", t + 120_000)))
        assertEquals(listOf(listOf("s1", "c1", "d1"), listOf("s2"), listOf("p1"), listOf("s3")), groups.map { g -> g.map { it.id } })
        assertEquals(6, groups.sumOf { it.size }) // every record is still shown
    }

    @Test fun compactValueUsesTheRecordsOwnNumbers() {
        val v = com.repforge.app.ui.health.healthRecordCompactValue(
            rec("c", "ActiveCaloriesBurnedRecord", 0, data = "{\"value\":15.42,\"unit\":\"kcal\"}"), "hu", "lépés", "Aktív kalória")
        assertEquals("15,42 kcal", v)
        val steps = com.repforge.app.ui.health.healthRecordCompactValue(
            rec("s", "StepsRecord", 0, data = "{\"value\":12,\"unit\":\"steps\"}"), "hu", "lépés", "Lépésszám")
        assertEquals("12 lépés", steps)
        val hr = com.repforge.app.ui.health.healthRecordCompactValue(
            rec("h", "HeartRateRecord", 0, data = "{\"samples\":[{\"value\":80}]}"), "hu", "lépés", "Pulzus")
        assertEquals("Pulzus · 1", hr)
    }
}
