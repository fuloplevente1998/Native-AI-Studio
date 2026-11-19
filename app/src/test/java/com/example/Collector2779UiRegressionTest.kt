package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.model.AppLanguage
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ProgramDayModel
import com.example.model.TrainingProgramModel
import com.example.ui.screens.WorkoutTabScreen
import com.example.ui.screens.workoutDayNextStarts
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.home.HomeWorkoutTarget
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Collector2779UiRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun program(days: Int, longTitleOn: Int = -1) = TrainingProgramModel("p", "Saját", "Own", "", "", emptyList(), true, true,
        (0 until days).map { i ->
            val code = "${'A' + i} nap"
            ProgramDayModel("p:${'a' + i}", code, if (i == longTitleOn) "Felsőtest – erő és stabilitás hosszú címmel" else code, code, 60, emptyList())
        })

    private fun event(day: Char, date: LocalDate, status: CalendarEventStatus = CalendarEventStatus.PLANNED) =
        CalendarDayEvent("e$day$date", date.dayOfMonth, "10:00", "x", 6, 60, status, dateIso = date.toString(), programId = "p", dayId = "$day")

    private fun show(state: TrainPilotUiState, starts: MutableList<Int> = mutableListOf(), edits: MutableList<Int> = mutableListOf()) {
        compose.setContent { TrainPilotTheme {
            WorkoutTabScreen(state = state, onOpenSettings = {}, onOpenQuickWorkout = {}, onOpenExerciseLibrary = {},
                onOpenOutdoorWorkout = {}, onOpenProgramsList = {}, onOpenProgramEditor = { _, i -> edits += i },
                onStartWorkoutDay = { starts += it }, onOpenCalendar = {}, onOpenCoach = {}, onOpenProgressionModal = {})
        } }
    }

    @Test fun threeDayProgramShowsAllDaysSideBySideWithEqualHeightAndEachNextDate() {
        val today = LocalDate.now()
        val events = listOf(event('a', today.minusDays(2)), event('a', today.plusDays(7)), event('b', today.plusDays(1)),
            event('c', today.plusDays(3)), event('c', today.plusDays(2), CalendarEventStatus.COMPLETED))
        val starts = mutableListOf<Int>(); val edits = mutableListOf<Int>()
        show(TrainPilotUiState(programs = listOf(program(3, longTitleOn = 1)), calendarEvents = events), starts, edits)
        val cards = (0..2).map { compose.onNodeWithTag("workout_tab_day_card_$it").fetchSemanticsNode().boundsInRoot }
        assertEquals("one row", 1, cards.map { it.top }.distinct().size)
        assertEquals("equal height", 1, cards.map { it.height }.distinct().size)
        assertTrue("side by side", cards[0].right <= cards[1].left && cards[1].right <= cards[2].left)
        compose.onNodeWithTag("workout_tab_day_date_1", useUnmergedTree = true).assertTextEquals("📅 Holnap, 10:00")
        compose.onNodeWithTag("workout_tab_day_date_2", useUnmergedTree = true).assertTextContains("10:00", substring = true)
        compose.onNodeWithTag("workout_tab_day_date_0", useUnmergedTree = true).assertTextContains("10:00", substring = true)
        compose.onNodeWithTag("workout_tab_day_dots").assertDoesNotExist()
        compose.onNodeWithTag("workout_tab_day_start_2").performClick()
        compose.onNodeWithTag("workout_tab_day_card_1").performClick()
        compose.runOnIdle { assertEquals(listOf(2), starts); assertEquals(listOf(1), edits) }
    }

    @Test fun nextStartsUseTheTargetForItsDayAndNeverInventADate() {
        val now = ZonedDateTime.now()
        val target = HomeWorkoutTarget("p", "b", "s", now.plusDays(1).toInstant().toString())
        val starts = workoutDayNextStarts(program(3), listOf(event('c', now.toLocalDate().minusDays(1))), target, now)
        assertEquals(setOf(1), starts.keys) // day A has nothing planned, day C only in the past
        assertEquals(target.startIso, starts[1])
    }

    @Test fun fiveDayProgramScrollsSidewaysAndKeepsEveryDayReachable() {
        val starts = mutableListOf<Int>()
        show(TrainPilotUiState(programs = listOf(program(5)),
            homeWorkoutTarget = HomeWorkoutTarget("p", "e", null, null)), starts)
        compose.onNodeWithTag("workout_tab_day_dots").assertExists()
        val root = compose.onNodeWithTag("workout_tab_day_cards").fetchSemanticsNode().boundsInRoot
        // Starts at the next day (E) so it is on screen without swiping.
        val e = compose.onNodeWithTag("workout_tab_day_card_4").fetchSemanticsNode().boundsInRoot
        assertTrue("next day visible: $e in $root", e.left >= root.left - 1 && e.right <= root.right + 1)
        compose.onNodeWithTag("workout_tab_day_start_0").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(0), starts) }
    }

    @Test fun workoutTilesStartTheirGraphicsOnTheSameLineAndTheTabHasNoMoreArrow() {
        show(TrainPilotUiState(language = AppLanguage.HU, programs = listOf(program(2))))
        val tops = listOf("workout_tab_exercise_picker", "workout_tab_timed_card", "workout_tab_outdoor_card")
            .map { compose.onNodeWithTag("${it}_graphic", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.toString(), 1, tops.distinct().size)
        // The old "scroll for more" chevron lived in TrainPilotScrollableBox; the tab no longer uses it.
        val source = java.io.File("src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt").readText()
        val tab = source.substringAfter("fun WorkoutTabScreen(").substringBefore("private fun ShortcutSquareCard")
        assertFalse(tab.contains("TrainPilotScrollableBox"))
    }

    @Test fun progressDetailSheetListsEveryWorkoutOfThePeriodNotOnlyFour() {
        val today = LocalDate.now()
        val workouts = (0 until 35).map { i ->
            com.example.model.LoggedWorkoutEntry(id = "w$i", titleHu = "Otthoni A/B", durationMinutes = 50,
                savedStartIso = today.minusDays(i * 2L).atTime(10, 0).toString())
        }
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.MainProgressVolumeAndMuscleSection(
                TrainPilotUiState(loggedWorkouts = workouts, selectedProgressRangeIndex = 3, expandedStatsCardIndex = 1),
                onSelectRange = {}, onSelectVolumeBar = {}, onSelectExpandedStatsCard = {}, onOpenWorkoutDetail = {})
        } }
        val rows = compose.onAllNodes(SemanticsMatcher("progress workout row") {
            it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null }?.startsWith("progress_workout_") == true
        }).fetchSemanticsNodes()
        assertEquals(35, rows.size)
    }

    @Test fun journalClearsAMonthFilterWithoutOpeningTheCalendar() {
        val today = LocalDate.now()
        val rows = listOf(
            com.example.model.LoggedWorkoutEntry(id = "now", titleHu = "Mai", savedStartIso = today.atTime(9, 0).toString()),
            com.example.model.LoggedWorkoutEntry(id = "old", titleHu = "Régi", savedStartIso = today.minusMonths(1).withDayOfMonth(1).atTime(9, 0).toString()))
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.LogTabScreen(TrainPilotUiState(loggedWorkouts = rows, language = AppLanguage.HU),
                com.example.localization.TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("log_clear_filter_chip").assertDoesNotExist()
        compose.onNodeWithTag("log_prev_month_button").performClick()
        compose.onNodeWithTag("log_card_now").assertDoesNotExist()
        compose.onNodeWithTag("log_clear_filter_chip").assertTextEquals("✕ Szűrő törlése").performClick()
        compose.onNodeWithTag("log_clear_filter_chip").assertDoesNotExist()
        compose.onNodeWithTag("log_card_now").assertExists()
        compose.onNodeWithTag("log_card_old").assertExists()
        compose.onNodeWithTag("log_calendar_sheet").assertDoesNotExist()
    }

    @Test fun stepChartKeepsMissingDaysEmptyInsteadOfZero() {
        val today = LocalDate.now()
        fun day(back: Long, steps: Int, synced: Boolean) = com.example.ui.viewmodel.WeeklyHealthDaySummary(
            today.minusDays(back).toString(), "", steps, 0, "", 0, 0, "HC", stepsAvailable = synced)
        val bars = com.example.ui.screens.dailyStepBars(listOf(day(0, 7711, true), day(1, 0, false), day(20, 999, true)), today)
        assertEquals(14, bars.size)
        assertEquals(7711, bars.last())
        assertNull(bars[12]) // not synced ≠ 0 steps
        assertTrue(bars.take(12).all { it == null })
    }

    @Test fun restDockSwitchesSizeWithAShortAnimationNotAJump() {
        var large by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { TrainPilotTheme {
            com.example.ui.components.StudioRestTimerDock(TrainPilotUiState(restTimerRemainingSec = 60, defaultRestSec = 90),
                large = large, onLarge = { large = it }, onAdjust = {}, onToggle = {}, onSkip = {}, skipLabel = "Kihagy")
        } }
        val compact = compose.onNodeWithTag("rest_dock_animated").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithTag("sticky_rest_timer_compact").performClick()
        compose.waitForIdle()
        val full = compose.onNodeWithTag("rest_dock_animated").fetchSemanticsNode().boundsInRoot.height
        assertTrue("large dock is taller: $compact -> $full", full > compact)
        compose.onNodeWithTag("sticky_rest_timer_large").assertExists()
        // The switch runs inside one AnimatedContent (fade + size), not an if/else swap.
        val source = java.io.File("src/main/java/com/example/ui/components/StudioRestTimerDock.kt").readText()
        assertTrue(source.contains("AnimatedContent(") && source.contains("SizeTransform("))
        compose.onNodeWithTag("sticky_rest_timer_compact").assertDoesNotExist()
    }
}
