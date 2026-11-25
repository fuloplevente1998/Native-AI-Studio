package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.LocalTrainPilotInSheet
import com.example.ui.components.sheetHostState
import com.example.ui.viewmodel.TrainPilotUiState

/** Keep the glass-specific scaffold, safe-area and backdrop wiring out of the route host. */
@Composable
internal fun GlassAwareScaffold(
    uiState: TrainPilotUiState,
    palette: TrainPilotPalette,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues, Modifier, Dp) -> Unit
) {
    // Only routes with an explicit in-scroll safe-area spacer can extend under glass navigation.
    // 2810: all five top-nav tabs (not only Home/Health/Settings) scroll under the header, so the
    // header, nav and pill lens see every tab's content (one recorded source, [captureGlassBackdrop]).
    val glassHost = uiState.sheetHostState()
    val glassUnderlap = palette.glassEffectsActive(LocalGlassOpticsEnabled.current) &&
        Build.VERSION.SDK_INT >= 31 && glassUnderlapRoute(glassHost.currentTab, glassHost.currentSubScreen)
    val headerBackdrop = if (glassUnderlap) rememberGlassBackdrop() else null
    Scaffold(
        modifier = Modifier.fillMaxSize().background(palette.background),
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CompositionLocalProvider(LocalGlassHeaderBackdrop provides headerBackdrop) {
                // 2811/2: without underlap (calendar, sub-screens, active workout) nothing of the page lies under
                // the top bar, so the scaffold colour showed as a solid band around e.g. the control strip: the
                // same page scene is painted behind the whole top bar (root coordinates, continuous with the page).
                if (palette.isLiquidGlass && !glassUnderlap) androidx.compose.foundation.layout.Box(
                    Modifier.trainPilotGlassScene(palette)) { topBar() }
                else topBar()
            }
        }
    ) { innerPadding ->
        val direction = LocalLayoutDirection.current
        val bodyPadding = if (glassUnderlap) PaddingValues(
            start = innerPadding.calculateStartPadding(direction),
            end = innerPadding.calculateEndPadding(direction),
            bottom = innerPadding.calculateBottomPadding()
        ) else innerPadding
        val capture = if (headerBackdrop != null) Modifier.captureGlassBackdrop(headerBackdrop) else Modifier
        // 2809/1: one living backdrop behind every glass screen; the pages above stay transparent.
        // 2811/1 round 3: the capture sits INSIDE the scene, so it records only the scrolled content: the
        // header draws the page scene itself (as on the calendar) and lays this content, blurred, on top.
        CompositionLocalProvider(LocalGlassRootBackdrop provides palette.isLiquidGlass) {
            content(bodyPadding, Modifier.trainPilotGlassScene(palette).then(capture),
                if (glassUnderlap) innerPadding.calculateTopPadding() else 0.dp)
        }
    }
}

/** The top-nav tabs; each one's scroll content starts with the [LocalGlassTopInset] spacer. */
internal val GlassUnderlapTabs: Set<MainTab> = setOf(MainTab.HOME, MainTab.PROGRAMS, MainTab.WORKOUT, MainTab.PROGRESS, MainTab.HEALTH)

/**
 * 2811/1: the top-bar shortcut screens that also scroll under the glass header (their lists start with the
 * [LocalGlassTopInset] spacer). The calendar keeps its pinned full-height layout (G2) and stays below.
 */
internal val GlassUnderlapShortcutTabs: Set<MainTab> = setOf(MainTab.LOG, MainTab.COACH)

/** Routes whose content extends under the glass header and is recorded for the header/nav lens. */
internal fun glassUnderlapRoute(tab: MainTab, subScreen: SubScreen): Boolean =
    subScreen == SubScreen.SETTINGS_MAIN ||
        (subScreen == SubScreen.NONE && (tab in GlassUnderlapTabs || tab in GlassUnderlapShortcutTabs))

/** True under [GlassAwareScaffold] in a glass palette: the root already paints the page backdrop. */
internal val LocalGlassRootBackdrop = staticCompositionLocalOf { false }

/** Glass page over the root backdrop: nothing to paint. In a sheet window: its own backdrop. */
@Composable
private fun glassPageIsTransparent(palette: TrainPilotPalette): Boolean =
    palette.isLiquidGlass && LocalGlassRootBackdrop.current && !LocalTrainPilotInSheet.current

/** Page background of the main tabs. Legacy palettes: exactly the previous page brush. */
@Composable
internal fun Modifier.trainPilotPageBackdrop(palette: TrainPilotPalette): Modifier =
    if (glassPageIsTransparent(palette)) this else background(palette.pageBrush).trainPilotGlassScene(palette)

/** Background of the other full screens. Legacy palettes: exactly the previous flat background. */
@Composable
internal fun Modifier.trainPilotScreenBackdrop(palette: TrainPilotPalette): Modifier = when {
    !palette.isLiquidGlass -> background(palette.background)
    glassPageIsTransparent(palette) -> this
    else -> background(palette.background).trainPilotGlassScene(palette)
}
