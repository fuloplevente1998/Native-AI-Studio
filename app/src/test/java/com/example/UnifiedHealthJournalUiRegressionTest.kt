package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.StudioHealthMetricsBrowser
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.HealthRecordDayDetails
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.time.ZoneOffset
import com.repforge.app.erase
import com.repforge.app.exportSnapshot
import com.repforge.app.healthDay
import com.repforge.app.importRecords
import com.repforge.app.samsungDay

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class UnifiedHealthJournalUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var store: HealthJournalStore
    private val hc = HealthJournalStore.HC
    private val samsung = HealthJournalStore.SAMSUNG
    private val day = LocalDate.now()

    @Before fun prepare() {
        store = HealthJournalStore.get(ApplicationProvider.getApplicationContext<Context>())
        store.erase()
    }
    @After fun clean() { store.erase() }

    private fun record(id: String, source: String = "watch", provider: String = hc): JSONObject {
        val time = day.atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return JSONObject().put("recordId", id).put("channel", provider).put("source", source)
            .put("type", "StepsRecord").put("day", day.toString()).put("startMs", time)
            .put("endMs", time + 60_000).put("lastModifiedMs", time)
            .put("data", JSONObject().put("value", 0).put("unit", "steps"))
    }
    // Compose 1.10 may deliver a snapshot invalidation and its semantics on different frames.
    // Wait for the actual UI condition, not an assumed number of recomposition frames.
    private fun awaitUi(condition: () -> Boolean) {
        val previous = compose.mainClock.autoAdvance
        compose.mainClock.autoAdvance = false
        try {
            compose.waitUntil(30_000) {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeByFrame()
                condition()
            }
        } finally { compose.mainClock.autoAdvance = previous }
    }

    private fun awaitTag(tag: String, useUnmergedTree: Boolean = false) = awaitUi {
        compose.onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }

    private fun awaitReloadDisabled() = awaitUi {
        compose.onAllNodes(hasTestTag("journal_reload") and isNotEnabled()).fetchSemanticsNodes().size == 1
    }
    private fun journal(samsungSupplement: Boolean = false) {
        compose.setContent { TrainPilotTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                StudioHealthMetricsBrowser("en", "fixture", samsungSupplement = samsungSupplement)
            }
        } }
    }

    @Test fun dayExpansionKeepsHcTotalsAndSupplementWithoutRecordList() {
        store.healthDay(day.toString(), JSONObject().put("steps", 0), store.generation())
        store.healthDay(day.toString(), JSONObject().put("source", "watch").put("steps", 0), store.generation())
        store.healthDay(day.toString(), JSONObject().put("source", "phone").put("steps", 10), store.generation())
        store.samsungDay(day.toString(), JSONObject().put("steps", 321).put("activeCalories", 27), JSONArray(), store.generation())
        store.importRecords(JSONArray().put(record("hcwatch")).put(record("hcphone", "phone"))
            .put(record("samsung", provider = samsung)), store.generation())
        val snapshot = store.exportSnapshot().toString()
        journal(samsungSupplement = true)
        val summary = "journal_summary_${day}_steps"
        awaitTag(summary, useUnmergedTree = true)
        compose.onNodeWithTag(summary, useUnmergedTree = true).assertTextEquals("0")
        compose.onNodeWithTag("studio_health_record_browser").assertDoesNotExist()
        val dayRow = compose.onNodeWithTag("journal_day_$day")
        dayRow.performScrollTo()
        val bounds = dayRow.getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue("Collapsed day height was $height", height in 48.dp..64.dp)
        dayRow.performClick()
        compose.onNodeWithTag("journal_metric_${day}_steps").performScrollTo()
        compose.onNode(hasAnyAncestor(hasTestTag("journal_metric_${day}_steps")) and hasText("0"), useUnmergedTree = true).assertExists()
        compose.onNode(hasAnyAncestor(hasTestTag("journal_metric_${day}_activeCalories")) and hasText("27 kcal"), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("journal_metric_${day}_weightKg").assertDoesNotExist()
        compose.onNodeWithTag("journal_records_toggle_$day").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_${day}_$samsung").assertDoesNotExist()
        compose.onNodeWithTag("health_record_hcwatch").assertDoesNotExist()
        compose.onNodeWithTag("health_record_hcphone").assertDoesNotExist()
        compose.onNodeWithTag("health_record_samsung").assertDoesNotExist()
        compose.onNodeWithTag("health_record_browser").assertDoesNotExist()
        compose.runOnIdle { assertEquals(snapshot, store.exportSnapshot().toString()) }
    }

    @Test fun todaySleepTileStillShowsItsStoredRecords() {
        val sleep = record("today-sleep").put("type", "SleepSessionRecord")
            .put("data", JSONObject().put("durationMinutes", 1.0))
        store.importRecords(JSONArray().put(sleep), store.generation())
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.HealthTabScreen(
                com.example.ui.viewmodel.TrainPilotUiState(primaryHealthSourceSamsung = false),
                com.example.localization.TrainPilotStrings.forLanguage(com.example.model.AppLanguage.HU),
                {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_sleep").performClick()
        awaitTag("health_record_today-sleep")
        compose.onNodeWithTag("health_record_today-sleep").assertExists()
        compose.onNodeWithTag("journal_records_toggle_$day").assertDoesNotExist()
    }

    @Test fun availableZeroRemainsVisibleWhileDeniedErroredAndMissingRowsAreHidden() {
        store.healthDay(day.toString(), JSONObject().put("steps", 0).put("weightKg", 70)
            .put("activeCalories", 20).put("averageHeartRate", 80)
            .put("permissions", JSONObject().put("READ_WEIGHT", false))
            .put("failedFields", JSONArray().put("activeCalories"))
            .put("metricProviders", JSONObject().put("averageHeartRate", samsung)), store.generation())
        journal()
        awaitTag("journal_day_$day")
        compose.onNodeWithTag("journal_day_$day").performScrollTo().performClick()
        compose.onNodeWithTag("journal_metric_${day}_steps").performScrollTo()
        compose.onNode(hasAnyAncestor(hasTestTag("journal_metric_${day}_steps")) and hasText("0"), useUnmergedTree = true).assertExists()
        listOf("weightKg", "activeCalories", "averageHeartRate", "vo2Max").forEach {
            compose.onNodeWithTag("journal_metric_${day}_$it").assertDoesNotExist()
        }
    }

    @Test fun refreshWaitsForNativeRevisionWhileBusyBlocksRepeat() {
        store.healthDay(day.toString(), JSONObject().put("steps", 1), store.generation())
        var requests = 0
        var revision by mutableLongStateOf(0)
        var busy by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent { TrainPilotTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                StudioHealthMetricsBrowser("en", revision.toString(),
                    onRefresh = { requests++; busy = true }, refreshBusy = busy, refreshError = error)
            }
        } }
        val summary = "journal_summary_${day}_steps"
        awaitTag(summary, useUnmergedTree = true)
        compose.onNodeWithTag("journal_reload").performScrollTo()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("journal_reload").performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, requests)
        awaitReloadDisabled()
        compose.onNodeWithTag("journal_reload").assertIsNotEnabled()
        // A real second tap while the provider is busy must not enqueue another request.
        compose.onNodeWithTag("journal_reload").performTouchInput { click() }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle {
            store.healthDay(day.toString(), JSONObject().put("steps", 7), store.generation())
            busy = false
            revision++
        }
        compose.waitUntil(30_000) {
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodesWithText("7", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(summary, useUnmergedTree = true).assertTextEquals("7")
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("journal_reload").performScrollTo()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("journal_reload").performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(2, requests)
        awaitReloadDisabled()
        compose.onNodeWithTag("journal_reload").assertIsNotEnabled()
        compose.onNodeWithTag("journal_reload").performTouchInput { click() }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle {
            assertEquals(2, requests)
            busy = false; error = "Samsung Health: PERMISSION_DENIED"; revision++
        }
        awaitTag("journal_refresh_error")
        compose.onNodeWithTag("journal_refresh_error").assertTextEquals("Samsung Health: PERMISSION_DENIED")
        compose.mainClock.autoAdvance = true
    }

    @Test fun periodSheetKeepsRangeOnCloseAndTwoTapsInAnyOrderGiveAValidPeriod() {
        journal()
        awaitTag("journal_day_$day")
        // 2772: one period button opens the shared period sheet (same as the Progress log).
        compose.onNodeWithTag("journal_period_button").performScrollTo().performClick()
        compose.onNodeWithTag("journal_calendar_sheet").assertExists()
        compose.onNodeWithTag("journal_jump_today_button").assertExists()
        compose.onNodeWithTag("journal_clear_filter_button").assertExists()
        compose.onNodeWithTag("journal_calendar_done").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("journal_calendar_sheet").assertDoesNotExist()
        compose.onNodeWithTag("journal_day_$day").assertExists()
        // Reverse order (later day first) still yields from <= to; today stays inside the period.
        compose.onNodeWithTag("journal_period_button").performScrollTo().performClick()
        compose.onNodeWithTag("journal_calendar_day_${day.dayOfMonth}").performClick()
        compose.onNodeWithTag("journal_calendar_day_1").performClick()
        compose.onNodeWithTag("journal_calendar_done").performClick()
        awaitTag("journal_day_$day")
        if (day.dayOfMonth > 1) compose.onNodeWithTag("journal_day_${day.minusDays(1)}").assertExists()
        // Today = only today; Clear = back to the last 30 days.
        compose.onNodeWithTag("journal_period_button").performScrollTo().performClick()
        compose.onNodeWithTag("journal_jump_today_button").performClick()
        compose.onNodeWithTag("journal_calendar_done").performClick()
        awaitTag("journal_day_$day")
        compose.onNodeWithTag("journal_day_${day.minusDays(1)}").assertDoesNotExist()
    }

    @Test fun foldingKeepsTheRecordPageAndAReplacementStartsANewGeneration() {
        val records = JSONArray()
        repeat(30) { records.put(record("r$it")) }
        store.importRecords(records, store.generation())
        var visible by mutableStateOf(true)
        var generation by mutableLongStateOf(store.generation())
        compose.setContent { TrainPilotTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                TextButton(onClick = { visible = !visible }, modifier = Modifier.testTag("fold_day")) { Text("Fold") }
                HealthRecordDayDetails(day, hc, "watch", "", "en", generation, "fixture", visible)
            }
        } }
        awaitTag("health_record_r29")
        compose.onNodeWithTag("journal_records_next_$day").performScrollTo().performClick()
        awaitTag("health_record_r4")
        compose.onNodeWithTag("health_record_r29").assertDoesNotExist()
        compose.onNodeWithTag("fold_day").performScrollTo().performClick()
        compose.onNodeWithTag("health_record_r4").assertDoesNotExist()
        compose.onNodeWithTag("fold_day").performScrollTo().performClick()
        compose.onNodeWithTag("health_record_r4").assertExists()
        compose.onNodeWithTag("health_record_r29").assertDoesNotExist()
        compose.runOnIdle {
            store.erase()
            store.importRecords(JSONArray().put(record("replacement")), store.generation())
            generation = store.generation()
        }
        awaitTag("health_record_replacement")
        compose.onNodeWithTag("health_record_r4").assertDoesNotExist()
        compose.onNodeWithTag("journal_records_previous_$day").assertIsNotEnabled()
    }
}
