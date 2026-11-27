package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Real route host, native settings store and the actual production theme/scaffold/root.
 * The Activity startup/migration gates have their own integration tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class LiquidGlass433RootThemeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun rapidThemeWritesAndRealRootPreserveOpenSettingsAndSelectedTile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TrainPilotSettingsStore(context, publishTheme = {})
        runBlocking {
            store.resetAllSettingsForEraseLocal()
            val p = TrainPilotPalettes.getById(1)
            store.setTheme(ThemePaletteOption(p.key, p.nameHu, p.nameEn, "#000000", "#000000", "matte"))
        }
        val model = TrainPilotViewModel().apply {
            nativeContext = context
            nativeSettingsStore = store
            nativeScope = compose.activity.lifecycleScope
            navigateToSubScreen(SubScreen.SETTINGS_MAIN)
        }
        val origin = model.uiState.value.backStack
        compose.setContent {
            val settings by store.settingsFlow.collectAsState(initial = com.repforge.app.data.store.TrainPilotUserSettings())
            val state by model.uiState.collectAsState()
            LaunchedEffect(settings.themeKey, settings.language, settings.restSeconds,
                settings.soundEnabled, settings.vibrationEnabled) {
                model.syncNativeSettings(settings.language, settings.themeKey, settings.restSeconds,
                    settings.soundEnabled, settings.vibrationEnabled)
            }
            val palette = remember(state.selectedPaletteId, settings.glassClarity) {
                TrainPilotPalettes.getById(state.selectedPaletteId).withGlassClarity(settings.glassClarity)
            }
            TrainPilotTheme(palette = palette) {
                GlassSceneHost(settings.glassBackdropImage, { null }) { TrainPilotAppRoot(model) }
            }
        }
        fun awaitPalette(id: Int, requireVisible: Boolean = true) {
            compose.waitUntil(30_000) {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                model.uiState.value.selectedPaletteId == id &&
                    model.pendingPaletteWrite == null
            }
            assertEquals(TrainPilotPalettes.getById(id).key, runBlocking { store.current().themeKey })
            compose.runOnIdle {
                assertEquals(SubScreen.SETTINGS_MAIN, model.uiState.value.currentSubScreen)
                assertEquals(origin, model.uiState.value.backStack)
            }
            // The asynchronous DataStore write can finish before the new viewport's
            // measured reveal. Wait for the actual selected semantics without scrolling
            // or reopening anything in the test. The visibility assertion remains.
            compose.waitUntil(30_000) {
                compose.onAllNodes(hasTestTag("settings_palette_choice_$id") and isSelected())
                    .fetchSemanticsNodes().size == 1
            }
            val tile = compose.onNodeWithTag("settings_palette_choice_$id").assertIsSelected()
            if (requireVisible) tile.assertIsDisplayed()
        }
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        list.performScrollToNode(hasTestTag("settings_theme_category_toggle_4"))
        compose.onNodeWithTag("settings_theme_category_toggle_4").performClick()
        list.performScrollToNode(hasTestTag("settings_palette_choice_35"))
        compose.onNodeWithTag("settings_palette_choice_35").performClick()
        awaitPalette(35)
        compose.runOnIdle {
            model.selectPalette(36)
            model.selectPalette(40)
            // Simulate a queued previous settings-flow snapshot arriving before the latest write.
            model.syncNativeSettings("hu", TrainPilotPalettes.getById(35).key)
            assertEquals(40, model.uiState.value.selectedPaletteId)
        }
        // The category remains open even while the latest palette is committed.
        awaitPalette(40, requireVisible = false)
        compose.onNodeWithTag("liquid_glass_clarity").assertExists()
        runBlocking {
            store.setGlassClarityForDataset(50, com.repforge.app.NativeDataReplacement.continuationForCurrentData(context))
        }
        compose.waitForIdle()
        compose.onNodeWithTag("settings_palette_choice_40").assertIsSelected()
        list.performScrollToNode(hasTestTag("settings_theme_category_toggle_1"))
        compose.onNodeWithTag("settings_theme_category_toggle_1").performClick()
        list.performScrollToNode(hasTestTag("settings_palette_choice_9"))
        compose.onNodeWithTag("settings_palette_choice_9").performClick()
        awaitPalette(9)
    }
}
