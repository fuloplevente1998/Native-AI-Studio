package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.*
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
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
class HomeCompactStatusRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun sixTodayCellsAndCompactCoachFitTheDefaultHomeViewport() {
        val program = TrainingProgramModel("p", "Alap A/B", "A/B", "", "", emptyList(), true, true,
            listOf(ProgramDayModel("a", "A", "Alap A", "A", 60, emptyList())))
        val state = TrainPilotUiState(profileComplete = true, programs = listOf(program),
            watchName = "Real Watch", watchConnected = true, watchAppAvailable = true,
            dailySteps = 100, activeCaloriesKcal = 37, currentHrBpm = 61, restingHrBpm = 55,
            dailyHealthAvailableFields = setOf("steps", "activeCalories"))
        var coach = 0
        var health = 0
        compose.setContent { TrainPilotTheme {
            Box(Modifier.size(393.dp, 680.dp).clipToBounds().testTag("home_viewport")) {
                HomeScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onOpenPalettePicker = {}, onOpenSettings = {}, onCycleDemoMode = {},
                    onStartOrContinueWorkout = {}, onOpenProgramDayDetails = {}, onOpenQuickWorkout = {},
                    onOpenWatchAndSync = {}, onOpenHealthSleep = {}, onOpenHealthHeart = {}, onOpenHealthSteps = {},
                    onOpenCoachReadiness = { coach++ }, onOpenProgramsList = {}, onOpenCalendarPlanner = {},
                    onOpenGoalsDialog = {}, onOpenPersonalPlanner = {}, onOpenHealthToday = { health++ })
            }
        } }
        for (key in listOf("sleep", "calories", "pulse", "steps", "workout", "recovery"))
            compose.onNodeWithTag("home_today_$key").assertIsDisplayed()
        compose.onNodeWithTag("home_today_pulse").assertTextContains("61 bpm")
        val viewport = compose.onNodeWithTag("home_viewport").getUnclippedBoundsInRoot()
        val card = compose.onNodeWithTag("home_coach_advice_card").assertIsDisplayed()
        assertTrue("Coach should fit without scrolling at default size", card.getUnclippedBoundsInRoot().bottom <= viewport.bottom)
        compose.onNodeWithTag("home_coach_summary", useUnmergedTree = true).assertTextContains("friss adat", substring = true)
        card.performClick(); assertEquals(1, coach)
        compose.onNodeWithTag("home_today_calories").performClick(); assertEquals(1, health)
    }
}
