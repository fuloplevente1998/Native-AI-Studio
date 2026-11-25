package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.selectionColors
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassControlFill
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.overlayRoleRim
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.glassTile
import com.example.ui.theme.liquidDropMaterial
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.fieldOutline
import com.example.ui.theme.readablePrimaryLabel
import androidx.compose.runtime.remember
import com.repforge.app.ui.theme.trainPilotClickable

/**
 * 2785: the app's own chip. Rounded, theme (palette) colours only: card background, thin card border;
 * selected = primary border, primary text and a light primary fill. Replaces Material FilterChip, whose
 * default white, square outline ignored the theme. [colors]/[border]/[shape] are accepted only so old
 * call sites compile; the look always comes from the palette.
 */
@Composable
fun TrainPilotFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") shape: Shape? = null,
    @Suppress("UNUSED_PARAMETER") colors: SelectableChipColors? = null,
    @Suppress("UNUSED_PARAMETER") border: BorderStroke? = null,
    @Suppress("UNUSED_PARAMETER") interactionSource: MutableInteractionSource? = null
) {
    val p = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(14.dp)
    val selection = remember(p) { p.selectionColors() }
    val content = when {
        !enabled -> p.textSecondary.copy(alpha = 0.5f)
        selected -> selection.foreground
        else -> p.textPrimary
    }
    // Use bounded translucent control paint even during active measurement.
    val glassChip = enabled && p.isLiquidGlass
    val overlay = glassChip && LocalGlassOverlay.current
    val role = if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button
    Surface(
        shape = shape,
        color = when {
            glassChip -> Color.Transparent
            selected -> selection.surface
            else -> p.surfaceCard
        },
        border = com.repforge.app.ui.theme.trainPilotFrameStroke(BorderStroke(1.dp, if (selected) selection.border else p.cardBorder)),
        modifier = modifier.heightIn(min = if (p.isLiquidGlass) 48.dp else 40.dp)
            .then(if (glassChip) Modifier.liquidDropMaterial(p, shape, role,
                accent = if (selected) p.primary else null, live = selected && !overlay)
                else Modifier)
            .semantics { this.selected = selected }
            .trainPilotClickable(shape, enabled = enabled) { onClick() }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides content,
            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(
                color = content, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                leadingIcon?.invoke()
                label()
                trailingIcon?.invoke()
            }
        }
    }
}

/** 2785: text field colours taken only from the palette (no white Material outline). */
@Composable
fun trainPilotFieldColors(p: TrainPilotPalette = LocalTrainPilotPalette.current): TextFieldColors {
    val selection = remember(p) { p.selectionColors() }
    val outline = remember(p) { p.fieldOutline() }
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
        disabledTextColor = p.textSecondary, errorTextColor = p.textPrimary,
        focusedContainerColor = p.glassTile(p.surfaceCard, elevated = true), unfocusedContainerColor = p.glassTile(p.surfaceCard),
        disabledContainerColor = p.surfaceCard, errorContainerColor = p.glassTile(p.surfaceCard),
        cursorColor = p.primary, errorCursorColor = p.danger,
        // Input boundaries are functional, not decorative: keep a visible quiet outline, focus and error.
        focusedBorderColor = if (p.isLiquidGlass) p.primary else selection.border,
        unfocusedBorderColor = if (p.isLiquidGlass) p.cardBorder.copy(alpha = .60f) else outline,
        disabledBorderColor = if (p.isLiquidGlass) Color.Transparent else p.cardBorder.copy(alpha = 0.5f), errorBorderColor = p.danger,
        focusedLabelColor = selection.foreground, unfocusedLabelColor = p.textSecondary,
        disabledLabelColor = p.textSecondary, errorLabelColor = p.danger,
        focusedPlaceholderColor = p.textSecondary, unfocusedPlaceholderColor = p.textSecondary,
        focusedLeadingIconColor = selection.foreground, unfocusedLeadingIconColor = p.textSecondary,
        focusedTrailingIconColor = selection.foreground, unfocusedTrailingIconColor = p.textSecondary,
        focusedSupportingTextColor = p.textSecondary, unfocusedSupportingTextColor = p.textSecondary,
        errorSupportingTextColor = p.danger,
        focusedPrefixColor = p.textSecondary, unfocusedPrefixColor = p.textSecondary,
        focusedSuffixColor = p.textSecondary, unfocusedSuffixColor = p.textSecondary
    )
}

