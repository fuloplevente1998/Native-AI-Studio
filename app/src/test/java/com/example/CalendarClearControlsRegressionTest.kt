package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.TrainPilotStrings
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarClearControlsRegressionTest {
    @get:Rule val compose=createComposeRule()
    @Test fun clearIsLeftOfPlanningAndOnlyConfirmationDispatchesAcrossFourLanguages() {
        var state by mutableStateOf(TrainPilotUiState(calendarYearMonth="2026-12"))
        var requests=0;var clears=0;var plans=0;var cancels=0
        compose.setContent { CompositionLocalProvider(LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(state.language)) {
            TrainPilotTheme {
                CalendarPlannerScreen(state,onBack={},onOpenSettings={},onSelectDay={},onOpenReplanModal={},
                    onApplyReplan={_,_,_->},onOpenWorkoutDetail={},onPlanMonthlyWorkouts={plans++},
                    onRequestClearCalendar={requests++;state=state.copy(showCalendarClearDialog=true)},
                    onConfirmClearCalendar={clears++;state=state.copy(showCalendarClearDialog=false)},
                    onCancelClearCalendar={cancels++;state=state.copy(showCalendarClearDialog=false)})
            }
        } }
        for(lang in AppLanguage.entries) {
            compose.runOnIdle {state=state.copy(language=lang)}
            val left=compose.onNodeWithTag("calendar_clear_action").performScrollTo().assertIsDisplayed()
            val right=compose.onNodeWithTag("calendar_plan_action").assertIsDisplayed()
            assertTrue(left.fetchSemanticsNode().boundsInRoot.left<right.fetchSemanticsNode().boundsInRoot.left)
            val before=clears
            left.performClick();compose.runOnIdle{assertEquals(before,clears)}
            compose.onNodeWithTag("calendar_clear_cancel").performClick()
            compose.runOnIdle{assertEquals(before,clears)}
            left.performClick();compose.onNodeWithTag("calendar_clear_confirm").performClick()
            compose.runOnIdle{assertEquals(before+1,clears)}
            right.performClick()
        }
        compose.runOnIdle{assertEquals(8,requests);assertEquals(4,clears);assertEquals(4,cancels);assertEquals(4,plans)}
    }
}
