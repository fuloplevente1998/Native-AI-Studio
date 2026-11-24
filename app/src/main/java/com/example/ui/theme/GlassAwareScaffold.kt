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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.MainTab
import com.example.model.SubScreen
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
    val glassUnderlap = palette.isLiquidGlass && LocalGlassOpticsEnabled.current &&
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
        content(bodyPadding, capture, if (glassUnderlap) innerPadding.calculateTopPadding() else 0.dp)
    }
}
