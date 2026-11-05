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
    private fun awaitTag(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun journal() {
        compose.setContent { TrainPilotTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                StudioHealthMetricsBrowser(hc, "en", "fixture")
            }
        } }
    }

    @Test fun dayExpansionShowsProviderProjectionAndAllOriginsWithoutAnotherSelector() {
        store.healthDay(day.toString(), JSONObject().put("steps", 0), store.generation())
        store.healthDay(day.toString(), JSONObject().put("source", "watch").put("steps", 0), store.generation())
        store.healthDay(day.toString(), JSONObject().put("source", "phone").put("steps", 10), store.generation())
        store.samsungDay(day.toString(), JSONObject().put("steps", 321), JSONArray(), store.generation())
        store.importRecords(JSONArray().put(record("hcwatch")).put(record("hcphone", "phone"))
            .put(record("samsung", provider = samsung)), store.generation())
        val snapshot = store.exportSnapshot().toString()
        journal()
        val summary = "journal_summary_${day}_steps"
        awaitTag(summary)
        compose.onNodeWithTag(summary).assertTextEquals("0")
        compose.onNodeWithTag("studio_health_record_browser").assertDoesNotExist()
        compose.onNodeWithTag("journal_day_$day").performScrollTo().performClick()
        awaitTag("health_record_hcwatch")
        compose.onNodeWithTag("journal_metric_${day}_steps").performScrollTo().assertTextContains("0")
        compose.onNodeWithTag("health_record_browser").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_$samsung").performScrollTo().performClick()
        awaitTag(summary)
        compose.onNodeWithTag(summary).assertTextEquals("321")
        compose.onNodeWithTag("health_record_hcwatch").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_$hc").performScrollTo().performClick()
        awaitTag(summary)
        // #151: provider aggregate remains authoritative; origin-specific values are never added.
        compose.onNodeWithTag(summary).assertTextEquals("0")
        compose.onNodeWithTag("journal_origins").assertDoesNotExist()
        compose.onNodeWithTag("journal_origin_all").assertDoesNotExist()
        compose.onNodeWithTag("journal_origin_phone").assertDoesNotExist()
        compose.onNodeWithTag("journal_day_$day").performScrollTo().performClick()
        awaitTag("health_record_hcphone")
        compose.onNodeWithTag("health_record_hcwatch").assertExists()
        compose.onNodeWithTag("journal_metric_${day}_weightKg").assertDoesNotExist()
        compose.runOnIdle { assertEquals(snapshot, store.exportSnapshot().toString()) }
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
        compose.onNodeWithTag("journal_metric_${day}_steps").performScrollTo().assertTextContains("0")
        listOf("weightKg", "activeCalories", "averageHeartRate", "vo2Max").forEach {
            compose.onNodeWithTag("journal_metric_${day}_$it").assertDoesNotExist()
        }
    }

    @Test fun refreshReadsSelectedProviderAndWaitsForNativeRevisionWhileBusyBlocksRepeat() {
        store.healthDay(day.toString(), JSONObject().put("steps", 1), store.generation())
        val requests = mutableListOf<String>()
        var revision by mutableLongStateOf(0)
        var busy by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent { TrainPilotTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                StudioHealthMetricsBrowser(hc, "en", revision.toString(),
                    onRefreshProvider = { requests.add(it); busy = true }, refreshBusy = busy, refreshError = error)
            }
        } }
        val summary = "journal_summary_${day}_steps"
        awaitTag(summary)
        compose.onNodeWithTag("journal_reload").performScrollTo().performClick()
        assertEquals(listOf(hc), requests)
        compose.onNodeWithTag("journal_reload").assertIsNotEnabled()
        compose.runOnIdle {
            store.healthDay(day.toString(), JSONObject().put("steps", 7), store.generation())
            busy = false
            revision++
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("7").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(summary).assertTextEquals("7")
        compose.onNodeWithTag("journal_provider_$samsung").performScrollTo().performClick()
        awaitTag(summary)
        compose.onNodeWithTag("journal_reload").performScrollTo().performClick()
        assertEquals(listOf(hc, samsung), requests)
        compose.runOnIdle { busy = false; error = "Samsung Health: PERMISSION_DENIED"; revision++ }
        compose.onNodeWithTag("journal_refresh_error").assertTextEquals("Samsung Health: PERMISSION_DENIED")
    }

    @Test fun cancellingDateSelectionPreservesRangeAndAnInvalidRangeCannotBeApplied() {
        journal()
        awaitTag("journal_day_$day")
        compose.onNodeWithTag("journal_range_from").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_next_year").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_cancel").performScrollTo().performClick()
        compose.onNodeWithTag("journal_range_apply").assertDoesNotExist()
        compose.onNodeWithTag("journal_day_$day").assertExists()
        compose.onNodeWithTag("journal_range_from").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_next_year").performScrollTo().performClick()
        // A selected day remains independent of the navigated month until a day is tapped.
        compose.onNodeWithTag("health_date_day_1").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_confirm").performScrollTo().performClick()
        compose.onNodeWithTag("journal_range_apply").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("journal_day_$day").assertExists()
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
