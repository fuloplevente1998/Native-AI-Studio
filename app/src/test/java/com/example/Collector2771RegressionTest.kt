package com.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.TrainPilotFormSheet
import com.example.ui.components.TrainPilotReorderState
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
@Config(sdk = [34], qualifiers = "w320dp-h760dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2771RegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun rowsMakeRoomLiveWhileAnotherRowIsDragged() {
        val state = TrainPilotReorderState()
        assertEquals(0f, state.displacementFor(1))
        // Row 1 of 0..4 lifted and dragged down 2.4 rows (100 px step).
        state.draggingKey = "b"; state.draggingIndex = 1; state.draggingLastIndex = 4
        state.draggingStepPx = 100f; state.offsetY = 240f
        assertEquals(0f, state.displacementFor(0))
        assertEquals(0f, state.displacementFor(1))
        assertEquals(-100f, state.displacementFor(2))
        assertEquals(-100f, state.displacementFor(3))
        assertEquals(0f, state.displacementFor(4))
        assertEquals(3, state.dropTarget(1, 4, 100f))
        // Dragged up past two rows: those slide down.
        state.offsetY = -160f
        state.draggingIndex = 3
        assertEquals(0f, state.displacementFor(0))
        assertEquals(100f, state.displacementFor(1))
        assertEquals(100f, state.displacementFor(2))
        assertEquals(0f, state.displacementFor(4))
        state.draggingKey = null
        assertEquals(0f, state.displacementFor(2))
    }

    @Test fun journalCalendarOpensAsSheetFitsNarrowScreenAndSwipesMonths() {
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.LogTabScreen(TrainPilotUiState(language = AppLanguage.HU),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("log_calendar_sheet").assertDoesNotExist()
        compose.onNodeWithTag("log_month_picker_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("log_calendar_sheet").assertExists()
        val width = compose.activity.resources.displayMetrics.widthPixels
        // Every day of the month fits (the Sunday column was clipped before).
        val month = java.time.YearMonth.now()
        (1..month.lengthOfMonth()).forEach { day ->
            val b = compose.onNodeWithTag("log_calendar_day_$day", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
            assertTrue("day $day clipped: $b / $width", b.right <= width + 0.5f && b.left >= -0.5f)
        }
        // Constant height: always 6 week rows.
        (0..5).forEach { compose.onNodeWithTag("log_calendar_week_$it", useUnmergedTree = true).assertExists() }
        val next = month.plusMonths(1)
        compose.onNodeWithTag("log_calendar_swipe_area").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        val nextName = listOf("január", "február", "március", "április", "május", "június", "július", "augusztus", "szeptember", "október", "november", "december")[next.monthValue - 1]
        compose.onNodeWithText("${next.year}. $nextName", ignoreCase = true).assertExists()
        compose.onNodeWithTag("log_calendar_done").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("log_calendar_sheet").assertDoesNotExist()
    }

    @Test fun formSheetKeepsDialogSlotsAndConfirmationsStayDialogs() {
        var open by mutableStateOf(true)
        var sheet by mutableStateOf(true)
        var saved = 0
        compose.setContent { TrainPilotTheme {
            if (open) TrainPilotFormSheet(
                onDismissRequest = { open = false },
                title = { Text("Cím") },
                text = { Text("Tartalom", Modifier.testTag("form_body")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { saved++ }, modifier = Modifier.testTag("form_ok")) { Text("Mentés") } },
                testTag = "form_sheet",
                asSheet = sheet
            )
        } }
        compose.onNodeWithTag("form_sheet").assertExists()
        compose.onNodeWithTag("form_body").assertExists()
        compose.onNodeWithTag("form_ok").performClick()
        compose.runOnIdle { assertEquals(1, saved) }
        compose.runOnIdle { open = false }
        compose.onNodeWithTag("form_sheet").assertDoesNotExist()
        compose.runOnIdle { sheet = false; open = true }
        compose.onNodeWithTag("form_sheet").assertDoesNotExist()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithTag("form_body").assertExists()
    }
}
