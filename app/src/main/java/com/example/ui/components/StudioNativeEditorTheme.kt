package com.example.ui.components

import androidx.compose.runtime.*
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Existing native editors use the selected Studio palette, including light themes. */
@Composable
internal fun StudioNativeEditorTheme(content: @Composable () -> Unit) {
    val p = LocalTrainPilotPalette.current
    val colors = remember(p) { com.example.ui.theme.nativeStudioColors(p) }
    CompositionLocalProvider(LocalTrainPilotColors provides colors, content = content)
}
