package com.repforge.app.ui.theme

import com.example.ui.theme.glassTouchBubble
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.trainPilotGlassMaterial
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.trainPilotGlassRim
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

/** Shared compact insets leave more space for translated labels without changing touch targets. */
object TrainPilotButtonLayout {
    val ContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val TextContentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
}

/** Material filled button with feedback spanning its painted surface. */
@Composable
fun TrainPilotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = com.example.ui.theme.LocalTrainPilotPalette.current
    val paintedGlow = enabled && palette.hasGlow && colors.containerColor == palette.primary
    val minimumTouchSize = LocalMinimumInteractiveComponentSize.current
    // Material's minimum touch target is larger than its painted 40 dp button. Put that
    // reserved space OUTSIDE the glow; otherwise the outline follows the 48 dp hit box
    // and leaves dark strips above and below the actual painted pill.
    val surface = if (paintedGlow) {
        modifier.layout { measurable, constraints ->
            val painted = measurable.measure(constraints)
            val minimumPx = if (!minimumTouchSize.value.isNaN()) {
                minimumTouchSize.roundToPx().coerceAtLeast(0)
            } else 0
            val width = maxOf(painted.width, minimumPx)
                .coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = maxOf(painted.height, minimumPx)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                painted.placeRelative((width - painted.width) / 2, (height - painted.height) / 2)
            }
        }.trainPilotSoftGlow(palette.primary, shape)
    } else modifier

    val materialButton: @Composable () -> Unit = {
        TrainPilotPressFeedback(colors, enabled, interactionSource) { feedbackColors, source ->
            Button(onClick, surface.glassPressMotion(source, enabled, shape).trainPilotGlassRim(palette, shape), enabled, shape, feedbackColors, elevation, border, contentPadding, source, content)
        }
    }
    if (paintedGlow) {
        // The parent layout above reserves the same touch space, while the glow and
        // Material surface now measure the identical visual size and shape.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            materialButton()
        }
    } else {
        materialButton()
    }
}

/** Standard primary actions only: callers must let the Material button supply its foreground. */
internal fun useGlassPrimaryAction(palette: TrainPilotPalette, enabled: Boolean, colors: ButtonColors): Boolean =
    palette.isLiquidGlass && enabled && colors.containerColor == palette.primary &&
        colors.contentColor == palette.onPrimary

internal fun glassPrimaryActionColors(palette: TrainPilotPalette, enabled: Boolean, colors: ButtonColors): ButtonColors =
    if (useGlassPrimaryAction(palette, enabled, colors))
        colors.copy(containerColor = Color.Transparent, contentColor = palette.textPrimary)
    else colors

/**
 * Shared primary action material for audited call sites. Uses the same Button/press feedback and
 * native accessibility semantics. Custom/semantic and disabled paints keep their exact colors.
 * Content must inherit its foreground, not hard-code onPrimary (which can be dark).
 */
@Composable
fun TrainPilotGlassPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = LocalTrainPilotPalette.current.primary,
        contentColor = LocalTrainPilotPalette.current.onPrimary),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalTrainPilotPalette.current
    val glass = useGlassPrimaryAction(palette, enabled, colors)
    TrainPilotButton(
        onClick = onClick,
        modifier = if (glass) modifier.trainPilotGlassMaterial(palette, shape, GlassMaterialRole.Button) else modifier,
        enabled = enabled,
        shape = shape,
        colors = glassPrimaryActionColors(palette, enabled, colors),
        elevation = if (glass) null else elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/** Text actions use the same native shape and full-surface interaction feedback. */
@Composable
fun TrainPilotTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = TrainPilotButtonLayout.TextContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    TrainPilotPressFeedback(colors, enabled, interactionSource) { feedbackColors, source ->
        TextButton(onClick, modifier.glassTouchBubble(source, enabled, shape), enabled, shape, feedbackColors, elevation, border, contentPadding, source, content)
    }
}
