package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens

/** Shared local style for the workout editor surfaces only. Matte fallback is always opaque. */
internal fun workoutEditorFill(p: TrainPilotPalette, legacy: Color): Color =
    if (!p.isLiquidGlass) legacy.copy(alpha = 1f)
    else lerp(p.glassOverlayFill(legacy), GlassContrast.limit(p.background, GlassContrast.target), .30f)

internal fun workoutEditorFrame(p: TrainPilotPalette): BorderStroke =
    if (p.isLiquidGlass || p.hasGlow) BorderStroke(1.dp, Brush.verticalGradient(listOf(
        p.primary.copy(alpha = .85f), p.cardBorder.copy(alpha = .42f), p.primary.copy(alpha = .55f))))
    else BorderStroke(1.dp, p.cardBorder.copy(alpha = .85f))

internal data class FeedbackChoicePaint(val fill: Color, val text: Color, val border: Color)

/** Six stable semantic hues; the selected state is explicit, not just colour. */
internal fun workoutFeedbackPaint(p: TrainPilotPalette, effort: String, selected: Boolean): FeedbackChoicePaint {
    val accent = when (effort) {
        "easy" -> Color(0xFF28A36B)
        "light" -> Color(0xFF219CA2)
        "right", "good" -> Color(0xFF268DDF)
        "challenging" -> Color(0xFFDE8B29)
        "hard" -> Color(0xFFE05264)
        "pain" -> Color(0xFFC052A1)
        else -> p.primary
    }
    if (p.isLiquidGlass) {
        val bounded = GlassContrast.limit(accent, GLASS_TILE_LUMA)
        val alpha = if (selected) .45f else (.22f + glassTileAlpha(p.glassClarity)).coerceAtMost(.40f)
        return FeedbackChoicePaint(bounded.copy(alpha = alpha), p.textPrimary,
            glassReadable(accent, 3.1f).copy(alpha = if (selected) 1f else .72f))
    }
    val fill = lerp(p.surfaceElevated.copy(alpha = 1f), accent, if (selected) .78f else .32f)
    val text = if (NativeThemeTokens.contrast(p.textPrimary, fill) >= 4.5f) p.textPrimary else NativeThemeTokens.onFill(fill)
    val border = if (selected) text else accent
    return FeedbackChoicePaint(fill, text, border)
}
