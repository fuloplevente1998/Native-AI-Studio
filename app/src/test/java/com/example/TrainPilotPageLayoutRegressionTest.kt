package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutFrame
import com.example.ui.components.PageLayoutGrid
import com.example.ui.components.PageLayoutTile
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.PageLayoutState
import com.example.ui.components.measuredReorderTarget
import com.example.ui.components.reconcilePageLayout
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WorkoutTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainPilotPageLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedOrderRejectsUnknownAndDuplicateBlocksAndIncludesNewBlocks() {
        assertEquals(listOf("health", "profile", "weekly"),
            reconcilePageLayout(listOf("obsolete", "health", "health"), listOf("profile", "health", "weekly")))
        val state = PageLayoutState("test", listOf("one", "two"), listOf("two", "one"))
        state.open()
        state.reset()
        assertEquals(listOf("one", "two"), state.draft)
        state.cancel()
        assertEquals(listOf("two", "one"), state.saved)
    }

    @Test fun dailyTilesDragBetweenRowsAndSaveTheMigratedOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
        val page = "grid_test"
        val ids = listOf("sleep", "calories", "pulse", "steps", "workout", "recovery")
        val legacy = linkedMapOf("daily_0" to ids.take(3), "daily_1" to ids.drop(3))
        val layout = PageLayoutState(page, listOf("today"), listOf("today"),
            mapOf("daily_1" to listOf("recovery", "steps", "workout")))
        val migrated = listOf("sleep", "calories", "pulse", "recovery", "steps", "workout")
        assertEquals(migrated, layout.childOrder("daily", ids, legacy))
        layout.open()
        compose.setContent {
            TrainPilotTheme {
                PageLayoutFrame(layout, AppLanguage.HU, listOf(PageLayoutBlock("today", "Mai állapot")),
                    rememberScrollState(), 8.dp) {
                    PageLayoutGrid(layout, "daily", ids.map { PageLayoutBlock(it, it) }, 8.dp, legacy) { _, modifier ->
                        Box(modifier.height(52.dp))
                    }
                }
            }
        }
        val sleep = compose.onNodeWithTag("page_layout_grid_daily_sleep")
        val steps = compose.onNodeWithTag("page_layout_grid_daily_steps")
        val start = sleep.getUnclippedBoundsInRoot()
        val end = steps.getUnclippedBoundsInRoot()
        sleep.performTouchInput {
            swipe(center, center + Offset((end.left - start.left).toPx(), (end.top - start.top).toPx()), 350)
        }
        val expected = listOf("calories", "pulse", "recovery", "steps", "sleep", "workout")
        compose.runOnIdle { assertEquals(expected, layout.childOrder("daily", ids, legacy)) }
        compose.onNodeWithTag("page_layout_save").performClick()
        val children = org.json.JSONObject(prefs.getString("${page}_children", "{}")!!)
        val stored = children.getJSONArray("daily")
        assertEquals(expected, List(stored.length()) { stored.getString(it) })
    }

    @Test fun healthEditorKeepsTheThreeColumnGridWhenTheOuterHandleWouldNarrowIt() {
        val ids = listOf("sleep", "calories", "steps")
        val blocks = ids.map { PageLayoutBlock(it, it) }
        val layout = PageLayoutState("health_test", listOf("today"), listOf("today"))
        layout.open()
        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.width(344.dp)) {
                    PageLayoutFrame(layout, AppLanguage.HU, listOf(PageLayoutBlock("today", "Mai állapot")),
                        rememberScrollState(), 8.dp) {
                        PageLayoutTile(layout, "today", hasHorizontalTiles = true, overlayHandle = true) {
                            Box(Modifier.padding(horizontal = 14.dp)) {
                                PageLayoutGrid(layout, "daily", blocks, 8.dp, compactEditor = true) { _, modifier ->
                                    Box(modifier.height(52.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        val handles = ids.map { compose.onNodeWithTag("page_layout_grid_daily_$it").getUnclippedBoundsInRoot() }
        assertEquals(1, handles.map { it.top }.distinct().size)
        assertTrue(handles.zipWithNext().all { (left, right) -> left.left < right.left })
    }

    @Test fun dragUsesTheMeasuredSpacingOfUnequalCards() {
        val heights = listOf(60f, 240f, 100f)
        assertEquals(0, measuredReorderTarget(heights, 10f, 0, 100f))
        assertEquals(1, measuredReorderTarget(heights, 10f, 0, 140f))
        assertEquals(2, measuredReorderTarget(heights, 10f, 0, 320f))
        assertEquals(0, measuredReorderTarget(heights, 10f, 1, -45f))
    }

    @Test fun workoutEditShowsTheRealModeTilesAndSavesTheirDraggedPosition() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
        prefs.edit().remove("workout").remove("workout_children").commit()
        var opened = 0
        compose.setContent {
            TrainPilotTheme {
                WorkoutTabScreen(TrainPilotUiState(), onOpenSettings = {}, onOpenQuickWorkout = {},
                    onOpenExerciseLibrary = { opened++ }, onOpenOutdoorWorkout = {}, onOpenProgramsList = {},
                    onOpenProgramEditor = { _, _ -> }, onStartWorkoutDay = {}, onOpenCalendar = {},
                    onOpenCoach = {}, onOpenProgressionModal = {})
            }
        }
        compose.onNodeWithTag("page_customize_workout").performScrollTo().performClick()
        compose.onNodeWithTag("page_layout_editor_workout").assertExists()
        val modesHandle = compose.onNodeWithTag("page_layout_handle_modes")
        modesHandle.assertExists()
        compose.onNodeWithTag("workout_tab_timed_card", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("workout_tab_outdoor_card", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("workout_tab_exercise_picker", useUnmergedTree = true)
            .performScrollTo().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, opened) }
        val quickHandle = compose.onNodeWithTag("page_layout_handle_quick")
        quickHandle.performScrollTo()
        val distance = modesHandle.getUnclippedBoundsInRoot().top - quickHandle.getUnclippedBoundsInRoot().top
        modesHandle.performTouchInput { swipe(center, center + Offset(0f, -distance.toPx()), 350) }
        val timedHandle = compose.onNodeWithTag("page_layout_horizontal_workout_modes_timed")
        val repsHandle = compose.onNodeWithTag("page_layout_horizontal_workout_modes_reps")
        timedHandle.performScrollTo()
        val sideDistance = timedHandle.getUnclippedBoundsInRoot().left - repsHandle.getUnclippedBoundsInRoot().left
        timedHandle.performTouchInput { swipe(center, center + Offset(-sideDistance.toPx(), 0f), 350) }
        val save = compose.onNodeWithTag("page_layout_save")
        save.assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf("modes", "quick"), prefs.getString("workout", "")!!.split(',').take(2))
            val children = org.json.JSONObject(prefs.getString("workout_children", "{}")!!)
                .getJSONArray("workout_modes")
            assertEquals("timed", children.getString(0))
            assertEquals("reps", children.getString(1))
            assertEquals(0, opened)
        }
        assertTrue(compose.onNodeWithTag("page_workout_block_modes", useUnmergedTree = true).getUnclippedBoundsInRoot().top <
            compose.onNodeWithTag("page_workout_block_quick", useUnmergedTree = true).getUnclippedBoundsInRoot().top)
        assertTrue(compose.onNodeWithTag("workout_tab_timed_card", useUnmergedTree = true).getUnclippedBoundsInRoot().left <
            compose.onNodeWithTag("workout_tab_exercise_picker", useUnmergedTree = true).getUnclippedBoundsInRoot().left)
    }

    @Test fun homeSupportsLongPressAndFooterAndSavesNewOrderWithoutOpeningTheTile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
        prefs.edit().remove("home").putString("health", "sources,today").apply()
        var profileOpened = 0
        compose.setContent {
            TrainPilotTheme {
                HomeScreen(TrainPilotUiState(), TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onStartOrContinueWorkout = {}, onOpenProgramDayDetails = {},
                    onOpenQuickWorkout = {}, onOpenWatchAndSync = {},
                    onOpenCoachReadiness = {},
                    onOpenGoalsDialog = {}, onOpenPersonalPlanner = { profileOpened++ })
            }
        }
        compose.onNodeWithText("Normál hipertrófia • Dupla progresszió").assertDoesNotExist()
        val pageBlock = compose.onNodeWithTag("page_home_block_profile", useUnmergedTree = true)
        pageBlock.performTouchInput { longClick(Offset(center.x, 8f)) }
        compose.onNodeWithTag("page_layout_editor_home").assertExists()
        compose.runOnIdle { assertEquals(0, profileOpened) }
        compose.onNodeWithTag("page_layout_cancel").performClick()

        compose.onNodeWithTag("page_customize_home").performScrollTo().performClick()
        val moveUp = compose.onNodeWithTag("page_layout_handle_program").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Mozgatás fel" }
        compose.runOnIdle { assertTrue(moveUp.action()) }
        compose.onNodeWithTag("page_layout_save").performClick()
        compose.runOnIdle {
            assertEquals(listOf("program", "profile"), prefs.getString("home", "")!!.split(',').take(2))
            assertEquals("sources,today", prefs.getString("health", ""))
        }
    }
}
