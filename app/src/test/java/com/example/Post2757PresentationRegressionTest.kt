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
import com.example.ui.components.QuickWorkoutHubSheet
import com.example.ui.screens.ExerciseLibraryScreen
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

    @Test fun quickSheetHostsThePickerInTabsStartsAnExerciseAndHandsOutdoorToTheLiveScreen() {
        // 2772: one sheet with Strength / Timed / Outdoor tabs; no separate picker or GPS page.
        val strength = ExerciseItem("one", "Első gyakorlat")
        val timed = ExerciseItem("two", "Plank", isTimed = true)
        val moreTimed = (1..16).map { ExerciseItem("timed_$it", "Időzített $it", isTimed = true) }
        var tab by mutableIntStateOf(0)
        var started: ExerciseItem? = null
        var outdoor: String? = null
        var dismissed = 0
        val state = TrainPilotUiState(exercises = listOf(strength, timed) + moreTimed)
        compose.setContent { TrainPilotTheme {
            QuickWorkoutHubSheet(AppLanguage.HU, tab, { tab = it }, { dismissed++ }, { outdoor = it },
                strengthContent = { ExerciseLibraryScreen(state, {}, {}, {}, {}, {}, onStartExerciseNow = { started = it }, embedded = true) },
                timedContent = { ExerciseLibraryScreen(state, {}, {}, {}, {}, {}, onStartExerciseNow = { started = it }, embedded = true, timedOnly = true) })
        } }
        fun assertBodyReachesSheetBottom() {
            val sheet = compose.onNodeWithTag("quick_workout_sheet_host_body").getUnclippedBoundsInRoot()
            val body = compose.onNodeWithTag("quick_workout_body").getUnclippedBoundsInRoot()
            assertEquals("Scrollable body must use the entire space below the tabs", sheet.bottom, body.bottom)
        }

        compose.onNodeWithTag("quick_workout_sheet").assertExists()
        assertBodyReachesSheetBottom()
        compose.onNodeWithText("Első gyakorlat").assertExists()
        compose.onNodeWithText("Plank").assertExists()
        compose.onNodeWithTag("quick_tab_1").performClick()
        compose.runOnIdle { assertEquals(1, tab) }
        assertBodyReachesSheetBottom()
        // Timed tab lists only time-based exercises.
        compose.onNodeWithText("Első gyakorlat").assertDoesNotExist()
        compose.onNodeWithText("Plank").assertExists()
        val last = moreTimed.last()
        compose.onNodeWithTag("exercise_library_start_now_${last.id}")
            .performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(last, started) }
        compose.onNodeWithTag("quick_tab_2").performClick()
        assertBodyReachesSheetBottom()
        compose.onNodeWithTag("quick_outdoor_kerékpározás").performClick()
        compose.onNodeWithTag("quick_outdoor_start").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Kerékpározás", outdoor) }
        compose.onNodeWithTag("quick_workout_close").performClick()
        compose.runOnIdle { assertEquals(1, dismissed); assertEquals(last, started) }
    }

    @Test fun quickSheetGrowsForLongerContentWithoutForcingShortTabsToFullHeight() {
        var tab by mutableIntStateOf(0)
        compose.setContent { TrainPilotTheme {
            QuickWorkoutHubSheet(AppLanguage.HU, tab, { tab = it }, {}, {},
                strengthContent = { androidx.compose.material3.Text("Short content") },
                timedContent = {
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxHeight())
                })
        } }
        val content = compose.onNodeWithTag("quick_workout_sheet_host_body")
        val short = content.getUnclippedBoundsInRoot().let { it.bottom - it.top }
        compose.onNodeWithTag("quick_tab_1").performClick()
        val tall = content.getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Short content must not reserve the full viewport", tall > short)
        compose.onNodeWithTag("quick_tab_0").performClick()
        assertEquals(short, content.getUnclippedBoundsInRoot().let { it.bottom - it.top })
    }

    @Test fun allSixHomeTodayTilesOpenTheMatchingHealthDetailAndCloseOnHome() {
        var journalFocus: Pair<String, String>? = null
        var openedCoach = false
        var state by mutableStateOf(TrainPilotUiState())
        compose.setContent { TrainPilotTheme {
            HomeScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                onStartOrContinueWorkout = {}, onOpenProgramDayDetails = {}, onOpenQuickWorkout = {},
                onOpenWatchAndSync = {}, onOpenCoachReadiness = {}, onOpenGoalsDialog = {},
                onOpenPersonalPlanner = {}, onOpenHealthJournal = { journalFocus = it },
                onNavigateFromToday = { openedCoach = it == com.example.model.SubScreen.COACH_DETAIL })
        } }
        val titles = mapOf(
            "sleep" to "Alvás részletek",
            "calories" to "Aktív kalória részletek",
            "pulse" to "Mai pulzus",
            "steps" to "Lépések részletek",
            "workout" to "Mai edzések",
            "recovery" to "Végső Coach készenlét"
        )
        titles.forEach { (tile, title) ->
            val node = compose.onNode(hasTestTag("home_today_$tile") and
                hasAnyAncestor(hasTestTag("home_today_status_group")))
            if (tile != "workout") node.assertTextContains("—")
            node.performScrollTo().performClick()
            val sheet = compose.onNodeWithTag("health_today_tile_sheet")
            sheet.assertExists()
            compose.onNodeWithText(title, substring = true).assertExists()
            compose.onNodeWithTag("health_today_tile_close").performClick()
            sheet.assertDoesNotExist()
            node.assertIsDisplayed()
        }
        compose.onNodeWithTag("home_today_sleep").performScrollTo().performClick()
        compose.onNodeWithTag("health_expanded_action_button").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(java.time.LocalDate.now().toString() to "SleepSessionRecord", journalFocus)
        }
        compose.onNodeWithTag("home_today_recovery").performScrollTo().performClick()
        compose.onNodeWithText("Végső Coach készenlét").assertExists()
        compose.onNodeWithTag("recovery_score_breakdown").assertDoesNotExist()
        compose.onNodeWithText("Samsung Energy").assertDoesNotExist()
        compose.runOnIdle {
            state = state.copy(permissionAccess = state.permissionAccess.copy(samsungHealthEnabled = true))
        }
        compose.onNodeWithText("Samsung Energy").assertExists()
        compose.onNodeWithTag("recovery_score_breakdown").assertExists()
        compose.runOnIdle {
            state = state.copy(permissionAccess = state.permissionAccess.copy(samsungHealthEnabled = false))
        }
        compose.onNodeWithText("Samsung Energy").assertDoesNotExist()
        compose.onNodeWithTag("recovery_score_breakdown").assertDoesNotExist()
        compose.onNodeWithTag("health_expanded_action_button").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedCoach) }
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
            AppLanguage.HU to listOf("Mentés (JSON)", "Helyi JSON/ZIP importálása"),
            AppLanguage.DE to listOf("Sichern (JSON)", "Lokale JSON-/ZIP-Datei importieren")
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
