package com.repforge.app

import android.view.Window
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import com.example.ui.theme.TrainPilotPalette

/** Keep system icons legible against the same background used by the app. */
internal object PaletteSystemBars {
    fun useDarkIcons(palette: TrainPilotPalette): Boolean = palette.background.luminance() > 0.179f

    fun apply(window: Window, palette: TrainPilotPalette) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val darkIcons = useDarkIcons(palette)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}
