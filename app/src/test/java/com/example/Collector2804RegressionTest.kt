package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.ExerciseSetItem
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.RestDockScrollSizing
import com.example.ui.components.StudioRestTimerDock
import com.example.ui.components.isActiveMeasurementScreen
import com.example.ui.components.lastSetDiffers
import com.example.ui.components.sheetHostState
import com.example.ui.screens.CloudSyncStatus
import com.example.ui.screens.cloudServiceStatus
import com.example.ui.screens.cloudSyncHeaderStatus
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 2804: #347 rest dock and workout bar, #348 "last time" line, #349 contrast palettes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Collector2804RegressionTest {
    @get:Rule val compose = createComposeRule()

    // ---- #347 ----

    @Test fun workoutBarFollowsTheScreenUnderAnOpenExerciseLibrarySheet() {
        val swapping = TrainPilotUiState(currentTab = MainTab.WORKOUT, currentSubScreen = SubScreen.EXERCISE_LIBRARY,
            backStack = listOf(NavRoute(MainTab.WORKOUT, SubScreen.ACTIVE_STRENGTH)))
        assertFalse(isActiveMeasurementScreen(swapping.currentSubScreen))
        assertTrue("the bar must stay while the library sheet is open",
            isActiveMeasurementScreen(swapping.sheetHostState().currentSubScreen))
    }

    @Test fun anUntouchedRestStaysHiddenAtTheStart() {
        compose.setContent { TrainPilotTheme {
            StudioRestTimerDock(TrainPilotUiState(restTimerRemainingSec = 90, defaultRestSec = 90, restTimerRunning = false),
                false, {}, {}, {}, {}, "Kihagyás", showIdle = false)
        } }
        compose.onNodeWithTag("sticky_rest_timer_compact").assertDoesNotExist()
    }

    @Test fun restModeIsShownByTheArrowOnlyWithSpokenLabels() {
        var large by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { TrainPilotTheme {
            StudioRestTimerDock(TrainPilotUiState(restTimerRemainingSec = 60, defaultRestSec = 90, restTimerRunning = true),
                large, { large = it }, {}, {}, {}, "Kihagyás")
        } }
        compose.onNodeWithContentDescription("Pihenő kibontása").performClick()
        compose.onNodeWithTag("sticky_rest_timer_large").assertExists()
        compose.onNodeWithText("Nagy mód", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Lapos mód").assertDoesNotExist()
        compose.onNodeWithText("Kompakt mód").assertDoesNotExist()
        compose.onNodeWithContentDescription("Pihenő összecsukása").performClick()
        compose.onNodeWithTag("sticky_rest_timer_compact").assertExists()
    }

    @Test fun tickingASetDoesNotOpenTheLargeRestOnItsOwn() {
        // Large mode comes only from the user's drag or the arrow; the old automatic scroll trigger is gone.
        val source = java.io.File("src/main/java/com/example/ui/screens/measure/ActiveStrengthWorkoutScreen.kt").readText()
        val strength = source.substringAfter("fun ActiveStrengthWorkoutScreen(").substringBefore("fun ActiveTimedWorkoutScreen(")
        assertFalse(strength.contains("restTimerManualOverride ?: isScrolledDown"))
        assertTrue(strength.contains("NestedScrollSource.UserInput"))
        assertTrue(strength.contains("showIdle = false"))
        // 2808/9: the strength list sizes the dock only through the user-scroll hook, which gates on UserInput.
        assertTrue(strength.contains("rememberRestDockScrollConnection("))
        assertTrue(strength.contains(".nestedScroll(userScrollSizesRest)"))
        val hook = java.io.File("src/main/java/com/example/ui/components/RestDockScrollSizing.kt").readText()
        assertTrue(hook.contains("source == NestedScrollSource.UserInput) userGesture = true"))
        // A scroll that is not the user's (ticking a set, layout shift) never opens large, at any position.
        assertFalse(RestDockScrollSizing.afterScroll(false, userDriven = false, scrolledToSetRows = true, deltaY = -400f))
        assertFalse(RestDockScrollSizing.afterScroll(false, userDriven = false, scrolledToSetRows = true, deltaY = 400f))
    }

    @Test fun userScrollOntoTheSetRowsOpensLargeAndScrollingBackUpReturnsCompact() {
        val top = RestDockScrollSizing.scrolledToSetRows(0, 0)
        val inHeader = RestDockScrollSizing.scrolledToSetRows(0, RestDockScrollSizing.HEADER_SCROLL_PX)
        val onSets = RestDockScrollSizing.scrolledToSetRows(0, RestDockScrollSizing.HEADER_SCROLL_PX + 1)
        val further = RestDockScrollSizing.scrolledToSetRows(3, 0)
        assertFalse(top); assertFalse(inHeader); assertTrue(onSets); assertTrue(further)
        // Down within the header: still compact; down onto the set rows: large.
        var large = RestDockScrollSizing.afterScroll(false, true, inHeader, -30f)
        assertFalse(large)
        large = RestDockScrollSizing.afterScroll(large, true, further, -30f)
        assertTrue(large)
        // Up while still on the set rows keeps large (no flicker mid-list) ...
        large = RestDockScrollSizing.afterScroll(large, true, onSets, 30f)
        assertTrue(large)
        // ... and back up above them returns compact (the 2808 bug: it stayed large).
        large = RestDockScrollSizing.afterScroll(large, true, inHeader, 30f)
        assertFalse("scrolling back up must return compact", large)
        // The arrow opened it at the top: a downward drag at the top keeps it, an upward one closes it.
        assertTrue(RestDockScrollSizing.afterScroll(true, true, top, -30f))
        assertFalse(RestDockScrollSizing.afterScroll(true, true, top, 30f))
        // The arrow closed it on the set rows: an upward drag there keeps it compact.
        assertFalse(RestDockScrollSizing.afterScroll(false, true, further, 30f))
    }

    @Test fun bilateralListUsesTheSameUserScrollSizing() {
        val source = java.io.File("src/main/java/com/example/ui/screens/measure/ActiveBilateralWorkoutScreen.kt").readText()
        assertTrue(source.contains("rememberRestDockScrollConnection(bilateralListState"))
        assertTrue(source.contains("state = bilateralListState"))
        assertTrue(source.contains(".nestedScroll(userScrollSizesRest)"))
    }

    // ---- #348 ----

    private fun set(kg: Float, reps: Int) = ExerciseSetItem(1, kg, reps, 2, false, id = "s")

    @Test fun lastTimeLineOnlyWhenItDiffersFromTheFields() {
        assertFalse(lastSetDiffers(set(5.5f, 15), set(5.5f, 15)))
        assertFalse("same at the shown precision", lastSetDiffers(set(5.501f, 15), set(5.5f, 15)))
        assertTrue(lastSetDiffers(set(5.5f, 15), set(6f, 15)))
        assertTrue(lastSetDiffers(set(5.5f, 12), set(5.5f, 15)))
        assertTrue(lastSetDiffers(set(0f, 12), set(0f, 10)))
    }

    // ---- #349 ----

    @Test fun threeContrastPalettesAreReplacedInPlace() {
        val all = TrainPilotPalettes.allPalettes
        assertEquals(32, all.size)
        assertEquals(8, all.count { it.id in 9..16 })
        val expected = mapOf(
            9 to Triple("copper_forge", "Kontraszt Grafit & Arany", listOf(0xFF101216, 0xFF262D36, 0xFF1B2129, 0xFFFFD426, 0xFF171305)),
            13 to Triple("titanium_lime", "Kontraszt Kobaltkék & Lime", listOf(0xFF0E1432, 0xFF293C8A, 0xFF1B285D, 0xFFD9FA3D, 0xFF181D06)),
            16 to Triple("espresso_bronze", "Kontraszt Petrol & Menta", listOf(0xFF0D2024, 0xFF1C3F46, 0xFF142F35, 0xFF69EDC2, 0xFF07291F)))
        for ((id, e) in expected) {
            val p = TrainPilotPalettes.getById(id)
            assertEquals("saved key kept", e.first, p.key)
            assertEquals(e.second, p.nameHu)
            assertEquals(e.third.map { androidx.compose.ui.graphics.Color(it) },
                listOf(p.background, p.surfaceCard, p.surfaceElevated, p.primary, p.onPrimary))
        }
        assertEquals("graphite_gold", TrainPilotPalettes.getById(1).key)
    }

    @Test fun contrastPalettesKeepNormalTextReadable() {
        // Text, state colours and (since 2806, palettes 12/15 fixed) the accent on every contrast palette.
        for (p in TrainPilotPalettes.allPalettes.filter { it.id in 9..16 }) {
            val accent = listOf(p.primary)
            for (fg in accent + listOf(p.textPrimary, p.textSecondary, p.success, p.warning, p.danger,
                p.sleepAccent, p.heartAccent, p.stepsAccent, p.energyAccent))
                for (bg in listOf(p.background, p.surfaceCard, p.surfaceElevated))
                    assertTrue("palette ${p.id}: ${NativeThemeTokens.contrast(fg, bg)}", NativeThemeTokens.contrast(fg, bg) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(p.onPrimary, p.primary) >= 4.5f)
        }
    }

    // ---- #352 ----

    @Test fun cloudStatusIsJudgedPerServiceAndNeverFromAnOldManualDate() {
        val now = java.time.Instant.parse("2026-11-24T12:00:00Z")
        val old = "2026-11-10T08:00:00Z"
        assertEquals(CloudSyncStatus.NEUTRAL, cloudServiceStatus(false, old, false, false, now))
        assertEquals(CloudSyncStatus.NEUTRAL, cloudServiceStatus(true, "", false, false, now))
        assertEquals("manual sync: an old date is no warning", CloudSyncStatus.OK, cloudServiceStatus(true, old, false, false, now))
        assertEquals(CloudSyncStatus.LATE, cloudServiceStatus(true, old, false, true, now))
        assertEquals(CloudSyncStatus.OK, cloudServiceStatus(true, "2026-11-23T08:00:00Z", false, true, now))
        assertEquals(CloudSyncStatus.ERROR, cloudServiceStatus(true, "2026-11-24T08:00:00Z", true, false, now))
        assertEquals(CloudSyncStatus.OK, cloudServiceStatus(true, "2026-11-23 04:04:33", false, false, now))
    }

    @Test fun cloudHeaderShowsTheMostSeriousRelevantState() {
        assertEquals(CloudSyncStatus.ERROR, cloudSyncHeaderStatus(listOf(CloudSyncStatus.ERROR, CloudSyncStatus.OK)))
        assertEquals(CloudSyncStatus.LATE, cloudSyncHeaderStatus(listOf(CloudSyncStatus.OK, CloudSyncStatus.LATE)))
        assertEquals("a never-run calendar does not hide a good Drive", CloudSyncStatus.OK,
            cloudSyncHeaderStatus(listOf(CloudSyncStatus.OK, CloudSyncStatus.NEUTRAL)))
        assertEquals(CloudSyncStatus.NEUTRAL, cloudSyncHeaderStatus(listOf(CloudSyncStatus.NEUTRAL, CloudSyncStatus.NEUTRAL)))
    }
}
