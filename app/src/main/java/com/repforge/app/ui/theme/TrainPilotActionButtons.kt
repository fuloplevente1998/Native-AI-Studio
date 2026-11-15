package com.repforge.app.ui.theme

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

/** Action colors are independent of the selected decorative theme. */
object TrainPilotActionAppearance {
    val saveBackground = Color(0xFF19382C)
    val saveForeground = Color(0xFF88E5B3)
    val saveOutline = Color(0xFF3ECF87)
    val negativeBackground = Color(0xFF3E2024)
    val negativeForeground = Color(0xFFFFB4B4)
    val negativeOutline = Color(0xFFEF6670)
    @Composable fun saveColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = saveBackground, contentColor = saveForeground,
        disabledContainerColor = saveBackground.copy(alpha = 0.5f), disabledContentColor = saveForeground.copy(alpha = 0.38f))
    @Composable fun negativeColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = negativeBackground, contentColor = negativeForeground,
        disabledContainerColor = negativeBackground.copy(alpha = 0.5f), disabledContentColor = negativeForeground.copy(alpha = 0.38f))
    fun saveBorder(enabled: Boolean = true) = BorderStroke(1.dp, saveOutline.copy(alpha = if (enabled) 0.7f else 0.3f))
    fun negativeBorder(enabled: Boolean = true) = BorderStroke(1.dp, negativeOutline.copy(alpha = if (enabled) 0.7f else 0.3f))
}

/** For a confirmation dialog whose committed action can be either save or delete. */
@Composable
fun TrainPilotActionButton(positive: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) {
    val colors = if (positive) TrainPilotActionAppearance.saveColors() else TrainPilotActionAppearance.negativeColors()
    TrainPilotPressFeedback(colors, enabled, null) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape,
        interactionSource = source, colors = feedbackColors,
        border = if (positive) TrainPilotActionAppearance.saveBorder(enabled) else TrainPilotActionAppearance.negativeBorder(enabled),
        contentPadding = contentPadding, content = content)
    }
}

@Composable
fun TrainPilotSaveButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp), contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    TrainPilotPressFeedback(TrainPilotActionAppearance.saveColors(), enabled, interactionSource) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape,
        colors = feedbackColors, border = TrainPilotActionAppearance.saveBorder(enabled),
        contentPadding = contentPadding, interactionSource = source, content = content)
    }
}

@Composable
fun TrainPilotNegativeButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp), contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) {
    TrainPilotPressFeedback(TrainPilotActionAppearance.negativeColors(), enabled, interactionSource) { feedbackColors, source ->
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape,
        colors = feedbackColors, border = TrainPilotActionAppearance.negativeBorder(enabled),
        contentPadding = contentPadding, interactionSource = source, content = content)
    }
}

@Composable
fun TrainPilotNegativeIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    OutlinedIconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = TrainPilotActionAppearance.negativeBackground,
            contentColor = TrainPilotActionAppearance.negativeForeground,
            disabledContainerColor = TrainPilotActionAppearance.negativeBackground.copy(alpha = 0.5f),
            disabledContentColor = TrainPilotActionAppearance.negativeForeground.copy(alpha = 0.38f)),
        border = TrainPilotActionAppearance.negativeBorder(enabled), interactionSource = interactionSource, content = content)
}
