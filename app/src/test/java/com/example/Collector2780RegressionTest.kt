package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppLanguage
import com.example.ui.screens.StudioStatisticsContent
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.WeeklyHealthDaySummary
import com.repforge.app.ui.progress.ProgressUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class Collector2780RegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingAStepColumnShowsThatDayAndAMissingDayIsNotZero() {
        val today = LocalDate.now()
        fun day(back: Long, steps: Int, synced: Boolean = true) = WeeklyHealthDaySummary(today.minusDays(back).toString(), "",
            steps, 0, "", 0, 0, "Health Connect", stepsAvailable = synced, stepsSourceLabelHu = "Health Connect")
        val days = listOf(day(0, 7711), day(1, 12189), day(2, 0, synced = false))
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(TrainPilotUiState(language = AppLanguage.HU, statsSubTab = 1, cardioFilterStepsMode = true,
                dailyActivityDays = days), ProgressUiState(), {}, {}, {}, {}, {}, {})
        } }
        // Today is selected by default, so the line is always there (no jump when tapping).
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextContains("711", substring = true)
        val todayColumn = compose.onNodeWithTag("statistics_steps_column_13").assertIsSelected()
        val yesterdayColumn = compose.onNodeWithTag("statistics_steps_column_12").assertIsNotSelected()
        yesterdayColumn.performTouchInput { click() }
        yesterdayColumn.assertIsSelected()
        todayColumn.assertIsNotSelected()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextContains("189", substring = true)
        compose.onNodeWithTag("statistics_steps_column_11").performTouchInput { click() }.assertIsSelected()
        yesterdayColumn.assertIsNotSelected()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextEquals("nincs szinkronizálva")
        compose.onNodeWithTag("statistics_steps_column_0").performTouchInput { click() }.assertIsSelected()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextEquals("nincs szinkronizálva")
    }

    @Test fun dailyFallbackKeepsHcZeroAndFillsOnlyMissingEnergyPulseAndSleep() {
        val hc = com.repforge.app.HealthJournalStore.HC
        val samsung = com.repforge.app.HealthJournalStore.SAMSUNG
        val zone = java.time.ZoneOffset.UTC
        val day = LocalDate.of(2026, 11, 25)
        val now = day.atTime(20, 0).toInstant(zone)
        fun row(provider: String, whenDay: LocalDate = day) =
            org.json.JSONObject().put("channel", provider).put("day", whenDay.toString())
        val primary = row(hc).put("activeCalories", 0)
            .put("restingHeartRate", 56)
            .put("failedFields", org.json.JSONArray().put("totalCalories"))
            .put("totalCalories", 500)
        val supplement = row(samsung).put("activeCalories", 120).put("totalCalories", 620)
            .put("averageHeartRate", 73).put("sleepMinutes", 400)
        val sources = com.repforge.app.ui.health.HealthDaySources(day, primary, supplement, now, zone)

        org.junit.Assert.assertEquals(com.repforge.app.ui.health.HealthDaySources.Value(0.0, hc), sources.metric("activeCalories"))
        org.junit.Assert.assertEquals(com.repforge.app.ui.health.HealthDaySources.Value(620.0, samsung), sources.metric("totalCalories"))
        org.junit.Assert.assertEquals(com.repforge.app.ui.health.HealthDaySources.Value(400.0, samsung), sources.sleep())
        org.junit.Assert.assertEquals(samsung, sources.pulse().provider)
        org.junit.Assert.assertEquals(73.0, sources.pulse().averageBpm!!, 0.0)
        org.junit.Assert.assertNull(sources.pulse().restingBpm)

        val wrongDay = row(samsung, day.minusDays(1)).put("totalCalories", 620)
        val primaryOnly = com.repforge.app.ui.health.HealthDaySources(day, primary, wrongDay, now, zone)
        org.junit.Assert.assertNull(primaryOnly.metric("totalCalories"))
        org.junit.Assert.assertNull(primaryOnly.sleep())
        org.junit.Assert.assertEquals(56.0, primaryOnly.pulse().restingBpm!!, 0.0)
    }
}
