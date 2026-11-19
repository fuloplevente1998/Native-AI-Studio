package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarCompactViewportRegressionTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(TrainPilotUiState(
        calendarYearMonth = "2026-03", planningCadenceHu = "Minden második nap",
        planningFirstDayHu = "Alap A", planningStartDate = "2026. 03. 01.",
        planningTime = "18:00", planningDurationMin = 60, planningWeeksCount = 4,
        calendarEvents = emptyList(), calendarUndoStack = listOf(emptyList())
    ))
    private val applies = mutableListOf<String>()
    private var activations = 0
    private var plans = 0
    private var undos = 0
    private val adds = mutableListOf<List<Any>>()
    private val updates = mutableListOf<List<Any>>()
    private val months = mutableListOf<String>()

    private fun render(height: Int = 720, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrainPilotTheme {
                    Box(Modifier.size(400.dp, height.dp).testTag("calendar_test_viewport")) {
                        CalendarPlannerScreen(state,
                            onBack = {}, onOpenSettings = {},
                            onSelectDay = { state = state.copy(selectedCalendarDay = it) },
                            onOpenReplanModal = { state = state.copy(showReplanModal = it) },
                            onApplyReplan = { _, _, id -> applies += id }, onOpenWorkoutDetail = {},
                            onActivateProgram = { activations++ },
                            onUpdatePlanningParams = { cadence, firstDay, startDate, time, duration, weeks ->
                                updates += listOf(cadence, firstDay, startDate, time, duration, weeks)
                                state = state.copy(planningCadenceHu = cadence, planningFirstDayHu = firstDay,
                                    planningStartDate = startDate, planningTime = time,
                                    planningDurationMin = duration, planningWeeksCount = weeks)
                            },
                            onPlanMonthlyWorkouts = { plans++ }, onUndoCalendarAction = { undos++ },
                            onAddPlannedWorkoutOnDay = { day, split, time, duration ->
                                adds += listOf(day, split, time, duration)
                            },
                            onSelectMonth = { months += it; state = state.copy(calendarYearMonth = it) })
                    }
                }
            }
        }
    }

    private fun assertInsideViewport(tag: String) {
        compose.onNodeWithTag(tag).assertIsDisplayed()
        val viewport = compose.onNodeWithTag("calendar_test_viewport").getUnclippedBoundsInRoot()
        val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertTrue("$tag top is inside viewport", bounds.top >= viewport.top)
        assertTrue("$tag bottom is inside viewport", bounds.bottom <= viewport.bottom)
        assertTrue("$tag left is inside viewport", bounds.left >= viewport.left)
        assertTrue("$tag right is inside viewport", bounds.right <= viewport.right)
    }

    private fun select(field: String, option: String) {
        compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("calendar_field_$field"))).performClick()
        compose.onNode(isPopup()).assertExists()
        compose.onNode(hasText(option) and hasAnyAncestor(isPopup())).performScrollTo().performClick()
    }

    @Test fun sixWeekMonthAndEntirePlannerFitWithoutScrollingAndActionsWork() {
        val month = YearMonth.parse(state.calendarYearMonth)
        assertEquals(6, (month.atDay(1).dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7)
        render()
        for (tag in listOf("calendar_month_panel", "calendar_day_31", "calendar_planning_block",
            "calendar_field_cadence", "calendar_field_first_day", "calendar_field_start_date",
            "calendar_field_time", "calendar_field_duration", "calendar_field_weeks",
            "calendar_plan_action", "calendar_replan_action", "calendar_undo_action")) assertInsideViewport(tag)
        compose.onNodeWithTag("calendar_plan_action").performClick()
        compose.onNodeWithTag("calendar_undo_action").performClick()
        assertEquals(1, plans)
        assertEquals(1, undos)
        compose.onNodeWithTag("calendar_replan_action").performClick()
        compose.runOnIdle { assertTrue(state.showReplanModal); state = state.copy(showReplanModal = false) }
        compose.onNodeWithTag("calendar_day_31").performClick()
        compose.onNodeWithTag("calendar_day_dialog").assertIsDisplayed()
        compose.onNodeWithTag("calendar_add_B").performClick()
        assertEquals(listOf(listOf(31, "B", "18:00", 60)), adds)
        compose.onNodeWithTag("calendar_day_dialog_close").performClick()
        compose.onNodeWithTag("calendar_day_dialog").assertDoesNotExist()
        assertInsideViewport("calendar_plan_action")
    }

    @Test fun selectingAnotherProgramOnlyAppliesItAfterConfirmationAndCancelDiscardsTheDraft() {
        fun program(id: String, active: Boolean) = com.example.model.TrainingProgramModel(
            id, id, id, "", "", emptyList(), true, active, emptyList())
        state = state.copy(programs = listOf(program("Original", true), program("Other", false)))
        render()
        fun chooseOther() {
            compose.onNodeWithTag("calendar_replan_action").performClick()
            compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("calendar_replan_program"))).performClick()
            compose.onNode(hasText("Other") and hasAnyAncestor(isPopup())).performClick()
            assertEquals(0, activations)
            assertTrue(applies.isEmpty())
        }
        chooseOther()
        compose.onNodeWithTag("calendar_replan_close").performClick()
        assertFalse(state.showReplanModal)
        chooseOther()
        compose.onNodeWithTag("calendar_replan_apply").performScrollTo().performClick()
        assertEquals(listOf("Other"), applies)
        assertEquals(0, activations)
    }

    @Test fun allSixFieldsForwardUpdatedParametersWithoutChangingOtherValues() {
        render()
        select("cadence", "Minden nap")
        select("first_day", "B nap")
        select("start_date", "2026. 03. 02.")
        select("time", "07:00")
        select("duration", "240")
        select("weeks", "12")
        assertEquals(listOf(
            listOf("Minden nap", "Alap A", "2026. 03. 01.", "18:00", 60, 4),
            listOf("Minden nap", "Alap B", "2026. 03. 01.", "18:00", 60, 4),
            listOf("Minden nap", "Alap B", "2026. 03. 02.", "18:00", 60, 4),
            listOf("Minden nap", "Alap B", "2026. 03. 02.", "07:00", 60, 4),
            listOf("Minden nap", "Alap B", "2026. 03. 02.", "07:00", 240, 4),
            listOf("Minden nap", "Alap B", "2026. 03. 02.", "07:00", 240, 12)
        ), updates)
        assertInsideViewport("calendar_plan_action")
        compose.onNodeWithTag("calendar_plan_action").performClick()
        assertEquals(1, plans)
    }

    @Test fun shortWindowWithLargeTranslatedTextKeepsPlanReachableByScrolling() {
        state = state.copy(language = AppLanguage.DE)
        render(height = 480, fontScale = 1.5f)
        compose.onNodeWithTag("calendar_plan_action").performScrollTo()
        assertInsideViewport("calendar_plan_action")
        compose.onNodeWithTag("calendar_plan_action").performClick()
        assertEquals(1, plans)
    }

    @Test fun horizontalSwipeChangesMonthButTapAndShortDragDoNot() {
        render()
        val panel = compose.onNodeWithTag("calendar_month_panel")
        panel.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(listOf("2026-04"), months)
        panel.performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(listOf("2026-04", "2026-03"), months)
        // A tiny horizontal drag (below the threshold) and a day tap never change the month.
        panel.performTouchInput { swipe(center, center + Offset(20f, 0f), 200) }
        compose.onNodeWithTag("calendar_day_10").performClick()
        compose.waitForIdle()
        assertEquals(listOf("2026-04", "2026-03"), months)
        assertEquals(10, state.selectedCalendarDay)
    }

    @Test fun monthPanelKeepsOneHeightAcrossFiveAndSixWeekMonths() {
        state = state.copy(calendarYearMonth = "2026-02") // Feb 2026 needs 5 week rows, March 2026 needs 6
        render()
        val five = compose.onNodeWithTag("calendar_month_panel").getUnclippedBoundsInRoot()
        val plannerBefore = compose.onNodeWithTag("calendar_planning_block").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("calendar_next_month").performClick() // March 2026: 6 rows
        compose.waitForIdle()
        assertEquals(listOf("2026-03"), months)
        val six = compose.onNodeWithTag("calendar_month_panel").getUnclippedBoundsInRoot()
        val plannerAfter = compose.onNodeWithTag("calendar_planning_block").getUnclippedBoundsInRoot()
        assertEquals("Month panel height must not jump", five.bottom - five.top, six.bottom - six.top)
        assertEquals("Planner stays pinned in place", plannerBefore.top, plannerAfter.top)
    }
}
