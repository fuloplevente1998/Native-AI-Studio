package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.model.LoggedWorkoutEntry
import com.example.ui.screens.CompactJournalSearchField
import com.example.ui.screens.LogWorkoutCard
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioCompactJournalUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchIs48dpAndRemainsEditable() {
        var query by mutableStateOf("")
        compose.setContent { TrainPilotTheme {
            Column(Modifier.width(350.dp)) {
                CompactJournalSearchField(query, { query = it }, "Edzések keresése...")
            }
        } }
        val field = compose.onNodeWithTag("log_search_input")
        val box = field.getUnclippedBoundsInRoot()
        assertEquals(48.dp, box.bottom - box.top)
        field.performTextInput("Otthoni")
        compose.runOnIdle { assertEquals("Otthoni", query) }
    }

    @Test fun loggedWorkoutRowHasOnlyKeyDataAndKeepsDetailAndDeleteActions() {
        var detailCalls = 0
        var deleteCalls = 0
        val workout = LoggedWorkoutEntry(
            "w1", "2026-11-02 08:00", "Otthoni A/B – Alap", 54,
            "54:00", "00:00", 15, 1254,
            "Hosszú, elhagyandó gyakorlatlista", hasPr = true
        )
        compose.setContent { TrainPilotTheme {
            Column(Modifier.width(350.dp)) {
                LogWorkoutCard(workout, onClick = {}, onOpenDetailAlways = { detailCalls++ },
                    onDelete = { deleteCalls++ }, deleteEnabled = true)
            }
        } }
        val row = compose.onNodeWithTag("log_card_w1")
        val bounds = row.getUnclippedBoundsInRoot()
        assertTrue("Journal row must be compact enough for 5–6 entries", bounds.bottom - bounds.top < 90.dp)
        compose.onNodeWithText("Otthoni A/B – Alap").assertExists()
        compose.onNodeWithText("54 perc · 15 sorozat · 1254 kg").assertExists()
        compose.onNodeWithText("Hosszú, elhagyandó gyakorlatlista").assertDoesNotExist()
        row.performClick()
        compose.runOnIdle { assertEquals(1, detailCalls) }
        compose.onNodeWithTag("journal_list_delete_w1").performClick()
        compose.runOnIdle { assertEquals(1, deleteCalls) }
    }
}
