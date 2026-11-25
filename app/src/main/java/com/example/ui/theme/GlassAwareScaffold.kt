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
    val glassHost = uiState.sheetHostState()
    val glassUnderlap = palette.glassEffectsActive(LocalGlassOpticsEnabled.current) &&
        Build.VERSION.SDK_INT >= 31 &&
        (glassHost.currentSubScreen == SubScreen.SETTINGS_MAIN ||
            (glassHost.currentSubScreen == SubScreen.NONE && glassHost.currentTab in setOf(MainTab.HOME, MainTab.HEALTH)))
    val headerBackdrop = if (glassUnderlap) rememberGlassBackdrop() else null
    Scaffold(
        modifier = Modifier.fillMaxSize().background(palette.background),
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CompositionLocalProvider(LocalGlassHeaderBackdrop provides headerBackdrop) {
                topBar()
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
        // 2809/1: one living backdrop behind every glass screen (inside the header capture, so the
        // header lens sees it too); the pages above stay transparent instead of repainting it.
        CompositionLocalProvider(LocalGlassRootBackdrop provides palette.isLiquidGlass) {
            content(bodyPadding, capture.then(Modifier.trainPilotGlassScene(palette)),
                if (glassUnderlap) innerPadding.calculateTopPadding() else 0.dp)
        }
    }
}

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
