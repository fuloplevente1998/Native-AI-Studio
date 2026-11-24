package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.NativeCalendarSyncPlan
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.snapshotsToPrune
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

/** 2806: simpler Google sync – last 3 Drive snapshots, 30-day calendar, one disconnect sheet; palettes 12/15. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2806RegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun file(n: Int) = JSONObject().put("id", "f$n").put("name", "repforge-sync-x-$n.json")
        .put("createdTime", "2026-11-%02dT10:00:00Z".format(n))

    @Test fun driveKeepsOnlyTheNewestThreeSnapshotsIncludingOldOnes() {
        val rows = listOf(5, 1, 9, 3, 7).map(::file)
        val pruned = snapshotsToPrune(rows).map { it.getString("id") }
        assertEquals("oldest first, the three newest stay", listOf("f1", "f3"), pruned)
        assertTrue(snapshotsToPrune(rows.take(3)).isEmpty())
        assertEquals("never removes the only snapshot", emptyList<JSONObject>(), snapshotsToPrune(listOf(file(1)), keep = 0))
    }

    private fun row(id: String, date: String, status: String = "planned") = CalendarScheduleEntity(id,
        "rf" + "0".repeat(63) + id,
        date, "${date}T18:00:00Z", "${date}T19:00:00Z", "p", "Program", "d", "Nap", status, 1L, "{}")

    @Test fun calendarKeepsOnlyTheNextThirtyDaysOfPlans() {
        val today = LocalDate.parse("2026-11-24")
        val ops = NativeCalendarSyncPlan.operations(listOf(
            row("1", "2026-11-23"), row("2", "2026-11-24"), row("3", "2026-12-24"),
            row("4", "2026-12-25"), row("5", "2026-11-30", status = "skipped")), today).toList()
        val byDate = ops.associateBy { op -> listOf("1", "2", "3", "4", "5").first { op.id.endsWith(it) } }
        assertTrue("past plan leaves Google Calendar", byDate.getValue("1").delete)
        assertFalse("today stays", byDate.getValue("2").delete)
        assertFalse("day 30 stays", byDate.getValue("3").delete)
        assertTrue("beyond 30 days is not sent", byDate.getValue("4").delete)
        assertTrue("skipped is removed", byDate.getValue("5").delete)
    }

    @Test fun googleCardHasOneSyncButtonAndOneDisconnectSheetWithBothExtrasOff() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("repforge-google", android.content.Context.MODE_PRIVATE).edit().clear()
            .putString("sub", "s").putString("email", "a@example.test")
            .putString("profile", "{\"sub\":\"s\",\"email\":\"a@example.test\",\"name\":\"Fiók neve\"}").commit()
        compose.setContent {
            TrainPilotTheme {
                SettingsMainScreen(uiState = TrainPilotUiState(), strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onBack = {}, onOpenPalettePicker = {}, onSelectLanguage = {}, onCycleRestTimer = {}, onToggleSoundAlert = {},
                    onToggleProgressionModal = {}, onToggleVibration = {}, onNavigateToSubScreen = {}, onShowBanner = {})
            }
        }
        compose.onNodeWithTag("settings_google_expand").performScrollTo().performClick()
        // The account state reaches the card asynchronously from the repository flow.
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("settings_google_sync_now_btn").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("settings_google_login_btn").assertDoesNotExist()
        compose.onNodeWithTag("settings_google_auto_sync").assertExists()
        for (gone in listOf("settings_google_maintenance_toggle", "settings_google_drive_sync_btn",
                "settings_google_calendar_sync_btn", "settings_google_recover_earlier", "settings_google_auto_drive"))
            compose.onNodeWithTag(gone).assertDoesNotExist()
        compose.onNodeWithTag("settings_google_disconnect_btn").performScrollTo().performClick()
        compose.onNodeWithTag("settings_google_disconnect_sheet").assertExists()
        compose.onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag("settings_google_disconnect_sheet")))
            .assertCountEquals(2).assertAll(isOff())
        compose.onNodeWithTag("settings_google_disconnect_cancel").performClick()
        compose.onNodeWithTag("settings_google_disconnect_sheet").assertDoesNotExist()
        context.getSharedPreferences("repforge-google", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }
}
