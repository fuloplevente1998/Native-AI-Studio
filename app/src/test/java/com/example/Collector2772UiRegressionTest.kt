package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.model.AppLanguage
import com.example.ui.components.ActiveWorkoutBottomBar
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.theme.TrainPilotActionAppearance
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2772UiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun bottomBarFinishSheetSavesOrDiscardsOnlyAfterConfirmationAndNextHasNoName() {
        var hasNext by mutableStateOf(true)
        var prev = 0; var next = 0; var finish = 0; var discard = 0
        compose.setContent { TrainPilotTheme {
            ActiveWorkoutBottomBar(AppLanguage.HU, enabled = true, hasNext = hasNext, completedSets = 2, totalSets = 9,
                elapsedLabel = "", tagPrefix = "active", onPrevious = { prev++ }, onNext = { next++ },
                onFinish = { finish++ }, onDiscard = { discard++ })
        } }
        compose.onNodeWithTag("active_next_exercise_btn").assertTextEquals("Következő ›").performClick()
        compose.onNodeWithTag("active_prev_exercise_btn").performClick()
        compose.runOnIdle { assertEquals(1, next); assertEquals(1, prev) }
        // Finish → keep training closes without side effects.
        compose.onNodeWithTag("active_finish_menu_btn").performClick()
        compose.onNodeWithText("2 / 9 sorozat kész").assertExists()
        compose.onNodeWithTag("active_finish_continue").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("active_finish_sheet").assertDoesNotExist()
        // Discard needs the confirmation dialog.
        compose.onNodeWithTag("active_finish_menu_btn").performClick()
        compose.onNodeWithTag("active_finish_discard").performClick()
        compose.runOnIdle { assertEquals(0, discard) }
        compose.onNodeWithTag("active_discard_confirm").performClick()
        compose.runOnIdle { assertEquals(1, discard); assertEquals(0, finish) }
        // Save & finish goes to the summary path.
        compose.onNodeWithTag("active_finish_menu_btn").performClick()
        compose.onNodeWithTag("active_finish_save").performClick()
        compose.runOnIdle { assertEquals(1, finish) }
        // Last exercise: right button reads "Mentés ›" (no duplicate "Befejezés").
        compose.runOnIdle { hasNext = false }
        compose.onNodeWithTag("active_next_exercise_btn").assertTextEquals("Mentés ›")
    }

    @Test fun actionColorsAreLightOnLightThemesAndDarkOnDarkThemes() {
        var lightBg = Color.Unspecified
        var darkBg = Color.Unspecified
        val palettes = com.example.ui.theme.TrainPilotPalettes.allPalettes
        val light = palettes.first { !it.isDark }
        val dark = palettes.first { it.isDark }
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(com.example.ui.theme.LocalTrainPilotPalette provides light) {
                lightBg = TrainPilotActionAppearance.negativeBackground
            }
            androidx.compose.runtime.CompositionLocalProvider(com.example.ui.theme.LocalTrainPilotPalette provides dark) {
                darkBg = TrainPilotActionAppearance.negativeBackground
            }
        }
        compose.runOnIdle {
            assertTrue("light theme must not get a dark block: $lightBg", lightBg.luminance() > 0.6f)
            assertTrue("dark theme keeps the deep tone: $darkBg", darkBg.luminance() < 0.2f)
        }
    }

    @Test fun onlyBundledFontWeightsAreUsed() {
        // The bundled fonts stop at Bold; Black/ExtraBold silently fall back to a system font.
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }
        val offenders = root.walkTopDown().filter { it.extension == "kt" }
            .filter { f -> Regex("FontWeight\\.(Black|ExtraBold|W800|W900)\\b").containsMatchIn(f.readText()) }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test fun workoutTabTilesAndExerciseLibraryRowCallTheirActions() {
        val calls = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.WorkoutTabScreen(
                state = com.example.ui.viewmodel.TrainPilotUiState(), onOpenSettings = {},
                onOpenQuickWorkout = { calls += "quick" }, onOpenExerciseLibrary = { calls += "strength" },
                onOpenOutdoorWorkout = { calls += "outdoor" }, onOpenProgramsList = {}, onOpenProgramEditor = { _, _ -> },
                onStartWorkoutDay = {}, onOpenCalendar = {}, onOpenCoach = {}, onOpenProgressionModal = {},
                onOpenTimed = { calls += "timed" }, onOpenExerciseCatalog = { calls += "catalog" })
        } }
        listOf("workout_tab_quick_card", "workout_tab_exercise_picker", "workout_tab_timed_card",
            "workout_tab_outdoor_card", "workout_tab_exercise_catalog").forEach {
            compose.onNodeWithTag(it).performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(listOf("quick", "strength", "timed", "outdoor", "catalog"), calls) }
    }

    @Test fun libraryButtonReadsSwapWhileReplacingTheActiveExercise() {
        val ex = com.example.model.ExerciseItem("one", "Első gyakorlat")
        var state by mutableStateOf(com.example.ui.viewmodel.TrainPilotUiState(exercises = listOf(ex)))
        var started: com.example.model.ExerciseItem? = null
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.ExerciseLibraryScreen(state, {}, {}, {}, {}, {}, onStartExerciseNow = { started = it })
        } }
        val before = compose.onNodeWithTag("exercise_library_start_now_one").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertFalse(before, before.contains("Csere"))
        compose.runOnIdle { state = state.copy(exerciseSwapMode = true) }
        compose.onNodeWithTag("exercise_library_start_now_one").assertContentDescriptionContains("Csere", substring = true).performClick()
        compose.runOnIdle { assertEquals(ex, started) }
    }
}
