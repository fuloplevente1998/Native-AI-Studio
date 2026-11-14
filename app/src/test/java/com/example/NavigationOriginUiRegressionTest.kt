package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.LoggedWorkoutEntry
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.TrainPilotRouteContent
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.screens.ProgressTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ui.theme.TrainPilotButton
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationOriginUiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realProgressWorkoutRowAndDetailCloseOrSystemBackRestoreSectionAndScroll() {
        val model = TrainPilotViewModel()
        val workout = LoggedWorkoutEntry(id = "return-workout", titleHu = "Visszatérési próba",
            dateLabelHu = "${LocalDate.now()} 12:00", savedStartIso = "${LocalDate.now()}T12:00:00Z",
            durationMinutes = 40, totalVolumeKg = 1200)
        val field = TrainPilotViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(model) as MutableStateFlow<TrainPilotUiState>
        flow.value = model.uiState.value.copy(currentTab = MainTab.PROGRESS, progressMainSection = 0,
            expandedStatsCardIndex = 2, loggedWorkouts = listOf(workout))
        compose.setContent { TrainPilotTheme {
            val state by model.uiState.collectAsState()
            val strings = TrainPilotStrings.forLanguage(state.language)
            BackHandler(state.currentSubScreen != SubScreen.NONE) { model.navigateBack() }
            Box(Modifier.size(400.dp, 480.dp)) { TrainPilotRouteContent(state) {
                if (state.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL) {
                    LogWorkoutDetailScreen(state, strings, onBack = { model.navigateBack() }, onOpenSettings = {},
                        onSelectDetailTab = model::selectLogDetailTab, onToggleExpandedExercise = {},
                        onOpenPhotos = {}, onOpenRouteMap = {}, onOpenWatchSync = {}, onOpenHealthTab = {})
                } else {
                    ProgressTabScreen(state, strings, onSelectMainSection = model::selectProgressMainSection,
                        onSelectExercise = model::selectProgressExercise, onSelectRange = model::selectProgressRange,
                        onSelectVolumeBar = model::selectVolumeBar, onSelectExpandedStatsCard = model::selectExpandedStatsCard,
                        onSelectStatsSubTab = model::selectStatsSubTab, onToggleCardioFilterStepsMode = model::toggleCardioFilterStepsMode,
                        onOpenWorkoutDetail = model::openLogWorkoutDetail, onOpenSettings = {}, onOpenPalettePicker = {})
                }
            } }
        } }
        for (systemBack in listOf(false, true)) {
            val row = compose.onNodeWithTag("progress_workout_${workout.id}").performScrollTo()
            val scroll = compose.onNodeWithTag("progress_overview_list")
                .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("The workout row must be reached by scrolling", scroll > 0f)
            if (!systemBack) {
                val bounds = row.fetchSemanticsNode().boundsInRoot
                fun pixels() = compose.runOnIdle {
                    val host = compose.activity.findViewById<View>(android.R.id.content)
                    val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
                    host.draw(Canvas(bitmap))
                    bitmap.asImageBitmap().toPixelMap()
                }
                val before = pixels()
                row.performTouchInput { down(center) }
                // Scrollable clickables defer their press indication to distinguish a drag.
                compose.runOnIdle {
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                        .idleFor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                }
                compose.mainClock.advanceTimeBy(200)
                val held = pixels()
                val y = (bounds.top + 4).roundToInt()
                for (x in listOf((bounds.left + 12).roundToInt(), (bounds.right - 12).roundToInt()))
                    assertNotEquals("Whole workout row must react, including its ends", before[x, y].toArgb(), held[x, y].toArgb())
                row.performTouchInput { cancel() }
                compose.runOnIdle { assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen) }
            }
            row.performClick()
            compose.onNodeWithTag("log_workout_detail_screen").assertExists()
            if (systemBack) compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            else compose.onNodeWithTag("subscreen_back_button").performScrollTo().performClick()
            compose.onNodeWithTag("progress_overview_list").assertExists()
            val returned = compose.onNodeWithTag("progress_overview_list")
                .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertEquals("Return to the same scrolled origin", scroll, returned, 1f)
            compose.runOnIdle {
                assertEquals(0, model.uiState.value.progressMainSection)
                assertEquals(2, model.uiState.value.expandedStatsCardIndex)
            }
        }
    }

    @Test fun aPoppedEditorDoesNotResurrectItsCancelledLocalDraft() {
        val model = TrainPilotViewModel()
        model.navigateToSubScreen(SubScreen.SETTINGS_MAIN)
        compose.setContent { TrainPilotTheme {
            val state by model.uiState.collectAsState()
            TrainPilotRouteContent(state) {
                if (state.currentSubScreen == SubScreen.BACKUP_RESTORE) {
                    var draft by rememberSaveable { mutableIntStateOf(0) }
                    TrainPilotButton(onClick = { draft++ }, modifier = Modifier.testTag("local_draft")) { Text("$draft") }
                } else Text("Settings")
            }
        } }
        compose.runOnIdle { model.navigateToSubScreen(SubScreen.BACKUP_RESTORE) }
        compose.onNodeWithTag("local_draft").performClick().assertTextEquals("1")
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithText("Settings").assertExists()
        compose.runOnIdle { model.navigateToSubScreen(SubScreen.BACKUP_RESTORE) }
        compose.onNodeWithTag("local_draft").assertTextEquals("0")
    }
}
