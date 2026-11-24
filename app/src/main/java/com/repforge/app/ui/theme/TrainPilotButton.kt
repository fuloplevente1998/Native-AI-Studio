package com.repforge.app.ui.theme

import com.example.ui.theme.glassTouchBubble
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
