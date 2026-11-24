package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import com.repforge.app.ui.theme.NativeThemeTokens

/** #349: shared selection paint for navigation, chips and embedded native editors.
 * Keep the fill subtle and the base card unchanged. Contrast is measured against
 * the opaque, composited surfaces rather than a translucent color in isolation.
 */
internal data class ThemeSelectionColors(
    val surface: Color,
    val foreground: Color,
    val border: Color
)

internal fun TrainPilotPalette.selectionColors(): ThemeSelectionColors {
    val alpha = if (isMatte || isHighContrast) 0.12f else 0.16f
    val selected = primary.copy(alpha = alpha).compositeOver(surfaceCard)
    val surfaces = listOf(background, surfaceCard, surfaceElevated, selected)
    val endpoint = if (isDark) Color.White else Color.Black
    fun readable(minimum: Float): Color {
        for (step in 0..100) {
            val candidate = lerp(primary, endpoint, step / 100f)
            if (surfaces.all { NativeThemeTokens.contrast(candidate, it) >= minimum }) {
                return candidate
            }
        }
        return endpoint
    }
    return ThemeSelectionColors(selected, readable(4.5f), readable(3f))
}

internal fun TrainPilotPalette.readablePrimaryLabel(): Color {
    if (NativeThemeTokens.contrast(onPrimary, primary) >= 4.5f) return onPrimary
    return if (NativeThemeTokens.contrast(Color.Black, primary) >=
        NativeThemeTokens.contrast(Color.White, primary)) Color.Black else Color.White
}
