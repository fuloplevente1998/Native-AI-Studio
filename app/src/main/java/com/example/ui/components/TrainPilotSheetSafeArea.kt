package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity

/** Reserve room outside the whole sheet, including its drag handle and header.
 * Raw top padding deliberately survives consumed content insets in an edge-to-edge modal.
 * Union takes the larger obstruction, avoiding double status-bar/cutout padding.
 */
@Composable
internal fun trainPilotSheetTopPadding(
    statusBar: WindowInsets = WindowInsets.statusBarsIgnoringVisibility,
    cutout: WindowInsets = WindowInsets.displayCutout
): Dp {
    val density = LocalDensity.current
    return with(density) { statusBar.union(cutout).getTop(density).toDp() }
}

@Composable
internal fun Modifier.trainPilotSheetTopSafeArea(
    statusBar: WindowInsets = WindowInsets.statusBarsIgnoringVisibility,
    cutout: WindowInsets = WindowInsets.displayCutout
): Modifier = padding(top = trainPilotSheetTopPadding(statusBar, cutout))
