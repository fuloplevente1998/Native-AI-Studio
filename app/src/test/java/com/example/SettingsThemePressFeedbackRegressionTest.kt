package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*
import java.time.Duration
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsThemePressFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realThemeCategoriesCoverTheirPaddingAndFollowTheirRoundOutline() {
        var host: View? = null
        var selected by mutableIntStateOf(9)
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(selected)) {
                val view = LocalView.current
                SideEffect { host = view }
                SettingsMainScreen(uiState = TrainPilotUiState(selectedPaletteId = selected),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU), onBack = {}, onOpenPalettePicker = {},
                    onSelectPalette = { selected = it }, onSelectLanguage = {}, onCycleRestTimer = {},
                    onToggleSoundAlert = {}, onToggleProgressionModal = {}, onToggleVibration = {},
                    onNavigateToSubScreen = {}, onShowBanner = {})
            }
        }
        fun pixels() = compose.runOnIdle {
            val view = requireNotNull(host)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        repeat(4) { index ->
            val tag = "settings_theme_category_toggle_$index"
            list.performScrollToNode(hasTestTag(tag))
            val node = compose.onNodeWithTag(tag)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            val card = compose.onNodeWithTag("settings_theme_category_$index").fetchSemanticsNode().boundsInRoot
            assertEquals("Header must own the full painted width, including padding", card.width, bounds.width, 1f)
            assertEquals("Collapsed category must have no unresponsive inner margin", card.height, bounds.height, 1f)
            val before = pixels()
            compose.mainClock.autoAdvance = false
            try {
                node.performTouchInput { down(center) }
                // Clickable delays press delivery in a LazyColumn while it waits to see
                // whether the gesture becomes a scroll. Drain Android's delayed callback
                // before advancing the separate Compose animation clock.
                compose.runOnIdle {
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getTapTimeout() + 32L))
                }
                compose.mainClock.advanceTimeBy(160)
                compose.runOnIdle { shadowOf(Looper.getMainLooper()).idle() }
                val held = pixels()
                val y = bounds.center.y.roundToInt()
                for (x in listOf((bounds.left + 4).roundToInt(), (bounds.right - 4).roundToInt()))
                    assertNotEquals("Category $index feedback must include side padding", before[x,y].toArgb(), held[x,y].toArgb())
                val cornerX = (bounds.left + 1).roundToInt()
                val cornerY = (bounds.top + 1).roundToInt()
                assertEquals("Category $index must retain its curved corner", before[cornerX,cornerY].toArgb(), held[cornerX,cornerY].toArgb())
                node.performTouchInput { cancel() }
                compose.mainClock.advanceTimeBy(400)
            } finally { compose.mainClock.autoAdvance = true }
        }
        // The new header hit area must not consume a child palette selection.
        list.performScrollToNode(hasTestTag("settings_theme_category_toggle_0"))
        compose.onNodeWithTag("settings_theme_category_toggle_0").performClick()
        list.performScrollToNode(hasTestTag("settings_palette_choice_1"))
        compose.onNodeWithTag("settings_palette_choice_1").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
    }
}
