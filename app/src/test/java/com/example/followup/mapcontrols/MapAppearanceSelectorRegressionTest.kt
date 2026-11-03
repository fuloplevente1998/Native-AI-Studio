package com.example.followup.mapcontrols

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.mapcontrols.MapAppearanceSelector
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.data.store.MapAppearance
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class MapAppearanceSelectorRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun condensedStyleControlKeepsAllThreePersistedChoicesReachable() {
        var selected by mutableStateOf(MapAppearance.FOLLOW_APP)
        compose.setContent {
            TrainPilotTheme {
                MapAppearanceSelector(appearance = selected, onSelect = { selected = it })
            }
        }
        compose.onNodeWithTag("route_map_style_surface").assertIsDisplayed()
        compose.onNodeWithTag("route_map_style_toggle").assertIsDisplayed().performClick()
        compose.onNodeWithTag("route_map_choose_dark").assertIsDisplayed().performClick()
        assertEquals(MapAppearance.DARK, selected)

        compose.onNodeWithTag("route_map_style_toggle").performClick()
        compose.onNodeWithTag("route_map_choose_light").assertIsDisplayed().performClick()
        assertEquals(MapAppearance.LIGHT, selected)

        compose.onNodeWithTag("route_map_style_toggle").performClick()
        compose.onNodeWithTag("route_map_follow_app").assertIsDisplayed().performClick()
        assertEquals(MapAppearance.FOLLOW_APP, selected)
        compose.onNodeWithTag("route_map_style_toggle").assertIsDisplayed()
    }
}
