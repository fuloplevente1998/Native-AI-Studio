package com.repforge.app.ui.theme

import com.example.ui.theme.glassTouchBubble
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
    val palette = com.example.ui.theme.LocalTrainPilotPalette.current
    val solid = border?.brush as? androidx.compose.ui.graphics.SolidColor
    val effectiveBorder = if (solid != null && liquidGlassDropsFrame(palette, solid.value)) null else border
    TrainPilotPressFeedback(colors, enabled, interactionSource) { feedbackColors, source ->
        OutlinedButton(onClick, modifier.glassTouchBubble(source, enabled, shape), enabled, shape, feedbackColors, elevation, effectiveBorder, contentPadding, source, content)
    }
}
