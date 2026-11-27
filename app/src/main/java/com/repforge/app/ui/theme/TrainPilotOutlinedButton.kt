package com.repforge.app.ui.theme

import com.example.ui.theme.glassTouchBubble
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.liquidDropMaterial
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Shared phone control outline; keeps native enabled/click/accessibility behaviour. */
@Composable
fun TrainPilotOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = TrainPilotFrames.control(MaterialTheme.colorScheme.outline),
    contentPadding: PaddingValues = TrainPilotButtonLayout.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    // 2808 #13: glass palettes drop neutral hairline outlines; semantic (accent/danger/success/warning) stay.
    val effectiveBorder = trainPilotFrameStroke(border)
    val palette = LocalTrainPilotPalette.current
    // Only the ordinary translucent default can be swapped; semantic/custom fills keep their paint.
    val glass = enabled && palette.isLiquidGlass &&
        colors.containerColor == MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
    val paint = if (glass) colors.copy(containerColor = Color.Transparent, contentColor = palette.textPrimary) else colors
    TrainPilotPressFeedback(paint, enabled, interactionSource) { feedbackColors, source ->
        val surface = when {
            glass -> modifier.liquidDropMaterial(palette, shape, GlassMaterialRole.Button, pressSource = source)
            enabled -> modifier.glassPressMotion(source, enabled, shape, stretchOnDrag = true, includeTouchBubble = false)
                .trainPilotGlassRim(palette, shape, GlassMaterialRole.Button)
            else -> modifier
        }
        OutlinedButton(onClick, surface.glassTouchBubble(source, enabled, shape), enabled, shape, feedbackColors, elevation, effectiveBorder, contentPadding, source, content)
    }
}
