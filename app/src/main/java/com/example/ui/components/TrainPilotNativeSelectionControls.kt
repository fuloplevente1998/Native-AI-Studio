package com.example.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.trainPilotPressIndication

/** Native selectable semantics, including radio buttons and disabled state. */
@Composable
internal fun Modifier.trainPilotSelectable(
    selected: Boolean, shape: Shape, enabled: Boolean = true, role: Role? = null,
    onClick: () -> Unit
): Modifier {
    if (!LocalTrainPilotPalette.current.isLiquidGlass)
        return selectable(selected, enabled = enabled, role = role, onClick = onClick)
    val source = remember { MutableInteractionSource() }
    return trainPilotPressIndication(shape, source, enabled)
        .selectable(selected, interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** The native slider still owns value, range, semantics, thumb, drag and release.
 * Only its touch indication is replaced, with an opt-in drag continuation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainPilotSlider(
    value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0, onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
) {
    if (!LocalTrainPilotPalette.current.isLiquidGlass) {
        Slider(value = value, onValueChange = onValueChange, modifier = modifier,
            enabled = enabled, valueRange = valueRange, steps = steps,
            onValueChangeFinished = onValueChangeFinished, colors = colors)
        return
    }
    val source = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Slider(value = value, onValueChange = onValueChange,
            modifier = modifier.trainPilotPressIndication(RoundedCornerShape(14.dp), source, enabled, trackDrag = true),
            enabled = enabled, valueRange = valueRange, steps = steps,
            onValueChangeFinished = onValueChangeFinished, colors = colors, interactionSource = source)
    }
}

/** Non-interactive checkbox children of clickable rows must not create another feedback owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainPilotCheckbox(
    checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier,
    enabled: Boolean = true, colors: CheckboxColors = CheckboxDefaults.colors(),
) {
    if (!LocalTrainPilotPalette.current.isLiquidGlass || onCheckedChange == null) {
        Checkbox(checked, onCheckedChange, modifier, enabled, colors)
        return
    }
    val source = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Checkbox(checked, onCheckedChange,
            modifier.trainPilotPressIndication(RoundedCornerShape(12.dp), source, enabled),
            enabled, colors, interactionSource = source)
    }
}
