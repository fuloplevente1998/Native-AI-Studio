package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.ui.components.HomeCoachAdviceCard
import com.example.ui.components.QuickWorkoutSheet
import com.example.ui.screens.BackupRestoreScreen
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
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Post2757PresentationRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun quickSheetFillsWindowSelectsExerciseAndCloses() {
        val first = ExerciseItem("one", "Első gyakorlat")
        val second = ExerciseItem("two", "Második gyakorlat")
        var started: ExerciseItem? = null
        var dismissed = 0
        compose.setContent { TrainPilotTheme {
            QuickWorkoutSheet(exercises = listOf(first, second), onStartStrengthQuick = {},
                onStartStrengthWithExercise = { started = it }, onStartTimedQuick = {},
                onStartOutdoorGps = {}, onBrowseExercises = {}, onDismiss = { dismissed++ })
        } }
        val sheet = compose.onNodeWithTag("quick_workout_sheet").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Sheet should open at full available height", (sheet.bottom - sheet.top).value >= 800f)
        compose.onNodeWithTag("quick_strength_picker").performScrollTo().performClick()
        compose.onNodeWithTag("quick_strength_option_two").performClick()
        compose.onNodeWithTag("quick_strength_start").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(second, started) }
        compose.onNodeWithContentDescription(TrainPilotStrings.forLanguage(AppLanguage.HU).closeAction).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test fun todayTilesShareFramePreserveMissingValuesAndDestinations() {
        val calls = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            HomeScreen(TrainPilotUiState(), TrainPilotStrings.forLanguage(AppLanguage.HU),
                onStartOrContinueWorkout = {}, onOpenProgramDayDetails = {}, onOpenQuickWorkout = {},
                onOpenWatchAndSync = {}, onOpenHealthSleep = { calls += "sleep" },
                onOpenHealthHeart = { calls += "pulse" }, onOpenHealthSteps = { calls += "steps" },
                onOpenCoachReadiness = { calls += "recovery" }, onOpenGoalsDialog = {},
                onOpenPersonalPlanner = {}, onOpenHealthToday = { calls += "today" })
        } }
        val destinations = mapOf("sleep" to "sleep", "calories" to "today", "pulse" to "pulse",
            "steps" to "steps", "workout" to "today", "recovery" to "recovery")
        destinations.forEach { (tile, destination) ->
            val node = compose.onNode(hasTestTag("home_today_$tile") and
                hasAnyAncestor(hasTestTag("home_today_status_group")))
            if (tile != "workout") node.assertTextContains("—")
            node.performScrollTo().performClick()
            compose.runOnIdle { assertEquals(destination, calls.last()) }
        }
        compose.runOnIdle { assertEquals(6, calls.size) }
    }

    @Test fun longCoachAdviceExpandsWithoutOpeningCoach() {
        var opened = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                TrainPilotTheme { Box(Modifier.width(260.dp)) {
                    HomeCoachAdviceCard(TrainPilotUiState(coachReadinessScore = 30),
                        TrainPilotStrings.forLanguage(AppLanguage.HU)) { opened++ }
                } }
            }
        }
        val summary = compose.onNodeWithTag("home_coach_summary", useUnmergedTree = true)
        val collapsedBounds = summary.getUnclippedBoundsInRoot()
        val collapsedHeight = collapsedBounds.bottom - collapsedBounds.top
        compose.onNodeWithTag("home_coach_expand_summary", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(0, opened) }
        val expandedBounds = summary.getUnclippedBoundsInRoot()
        assertTrue(expandedBounds.bottom - expandedBounds.top > collapsedHeight)
        compose.onNodeWithTag("home_coach_advice_card").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun backupLabelsFitAtLargeFontInHungarianAndGerman() {
        var language by mutableStateOf(AppLanguage.HU)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                TrainPilotTheme { Box(Modifier.width(320.dp)) {
                    BackupRestoreScreen(TrainPilotUiState(language = language), TrainPilotStrings.forLanguage(language),
                        onBack = {}, onOpenSettings = {}, onSelectFormat = {}, onToggleIncludeHealth = {}, onShowBanner = {})
                } }
            }
        }
        val labels = mapOf(
            AppLanguage.HU to listOf("Mentés (JSON)", "Visszaállítás / Importálás"),
            AppLanguage.DE to listOf("Sichern (JSON)", "Wiederherstellen / Importieren")
        )
        labels.forEach { (lang, texts) ->
            compose.runOnIdle { language = lang }
            texts.forEach { label ->
                compose.onNodeWithTag("backup_restore_screen").performScrollToNode(hasText(label))
                val text = compose.onNodeWithText(label, useUnmergedTree = true)
                text.performScrollTo().assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                val action = text.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
                assertTrue("No text layout for $label", action.action!!.invoke(layouts))
                assertTrue("No text layout for $label", layouts.isNotEmpty())
                assertFalse("Label overflows: $label", layouts.single().hasVisualOverflow)
                val button = compose.onNode(hasClickAction() and hasAnyDescendant(hasText(label)), useUnmergedTree = true)
                val textBounds = text.getUnclippedBoundsInRoot()
                val buttonBounds = button.getUnclippedBoundsInRoot()
                assertTrue("Label exceeds button: $label", textBounds.left >= buttonBounds.left &&
                    textBounds.right <= buttonBounds.right && textBounds.top >= buttonBounds.top && textBounds.bottom <= buttonBounds.bottom)
            }
        }
    }
}
