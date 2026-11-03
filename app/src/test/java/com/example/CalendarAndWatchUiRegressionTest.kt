package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.screens.WatchAndSyncScreen
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarAndWatchUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calendarActuallyPaintsLightThenDarkOnThemeChange() {
        var palette by mutableStateOf(TrainPilotPalettes.allPalettes.first { !it.isDark })
        compose.setContent { TrainPilotTheme(palette) {
            CalendarPlannerScreen(TrainPilotUiState(), {}, {}, {}, {}, { _, _ -> }, {})
        } }
        fun panelLuminance(): Float {
            val image = compose.onNodeWithTag("calendar_month_panel").captureToImage().toPixelMap()
            return image[image.width / 2, 8].luminance()
        }
        assertTrue("Light calendar must not remain a dark panel", panelLuminance() > 0.5f)
        compose.runOnIdle { palette = TrainPilotPalettes.allPalettes.first { it.isDark } }
        assertTrue("Dark calendar must follow the changed palette", panelLuminance() < 0.3f)
    }
    @Test fun tappingTheConnectedWatchRequestsInfoAndBusyPreventsDuplicates() {
        var state by mutableStateOf(TrainPilotUiState(watchConnected = true, watchAppAvailable = true, watchName = "Real Watch"))
        var requests = 0
        compose.setContent { TrainPilotTheme {
            WatchAndSyncScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {},
                onRefreshWatchInfo = { requests++ })
        } }
        compose.onNodeWithTag("watch_device_info_card").performClick()
        assertEquals(1, requests)
        compose.runOnIdle { state = state.copy(watchInfoLoading = true, watchInfoMessage = "Óraadatok lekérése…") }
        compose.onNodeWithTag("watch_device_info_card").assertIsNotEnabled()
        assertEquals(1, requests)
    }
}