/** 2785: the app's own text field (rounded, palette colours). Same parameters as OutlinedTextField. */
@Composable
fun TrainPilotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    @Suppress("UNUSED_PARAMETER") shape: Shape? = null,
    @Suppress("UNUSED_PARAMETER") colors: TextFieldColors? = null
) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        trailingIcon = trailingIcon, prefix = prefix, suffix = suffix, supportingText = supportingText, isError = isError,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        singleLine = singleLine, maxLines = maxLines, minLines = minLines, interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp), colors = trainPilotFieldColors())
}

@Composable
fun TrainPilotTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    @Suppress("UNUSED_PARAMETER") shape: Shape? = null,
    @Suppress("UNUSED_PARAMETER") colors: TextFieldColors? = null
) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        trailingIcon = trailingIcon, prefix = prefix, suffix = suffix, supportingText = supportingText, isError = isError,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        singleLine = singleLine, maxLines = maxLines, minLines = minLines, interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp), colors = trainPilotFieldColors())
}

/** 2785: main action of a form (theme accent fill, e.g. gold "Mentés"). */
@Composable
fun TrainPilotPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = LocalTrainPilotPalette.current
    val glass = enabled && p.isLiquidGlass
    val shape = if (glass) RoundedCornerShape(50) else RoundedCornerShape(14.dp)
    Surface(shape = shape, color = if (glass) Color.Transparent else if (enabled) p.primary else p.primary.copy(alpha = 0.4f),
        border = if (glass) BorderStroke(1.dp, p.selectionColors().border) else null,
        modifier = modifier.heightIn(min = 48.dp)
            .then(if (glass) Modifier.liquidDropMaterial(p, shape, GlassMaterialRole.Selection,
                accent = p.primary, allowMeasurement = true) else Modifier)
            .trainPilotClickable(shape, enabled = enabled) { onClick() }) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (glass) p.textPrimary else p.readablePrimaryLabel(), maxLines = 1)
        }
    }
}

/** 2785: secondary action of a form (dark/elevated surface, e.g. "Mégse"). */
@Composable
fun TrainPilotSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = LocalTrainPilotPalette.current
    val glass = enabled && p.isLiquidGlass
    val shape = if (glass) RoundedCornerShape(50) else RoundedCornerShape(14.dp)
    Surface(shape = shape, color = if (glass) Color.Transparent else p.surfaceElevated, border = com.repforge.app.ui.theme.trainPilotFrameStroke(BorderStroke(1.dp, p.cardBorder)),
        modifier = modifier.heightIn(min = 48.dp)
            .then(if (glass) Modifier.liquidDropMaterial(p, shape, GlassMaterialRole.Button) else Modifier)
            .trainPilotClickable(shape, enabled = enabled) { onClick() }) {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (enabled) p.textPrimary else p.textSecondary, maxLines = 1)
        }
    }
}

/** Material switch semantics, gestures, sizing and legacy colors are unchanged. Glass tracks are
 * bounded, inexpensive state paint; no independent blur, layer capture or animation. */
@Composable
internal fun TrainPilotSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.SwitchColors = androidx.compose.material3.SwitchDefaults.colors()
) {
    val p = LocalTrainPilotPalette.current
    val glassColors = if (p.isLiquidGlass) androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = p.primary,
        checkedTrackColor = p.glassControlFill(p.surfaceElevated, GlassMaterialRole.Selection),
        checkedBorderColor = p.primary,
        uncheckedThumbColor = p.textSecondary,
        uncheckedTrackColor = p.glassTile(p.surfaceElevated),
        uncheckedBorderColor = p.cardBorder
    ) else colors
    androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange,
        modifier = modifier, enabled = enabled, colors = glassColors)
}
