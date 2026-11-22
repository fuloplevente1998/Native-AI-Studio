package com.repforge.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
    val surface = if (enabled && colors.containerColor == palette.primary)
        modifier.trainPilotSoftGlow(palette.primary, shape) else modifier
    TrainPilotPressFeedback(colors, enabled, interactionSource) { feedbackColors, source ->
        Button(onClick, surface, enabled, shape, feedbackColors, elevation, border, contentPadding, source, content)
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
        TextButton(onClick, modifier, enabled, shape, feedbackColors, elevation, border, contentPadding, source, content)
    }
}
