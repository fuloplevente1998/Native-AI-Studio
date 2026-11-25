package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    // 2812: the five primary tabs plus Coach and Journal scroll under the header, so the
    // header, nav and pill lens see every tab's content (one recorded source, [captureGlassBackdrop]).
    val glassHost = uiState.sheetHostState()
    // Measurement keeps the expensive general optics gate closed. Only its fixed chrome
    // samples content, on supported glass devices outside battery saver.
    val measurementChrome = KyantGlass.active(palette) &&
        glassMeasurementChromeRoute(glassHost.currentSubScreen) && !rememberGlassPowerSave(palette.isLiquidGlass)
    val glassUnderlap = measurementChrome || (palette.glassEffectsActive(LocalGlassOpticsEnabled.current) &&
        Build.VERSION.SDK_INT >= 31 && glassUnderlapRoute(glassHost.currentTab, glassHost.currentSubScreen))
    val kyant = KyantGlass.active(palette) && (LocalGlassOpticsEnabled.current || measurementChrome)
    val headerBackdrop = if (glassUnderlap && !kyant) rememberGlassBackdrop() else null
    val sceneLayer = if (kyant) rememberKyantSceneBackdrop(palette) else null
    val contentLayer = if (kyant && glassUnderlap) rememberLayerBackdrop() else null
    val kyantScene = sceneLayer?.let { rememberKyantScene(it) }
    val kyantHeader = sceneLayer?.let { rememberKyantHeaderScene(it, contentLayer) }
    // Keep the scaffold at a stable composition position when optics are toggled.
    Box(Modifier.fillMaxSize().background(palette.background)
        .then(if (sceneLayer == null) Modifier.trainPilotGlassScene(palette) else Modifier)) {
        if (sceneLayer != null) KyantSceneBackground(sceneLayer, Modifier.matchParentSize())
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (palette.isLiquidGlass) Color.Transparent else palette.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CompositionLocalProvider(LocalGlassHeaderBackdrop provides headerBackdrop,
                    LocalKyantScene provides kyantScene, LocalKyantHeader provides kyantHeader,
                    LocalGlassChromeUnderlap provides glassUnderlap) {
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
            val capture = when {
                contentLayer != null -> Modifier.layerBackdrop(contentLayer)
                headerBackdrop != null -> Modifier.captureGlassBackdrop(headerBackdrop)
                else -> Modifier
            }
            // 2809/1: one living backdrop behind every glass screen (inside the header capture, so the
            // header lens sees it too); the pages above stay transparent instead of repainting it.
            // Only the top bar may sample captured body content; body controls must not sample themselves.
            CompositionLocalProvider(LocalGlassRootBackdrop provides palette.isLiquidGlass,
                LocalKyantScene provides kyantScene, LocalKyantHeader provides null,
                LocalGlassChromeUnderlap provides glassUnderlap) {
                content(bodyPadding, capture,
                    if (glassUnderlap) innerPadding.calculateTopPadding() else 0.dp)
            }
        }
    }
}

/** Tabs with an in-scroll [LocalGlassTopInset] safe area. Calendar keeps its padded host. */
internal val GlassUnderlapTabs: Set<MainTab> = setOf(MainTab.HOME, MainTab.PROGRAMS, MainTab.WORKOUT, MainTab.PROGRESS, MainTab.HEALTH, MainTab.COACH, MainTab.LOG)

/** Routes whose content extends under the glass header and is recorded for the header/nav lens. */
internal fun glassUnderlapRoute(tab: MainTab, subScreen: SubScreen): Boolean =
    subScreen == SubScreen.SETTINGS_MAIN || subScreen == SubScreen.COACH_DETAIL ||
        (subScreen == SubScreen.NONE && tab in GlassUnderlapTabs)

/** Scrolling workout routes only; the outdoor map keeps its existing protected layout. */
internal fun glassMeasurementChromeRoute(screen: SubScreen): Boolean = screen == SubScreen.ACTIVE_STRENGTH ||
    screen == SubScreen.ACTIVE_TIMED || screen == SubScreen.ACTIVE_BILATERAL

internal val LocalGlassChromeUnderlap = staticCompositionLocalOf { false }

/** True under [GlassAwareScaffold] in a glass palette: the root already paints the page backdrop. */
internal val LocalGlassRootBackdrop = staticCompositionLocalOf { false }

/** Glass page over the root backdrop: nothing to paint. In a sheet window: its own backdrop. */
@Composable
private fun glassPageIsTransparent(palette: TrainPilotPalette): Boolean {
    if (!palette.isLiquidGlass) return false
    if (LocalGlassOverlay.current) return true
    if (LocalGlassChromeUnderlap.current && LocalKyantScene.current.forThisWindow() != null) return true
    // Dialogs inherit composition locals but not the recording window. Do not hide their background.
    if (KyantGlass.active(palette) && LocalGlassOpticsEnabled.current)
        return LocalKyantScene.current.forThisWindow() != null
    return LocalGlassRootBackdrop.current && !LocalTrainPilotInSheet.current
}

/** Preserve the original opaque route host when no Kyant source is available. */
@Composable
internal fun glassContentBackground(palette: TrainPilotPalette): Color =
    if (palette.isLiquidGlass && (LocalGlassRootBackdrop.current || LocalKyantScene.current.forThisWindow() != null)) Color.Transparent else palette.background

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
