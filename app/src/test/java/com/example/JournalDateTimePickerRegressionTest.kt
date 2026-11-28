package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.theme.TrainPilotPalettes
import com.repforge.app.ui.journal.JournalDateTimeField
import com.repforge.app.ui.journal.JournalDateTimeSheet
import com.repforge.app.ui.journal.JournalLocalTime
import java.time.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class JournalDateTimePickerRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("Europe/Budapest")
    private val original = "2026-11-25T21:57:36.123Z"

    @Test fun changingOnlyLocalComponentsPreservesPrecisionAndResolvesZone() {
        assertEquals(original, JournalLocalTime.edit(original, zone, hour = 22, minute = 57))
        assertEquals("2026-11-25T22:48:36.123Z", JournalLocalTime.edit(original, zone, hour = 23, minute = 48))
        assertEquals("2026-07-25T20:57:36.123Z", JournalLocalTime.edit(original, zone, date = LocalDate.of(2026, 7, 25)))
        val gap = "2026-03-29T00:30:12Z"
        assertNull(JournalLocalTime.edit(gap, zone, hour = 2))
        val overlap = "2026-10-25T01:30:12Z"
        assertEquals("2026-10-25T01:45:12Z", JournalLocalTime.edit(overlap, zone, minute = 45))
    }

    @Test fun actualDateFieldOpensPickerCancelDoesNotEditAndDoneChangesOnlyDate() {
        var value by mutableStateOf(original)
        var picker by mutableStateOf<String?>(null)
        var liquid by mutableStateOf(false)
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(if (liquid) 40 else 1)) {
                JournalDateTimeField(value, zone, "hu", "Kezdés", "date_field", true,
                    onDate = { picker = "date" }, onTime = { picker = "time" })
                picker?.let {
                    JournalDateTimeSheet(value, zone, "hu", it == "date", onDismiss = { picker = null },
                        onConfirm = { value = it; picker = null })
                }
            }
        }
        compose.onNodeWithText("22:57").assertExists()
        compose.onNodeWithText("+01:00", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("date_field_date").performClick()
        compose.onNodeWithTag("journal_date_picker_day_26").performClick()
        compose.onNodeWithTag("journal_date_picker_cancel").performClick()
        compose.runOnIdle { assertEquals(original, value); liquid = true }
        compose.onNodeWithTag("date_field_date").performClick()
        compose.onNodeWithTag("journal_date_picker_day_26").performClick()
        compose.onNodeWithTag("journal_date_picker_done").performClick()
        compose.runOnIdle { assertEquals("2026-11-26T21:57:36.123Z", value) }
    }

    @Test fun actualHourWheelTapChangesHourButKeepsMinuteAndSeconds() {
        var value by mutableStateOf(original)
        var open by mutableStateOf(true)
        compose.setContent { TrainPilotTheme {
            if (open) JournalDateTimeSheet(value, zone, "hu", false, onDismiss = { open = false },
                onConfirm = { value = it; open = false })
        } }
        compose.onNodeWithTag("journal_hour_item_1222").assertIsSelected()
        compose.onNodeWithTag("journal_hour_item_1223").performClick()
        compose.onNodeWithTag("journal_hour_item_1223").assertIsSelected()
        compose.onNodeWithTag("journal_time_picker_done").performClick()
        compose.runOnIdle { assertEquals("2026-11-25T22:57:36.123Z", value) }
    }
}
