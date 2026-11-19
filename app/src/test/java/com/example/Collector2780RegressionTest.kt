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
        compose.onNodeWithTag("statistics_steps_column_12").performClick()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextContains("189", substring = true)
        compose.onNodeWithTag("statistics_steps_column_11").performClick()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextEquals("nincs szinkronizálva")
        compose.onNodeWithTag("statistics_steps_column_0").performClick()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true).assertTextEquals("nincs szinkronizálva")
    }

    @Test fun healthConnectDayExplainsMissingActiveCaloriesOnlyWhenTheyAreMissing() {
        val hc = com.repforge.app.HealthJournalStore.HC
        org.junit.Assert.assertTrue(com.example.ui.components.healthConnectLacksActiveCalories(hc, setOf("steps", "totalCalories")))
        org.junit.Assert.assertFalse(com.example.ui.components.healthConnectLacksActiveCalories(hc, setOf("steps", "activeCalories")))
        org.junit.Assert.assertFalse(com.example.ui.components.healthConnectLacksActiveCalories(hc, emptySet())) // empty day: no claim
        org.junit.Assert.assertFalse(com.example.ui.components.healthConnectLacksActiveCalories(
            com.repforge.app.HealthJournalStore.SAMSUNG, setOf("steps")))
    }
}
