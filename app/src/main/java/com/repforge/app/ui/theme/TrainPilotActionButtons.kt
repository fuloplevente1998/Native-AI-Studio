package com.repforge.app.ui.theme

import com.example.ui.theme.glassTouchBubble
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassStateTile
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Action colors (save = green, negative = red) independent of the decorative accent. Dark themes keep
 * the deep tonal look; light themes get light tonal containers with dark text (2772: no dark blocks
 * on light screens).
 */
object TrainPilotActionAppearance {
    private val dark: Boolean @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = com.example.ui.theme.LocalTrainPilotPalette.current.isDark
    val saveBackground: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) Color(0xFF19382C) else Color(0xFFDDF3E8)
    val saveForeground: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) Color(0xFF88E5B3) else Color(0xFF146C43)
    /** Semantic save/delete outlines of dark palettes; Liquid Glass keeps them (2809/2). */
    val DARK_SAVE_OUTLINE = Color(0xFF3ECF87)
    val DARK_NEGATIVE_OUTLINE = Color(0xFFEF6670)
    val saveOutline: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) DARK_SAVE_OUTLINE else Color(0xFF4FB985)
    val negativeBackground: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) Color(0xFF3E2024) else Color(0xFFFBE3E3)
    val negativeForeground: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) Color(0xFFFFB4B4) else Color(0xFFB3261E)
    val negativeOutline: Color @Composable @androidx.compose.runtime.ReadOnlyComposable
        get() = if (dark) DARK_NEGATIVE_OUTLINE else Color(0xFFD9878A)
    @Composable fun saveColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = saveBackground, contentColor = saveForeground,
        disabledContainerColor = saveBackground.copy(alpha = 0.5f), disabledContentColor = saveForeground.copy(alpha = 0.38f))
    @Composable fun negativeColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = negativeBackground, contentColor = negativeForeground,
        disabledContainerColor = negativeBackground.copy(alpha = 0.5f), disabledContentColor = negativeForeground.copy(alpha = 0.38f))
    @Composable fun saveBorder(enabled: Boolean = true) = BorderStroke(1.dp, saveOutline.copy(alpha = if (enabled) 0.7f else 0.3f))
    @Composable fun negativeBorder(enabled: Boolean = true) = BorderStroke(1.dp, negativeOutline.copy(alpha = if (enabled) 0.7f else 0.3f))
}

/** For a confirmation dialog whose committed action can be either save or delete. */
@Composable
fun TrainPilotActionButton(positive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding, content: @Composable RowScope.() -> Unit) {
    val colors = if (positive) TrainPilotActionAppearance.saveColors() else TrainPilotActionAppearance.negativeColors()
    TrainPilotPressFeedback(colors, enabled, null) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).trainPilotGlassRim(com.example.ui.theme.LocalTrainPilotPalette.current, shape, com.example.ui.theme.GlassRole.Button).glassTouchBubble(source, enabled, shape), enabled, shape,
        interactionSource = source, colors = feedbackColors,
        border = if (positive) TrainPilotActionAppearance.saveBorder(enabled) else TrainPilotActionAppearance.negativeBorder(enabled),
        contentPadding = contentPadding, content = content)
    }
}

@Composable
fun TrainPilotSaveButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp), contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    TrainPilotPressFeedback(TrainPilotActionAppearance.saveColors(), enabled, interactionSource) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).trainPilotGlassRim(com.example.ui.theme.LocalTrainPilotPalette.current, shape, com.example.ui.theme.GlassRole.Button).glassTouchBubble(source, enabled, shape), enabled, shape,
        colors = feedbackColors, border = TrainPilotActionAppearance.saveBorder(enabled),
        contentPadding = contentPadding, interactionSource = source, content = content)
    }
}

@Composable
fun TrainPilotNegativeButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp), contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    TrainPilotPressFeedback(TrainPilotActionAppearance.negativeColors(), enabled, interactionSource) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp).trainPilotGlassRim(com.example.ui.theme.LocalTrainPilotPalette.current, shape, com.example.ui.theme.GlassRole.Button).glassTouchBubble(source, enabled, shape), enabled, shape,
        colors = feedbackColors, border = TrainPilotActionAppearance.negativeBorder(enabled),
        contentPadding = contentPadding, interactionSource = source, content = content)
    }
}

@Composable
fun TrainPilotNegativeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    // 2811/G3: glass = translucent red state tint (outline + glyph stay red); legacy: the tonal red block.
    val container = com.example.ui.theme.LocalTrainPilotPalette.current.glassStateTile(
        TrainPilotActionAppearance.negativeOutline, TrainPilotActionAppearance.negativeBackground, strong = true)
    OutlinedIconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = container,
            contentColor = TrainPilotActionAppearance.negativeForeground,
            disabledContainerColor = TrainPilotActionAppearance.negativeBackground.copy(alpha = 0.5f),
            disabledContentColor = TrainPilotActionAppearance.negativeForeground.copy(alpha = 0.38f)),
        border = TrainPilotActionAppearance.negativeBorder(enabled), interactionSource = interactionSource, content = content)
}
