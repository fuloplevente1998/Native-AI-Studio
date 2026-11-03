package com.example.ui.components

import androidx.compose.runtime.*
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors

/** Existing native editors use the selected Studio palette, including light themes. */
@Composable
internal fun StudioNativeEditorTheme(content: @Composable () -> Unit) {
    val p = LocalTrainPilotPalette.current
    val colors = remember(p) { TrainPilotExtendedColors(bg = p.background, card = p.surfaceCard, card2 = p.surfaceElevated,
        line = p.cardBorder, text = p.textPrimary, muted = p.textSecondary, accent = p.primary, accent2 = p.secondary,
        onAccent = p.onPrimary, dangerText = p.danger, success = p.success, emphasisBorder = p.sectionBorder) }
    CompositionLocalProvider(LocalTrainPilotColors provides colors, content = content)
}
