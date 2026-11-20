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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalette
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
    val content = when {
        !enabled -> p.textSecondary.copy(alpha = 0.5f)
        selected -> p.primary
        else -> p.textPrimary
    }
    Surface(
        shape = shape,
        color = if (selected) p.primary.copy(alpha = 0.16f) else p.surfaceCard,
        border = BorderStroke(1.dp, if (selected) p.primary else p.cardBorder),
        modifier = modifier.heightIn(min = 40.dp).trainPilotClickable(shape, enabled = enabled) { onClick() }
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
fun trainPilotFieldColors(p: TrainPilotPalette = LocalTrainPilotPalette.current): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
        disabledTextColor = p.textSecondary, errorTextColor = p.textPrimary,
        focusedContainerColor = p.surfaceCard, unfocusedContainerColor = p.surfaceCard,
        disabledContainerColor = p.surfaceCard, errorContainerColor = p.surfaceCard,
        cursorColor = p.primary, errorCursorColor = p.danger,
        focusedBorderColor = p.primary, unfocusedBorderColor = p.cardBorder,
        disabledBorderColor = p.cardBorder.copy(alpha = 0.5f), errorBorderColor = p.danger,
        focusedLabelColor = p.primary, unfocusedLabelColor = p.textSecondary,
        disabledLabelColor = p.textSecondary, errorLabelColor = p.danger,
        focusedPlaceholderColor = p.textSecondary, unfocusedPlaceholderColor = p.textSecondary,
        focusedLeadingIconColor = p.primary, unfocusedLeadingIconColor = p.textSecondary,
        focusedTrailingIconColor = p.primary, unfocusedTrailingIconColor = p.textSecondary,
        focusedSupportingTextColor = p.textSecondary, unfocusedSupportingTextColor = p.textSecondary,
        errorSupportingTextColor = p.danger,
        focusedPrefixColor = p.textSecondary, unfocusedPrefixColor = p.textSecondary,
        focusedSuffixColor = p.textSecondary, unfocusedSuffixColor = p.textSecondary
    )

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
